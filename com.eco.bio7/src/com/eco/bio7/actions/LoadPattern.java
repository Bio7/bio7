package com.eco.bio7.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.jobs.LoadWorkspaceJob;

public class LoadPattern extends Action {

	private LoadWorkspaceJob ab;

	protected final IWorkbenchWindow window;

	public LoadPattern(String text, IWorkbenchWindow window) {
		super(text);
		this.window = window;

		setId("com.eco.bio7.load_pattern");

		setImageDescriptor(Bio7Plugin.getImageDescriptor("/icons/bio7new.png"));
	}

	public void run() {

		Shell s = new Shell(SWT.ON_TOP);
		FileDialog fd = new FileDialog(s, SWT.OPEN);
		fd.setText("Load");

		String[] filterExt = { "*.xml", "*.*" };
		fd.setFilterExtensions(filterExt);
		String selected = fd.open();
		if (selected != null) {
			//int size = get_size(selected);
            
			ab = new LoadWorkspaceJob(selected);
			ab.setUser(true);
			ab.schedule();
			 
			

		}

	}
	
}