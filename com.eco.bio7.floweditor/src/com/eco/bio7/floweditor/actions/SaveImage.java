package com.eco.bio7.floweditor.actions;

import com.eco.bio7.floweditor.shapes.ImageSaveUtil;
import com.eco.bio7.floweditor.shapes.ShapesEditor;
import org.eclipse.jface.action.Action;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

public class SaveImage extends Action {

	public SaveImage(String text, IWorkbenchWindow window) {
		super(text);

		setId("Image");

	}

	public void run() {

		IEditorPart editor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();

		ImageSaveUtil.save(editor, ((ShapesEditor) editor).getviewer());

	}

}
