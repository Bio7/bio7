package com.eco.bio7.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.IWorkbenchWindow;

import com.eco.bio7.libreoffice.LibreOfficeSendValueJob;

public class OfficeSendValueAction extends Action {

	protected final IWorkbenchWindow window;

	public OfficeSendValueAction(String text, IWorkbenchWindow window) {
		super(text);
		this.window = window;
		setId("com.eco.bio7.office_send_value");
	
	}

	public void run() {

		LibreOfficeSendValueJob job = new LibreOfficeSendValueJob();
		job.setUser(true);
		job.schedule();

	}
	

}