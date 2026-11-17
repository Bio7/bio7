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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.ui.texteditor.ITextEditor;

import com.eco.bio7.pythonedit.analysis.PythonReconcilingStrategy;

/**
 * The code analysis of the Python editor for the Python chunks of a markdown
 * editor (as {@link RChunkAnalysis} for the R chunks): the errors and warnings
 * of the compiler, ruff or pyflakes and the modules which are not installed as
 * markers of the file, after a pause in typing. The analysis runs in a
 * background job (the analysis process of the Python editor).
 */
public final class PythonChunkAnalysis {

	/** The marker type of the Python problems of markdown documents (a problem marker). */
	public static final String MARKER = "com.eco.bio7.markdownedit.pythonproblem";
	private static final int DELAY = 600;
	/*
	 * Not for chunks: an import in a later chunk is not at the top of the module
	 * (E402), the imports of a chunk are followed by its code (I001: the import
	 * block isn't formatted), and the last expression of a chunk is its output (B018)
	 */
	private static final Set<String> WITHOUT = Set.of("E402", "I001", "B018");

	private final ISourceViewer viewer;
	private final IResource resource;
	private final AtomicLong generation = new AtomicLong();
	private boolean pending;
	private final Runnable analyze = () -> {
		pending = false;
		analyze();
	};

	private PythonChunkAnalysis(ITextEditor editor, ISourceViewer viewer) {
		this.viewer = viewer;
		this.resource = editor.getEditorInput().getAdapter(IResource.class);
		IDocument doc = viewer.getDocument();
		IDocumentListener listener = new IDocumentListener() {
			@Override
			public void documentAboutToBeChanged(DocumentEvent event) {
			}

			@Override
			public void documentChanged(DocumentEvent event) {
				schedule();
			}
		};
		doc.addDocumentListener(listener);
		viewer.getTextWidget().addDisposeListener(_ -> doc.removeDocumentListener(listener));
	}

	/**
	 * Starts the analysis of the Python chunks of the editor (a file of the
	 * workspace). UI thread. Without the Python editor nothing happens.
	 */
	public static void install(ITextEditor editor, ISourceViewer viewer) {
		if (viewer == null || viewer.getDocument() == null || editor.getEditorInput().getAdapter(IResource.class) == null) {
			return;
		}
		try {
			PythonChunkAnalysis a = new PythonChunkAnalysis(editor, viewer);
			a.analyze();
		} catch (LinkageError e) {
			/* The Python editor is not installed */
		}
	}

	private void schedule() {
		StyledText text = viewer.getTextWidget();
		if (!pending && text != null && !text.isDisposed()) {
			pending = true;
			text.getDisplay().timerExec(DELAY, analyze);
		}
	}

	/* The Python code of the chunks (UI thread), the analysis in a job, then the markers */
	private void analyze() {
		IDocument doc = viewer.getDocument();
		StyledText text = viewer.getTextWidget();
		if (doc == null || text == null || text.isDisposed()) {
			return;
		}
		boolean chunks = PythonChunkCode.hasPythonChunks(doc);
		String code = chunks ? PythonChunkCode.virtualCode(doc) : "";
		/* The offsets of the markers: a copy of the document (lines and columns as the code) */
		IDocument copy = new Document(doc.get());
		String path = PythonChunkCode.analysisPath(resource);
		long gen = generation.incrementAndGet();
		Job job = new Job("Python Chunk Analysis") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				if (gen != generation.get()) {
					return Status.OK_STATUS;
				}
				List<Map<String, Object>> markers = List.of();
				if (chunks) {
					Map<String, Object> result = PythonReconcilingStrategy.analyze(code, path);
					if (result == null) {
						/* No Python for the analysis: the markers stay as they are */
						return Status.OK_STATUS;
					}
					markers = PythonReconcilingStrategy.markers(copy, result, WITHOUT);
				}
				if (gen == generation.get()) {
					setMarkers(markers, gen);
				}
				return Status.OK_STATUS;
			}
		};
		job.setSystem(true);
		job.schedule();
	}

	private void setMarkers(List<Map<String, Object>> markers, long gen) {
		WorkspaceJob job = new WorkspaceJob("Python chunk markers") {
			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
				if (!resource.exists() || gen != generation.get()) {
					return Status.OK_STATUS;
				}
				resource.deleteMarkers(MARKER, false, IResource.DEPTH_ZERO);
				for (Map<String, Object> m : markers) {
					m.remove(com.eco.bio7.pythonedit.analysis.PythonFixes.FIXES);
					m.remove("bio7.code");
					resource.createMarker(MARKER, m);
				}
				return Status.OK_STATUS;
			}
		};
		job.setRule(resource);
		job.setSystem(true);
		job.schedule();
	}
}
