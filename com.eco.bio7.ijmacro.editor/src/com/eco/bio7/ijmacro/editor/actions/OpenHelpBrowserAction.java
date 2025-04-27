package com.eco.bio7.ijmacro.editor.actions;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import com.eco.bio7.ijmacro.editor.IJMacroEditorPlugin;


public class OpenHelpBrowserAction extends Action {
	private String htmlHelpText;
	private String link;

	public OpenHelpBrowserAction(String link) {
		super("Help Browser");
		setId("com.eco.bio7.ijmacro_editor_help_browser");
		ImageDescriptor desc = IJMacroEditorPlugin.getImageDescriptor("/icons/help_contents.png");
		this.setImageDescriptor(desc);
		this.link=link;

		setText("Help Browser");

	}

	public void run() {
		Job job = new Job("Html help") {
			private String url;

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				monitor.beginTask("Help ...", IProgressMonitor.UNKNOWN);

				
				Display display = getDisplay();
				display.asyncExec(new Runnable() {

					public void run() {
						IWorkbenchBrowserSupport support =
								  PlatformUI.getWorkbench().getBrowserSupport();
								IWebBrowser browser = null;
								try {
									browser = support.createBrowser("ij.macro.editor");
								} catch (PartInitException e1) {
									// TODO Auto-generated catch block
									e1.printStackTrace();
								}
								try {
									browser.openURL(new URL(link));
								} catch (PartInitException | MalformedURLException e) {
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

					
				} else {

				}
			}
		});
		// job.setSystem(true);
		job.schedule();
	}
	/**
	 * Returns a default display.
	 * 
	 * @return a display
	 */
	public static Display getDisplay() {
		Display display = Display.getCurrent();
		// may be null if outside the UI thread
		if (display == null)
			display = Display.getDefault();
		return display;
	}

}