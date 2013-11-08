package com.eco.bio7.floweditor.actions;

import com.eco.bio7.floweditor.shapes.ShapesEditor;
import org.eclipse.jface.action.Action;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

public class DebugMode extends Action {

	public DebugMode(String text, IWorkbenchWindow window) {
		super(text, AS_CHECK_BOX);

		setId("DebugMode");
		

	}

	public void run() {
		ShapesEditor shapeeditor = get_editor();

		if (shapeeditor.debug) {
			shapeeditor.debug = false;
			this.setChecked(false);
			//System.out.println(shapeeditor.debug);

		} else {

			shapeeditor.debug = true;
			this.setChecked(true);
			//System.out.println(shapeeditor.debug);

		}

	}

	private ShapesEditor get_editor() {
		IEditorPart editor = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		ShapesEditor shapeeditor = (ShapesEditor) editor;
		return shapeeditor;
	}

}