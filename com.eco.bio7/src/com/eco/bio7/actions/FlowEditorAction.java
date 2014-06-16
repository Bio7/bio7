package com.eco.bio7.actions;

import org.eclipse.jface.action.Action;
import com.eco.bio7.batch.BatchModel;

public class FlowEditorAction extends Action {

	public FlowEditorAction(String text){
		super (text);
		setId("com.eco.bio7.executeflow");
		setActionDefinitionId("com.eco.bio7.executeflowaction");
	}

	public void run() {
		if (BatchModel.isPause() == false) {
			BatchModel.batch(false);
		} else {
			BatchModel.setPause(false);
			BatchModel.batchStart();
		}

	}

}
