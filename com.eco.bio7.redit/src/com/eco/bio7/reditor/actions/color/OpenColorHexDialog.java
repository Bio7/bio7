package com.eco.bio7.reditor.actions.color;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import com.eco.bio7.util.Util;
/*For this class only a command is defined. It is used in the RColorInformationControl hover!*/
public class OpenColorHexDialog extends Action {

	private final IWorkbenchWindow window;

	private int offset;

	private int theLength;

	private boolean nameDefined = false;
	/*String[] customColorPalette = new String[] { "#FFFFFF", "#000000", "#A52A2A", "#0000FF", "#A52A2A", "#006400",
			"#FF0000" + "#FFFF00", "#A9A9A9", "#228B22", "#7A7A7A" };*/

	public OpenColorHexDialog(String text, IWorkbenchWindow window, int offset, int theLength) {
		super(text);
		this.window = window;
		this.offset = offset;
		this.theLength = theLength;
		setActionDefinitionId("com.eco.bio7.r_editor_open_color_hex_dialog");

	}

	public void run() {

		IEditorPart editore = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
				.getActiveEditor();

		ITextEditor editor = (ITextEditor) editore;
		IDocumentProvider dp = editor.getDocumentProvider();
		IDocument doc = dp.getDocument(editor.getEditorInput());
		ColorDialog dlg = new ColorDialog(Util.getShell());
		dlg.setText("Choose a Color");
		/* Set some custom defined colors! */
		/*RGB[] rgbValues = new RGB[customColorPalette.length];
		for (int i = 0; i < rgbValues.length; i++) {
			Color co = hex2Rgb(Util.getShell().getDisplay(), customColorPalette[i]);
			rgbValues[i] = new RGB(co.getRed(), co.getGreen(), co.getBlue());
		}
		dlg.setRGBs(rgbValues);*/

		RGB rgb = dlg.open();

		if (rgb != null) {

			String hex = String.format("#%02x%02x%02x", rgb.red, rgb.green, rgb.blue);

			/*for (int i = 0; i < RColors.hexValues.length; i++) {
				//
				String cols = RColors.hexValues[i].toLowerCase();

				if (cols.equals(hex)) {

					nameDefined = true;
					try {

						// int off = doc.getLineOffset(startline);
						doc.replace(offset, theLength, RColors.colorNames[i]);

					} catch (BadLocationException e) {

						e.printStackTrace();
					}
					break;
				}
			}*/
			//if (nameDefined == false) {
				try {
					StringBuffer buff = new StringBuffer();
					buff.append(hex);
					doc.replace(offset, theLength, buff.toString());

				} catch (BadLocationException e) {

					e.printStackTrace();
				}
			//}
		}

	}

	/*public Color hex2Rgb(Display dis, String colorStr) {
		return new Color(dis, Integer.valueOf(colorStr.substring(1, 3), 16),
				Integer.valueOf(colorStr.substring(3, 5), 16), Integer.valueOf(colorStr.substring(5, 7), 16));
	}*/

}