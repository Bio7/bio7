package com.eco.bio7.actions;

import java.awt.Cursor;
import org.eclipse.jface.action.Action;
import org.eclipse.ui.IWorkbenchWindow;

import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.discrete.Quad2d;

public class EnableSelection extends Action {

	protected final IWorkbenchWindow window;

	public EnableSelection(String text, IWorkbenchWindow window) {
		super(text);
		this.window = window;

		setId("com.eco.bio7.enable_selection");

		setActionDefinitionId("com.eco.bio7.Enable_Selection");
		setImageDescriptor(Bio7Plugin.getImageDescriptor("/icons/bio7new.png"));
	}

	public void run() {

		//Quad2d.getQuad2dInstance().setCursor(new Cursor(1));
		Quad2d.getQuad2dInstance().selectionenabled = true;
		Quad2d.getQuad2dInstance().donotdrag = true;

	}

}