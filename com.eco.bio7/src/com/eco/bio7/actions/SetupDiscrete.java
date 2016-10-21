package com.eco.bio7.actions;

import java.io.File;
import org.eclipse.jface.action.Action;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;

import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.jobs.SaveWorkspaceJob;
import com.eco.bio7.methods.Compiled;

public class SetupDiscrete extends Action {

	private final IWorkbenchWindow window;

	private SaveWorkspaceJob ab;

	public SetupDiscrete(String text, IWorkbenchWindow window) {
		super(text);
		this.window = window;

		setId("com.eco.bio7.setup_discrete");

		setImageDescriptor(com.eco.bio7.Bio7Plugin.getImageDescriptor("icons/maintoolbar/setup.png"));
		this.setToolTipText("Setup - Invokes the Java setup method if compiled available!");
	}

	public void run() {
		
		if(Compiled.getModel()!=null){

			Class cla = Compiled.getModel().getClass();
			try {
				if (cla.getMethod("setup", null) != null) {
					Compiled.getModel().setup();
				}

			} catch (SecurityException e) {
				
				
			} catch (NoSuchMethodException e) {
				Bio7Dialog.message("No setup method available!");
				}
			}
			else{

			     Bio7Dialog.message("No compiled method available !");
			}

		
	}

}
