package com.eco.bio7.actions;

import java.io.File;
import org.eclipse.jface.action.Action;
import org.eclipse.ui.IWorkbenchWindow;
import com.eco.bio7.jobs.ImageMacroWorkspaceJob;

public class ExecuteImageMacroAction extends Action {

	protected final IWorkbenchWindow window;

	private File file;

	private String text;

	public ExecuteImageMacroAction(String text, IWorkbenchWindow window, File file) {
		super(text);
		this.window = window;
		this.file = file;
		this.text = text;
		setId("com.eco.bio7.execute_ij_macro");
		
	}

	public void run() {
		if (text.equals("Empty")) {
			System.out.println("No script available");
		}

		else {

			ImageMacroWorkspaceJob Do = new ImageMacroWorkspaceJob(file);
			Do.setUser(true);
			Do.schedule();

		}

	}

}