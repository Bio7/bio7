/*******************************************************************************
 * Copyright (c) 2007-2026 M. Austenfeld
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/
package com.eco.bio7.image.ui;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

import ij.swt.CodeEditor;
import ij.swt.CodeEditorProvider;
import ij.swt.CodeMarker;

/**
 * Supplies the open text editors of the workbench (R, Python, Java, ImageJ
 * macro editors, etc.) to SWTImageJ tools (see {@link ij.swt.CodeEditors}),
 * e.g., to an assistant which writes generated code into a Bio7 editor. New
 * code is saved in the folder of the active editor's file or in the workspace
 * project {@value #PROJECT_NAME} and opened with the editor registered for the
 * file extension. Markers with quick fixes are placed with
 * {@link WorkbenchCodeMarkers} and do not change the markers of the editors.
 *
 * @author M. Austenfeld
 */
public class WorkbenchCodeEditors implements CodeEditorProvider {

	/** The workspace project for new code if the active editor has no workspace file. */
	public static final String PROJECT_NAME = "LLM_Code";

	private static <T> T sync(Supplier<T> supplier) {
		AtomicReference<T> ref = new AtomicReference<T>();
		Util.getDisplay().syncExec(() -> ref.set(supplier.get()));
		return ref.get();
	}

	private static void syncRun(Runnable runnable) {
		Util.getDisplay().syncExec(runnable);
	}

	@Override
	public String getName() {
		return "Bio7";
	}

	@Override
	public List<CodeEditor> getEditors() {
		return sync(() -> {
			List<CodeEditor> list = new ArrayList<CodeEditor>();
			for (IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
				for (IWorkbenchPage page : window.getPages()) {
					for (IEditorReference ref : page.getEditorReferences()) {
						/* Editors which have not been restored yet are not listed! */
						ITextEditor editor = textEditor(ref.getEditor(false));
						if (editor != null) {
							list.add(new WorkbenchCodeEditor(editor));
						}
					}
				}
			}
			return list;
		});
	}

	@Override
	public CodeEditor getActiveEditor() {
		return sync(() -> {
			IWorkbenchPage page = Bio7ImageJ.getPage();
			ITextEditor editor = page == null ? null : textEditor(page.getActiveEditor());
			return editor == null ? null : new WorkbenchCodeEditor(editor);
		});
	}

	@Override
	public CodeEditor openEditor(String title, String text) {
		return sync(() -> {
			IWorkbenchPage page = Bio7ImageJ.getPage();
			if (page == null) {
				return null;
			}
			try {
				IFile file = newFile(page, title);
				file.create(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)), true, null);
				ITextEditor editor = textEditor(IDE.openEditor(page, file, true));
				return editor == null ? null : new WorkbenchCodeEditor(editor);
			} catch (CoreException e) {
				e.printStackTrace();
				return null;
			}
		});
	}

	private static ITextEditor textEditor(IEditorPart part) {
		if (part == null) {
			return null;
		}
		if (part instanceof ITextEditor) {
			return (ITextEditor) part;
		}
		return part.getAdapter(ITextEditor.class);
	}

	/* A not existing file in the folder of the active editor's file or in the project for new code! */
	private static IFile newFile(IWorkbenchPage page, String title) throws CoreException {
		IContainer folder = null;
		IEditorPart active = page.getActiveEditor();
		if (active != null && active.getEditorInput() instanceof IFileEditorInput) {
			folder = ((IFileEditorInput) active.getEditorInput()).getFile().getParent();
		}
		if (folder == null || !folder.isAccessible()) {
			IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(PROJECT_NAME);
			if (!project.exists()) {
				project.create(null);
			}
			if (!project.isOpen()) {
				project.open(null);
			}
			folder = project;
		}
		String name = title == null || title.isBlank() ? "Code.txt" : new File(title).getName();
		int dot = name.lastIndexOf('.');
		String base = dot > 0 ? name.substring(0, dot) : name;
		String ext = dot > 0 ? name.substring(dot) : "";
		IFile file = folder.getFile(IPath.fromOSString(name));
		for (int i = 2; file.exists(); i++) {
			file = folder.getFile(IPath.fromOSString(base + "_" + i + ext));
		}
		return file;
	}

	/** A workbench text editor. Two instances for the same editor are equal. */
	private static final class WorkbenchCodeEditor implements CodeEditor {

		private final ITextEditor editor;

		WorkbenchCodeEditor(ITextEditor editor) {
			this.editor = editor;
		}

		/* Null if the editor has been closed. Must be called in the UI thread! */
		private IDocument document() {
			IDocumentProvider provider = editor.getDocumentProvider();
			return provider == null ? null : provider.getDocument(editor.getEditorInput());
		}

		@Override
		public String getTitle() {
			return sync(() -> {
				File file = file();
				return file != null ? file.getName() : editor.getTitle();
			});
		}

		@Override
		public File getFile() {
			return sync(this::file);
		}

		/* Must be called in the UI thread! */
		private File file() {
			IEditorInput input = editor.getEditorInput();
			if (input instanceof IFileEditorInput) {
				IPath location = ((IFileEditorInput) input).getFile().getLocation();
				return location == null ? null : location.toFile();
			}
			if (input instanceof IURIEditorInput) {
				try {
					return new File(((IURIEditorInput) input).getURI());
				} catch (IllegalArgumentException e) {
					return null;
				}
			}
			return null;
		}

		@Override
		public String getText() {
			return sync(() -> {
				IDocument doc = document();
				return doc == null ? null : doc.get();
			});
		}

		@Override
		public String getSelectedText() {
			return sync(() -> {
				ISelection selection = editor.getSelectionProvider() == null ? null : editor.getSelectionProvider().getSelection();
				if (selection instanceof ITextSelection) {
					String text = ((ITextSelection) selection).getText();
					return text == null ? "" : text;
				}
				return "";
			});
		}

		/* Replaces a range so that the change can be undone in the editor! */
		private void replace(int offset, int length, String text, boolean selectEnd) {
			IDocument doc = document();
			if (doc == null) {
				return;
			}
			try {
				doc.replace(offset, length, text);
				editor.selectAndReveal(selectEnd ? offset + text.length() : offset, 0);
			} catch (BadLocationException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void setText(String text) {
			syncRun(() -> {
				IDocument doc = document();
				if (doc != null) {
					replace(0, doc.getLength(), text, false);
				}
			});
		}

		@Override
		public void insertText(String text) {
			syncRun(() -> {
				ISelection selection = editor.getSelectionProvider() == null ? null : editor.getSelectionProvider().getSelection();
				if (selection instanceof ITextSelection) {
					ITextSelection sel = (ITextSelection) selection;
					replace(sel.getOffset(), sel.getLength(), text, true);
				} else {
					IDocument doc = document();
					if (doc != null) {
						replace(doc.getLength(), 0, text, true);
					}
				}
			});
		}

		@Override
		public void appendText(String text) {
			syncRun(() -> {
				IDocument doc = document();
				if (doc != null) {
					replace(doc.getLength(), 0, text, true);
				}
			});
		}

		@Override
		public void show() {
			syncRun(() -> {
				if (editor.getSite() != null && editor.getSite().getPage() != null) {
					editor.getSite().getPage().activate(editor);
				}
			});
		}

		@Override
		public boolean isOpen() {
			return sync(() -> editor.getDocumentProvider() != null && editor.getSite() != null && editor.getSite().getPage() != null
					&& editor.getSite().getPage().findEditor(editor.getEditorInput()) != null);
		}

		@Override
		public boolean supportsMarkers() {
			return sync(() -> WorkbenchCodeMarkers.supported(editor));
		}

		@Override
		public boolean addMarker(CodeMarker marker) {
			return sync(() -> {
				WorkbenchCodeMarkers markers = WorkbenchCodeMarkers.of(editor, true);
				return markers != null && markers.add(marker);
			});
		}

		@Override
		public boolean removeMarker(CodeMarker marker) {
			return sync(() -> {
				WorkbenchCodeMarkers markers = WorkbenchCodeMarkers.of(editor, false);
				return markers != null && markers.remove(marker);
			});
		}

		@Override
		public boolean hasMarker(CodeMarker marker) {
			return sync(() -> {
				WorkbenchCodeMarkers markers = WorkbenchCodeMarkers.of(editor, false);
				return markers != null && markers.has(marker);
			});
		}

		@Override
		public List<CodeMarker> getMarkers() {
			return sync(() -> {
				WorkbenchCodeMarkers markers = WorkbenchCodeMarkers.of(editor, false);
				return markers == null ? new ArrayList<CodeMarker>() : markers.list();
			});
		}

		@Override
		public boolean applyQuickFix(CodeMarker marker) {
			return sync(() -> {
				WorkbenchCodeMarkers markers = WorkbenchCodeMarkers.of(editor, false);
				return markers != null && markers.apply(marker);
			});
		}

		@Override
		public void clearMarkers() {
			syncRun(() -> {
				WorkbenchCodeMarkers markers = WorkbenchCodeMarkers.of(editor, false);
				if (markers != null) {
					markers.clear();
				}
			});
		}

		@Override
		public boolean equals(Object o) {
			return o instanceof WorkbenchCodeEditor && ((WorkbenchCodeEditor) o).editor == editor;
		}

		@Override
		public int hashCode() {
			return System.identityHashCode(editor);
		}
	}
}
