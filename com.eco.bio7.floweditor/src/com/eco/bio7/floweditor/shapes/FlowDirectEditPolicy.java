package com.eco.bio7.floweditor.shapes;


import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editpolicies.DirectEditPolicy;
import org.eclipse.gef.requests.DirectEditRequest;

public class FlowDirectEditPolicy extends DirectEditPolicy {

	
	protected Command getDirectEditCommand(DirectEditRequest request) {
		DirectEditCommand command = new DirectEditCommand();
		command.setModel(getHost().getModel());		
		command.setText((String) request.getCellEditor().getValue());
		return command;
	}

	
	protected void showCurrentEditValue(DirectEditRequest request) {
	}
}
