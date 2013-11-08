package com.eco.bio7.floweditor.shapes;

import com.eco.bio7.floweditor.model.Shape;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.commands.Command;

public class ChangeConstraintCommand extends Command {
	private Shape shapemodel;
	private Rectangle constraint;
	private Rectangle oldConstraint;

	public void execute() {

		shapemodel.setConstraint(constraint);
	}

	public void setConstraint(Rectangle rect) {
		constraint = rect;
	}

	public void setModel(Object model) {
		shapemodel = (Shape) model;

		oldConstraint = shapemodel.getConstraint();
	}

	public void undo() {
		shapemodel.setConstraint(oldConstraint);
	}

}
