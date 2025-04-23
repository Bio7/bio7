package com.eco.bio7.reditor.actions.color;

import org.eclipse.jface.action.Action;
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
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import com.eco.bio7.util.Util;

public class OpenColorDialogRGBToHex extends Action {

	private final IWorkbenchWindow window;
	

	public OpenColorDialogRGBToHex(String text, IWorkbenchWindow window) {
		super(text);
		this.window = window;
		setActionDefinitionId("com.eco.bio7.r_editor_open_color_rgbtohex_dialog");

	}

	public void run() {

		IEditorPart editore = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
				.getActiveEditor();

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

	}

	public Color hex2Rgb(Display dis, String colorStr) {
		return new Color(dis, Integer.valueOf(colorStr.substring(1, 3), 16),
				Integer.valueOf(colorStr.substring(3, 5), 16), Integer.valueOf(colorStr.substring(5, 7), 16));
	}

}