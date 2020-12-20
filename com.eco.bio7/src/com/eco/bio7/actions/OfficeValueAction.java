package com.eco.bio7.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.IWorkbenchWindow;

import com.eco.bio7.libreoffice.LibreOfficeValueJob;

public class OfficeValueAction extends Action {

	private final IWorkbenchWindow window;

	public OfficeValueAction(String text, IWorkbenchWindow window) {
		
		super(text);
		this.window = window;
		setId("com.eco.bio7.office_value");
		
	}

	public void run() {

		LibreOfficeValueJob job = new LibreOfficeValueJob();
		job.setUser(true);
		job.schedule();

	}

}