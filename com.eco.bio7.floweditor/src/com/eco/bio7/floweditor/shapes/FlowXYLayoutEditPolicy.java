package com.eco.bio7.floweditor.shapes;

import com.eco.bio7.floweditor.model.Shape;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editpolicies.XYLayoutEditPolicy;
import org.eclipse.gef.requests.CreateRequest;

public class FlowXYLayoutEditPolicy extends XYLayoutEditPolicy {

	protected Command createAddCommand(EditPart child, Object constraint) {

		return null;
	}

	protected Command createChangeConstraintCommand(EditPart child,
			Object constraint) {

		ChangeConstraintCommand command = new ChangeConstraintCommand();

		command.setModel(child.getModel());
		command.setConstraint((Rectangle) constraint);

		return command;

	}

	protected Command getCreateCommand(CreateRequest request) {
		CreateCommand command = new CreateCommand();

		Rectangle constraint = (Rectangle) getConstraintFor(request);

		Shape model = (Shape) request.getNewObject();

		model.setConstraint(constraint);

		command.setContentsModel(getHost().getModel());
		command.setFlowModel(model);
		return command;
	}

	protected Command getDeleteDependantCommand(Request request) {

		return null;
	}

}
