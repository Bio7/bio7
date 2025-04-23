package com.eco.bio7.reditor.actions;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Display;
import org.osgi.framework.Bundle;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;
import com.eco.bio7.browser.BrowserView;
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
		ImageDescriptor desc = Bio7REditorPlugin.getImageDescriptor("/icons/help_contents.png");
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
							/* Do we have a black theme? */
							if (isThemeBlack) {
								// System.out.println(brow.getEngine().getLocation());

								/* Load a CSS applied to the R HTML helpfile! */
								Bundle bundle = Platform.getBundle("com.eco.bio7.themes");
								URL fileURL = bundle.getEntry("javafx/Bio7BrowserDarkHTML.css");
								File file = null;
								try {
									file = new File(FileLocator.resolve(fileURL).toURI());
								} catch (URISyntaxException e1) {
									e1.printStackTrace();
								} catch (IOException e1) {
									e1.printStackTrace();
								}
								String path = file.getPath().replace("\\", "/");
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
									BrowserView b = BrowserView.getBrowserInstance();
									try {
										b.setLocation(url);
									} catch (Exception e) {

										e.printStackTrace();
									}
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