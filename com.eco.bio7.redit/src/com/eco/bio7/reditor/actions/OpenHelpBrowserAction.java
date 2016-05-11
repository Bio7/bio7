package com.eco.bio7.reditor.actions;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import com.eco.bio7.browser.BrowserView;
import com.eco.bio7.rbridge.RState;
import com.eco.bio7.reditors.REditorTextHover;
import com.eco.bio7.reditors.RConfiguration;
import com.eco.bio7.reditors.REditor;

public class OpenHelpBrowserAction extends Action {
	private String htmlHelpText;

	public OpenHelpBrowserAction() {
		super("Help Browser");
		setId("com.eco.bio7.r_editor_help_browser");
		ImageDescriptor desc = ImageDescriptor.createFromImage(new Image(Display.getCurrent(), getClass().getResourceAsStream("/pics/help_contents.png")));
		this.setImageDescriptor(desc);

		setText("Help Browser");

	}

	public void run() {
		Job job = new Job("Html help") {
			private String url;

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				monitor.beginTask("Help ...", IProgressMonitor.UNKNOWN);

				try {
					RConnection c = REditor.getRserveConnection();
					if (c != null) {
						if (RState.isBusy() == false) {
							RState.setBusy(true);
							Display display = ActionUtil.openBrowser();

							htmlHelpText = REditorTextHover.getHtmlHelpText();

							c.eval("try(.bio7TempHtmlHelpFile <- paste(tempfile(), \".html\", sep=\"\"))").toString();
							c.eval("try(tools::Rd2HTML(utils:::.getHelpFile(?" + htmlHelpText + "),.bio7TempHtmlHelpFile,package=\"tools\", stages=c(\"install\", \"render\")))");
							String out = null;
							try {
								out = (String) c.eval("try(.bio7TempHtmlHelpFile)").asString();
							} catch (REXPMismatchException e) {

								e.printStackTrace();
							}

							String pattern = "file:///" + out;
							url = pattern.replace("\\", "/");

							display.asyncExec(new Runnable() {

								public void run() {
									BrowserView b = BrowserView.getBrowserInstance();
									try {
										b.setLocation(url);
									} catch (Exception e) {
										// TODO Auto-generated catch
										// block
										e.printStackTrace();
									}
								}
							});
						} else {
							System.out.println("Rserve is busy!");
						}
					}

				} catch (RserveException e1) {

					e1.printStackTrace();
				}

				monitor.done();
				return Status.OK_STATUS;
			}

		};
		job.addJobChangeListener(new JobChangeAdapter() {
			public void done(IJobChangeEvent event) {
				if (event.getResult().isOK()) {

					RState.setBusy(false);
				} else {

				}
			}
		});
		// job.setSystem(true);
		job.schedule();
	}

}