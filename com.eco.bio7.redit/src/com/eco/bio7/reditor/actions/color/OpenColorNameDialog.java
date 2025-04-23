package com.eco.bio7.reditor.actions.color;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import com.eco.bio7.util.Util;

/*For this class only a command is defined. It is used in the RColorInformationControl hover!*/
public class OpenColorNameDialog extends Action {

	private final IWorkbenchWindow window;

	private int offset;

	private int theLength;

	private boolean nameDefined = false;

	private String colorName;

	private String selColorName;

	public OpenColorNameDialog(String text, IWorkbenchWindow window, int offset, int theLength, String selColorName) {
		super(text);
		this.window = window;
		this.offset = offset;
		this.theLength = theLength;
		this.selColorName=selColorName;
		setActionDefinitionId("com.eco.bio7.r_editor_open_color_name_dialog");

	}

	public void run() {

		IEditorPart editore = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
				.getActiveEditor();

		ITextEditor editor = (ITextEditor) editore;
		IDocumentProvider dp = editor.getDocumentProvider();
		IDocument doc = dp.getDocument(editor.getEditorInput());
		ColorDialogName ld = new ColorDialogName(Util.getShell(),selColorName);
        
		if (Window.OK == ld.open()) {
			colorName = ld.getColorByName();

		}
		if (colorName != null) {
			try {
				doc.replace(offset, theLength, colorName);
			} catch (BadLocationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

}