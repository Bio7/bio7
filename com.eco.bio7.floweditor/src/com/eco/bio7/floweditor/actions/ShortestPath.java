package com.eco.bio7.floweditor.actions;

import com.eco.bio7.floweditor.shapes.ShapesEditor;
import org.eclipse.jface.action.Action;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

public class ShortestPath extends Action {

	public ShortestPath(String text, IWorkbenchWindow window) {
		super(text, AS_RADIO_BUTTON);

		setId("ShortestPath");
		setChecked(true);

	}

	public void run() {

		IEditorPart editor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();

		ShapesEditor shapes = (ShapesEditor) editor;
		shapes.diagram.setConnectionRouter(2);

	}

}