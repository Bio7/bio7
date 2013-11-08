package com.eco.bio7.actions;

import org.eclipse.jface.action.Action;
import com.eco.bio7.discrete3d.Quad3d;

public class MatrixQuad3DAction extends Action {

	public MatrixQuad3DAction() {
		super("nitrate", AS_RADIO_BUTTON);
		setId("com.eco.bio7.enable_nitrate3d");

		setText("Nitrate");
	}

	public void run() {
		

	}

	public void dispose() {

	}

}