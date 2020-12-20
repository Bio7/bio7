package com.eco.bio7.floweditor.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import com.eco.bio7.floweditor.shapes.ShapesEditor;

public class SetGridSize extends Action {

	public SetGridSize(String text, IWorkbenchWindow window) {
		super(text, AS_PUSH_BUTTON);

		setId("SetGridSize");
		

	}

	public void run() {
		

		Display display = PlatformUI.getWorkbench().getDisplay();
		GridSettings st=new GridSettings(new Shell(display),SWT.NORMAL);
        st.open();
       
	}

	private ShapesEditor getEditor() {
		IEditorPart editor = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		ShapesEditor shapeeditor = (ShapesEditor) editor;
		return shapeeditor;
	}

}