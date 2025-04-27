package com.eco.bio7.ijmacro.editor.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import com.eco.bio7.image.Util;

public class OpenColorDialogRGBToHex extends AbstractHandler {

	public Color hex2Rgb(Display dis, String colorStr) {
		return new Color(dis, Integer.valueOf(colorStr.substring(1, 3), 16), Integer.valueOf(colorStr.substring(3, 5), 16), Integer.valueOf(colorStr.substring(5, 7), 16));
	}

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

		ColorDialog dlg = new ColorDialog(Util.getShell());

		if (selection.isEmpty() == false) {
			/* Convert selection to RGB if possible! */
			Color col = null;
			try {
				col = hex2Rgb(Util.getShell().getDisplay(), selText);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				// e.printStackTrace();
			}

			if (col != null) {
				dlg.setRGB(col.getRGB());
			}

		}
		dlg.setText("Choose a Color");

		RGB rgb = dlg.open();
		if (rgb != null) {

			StringBuffer buff = new StringBuffer();
			/* Convert to hexadecimal! */
			String hex = String.format("#%02x%02x%02x", rgb.red, rgb.green, rgb.blue);
			buff.append(hex);

			try {
				doc.replace(selection.getOffset(), selection.getLength(), buff.toString());

			} catch (BadLocationException e) {

				e.printStackTrace();
			}
		}
		return null;
	}

}