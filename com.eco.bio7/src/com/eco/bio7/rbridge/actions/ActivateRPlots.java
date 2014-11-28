package com.eco.bio7.rbridge.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.IWorkbenchWindow;
import com.eco.bio7.batch.BatchModel;
import com.eco.bio7.batch.FileRoot;
import com.eco.bio7.compile.RScript;
import com.eco.bio7.rbridge.RServe;

public class ActivateRPlots extends Action {

	private final IWorkbenchWindow window;

	public ActivateRPlots(String text, IWorkbenchWindow window) {
		super(text);
		this.window = window;

		setId("com.eco.bio7.activate_r_plots");

		setActionDefinitionId("com.eco.bio7.activatRPlot");

	}

	public void run() {

		if (RServe.isAliveDialog()) {

			String filep = FileRoot.getRScriptLocation() + "/Activate_Plot.R";

			String result = BatchModel.fileToString(filep);

			RScript.rScriptJob(result, null);

		}

	}

}