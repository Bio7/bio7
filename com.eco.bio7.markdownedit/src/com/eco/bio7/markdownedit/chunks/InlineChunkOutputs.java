/*******************************************************************************
 * Copyright (c) 2007-2026 M. Austenfeld
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * Contributors:
 * Marcel Austenfeld - initial API and implementation
 *******************************************************************************/
package com.eco.bio7.markdownedit.chunks;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DefaultPositionUpdater;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewerExtension2;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.AnnotationPainter;
import org.eclipse.jface.text.source.IAnnotationAccess;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.inlined.AbstractInlinedAnnotation;
import org.eclipse.jface.text.source.inlined.InlinedAnnotationSupport;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * The R chunks of a markdown editor: an action bar above each chunk (Run, Run
 * Above, Clear) and the output of a run drawn below the chunk. The document is
 * not changed. One per editor.
 */
public final class InlineChunkOutputs {

	/* The opening fence of an R chunk: ```{r}, ```{r name, echo = FALSE}, ```{R} */
	private static final Pattern START = Pattern.compile("^\\s*(`{3,})\\s*\\{\\s*[rR](?:[\\s,}].*)?$");
	private static final Pattern END = Pattern.compile("^\\s*`{3,}\\s*$");
	/* The bars follow the document after a pause of typing */
	private static final int REFRESH_DELAY = 400;
	/* The positions of the closing fences of the chunks with an output (updated with the document) */
	private static final String FENCES = "bio7.chunk.fences";

	private static final Map<ITextEditor, InlineChunkOutputs> OUTPUTS = new WeakHashMap<>();

	private final ISourceViewer viewer;
	private final InlinedAnnotationSupport support = new InlinedAnnotationSupport();
	private final List<ChunkOutput> outputs = new ArrayList<>();
	private final List<ChunkHeaderAnnotation> headers = new ArrayList<>();
	private boolean refreshPending;
	/* The hand cursor is shown (over a button of a bar) */
	private boolean handCursor;
	private boolean lineRemoved;
	private boolean scrollRedrawPending;
	private final Runnable refresh = () -> {
		refreshPending = false;
		refresh();
	};

	/** A chunk: its code and the lines of its fences. */
	record Chunk(String code, int startLine, int endLine) {
	}

	private InlineChunkOutputs(ISourceViewer viewer) {
		this.viewer = viewer;
		IAnnotationAccess access = new IAnnotationAccess() {
			@Override
			@SuppressWarnings("deprecation")
			public Object getType(Annotation annotation) {
				return annotation.getType();
			}

			@Override
			@SuppressWarnings("deprecation")
			public boolean isMultiLine(Annotation annotation) {
				return true;
			}

			@Override
			@SuppressWarnings("deprecation")
			public boolean isTemporary(Annotation annotation) {
				return true;
			}
		};
		AnnotationPainter painter = new AnnotationPainter(viewer, access);
		((ITextViewerExtension2) viewer).addPainter(painter);
		support.install(viewer, painter);
		StyledText text = viewer.getTextWidget();
		text.addDisposeListener(_ -> disposeOutputs());
		/*
		 * A mouse down in a header (the bar, the output) does not move the cursor: the
		 * text widget would take a wrong line and scroll (and the button would no
		 * longer be under the mouse when it is released)
		 */
		org.eclipse.swt.widgets.Listener filter = e -> {
			if (e.widget != text || text.isDisposed()) {
				return;
			}
			org.eclipse.swt.graphics.Point p = new org.eclipse.swt.graphics.Point(e.x, e.y);
			for (ChunkHeaderAnnotation h : headers) {
				if (h.contains(p)) {
					e.type = org.eclipse.swt.SWT.None;
					e.doit = false;
					return;
				}
			}
		};
		/*
		 * After scrolling the visible area is painted once more completely: a scrolled
		 * view only paints the new strip, a part of a tall output or of the text below
		 * it could stay blank
		 */
		viewer.addViewportListener(_ -> {
			if (!headers.isEmpty() && !scrollRedrawPending) {
				scrollRedrawPending = true;
				text.getDisplay().asyncExec(() -> {
					scrollRedrawPending = false;
					if (!text.isDisposed()) {
						text.redraw();
					}
				});
			}
		});
		org.eclipse.swt.widgets.Display display = text.getDisplay();
		display.addFilter(org.eclipse.swt.SWT.MouseDown, filter);
		display.addFilter(org.eclipse.swt.SWT.MouseDoubleClick, filter);
		text.addDisposeListener(_ -> {
			display.removeFilter(org.eclipse.swt.SWT.MouseDown, filter);
			display.removeFilter(org.eclipse.swt.SWT.MouseDoubleClick, filter);
		});
		IDocumentListener listener = new IDocumentListener() {
			@Override
			public void documentAboutToBeChanged(DocumentEvent event) {
				try {
					IDocument d = event.getDocument();
					lineRemoved = event.getLength() > 0 && d.getLineOfOffset(event.getOffset()) != d.getLineOfOffset(event.getOffset() + event.getLength());
				} catch (BadLocationException e) {
					lineRemoved = true;
				}
			}

			@Override
			public void documentChanged(DocumentEvent event) {
				/* New or removed lines: at once (the outputs and bars move with their lines) */
				String t = event.getText();
				if ((t != null && (t.indexOf('\n') >= 0 || t.indexOf('\r') >= 0)) || lineRemoved) {
					lineRemoved = false;
					Display.getCurrent().asyncExec(() -> refresh());
				} else {
					scheduleRefresh();
				}
			}
		};
		IDocument doc = viewer.getDocument();
		if (doc != null) {
			doc.addDocumentListener(listener);
			if (!doc.containsPositionCategory(FENCES)) {
				doc.addPositionCategory(FENCES);
				doc.addPositionUpdater(new DefaultPositionUpdater(FENCES));
			}
		}
		text.addDisposeListener(_ -> {
			IDocument d = viewer.getDocument();
			if (d != null) {
				d.removeDocumentListener(listener);
			}
		});
	}

	/** The chunk support of the editor (the bars are shown). UI thread. */
	public static InlineChunkOutputs of(ITextEditor editor, ISourceViewer viewer) {
		InlineChunkOutputs o = OUTPUTS.get(editor);
		if (o == null) {
			o = new InlineChunkOutputs(viewer);
			OUTPUTS.put(editor, o);
			o.refresh();
		}
		return o;
	}

	/** Runs the R chunk at the cursor. UI thread. */
	public void runChunkAtCursor(ITextSelection selection) {
		Chunk chunk = chunkAt(viewer.getDocument(), selection.getStartLine());
		if (chunk == null) {
			Display.getCurrent().beep();
			return;
		}
		run(List.of(chunk));
	}

	/** Runs all R chunks of the document (in order). UI thread. */
	public void runAll() {
		List<Chunk> chunks = allChunks(viewer.getDocument());
		if (!chunks.isEmpty()) {
			run(chunks);
		}
	}

	/** Removes the output of all chunks. UI thread. */
	public void clear() {
		disposeOutputs();
		refresh();
	}

	private void scheduleRefresh() {
		if (!refreshPending) {
			refreshPending = true;
			viewer.getTextWidget().getDisplay().timerExec(REFRESH_DELAY, refresh);
		}
	}

	/* A click into a header: a button of the bar of the chunk below */
	private void clicked(MouseEvent e) {
		for (ChunkHeaderAnnotation h : headers) {
			ChunkHeaderAnnotation.BarAction action = h.buttonAt(e.x, e.y);
			if (action == null) {
				continue;
			}
			IDocument doc = viewer.getDocument();
			Position p = h.getPosition();
			if (p.isDeleted()) {
				return;
			}
			try {
				int line = doc.getLineOfOffset(p.getOffset());
				Chunk chunk = chunkAt(doc, line);
				if (chunk == null) {
					return;
				}
				switch (action) {
				case RUN -> run(List.of(chunk));
				case RUN_ABOVE -> {
					List<Chunk> above = new ArrayList<>();
					for (Chunk c : allChunks(doc)) {
						if (c.endLine() < chunk.startLine()) {
							above.add(c);
						}
					}
					if (!above.isEmpty()) {
						run(above);
					}
				}
				case CLEAR -> {
					ChunkOutput o = outputOf(doc, chunk);
					if (o != null) {
						removeOutput(o);
						refresh();
					}
				}
				}
			} catch (BadLocationException ex) {
				/* Changed meanwhile */
			}
			return;
		}
	}

	private void hover(MouseEvent e) {
		StyledText text = viewer.getTextWidget();
		boolean onButton = false;
		for (ChunkHeaderAnnotation h : headers) {
			if (h.buttonAt(e.x, e.y) != null) {
				onButton = true;
				break;
			}
		}
		/* Only on a change (setting the cursor on every move flickers) */
		if (onButton != handCursor) {
			handCursor = onButton;
			text.setCursor(onButton ? text.getDisplay().getSystemCursor(org.eclipse.swt.SWT.CURSOR_HAND) : null);
		}
	}

	private void run(List<Chunk> chunks) {
		IDocument doc = viewer.getDocument();
		/* The code of the chunks above each chunk (the hint for a missing object) */
		List<Chunk> all = allChunks(doc);
		List<String> codeAbove = new ArrayList<>();
		for (Chunk c : chunks) {
			StringBuilder above = new StringBuilder();
			for (Chunk a : all) {
				if (a.endLine() < c.startLine()) {
					above.append(a.code()).append('\n');
				}
			}
			codeAbove.add(above.toString());
		}
		List<Position> anchors = new ArrayList<>();
		for (Chunk c : chunks) {
			Position p = fence(doc, c);
			anchors.add(p);
			if (p != null) {
				ChunkOutput old = outputAt(p);
				if (old != null) {
					/* The former output stays (no jump of the height) with a label */
					old.running = true;
					viewer.getTextWidget().redraw();
				} else {
					setOutput(p, "Running...", List.of(), false);
				}
			}
		}
		refresh();
		int[] size = plotSize();
		Job job = new Job("R Markdown Chunk") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				for (int i = 0; i < chunks.size(); i++) {
					Position p = anchors.get(i);
					if (p == null || monitor.isCanceled()) {
						continue;
					}
					long start = System.nanoTime();
					RChunks.Output result = withHint(RChunks.evaluate(chunks.get(i).code(), size[0], size[1]), codeAbove.get(i));
					/* A chunk without printed output (e.g., assignments, library()): it was run */
					RChunks.Output out = result.text().isBlank() && result.plots().isEmpty() && !result.error()
							? new RChunks.Output(String.format(java.util.Locale.ROOT, "\u2713 Done (%.1f s)", (System.nanoTime() - start) / 1e9), List.of(), false)
							: result;
					Display.getDefault().asyncExec(() -> {
						if (!viewer.getTextWidget().isDisposed()) {
							setOutput(p, out.text(), out.plots(), out.error());
							refresh();
						}
					});
					if (out.error()) {
						/* Run All stops at an error */
						for (int j = i + 1; j < chunks.size(); j++) {
							Position q = anchors.get(j);
							if (q != null) {
								Display.getDefault().asyncExec(() -> {
									if (!viewer.getTextWidget().isDisposed()) {
										removeOutputAt(q);
										refresh();
									}
								});
							}
						}
						break;
					}
				}
				return Status.OK_STATUS;
			}
		};
		job.schedule();
	}

	/*
	 * An object which is not found but created in a chunk above: the hint to run
	 * the chunks above first
	 */
	private static RChunks.Output withHint(RChunks.Output out, String codeAbove) {
		if (!out.error() || out.text() == null) {
			return out;
		}
		Matcher m = Pattern.compile("(?:object|Objekt) '([^']+)' (?:not found|nicht gefunden)").matcher(out.text());
		if (!m.find()) {
			return out;
		}
		String name = m.group(1);
		String q = Pattern.quote(name);
		/* An assignment at the start of a line (not an argument name), ->, assign() or data() */
		boolean createdAbove = Pattern.compile("(?m)^\\s*" + q + "\\s*(<-|<<-|=(?!=))|->>?\\s*" + q + "(?![\\w.])|assign\\(\\s*[\"']" + q + "[\"']|data\\(\\s*[\"']?" + q
				+ "(?![\\w.])").matcher(codeAbove).find();
		if (!createdAbove) {
			return out;
		}
		String hint = "Hint: '" + name + "' is created in a chunk above: run it first (\u25B6\u25B6 Run Above).";
		return new RChunks.Output(out.text().stripTrailing() + "\n" + hint, out.plots(), true);
	}

	/* The output of a chunk replaces its former output */
	private void setOutput(Position fence, String text, List<Path> plots, boolean error) {
		if (fence.isDeleted()) {
			return;
		}
		/* The former output (the fence position stays) */
		outputs.removeIf(o -> {
			if (o.fence == fence) {
				o.dispose();
				return true;
			}
			return false;
		});
		outputs.add(new ChunkOutput(fence, viewer.getTextWidget().getDisplay(), text, plots, error));
	}

	private ChunkOutput outputAt(Position fence) {
		for (ChunkOutput o : outputs) {
			if (o.fence == fence) {
				return o;
			}
		}
		return null;
	}

	private void removeOutputAt(Position fence) {
		for (ChunkOutput o : new ArrayList<>(outputs)) {
			if (o.fence == fence) {
				removeOutput(o);
			}
		}
	}

	private void removeOutput(ChunkOutput o) {
		outputs.remove(o);
		o.dispose();
		/* The fence is no longer tracked if no output uses it */
		if (outputs.stream().noneMatch(x -> x.fence == o.fence)) {
			try {
				viewer.getDocument().removePosition(FENCES, o.fence);
			} catch (Exception e) {
				/* Already removed */
			}
		}
	}

	/*
	 * The headers of the lines: the output of a chunk (above the line after its
	 * closing fence) and the bar of a chunk (above its opening fence), one header
	 * if both are above the same line
	 */
	private void refresh() {
		StyledText text = viewer.getTextWidget();
		IDocument doc = viewer.getDocument();
		if (text == null || text.isDisposed() || doc == null) {
			return;
		}
		/* An output whose chunk (its closing fence) was removed */
		for (ChunkOutput o : new ArrayList<>(outputs)) {
			if (o.fence.isDeleted()) {
				removeOutput(o);
			}
		}
		TreeMap<Integer, Object[]> lines = new TreeMap<>();
		try {
			for (ChunkOutput o : outputs) {
				/* Drawn above the line after the closing fence */
				int line = doc.getLineOfOffset(o.fence.getOffset()) + 1;
				if (line < doc.getNumberOfLines()) {
					lines.computeIfAbsent(line, _ -> new Object[2])[0] = o;
				}
			}
			for (Chunk c : allChunks(doc)) {
				lines.computeIfAbsent(c.startLine(), _ -> new Object[2])[1] = Boolean.TRUE;
			}
		} catch (BadLocationException e) {
			return;
		}
		/*
		 * Unchanged (e.g., typing within a line): the annotations stay. Compared with
		 * the lines where the headers are now (a new line at the start of a line moves
		 * its header down)
		 */
		StringBuilder sig = new StringBuilder();
		for (var e : lines.entrySet()) {
			sig.append(e.getKey()).append(':').append(System.identityHashCode(e.getValue()[0])).append(e.getValue()[1] != null).append(';');
		}
		StringBuilder now = new StringBuilder();
		try {
			for (ChunkHeaderAnnotation h : headers) {
				Position p = h.getPosition();
				if (p.isDeleted()) {
					now.append("deleted;");
					continue;
				}
				now.append(doc.getLineOfOffset(p.getOffset())).append(':').append(System.identityHashCode(h.output())).append(h.hasBar()).append(';');
			}
		} catch (BadLocationException e) {
			now.append("invalid");
		}
		if (sig.toString().equals(now.toString())) {
			return;
		}
		/*
		 * The headers which stay in their line are updated in place; only new and
		 * removed headers change the annotations of the editor (else the lines lose
		 * their space for a moment: the editor flashes)
		 */
		Map<Integer, ChunkHeaderAnnotation> existing = new java.util.HashMap<>();
		for (ChunkHeaderAnnotation h : headers) {
			Position p = h.getPosition();
			if (!p.isDeleted()) {
				try {
					existing.putIfAbsent(doc.getLineOfOffset(p.getOffset()), h);
				} catch (BadLocationException e) {
					/* Removed below */
				}
			}
		}
		List<ChunkHeaderAnnotation> next = new ArrayList<>();
		boolean changed = false;
		try {
			for (var e : lines.entrySet()) {
				ChunkOutput o = (ChunkOutput) e.getValue()[0];
				boolean bar = e.getValue()[1] != null;
				ChunkHeaderAnnotation h = existing.remove(e.getKey());
				if (h != null) {
					h.update(o, bar);
					next.add(h);
					continue;
				}
				IRegion r = doc.getLineInformation(e.getKey());
				/* Not empty (the painter skips empty positions) */
				Position p = new Position(r.getOffset(), Math.min(1, doc.getLength() - r.getOffset()));
				next.add(new ChunkHeaderAnnotation(p, viewer, o, bar, this::clicked, this::hover, _ -> {
					if (handCursor) {
						handCursor = false;
						text.setCursor(null);
					}
				}));
				changed = true;
			}
		} catch (BadLocationException e) {
			return;
		}
		changed |= !existing.isEmpty();
		headers.clear();
		headers.addAll(next);
		if (!changed) {
			/* The heights may have changed: set at once (the first visible line stays) */
			int top = text.getTopIndex();
			applyHeights(doc, text);
			text.setTopIndex(top);
			redrawAll(text);
			return;
		}
		/* The first visible text line stays at the top (the heights above it change) */
		int top = text.getTopIndex();
		support.updateAnnotations(new HashSet<AbstractInlinedAnnotation>(headers));
		resetFreedLines(doc, text);
		applyHeights(doc, text);
		/* The new heights at once (not only after scrolling) */
		text.setTopIndex(top);
		redrawAll(text);
	}

	/*
	 * Repaints the editor with its rulers (the line numbers): the heights of lines
	 * changed. Again a moment later, after the editor has painted the headers (else
	 * parts of the text or the line numbers may stay blank until scrolling)
	 */
	private void redrawAll(StyledText text) {
		org.eclipse.swt.widgets.Composite parent = text.getParent();
		if (parent == null || parent.isDisposed()) {
			return;
		}
		org.eclipse.swt.graphics.Rectangle r = parent.getClientArea();
		parent.redraw(r.x, r.y, r.width, r.height, true);
		parent.update();
		text.getDisplay().timerExec(80, () -> {
			if (!parent.isDisposed()) {
				org.eclipse.swt.graphics.Rectangle r2 = parent.getClientArea();
				parent.redraw(r2.x, r2.y, r2.width, r2.height, true);
			}
		});
	}

	/* The space of each header (the editor would set it only while painting) */
	private void applyHeights(IDocument doc, StyledText text) {
		for (ChunkHeaderAnnotation h : headers) {
			Position p = h.getPosition();
			if (p.isDeleted()) {
				continue;
			}
			try {
				int line = doc.getLineOfOffset(p.getOffset());
				if (viewer instanceof org.eclipse.jface.text.ITextViewerExtension5 ext) {
					line = ext.modelLine2WidgetLine(line);
				}
				int height = h.getHeight();
				if (line >= 0 && line < text.getLineCount() && text.getLineVerticalIndent(line) != height) {
					text.setLineVerticalIndent(line, height);
				}
			} catch (BadLocationException e) {
				/* Changed meanwhile */
			}
		}
	}

	/*
	 * A removed header leaves the space of its line (the vertical indent is not
	 * reset): every line without a header gets its normal height back
	 */
	private void resetFreedLines(IDocument doc, StyledText text) {
		java.util.Set<Integer> keep = new HashSet<>();
		for (ChunkHeaderAnnotation h : headers) {
			Position p = h.getPosition();
			if (p.isDeleted()) {
				continue;
			}
			try {
				int line = doc.getLineOfOffset(p.getOffset());
				if (viewer instanceof org.eclipse.jface.text.ITextViewerExtension5 ext) {
					line = ext.modelLine2WidgetLine(line);
				}
				keep.add(line);
			} catch (BadLocationException e) {
				/* Changed meanwhile */
			}
		}
		int count = text.getLineCount();
		for (int i = 0; i < count; i++) {
			if (text.getLineVerticalIndent(i) != 0 && !keep.contains(i)) {
				text.setLineVerticalIndent(i, 0);
			}
		}
	}

	private void disposeOutputs() {
		for (ChunkOutput o : new ArrayList<>(outputs)) {
			removeOutput(o);
		}
	}

	/* The size of a plot (points): the width of the editor, at most 720 */
	private int[] plotSize() {
		int w = viewer.getTextWidget().getClientArea().width - viewer.getTextWidget().getLeftMargin() - 40;
		w = Math.max(320, Math.min(720, w));
		return new int[] { w, (int) Math.round(w * 0.62) };
	}

	/* The output of a chunk if it has one (its closing fence is the line of the chunk) */
	private ChunkOutput outputOf(IDocument doc, Chunk c) throws BadLocationException {
		for (ChunkOutput o : outputs) {
			if (!o.fence.isDeleted() && doc.getLineOfOffset(o.fence.getOffset()) == c.endLine()) {
				return o;
			}
		}
		return null;
	}

	/*
	 * The position of the closing fence of a chunk (the output is drawn above the
	 * next line: a new line at the end of the document if the fence is the last
	 * line)
	 */
	private Position fence(IDocument doc, Chunk c) {
		try {
			ChunkOutput existing = outputOf(doc, c);
			if (existing != null) {
				return existing.fence;
			}
			if (c.endLine() + 1 >= doc.getNumberOfLines()) {
				doc.replace(doc.getLength(), 0, System.lineSeparator());
			}
			IRegion r = doc.getLineInformation(c.endLine());
			Position p = new Position(r.getOffset(), r.getLength());
			doc.addPosition(FENCES, p);
			return p;
		} catch (BadLocationException | org.eclipse.jface.text.BadPositionCategoryException e) {
			return null;
		}
	}

	/* The R chunk which contains the line (also its fences) */
	static Chunk chunkAt(IDocument doc, int line) {
		for (Chunk c : allChunks(doc)) {
			if (line >= c.startLine() && line <= c.endLine()) {
				return c;
			}
		}
		return null;
	}

	static List<Chunk> allChunks(IDocument doc) {
		List<Chunk> chunks = new ArrayList<>();
		int n = doc.getNumberOfLines();
		try {
			for (int i = 0; i < n; i++) {
				String text = line(doc, i);
				if (START.matcher(text).matches()) {
					StringBuilder code = new StringBuilder();
					int j = i + 1;
					for (; j < n; j++) {
						String l = line(doc, j);
						if (END.matcher(l).matches()) {
							break;
						}
						code.append(l).append('\n');
					}
					if (j >= n) {
						/* Not closed */
						break;
					}
					chunks.add(new Chunk(code.toString(), i, j));
					i = j;
				} else if (text.trim().startsWith("```") && !END.matcher(text).matches()) {
					/* Another fenced block (not R): skipped to its end */
					for (int j = i + 1; j < n; j++) {
						if (END.matcher(line(doc, j)).matches()) {
							i = j;
							break;
						}
					}
				}
			}
		} catch (BadLocationException e) {
			/* Changed meanwhile */
		}
		return chunks;
	}

	private static String line(IDocument doc, int i) throws BadLocationException {
		IRegion r = doc.getLineInformation(i);
		return doc.get(r.getOffset(), r.getLength());
	}
}
