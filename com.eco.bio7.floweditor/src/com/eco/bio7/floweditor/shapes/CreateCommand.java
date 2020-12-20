package com.eco.bio7.floweditor.shapes;


import com.eco.bio7.floweditor.model.Shape;
import org.eclipse.gef.commands.Command;


public class CreateCommand extends Command {
	private ContentsModel contentsModel;
	private Shape shapemodel;

	
	public void execute() {
		contentsModel.addChild(shapemodel);
	}

	public void setContentsModel(Object model) {
		contentsModel = (ContentsModel) model;
	}

	public void setFlowModel(Object model) {
		shapemodel = (Shape) model;
	}

	
	public void undo() {
		contentsModel.removeChild(shapemodel);
	}
}
