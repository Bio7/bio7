package com.eco.bio7.reditor.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import com.eco.bio7.util.Util;

public class OpenDirDialog extends Action {
	private String dir;

	public OpenDirDialog(String text, IWorkbenchWindow window) {
		super(text);

		setActionDefinitionId("com.eco.bio7.r_editor_open_directory_dialog");

	}

	public void run() {

		DirectoryDialog dlg = new DirectoryDialog(Util.getShell());

		dlg.setText("Select directory path!");

		dlg.setMessage("Select directory!");

		dir = dlg.open();
		if (dir != null) {
			dir = dir.replace("\\", "/");
			/* We print the path of the selected folder! */
			IEditorPart rEditor = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
			if (rEditor != null) {
				ITextEditor editor = (ITextEditor) rEditor;
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

	}

}