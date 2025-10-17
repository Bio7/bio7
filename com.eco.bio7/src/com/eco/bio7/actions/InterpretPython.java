/*******************************************************************************
 * Copyright (c) 2007-2025 M. Austenfeld
 * 
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * Contributors:
 * Marcel Austenfeld - initial API and implementation
 *******************************************************************************/
package com.eco.bio7.actions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;
import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.compile.PythonInterpreter;
import com.eco.bio7.console.ConsolePageParticipant;
import com.eco.bio7.rcp.StartBio7Utils;

public class InterpretPython extends Action {

	private IFile file;
	protected Process proc;
	protected Runtime rt;
	protected boolean canceled;
	private static boolean isBlender = false;

	public InterpretPython(String text) {
		super(text);

		setId("com.eco.bio7.interpretpython");
		setActionDefinitionId("com.eco.bio7.interpretpythonaction");

	}

	@Override
	public void run() {
		StartBio7Utils utils = StartBio7Utils.getConsoleInstance();
		if (utils != null) {
			utils.cons.clear();
		}

		IEditorPart editor = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
				.getActiveEditor();
		if (editor == null) {
			return;
		}
		IDocument doc = ((ITextEditor) editor).getDocumentProvider().getDocument(editor.getEditorInput());
		if (editor.getEditorInput() instanceof IFileEditorInput) {

			file = ((IFileEditorInput) editor.getEditorInput()).getFile();
		}

		if (file.getFileExtension().equals("py")) {

			String a = doc.get();
			IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
			boolean cPython = store.getBoolean("python_pipe");
			String sel = store.getString("python_select");
			String blenderSel = store.getString("blender_options");
			boolean extraProcess = store.getBoolean("python_process_extra");
			String pathPython = store.getString("python_pipe_path");

			if (cPython == true) {
				String selectionConsole = ConsolePageParticipant.getInterpreterSelection();

				if (editor.isDirty()) {
					editor.doSave(new NullProgressMonitor());
				}
				IEditorInput editorInput = editor.getEditorInput();
				IFile aFile = null;

				if (editorInput instanceof IFileEditorInput) {
					aFile = ((IFileEditorInput) editorInput).getFile();
				}
				String loc = aFile.getLocation().toString();

				if (sel.equals("Python")) {

					boolean python3x = store.getBoolean("python_3x");
					if (python3x == false) {
						if (extraProcess == false) {
							/* Execute in Bio7 Python Shell! */
							if (selectionConsole.equals("Python")) {
								ConsolePageParticipant.pipeInputToConsole("execfile('" + loc + "')", true, true);
								ConsolePageParticipant.pipeInputToConsole("", true, true);
							} else {

								Bio7Dialog.message("Please start the \"Native Python\" Shell in the Bio7 console!");
							}
						} else {
							/* Execute in seperate process! */
							executePythonProcess(pathPython, loc);
						}
					} else {
						if (extraProcess == false) {
							/* Execute in Bio7 Python Shell! */
							if (selectionConsole.equals("Python")) {
								ConsolePageParticipant.pipeInputToConsole(

										"exec(compile(open('" + loc + "').read(),'" + loc + "', 'exec'))", true, true);
							} else {

								Bio7Dialog.message("Please start the \"Native Python\" Shell in the Bio7 console!");
							}
						} else {
							/* Execute in seperate process! */

							executePythonProcess(pathPython, loc);
						}
					}

				}

				else if (sel.equals("Blender")) {
					if (selectionConsole.equals("shell")) {
						String path = store.getString("path_blender");
						String blenderArgs = store.getString("blender_args");
						if (blenderSel.equals("pscript")) {

							ConsolePageParticipant.pipeInputToConsole(
									"\"" + path + "/blender\"" + " " + blenderArgs + " -P " + loc, true, true);
						} else if (blenderSel.equals("interactive")) {
							if (isBlender == false) {
								ConsolePageParticipant.pipeInputToConsole(
										"\"" + path + "/blender\"" + " " + blenderArgs + " --python-console", true,
										true);
								isBlender = true;
							}
							ConsolePageParticipant.pipeInputToConsole(store.getString("before_script_blender"), true,
									true);
							ConsolePageParticipant.pipeInputToConsole(
									"exec(compile(open('" + loc + "').read(),'" + loc + "', 'exec'))", true, true);
							ConsolePageParticipant.pipeInputToConsole(store.getString("after_script_blender"), true,
									true);
							System.out.println(
									"Please restart the Bio7 native console for a new interactive session if you have closed Blender!");
						} else {
							ConsolePageParticipant.pipeInputToConsole("\"" + path + "/blender\"" + " " + blenderArgs,
									true, true);
						}
					} else {
						Bio7Dialog.message("Please start the Shell in the Bio7 Console\n"
								+ "to interpret the Python script in Blender!");
					}
				}

			}

			else {

				PythonInterpreter.interpretJob(a, null);
			}
		}

	}

	private void executePythonProcess(String pathPython, String loc) {
		/* Change the path sep. for all OS! */
		pathPython = pathPython.replace("\\", "/");

		if (pathPython.isEmpty() == false) {
			pathPython = pathPython + "/python";
		} else {
			pathPython = "python";
		}
		String[] cmd = { pathPython, loc

		};

		Job job = new Job("Interpret Python") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				monitor.beginTask("Python process running...", IProgressMonitor.UNKNOWN);
				rt = Runtime.getRuntime();
				proc = null;
				/* We start a thread here to make the process destroy possible! */
				new Thread() {
					public void run() {
						try {
							proc = rt.exec(cmd);
						} catch (IOException e1) {

							e1.printStackTrace();
						}

						BufferedReader input = new BufferedReader(new InputStreamReader(proc.getInputStream()));

						BufferedReader error = new BufferedReader(new InputStreamReader(proc.getErrorStream()));

						// Output
						String out = null;
						try {
							while ((out = input.readLine()) != null) {
								System.out.println(out);
							}
						} catch (IOException e) {

							e.printStackTrace();
						}
						// Errors
						try {
							while ((out = error.readLine()) != null) {
								System.out.println(out);
							}
						} catch (IOException e) {

							e.printStackTrace();
						}
					}
				}.start();

				canceled = false;
				/* Here we watch if the process is cancelled in the job interface! */
				while (!canceled) {

					if (monitor.isCanceled()) {
						try {
							throw new InterruptedException();
						} catch (InterruptedException e) {
							canceled = true;
							proc.destroy();
						}
					}
					if (proc != null) {
						if (proc.isAlive() == false) {
							canceled = true;
						}
					}

					try {
						Thread.sleep(100);
					} catch (final InterruptedException e) {
					}
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

	public static void setBlenderInteractive(boolean isBlender) {
		InterpretPython.isBlender = isBlender;
	}

}
