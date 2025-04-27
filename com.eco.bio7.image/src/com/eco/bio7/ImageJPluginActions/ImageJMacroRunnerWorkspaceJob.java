package com.eco.bio7.ImageJPluginActions;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import ij.plugin.Macro_Runner;

public class ImageJMacroRunnerWorkspaceJob extends WorkspaceJob implements IJobChangeListener {

	private IPreferenceStore store;
	private String path;
	private Process process;
	private boolean evalExt;
	private String ijpath;
	private String javaArgs;

	public ImageJMacroRunnerWorkspaceJob(String path) {
		super("ImageJ selected interpreter in progress....");
		this.path = path;
		store = new ScopedPreferenceStore(InstanceScope.INSTANCE, "com.eco.bio7.ijmacro.editor");
		if (store == null) {
			evalExt = false;
		} else {
			evalExt = store.getBoolean("EVALUATE_EXTERNAL");
			ijpath = store.getString("LOCATION_EXTERNAL");
			javaArgs = store.getString("OPTIONS_EXTERNA");
		}

	}

	public IStatus runInWorkspace(IProgressMonitor monitor) {
		monitor.beginTask("ImageJ selected interpreter  is running.....", IProgressMonitor.UNKNOWN);
		

		if (evalExt) {
			try {
				process = new ProcessBuilder(ijpath, javaArgs, path).start();

				// get the error stream of the process and print it
				InputStream error = process.getErrorStream();
				for (int i = 0; i < error.available(); i++) {
					System.out.println("" + error.read());
				}

				try {
					while (!process.waitFor(1, TimeUnit.SECONDS)) {
						// check for cancelling!
						if (monitor.isCanceled()) {
							process.destroy();
							return Status.CANCEL_STATUS;
						}
					}
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} else {
			new Macro_Runner().run(path);
		}

		if (monitor.isCanceled()) {
			try {
				process.destroy();
				throw new InterruptedException();
			} catch (InterruptedException e) {

			}
			return Status.CANCEL_STATUS;
		}

		return Status.OK_STATUS;
	}

	public void aboutToRun(IJobChangeEvent event) {

	}

	public void awake(IJobChangeEvent event) {

	}

	public void done(IJobChangeEvent event) {

	}

	public void running(IJobChangeEvent event) {

	}

	public void scheduled(IJobChangeEvent event) {

	}

	public void sleeping(IJobChangeEvent event) {

	}

}
