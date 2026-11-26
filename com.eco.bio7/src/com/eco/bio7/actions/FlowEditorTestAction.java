package com.eco.bio7.actions;

import org.eclipse.jface.action.Action;
import com.eco.bio7.batch.BatchModel;

public class FlowEditorTestAction extends Action {

	public FlowEditorTestAction(String text) {
		super(text);
		setId("com.eco.bio7.debugflow");
		setActionDefinitionId("com.eco.bio7.debugflowaction");
	}

	public void run() {
		/* A step of a running flow is executed: the flow continues when it has finished */
		if (BatchModel.isRunning()) {
			com.eco.bio7.batch.Bio7Dialog.message("A flow is running.\nPlease stop it to start a new flow.");
			return;
		}
		BatchModel.batch(true);

	}

}
