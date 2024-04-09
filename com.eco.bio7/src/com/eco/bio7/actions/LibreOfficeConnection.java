package com.eco.bio7.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.IWorkbenchWindow;

import com.eco.bio7.libreoffice.LibreOfficeConnectionJob;

public class LibreOfficeConnection extends Action {

	protected final IWorkbenchWindow window;

	public LibreOfficeConnection(String text, IWorkbenchWindow window) {
		
		super(text);
		this.window = window;
		setId("com.eco.bio7.openoffice_connection");
		setImageDescriptor(com.eco.bio7.Bio7Plugin.getImageDescriptor("/icons/maintoolbar/libre_office.png"));
	}

	public void run() {

		LibreOfficeConnectionJob job = new LibreOfficeConnectionJob();
		job.schedule();

	}

}