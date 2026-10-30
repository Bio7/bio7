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
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;
import com.eco.bio7.rbridge.RState;
import com.eco.bio7.reditor.Bio7REditorPlugin;
import com.eco.bio7.reditors.REditor;
import com.eco.bio7.reditors.REditorTextHover;

public class OpenHelpBrowserAction extends Action {
	private String htmlHelpText;
	public static boolean isThemeBlack = false;

	public OpenHelpBrowserAction() {
		super("Help Browser");
		setId("com.eco.bio7.r_editor_help_browser");
		ImageDescriptor desc = Bio7REditorPlugin.getImageDescriptor("/icons/help_contents.svg");
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
							String out = null;
							c.eval("try(.bio7TempHtmlHelpFile <- paste(tempfile(), \".html\", sep=\"\"))").toString();
							/* The CSS of the theme (a custom theme or the dark theme) for the R HTML help file! */
							String themeCss = com.eco.bio7.util.HtmlTheme.getCssPath();
							if (themeCss != null) {
								String path = themeCss;
								// String css = Util.fileToString(path);
								c.assign("pathcss", path);
								c.eval("try(tools::Rd2HTML(utils:::.getHelpFile(?" + htmlHelpText
										+ "),.bio7TempHtmlHelpFile,package=\"tools\", stages=c(\"install\", \"render\"),stylesheet = pathcss))");
							}

							else {

								c.eval("try(tools::Rd2HTML(utils:::.getHelpFile(?" + htmlHelpText
										+ "),.bio7TempHtmlHelpFile,package=\"tools\", stages=c(\"install\", \"render\")))");

							}
							try {
								out = (String) c.eval("try(.bio7TempHtmlHelpFile)").asString();
							} catch (REXPMismatchException e) {

								e.printStackTrace();
							}

							String pattern = "file:///" + out;
							url = pattern.replace("\\", "/");

							display.asyncExec(new Runnable() {

								public void run() {
									ActionUtil.setBrowserLocation(url);
									
								}
							});
						} else {
							System.out.println("Rserve is busy!");
						}
					}

				} catch (RserveException e1) {

					// e1.printStackTrace();

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
					RState.setBusy(true);
				}
			}
		});
		// job.setSystem(true);
		job.schedule();
	}

}