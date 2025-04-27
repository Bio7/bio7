package com.eco.bio7.ijmacro.editor.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import com.eco.bio7.image.Util;

public class OpenDirDialog extends AbstractHandler {
	private String dir;

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		DirectoryDialog dlg = new DirectoryDialog(Util.getShell());

		dlg.setText("Select directory path!");

		dlg.setMessage("Select directory!");

		dir = dlg.open();
		if (dir != null) {
			dir = dir.replace("\\", "/");
			/* We print the path of the selected folder! */
			IEditorPart ijMacroRditor = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
			if (ijMacroRditor != null) {
				ITextEditor editor = (ITextEditor) ijMacroRditor;
				IDocumentProvider dp = editor.getDocumentProvider();
				IDocument doc = dp.getDocument(editor.getEditorInput());

				ISelectionProvider sp = editor.getSelectionProvider();

				ISelection selectionsel = sp.getSelection();

				ITextSelection selection = (ITextSelection) selectionsel;
				int offset = selection.getOffset();
				try {
					doc.replace(offset, 0, dir);
				} catch (BadLocationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		}

		return null;
	}

}