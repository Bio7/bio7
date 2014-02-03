package com.eco.bio7.reditor.refactor;

import org.eclipse.compare.CompareUI;
import org.eclipse.jface.action.Action;
import org.eclipse.ui.IWorkbenchWindow;

public class CompareEditorAction extends Action {

	private IWorkbenchWindow window;

	public CompareEditorAction(String text, IWorkbenchWindow window) {
		super(text);
		this.window = window;

	}

	public void run() {

		CompareUI.openCompareEditor(new CompareInput());
	}

}