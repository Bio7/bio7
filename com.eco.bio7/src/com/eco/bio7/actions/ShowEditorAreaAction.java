package com.eco.bio7.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;

public class ShowEditorAreaAction extends Action {

	private final IWorkbenchWindow window;

	public ShowEditorAreaAction(String text, IWorkbenchWindow window) {
		super(text);
		this.window = window;
		setId("com.eco.bio7.show_editor");

	}

	public void run() {
		IWorkbenchWindow window = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow();
		ActionFactory.IWorkbenchAction maximize = ActionFactory.SHOW_EDITOR
				.create(window);
		maximize.run();

	}

}