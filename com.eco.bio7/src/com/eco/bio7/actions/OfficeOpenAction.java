package com.eco.bio7.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.IWorkbenchWindow;

import com.eco.bio7.libreoffice.LibreOpenJob;

public class OfficeOpenAction extends Action {

	protected final IWorkbenchWindow window;

	public OfficeOpenAction(String text, IWorkbenchWindow window) {
		super(text);
		this.window = window;
		setId("com.eco.bio7.office_open");
		
	}

	public void run() {

		LibreOpenJob job = new LibreOpenJob();
		job.setUser(true);
		job.addJobChangeListener(job);
		job.schedule();

	}
	

}