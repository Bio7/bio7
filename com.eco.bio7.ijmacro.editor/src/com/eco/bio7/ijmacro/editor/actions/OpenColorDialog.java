package com.eco.bio7.ijmacro.editor.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import com.eco.bio7.image.Util;

public class OpenColorDialog extends AbstractHandler {

	private boolean hex;

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		

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
			if (hex == false) {
				buff.append(Integer.toString(rgb.red) + ",");
				buff.append(Integer.toString(rgb.green) + ",");
				buff.append(Integer.toString(rgb.blue));
			} else {
				String hex = String.format("#%02x%02x%02x", rgb.red, rgb.green, rgb.blue);
				buff.append(hex);
			}
			try {

				// int off = doc.getLineOffset(startline);
				doc.replace(selection.getOffset(), selection.getLength(), buff.toString());

			} catch (BadLocationException e) {

				e.printStackTrace();
			}
		}
		return null;
	}

}