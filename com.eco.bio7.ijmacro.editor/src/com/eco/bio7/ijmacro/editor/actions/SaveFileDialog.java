package com.eco.bio7.ijmacro.editor.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import com.eco.bio7.image.Util;

public class SaveFileDialog extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IEditorPart editore = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();

		ITextEditor editor = (ITextEditor) editore;
		IDocumentProvider dp = editor.getDocumentProvider();
		IDocument doc = dp.getDocument(editor.getEditorInput());

		ISelectionProvider sp = editor.getSelectionProvider();
		ISelection selectionsel = sp.getSelection();
		ITextSelection selection = (ITextSelection) selectionsel;
		int offset = selection.getOffset();
		int length = selection.getLength();

		FileDialog fd = new FileDialog(Util.getShell(), SWT.SAVE);
		fd.setText("Save");

		/* For multiple extensions for one filetype, semicolon can be used! */
		String[] filterExt = { "*.*", "*.tif", "*.jpg", "*.csv", "*.png", "*.xls" };
		fd.setFilterExtensions(filterExt);
		String selected = fd.open();
		if (selected != null) {
			selected = selected.replace("\\", "/");
			try {
				doc.replace(offset, length, selected);
			} catch (BadLocationException e) {

				e.printStackTrace();
			}
		}

		return null;
	}

}