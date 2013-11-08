package com.eco.bio7.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import com.eco.bio7.batch.BatchModel;

public class FlowEditorTestAction implements IEditorActionDelegate {

	public void dispose() {

	}

	public void init(IWorkbenchWindow window) {

	}

	public void run(IAction action) {
		if (BatchModel.isPause() == false) {
			BatchModel.batch(true);
		} else {
			BatchModel.setPause(false);
			BatchModel.batchStart();
		}

	}

	public void selectionChanged(IAction action, ISelection selection) {

	}

	public void setActiveEditor(IAction action, IEditorPart targetEditor) {

	}

}
