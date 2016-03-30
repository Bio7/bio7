package com.eco.bio7.reditor.actions;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;

import com.eco.bio7.browser.BrowserView;
import com.eco.bio7.reditors.RConfiguration;

public class OpenWebHelpBrowser extends Action {
	private boolean canBrowse = true;
	private String htmlHelpText;
	public OpenWebHelpBrowser() {
		super("Web Help");
		setId("com.eco.bio7.r_editor_web_help");

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

					htmlHelpText = RConfiguration.htmlHelpText;

					display.asyncExec(new Runnable() {

						public void run() {
							BrowserView b = BrowserView.getBrowserInstance();
							try {
								b.setLocation("http://www.rdocumentation.org" + "#" + htmlHelpText);
							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
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