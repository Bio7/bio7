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
		if (BatchModel.isPause() == false) {
			BatchModel.batch(true);
		} else {
			BatchModel.setPause(false);
			BatchModel.batchStart();
		}

	}

}
