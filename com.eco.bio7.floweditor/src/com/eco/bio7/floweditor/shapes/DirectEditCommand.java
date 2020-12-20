package com.eco.bio7.floweditor.shapes;

import com.eco.bio7.floweditor.model.EllipticalShape;
import com.eco.bio7.floweditor.model.Shape;
import org.eclipse.gef.commands.Command;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

public class DirectEditCommand extends Command {
	private String oldText, newText;
	private Shape shapemodel;

	public void execute() {
		if (shapemodel instanceof EllipticalShape) {
			oldText = shapemodel.getTextInitloop();
			try { 
				Integer.parseInt((String) newText); 
				} 
			catch(NumberFormatException exc){
					
				Display display = PlatformUI.getWorkbench().getDisplay();
				display.syncExec(new Runnable() {
					public void run() {
						MessageBox messageBox = new MessageBox(new Shell(),

						SWT.ICON_WARNING);
						messageBox.setText("Info!");
						messageBox.setMessage("Not an integer number!");
						messageBox.open();
					}
				});
			 
			 }
			shapemodel.setTextInitloop(newText);
			oldText = shapemodel.getTextLoopcount();
			shapemodel.setTextLoopcount(newText);
			shapemodel.setText(newText);
		}

		else {
			oldText = shapemodel.getText();

			shapemodel.setText(newText);

		}

	}

	public void setModel(Object model) {
		shapemodel = (Shape) model;
	}

	public void setText(String text) {
		newText = text;
	}

	public void undo() {
		shapemodel.setText(oldText);
	}
}
