package com.eco.bio7.actions;

import java.io.File;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.Action;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

import com.eco.bio7.compile.CompileClassAndMultipleClasses;
import com.eco.bio7.compile.GroovyInterpreter;
import com.eco.bio7.compile.JavaScriptInterpreter;
import com.eco.bio7.compile.BeanShellInterpreter;
import com.eco.bio7.compile.PythonInterpreter;
import com.eco.bio7.jobs.ImageMacroWorkspaceJob;

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

		if (file.getName().endsWith(".ijm")) {

			ImageMacroWorkspaceJob job = new ImageMacroWorkspaceJob(file);

			job.addJobChangeListener(new JobChangeAdapter() {
				public void done(IJobChangeEvent event) {
					if (event.getResult().isOK()) {

					}
				}
			});

			job.schedule();
		}

		else if (file.getName().endsWith(".bsh")) {

			BeanShellInterpreter.interpretJob(null, file.toString());

		} else if (file.getName().endsWith(".groovy")) {

			GroovyInterpreter.interpretJob(null, file.toString());

		} else if (file.getName().endsWith(".py")) {

			PythonInterpreter.interpretJob(null, file.toString());

		} else if (file.getName().endsWith(".js")) {

			JavaScriptInterpreter.interpretJob(null, file.toString());

		}

		else if (file.getName().endsWith(".txt")) {
			File fileToOpen = file;

			if (fileToOpen.exists() && fileToOpen.isFile()) {
				IFileStore fileStore = EFS.getLocalFileSystem().getStore(fileToOpen.toURI());
				IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();

				try {
					IDE.openEditorOnFileStore(page, fileStore);
				} catch (PartInitException ex) {
					// Put your exception handler here if you wish to
				}
			}
		}

		else if (file.getName().endsWith(".java")) {

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