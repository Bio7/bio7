package com.eco.bio7.reditor.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

import com.eco.bio7.util.Util;

public class OpenColorDialog extends Action {

	private final IWorkbenchWindow window;

	public OpenColorDialog(String text, IWorkbenchWindow window) {
		super(text);
		this.window = window;
		setActionDefinitionId("com.eco.bio7.r_editor_open_color_dialog");

	}

	public void run() {

		int offset = 0;

		IEditorPart editore = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();

		ITextEditor editor = (ITextEditor) editore;
		IDocumentProvider dp = editor.getDocumentProvider();
		IDocument doc = dp.getDocument(editor.getEditorInput());

		ISelectionProvider sp = editor.getSelectionProvider();
		ISelection selectionsel = sp.getSelection();
		ITextSelection selection = (ITextSelection) selectionsel;

		String selText = selection.getText();
		String[] colStr = selText.split(",");
		ColorDialog dlg = new ColorDialog(Util.getShell());
		RGB rgbCol = null;
		try {
			rgbCol = new RGB(Integer.parseInt(colStr[0]), Integer.parseInt(colStr[1]), Integer.parseInt(colStr[2]));
		} catch (NumberFormatException e1) {
			// TODO Auto-generated catch block
			// e1.printStackTrace();
		}
		if (rgbCol != null) {
			dlg.setRGB(rgbCol);
		}

		dlg.setText("Choose a Color");

		RGB rgb = dlg.open();
		if (rgb != null) {
			StringBuffer buff = new StringBuffer();
			buff.append(Integer.toString(rgb.red) + ",");
			buff.append(Integer.toString(rgb.green) + ",");
			buff.append(Integer.toString(rgb.blue));
			try {

				// int off = doc.getLineOffset(startline);
				doc.replace(selection.getOffset(), selection.getLength(), buff.toString());

			} catch (BadLocationException e) {

				e.printStackTrace();
			}
		}

	}

}