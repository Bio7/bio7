package com.eco.bio7.reditor.actions;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Display;

import com.eco.bio7.reditor.Bio7REditorPlugin;
import com.eco.bio7.reditors.REditorTextHover;

public class OpenWebHelpBrowser extends Action {
	private boolean canBrowse = true;
	private String htmlHelpText;

	public OpenWebHelpBrowser() {
		super("Web Help");
		setId("com.eco.bio7.r_editor_web_help");
		ImageDescriptor desc = Bio7REditorPlugin.getImageDescriptor("/icons/help_search.png");
		this.setImageDescriptor(desc);
		setText("Web Help");

	}

	public void run() {
		if (canBrowse) {

			Job job = new Job("Html help") {
				private String url;

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					monitor.beginTask("Help ...", IProgressMonitor.UNKNOWN);
					canBrowse = false;
					Display display = ActionUtil.openBrowser();

					htmlHelpText = REditorTextHover.getHtmlHelpText();

					display.asyncExec(new Runnable() {

						public void run() {
							ActionUtil.setBrowserLocation("https://www.rdocumentation.org/search?q="  + htmlHelpText);
						}
					});

					monitor.done();
					return Status.OK_STATUS;
				}

			};
			job.addJobChangeListener(new JobChangeAdapter() {
				public void done(IJobChangeEvent event) {
					if (event.getResult().isOK()) {
						canBrowse = true;
					} else {

					}
				}
			});
			// job.setSystem(true);
			job.schedule();
		}
	}

}