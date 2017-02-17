package com.eco.bio7.actions;

import java.io.File;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.Action;
import org.eclipse.ui.IWorkbenchWindow;
import com.eco.bio7.compile.CompileClassAndMultipleClasses;
import com.eco.bio7.compile.GroovyInterpreter;
import com.eco.bio7.compile.JavaScriptInterpreter;
import com.eco.bio7.compile.BeanShellInterpreter;
import com.eco.bio7.compile.PythonInterpreter;

public class ExecuteScriptAction extends Action {

	private File file;
	private String text;

	public ExecuteScriptAction(String text, IWorkbenchWindow window, File file) {
		super(text);
		this.file = file;
		this.text = text;
		setId("com.eco.bio7.execute_bsh_script");

	}

	public void run() {
		if (text.equals("Empty")) {
			System.out.println("No script available!");
		}

		else if (text.equals(".txt") || text.equals(".ijm")) {

			ij.IJ.runMacroFile(file.getAbsolutePath());

		} else {
			if (file.getName().endsWith(".bsh")) {

				BeanShellInterpreter.interpretJob(null, file.toString());

			} else if (file.getName().endsWith(".groovy")) {

				GroovyInterpreter.interpretJob(null, file.toString());

			} else if (file.getName().endsWith(".py")) {

				PythonInterpreter.interpretJob(null, file.toString());

			} else if (file.getName().endsWith(".js")) {

				JavaScriptInterpreter.interpretJob(null, file.toString());

			} else if (file.getName().endsWith(".java")) {

				Job job = new Job("Compile Java") {
					@Override
					protected IStatus run(IProgressMonitor monitor) {
						monitor.beginTask("Compile Java...", IProgressMonitor.UNKNOWN);
						String name = file.getName().replaceFirst("[.][^.]+$", "");
						// IWorkspace workspace = ResourcesPlugin.getWorkspace();
						IPath location = Path.fromOSString(file.getAbsolutePath());

						// IFile ifile = workspace.getRoot().getFileForLocation(location);
						CompileClassAndMultipleClasses cp = new CompileClassAndMultipleClasses();
						try {
							cp.compileAndLoad(new File(location.toOSString()), new File(location.toOSString()).getParent(), name, null, true);
						} catch (Exception e) {
							// TODO Auto-generated catch block
							// Bio7Dialog.message(e.getMessage());
						}

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

		}

	}

}