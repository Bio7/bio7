package com.eco.bio7.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.IWorkbenchWindow;

import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.compile.Model;
import com.eco.bio7.methods.Compiled;

/**
 * Invokes the setup method of the compiled Java model (see
 * {@link Model#setup()}). Without a compiled model a message is displayed.
 */
public class Setup extends Action {

	protected final IWorkbenchWindow window;

	public Setup(String text, IWorkbenchWindow window) {
		super(text);
		this.window = window;

		setId("com.eco.bio7.setup");

		setImageDescriptor(com.eco.bio7.Bio7Plugin.getImageDescriptor("icons/maintoolbar/setup.png"));
		this.setToolTipText("Setup - Invokes the Java setup method if compiled available!");
	}

	public void run() {
		Model model = Compiled.getModel();
		if (model == null) {
			Bio7Dialog.message("No compiled model available!");
			return;
		}
		/* The setup method of the Model superclass displays a message if the compiled class has none! */
		model.setup();
	}

}
