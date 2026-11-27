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
		/* A flow is running (a step is executed): stop it and start again? */
		if (BatchModel.isRunning()) {
			boolean restart = org.eclipse.jface.dialogs.MessageDialog.openQuestion(org.eclipse.swt.widgets.Display.getDefault().getActiveShell(), "Flow",
					"A flow is running.\nStop it and start the flow again?\n\n(Stop a flow with the stop action of the toolbar or Ctrl+Alt+Shift+X.)");
			if (!restart) {
				return;
			}
			com.eco.bio7.actions.Bio7Action.stopFlow();
		}
		BatchModel.batch(false);

	}

}
