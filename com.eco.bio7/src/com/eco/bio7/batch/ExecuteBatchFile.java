/*******************************************************************************
 * Copyright (c) 2007-2012 M. Austenfeld
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/

package com.eco.bio7.batch;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;
import org.rosuda.REngine.REXPLogical;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.browser.BrowserView;
import com.eco.bio7.collection.Work;
import com.eco.bio7.compile.BeanShellInterpreter;
import com.eco.bio7.compile.Compile;
import com.eco.bio7.compile.CompileClassAndMultipleClasses;
import com.eco.bio7.compile.GroovyInterpreter;
import com.eco.bio7.compile.PythonInterpreter;
import com.eco.bio7.compile.RScript;
import com.eco.bio7.console.ConsolePageParticipant;
import com.eco.bio7.floweditor.shapes.ShapesPlugin;
import com.eco.bio7.javaeditor.Bio7EditorPlugin;
import com.eco.bio7.jobs.ImageFlowMacroWorkspaceJob;
import com.eco.bio7.preferences.PreferenceConstants;
import com.eco.bio7.rbridge.RServe;
import com.eco.bio7.rbridge.RState;

public class ExecuteBatchFile {

	// private static Process process = null;
	private static boolean finished;
	private static boolean classbody;

	public static void intercompile(String fileprop, String fileextension) {
		/* Call interpreter or compiler etc. from the different extensions */
		IWorkspaceRoot root = ShapesPlugin.getWorkspace().getRoot();
		String fileroot = root.getLocation().toString();
		// Get the absolute path from the relative path!

		/* Interprets a BeanShell script! */
		if (fileextension.equals("bsh")) {
			BatchModel.pause();
			fileprop = fileroot + fileprop;

			String fileproper = fileprop.replace("//", "////");// Conversion
			// For Windows!
			/*
			 * if (BatchModel.isDebug()) { Interpreter.interpretJob(null, fileproper, a); }
			 */

			// else {
			BeanShellInterpreter.interpretJob(null, fileproper);

			// }

		}
		/* Interprets a Groovy script! */
		else if (fileextension.equals("groovy")) {
			BatchModel.pause();
			fileprop = fileroot + fileprop;

			String fileproper = fileprop.replace("//", "////");// Conversion

			GroovyInterpreter.interpretJob(null, fileproper);

		}

		/* Interprets a jython script! */
		else if (fileextension.equals("py")) {
			BatchModel.pause();
			fileprop = fileroot + fileprop;

			String fileproper = fileprop.replace("//", "////");// Conversion

			IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
			boolean cPython = store.getBoolean("python_pipe");
			String sel = store.getString("python_select");
			/* Native execution of python if started in the Shell! */
			if (cPython == true) {
				String selectionConsole = ConsolePageParticipant.getInterpreterSelection();
				String blenderSel = store.getString("blender_options");
				if (sel.equals("Python")) {

					if (selectionConsole.equals("Python")) {
						ConsolePageParticipant.pipeInputToConsole("execfile('" + fileproper + "')",true,true);
					} else {
						System.out.println("Please start the \"Native Python\" Shell in the Bio7 console!");
					}
				}

				else if (sel.equals("Blender")) {
					if (selectionConsole.equals("shell")) {
						String path = store.getString("path_blender");
						String blenderArgs = store.getString("blender_args");
						if (blenderSel.equals("pscript")) {

							ConsolePageParticipant.pipeInputToConsole("\"" + path + "/blender\"" + " " + blenderArgs + " -P " + fileproper,true,true);
						} else if (blenderSel.equals("interactive")) {

							ConsolePageParticipant.pipeInputToConsole("\"" + path + "/blender\"" + " " + blenderArgs + " --python-console",true,true);
							ConsolePageParticipant.pipeInputToConsole(store.getString("before_script_blender"),true,true);
							ConsolePageParticipant.pipeInputToConsole("exec(compile(open('" + fileproper + "').read(),'" + fileproper + "', 'exec'))",true,true);
							ConsolePageParticipant.pipeInputToConsole(store.getString("after_script_blender"),true,true);
						} else {
							ConsolePageParticipant.pipeInputToConsole("\"" + path + "/blender\"" + " " + blenderArgs,true,true);
						}
					} else {
						System.out.println("Please start the Shell in the Bio7 Console\n" + "to interpret the Python script in Blender!");
					}
				}

			} else {
				/* Normal execution of Jython! */
				PythonInterpreter.interpretJob(null, fileproper);
			}

		}
		/* Interprets a R script! */
		else if (fileextension.equals("R") || fileextension.equals("r")) {
			BatchModel.pause();
			fileprop = fileroot + fileprop;
			String fileproper = fileprop.replace("//", "////");

			IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
			boolean rPipe = store.getBoolean("r_pipe");

			if (rPipe == true) {
				ConsolePageParticipant.pipeInputToConsole("source('" + fileprop + "')",true,true);
			} else {
				/* If the OS is Linux! */
				if (Bio7Dialog.getOS().equals("Linux")) {
					String result = BatchModel.fileToString(fileproper);

					RScript.rscriptjob(result, null);

				}
				/* If the OS is MacOSX! */
				else if (Bio7Dialog.getOS().equals("Mac")) {
					String result = BatchModel.fileToString(fileproper);

					RScript.rscriptjob(result, null);

				}
				/* If the OS is Windows! */
				else {

					RScript.rscriptjob(null, fileprop);

				}
			}
		}
		/* Compiles a Java file! */
		else if (fileextension.equals("java")) {
			IPreferenceStore store = Bio7EditorPlugin.getDefault().getPreferenceStore();
			classbody = store.getBoolean("classbody");
			BatchModel.pause();
			fileprop = fileroot + fileprop;
			/* Compile a classbody! */
			if (classbody) {
				String result = BatchModel.fileToString(fileprop);

				Compile.compileJob(result);

			}
			/* Compile a main class and multiple classes! */
			else {
				IWorkspace workspace = ResourcesPlugin.getWorkspace();
				IPath location = Path.fromOSString(new File(fileprop).getAbsolutePath());
				IFile ifile = workspace.getRoot().getFileForLocation(location);
				CompileClassAndMultipleClasses cp = new CompileClassAndMultipleClasses();
				cp.compileClasses(ifile, ifile, null);

			}

		}

		/* Executes a nested flow! */
		else if (fileextension.equals("ecoflow")) {
			/* At the moment only works with one component inside a child flow! */
			IEditorPart editorstart = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
			IFile ifilestart = ((IFileEditorInput) editorstart.getEditorInput()).getFile();
			BatchModel.getS().push(ifilestart);

			BatchModel.recursiveBatch(fileprop);

		}
		/* Executes an ImageJ macro! */
		else if (fileextension.equals("txt")) {
			// final Shape shape = a;
			BatchModel.pause();
			fileprop = fileroot + fileprop;
			final File file = new File(fileprop);
			ImageFlowMacroWorkspaceJob job = new ImageFlowMacroWorkspaceJob(file);

			job.addJobChangeListener(new JobChangeAdapter() {
				public void done(IJobChangeEvent event) {
					if (event.getResult().isOK()) {
						BatchModel.resume();
					}
				}
			});

			job.schedule();

		}
		/* Executes an application! */
		else if (fileextension.equals("exe")) {

			final File file = new File(fileprop);
			// final Runtime rt;

			// rt = Runtime.getRuntime();

			IWorkbench wb = PlatformUI.getWorkbench();
			IProgressService ps = wb.getProgressService();

			try {
				ps.busyCursorWhile(new IRunnableWithProgress() {
					public void run(IProgressMonitor pm) {

						/*
						 * try { process = rt.exec(file.getAbsolutePath() + " " + BatchModel.getArgument()); } catch (IOException e) {
						 * 
						 * e.printStackTrace(); }
						 */
						List<String> args = new ArrayList<String>();
						args.add(file.getAbsolutePath());
						args.add(BatchModel.getArgument());

						ProcessBuilder pb = new ProcessBuilder(args);
						pb.redirectErrorStream();
						try {
							pb.start();

						} catch (IOException e) {
							e.printStackTrace();

						}

					}
				});
			} catch (InvocationTargetException e) {

				e.printStackTrace();
			} catch (InterruptedException e) {

				e.printStackTrace();
			}

			BatchModel.setArgument(null);
		}
		/* Executes a batch file(Windows)! */
		else if (fileextension.equals("bat")) {
			/*
			 * if (BatchModel.isDebug()) { a.setDefaultColor(); }
			 */

			final File file = new File(fileprop);
			// final Runtime rt;

			// rt = Runtime.getRuntime();

			IWorkbench wb = PlatformUI.getWorkbench();
			IProgressService ps = wb.getProgressService();

			try {
				ps.busyCursorWhile(new IRunnableWithProgress() {
					public void run(IProgressMonitor pm) {

						/*
						 * try { process = rt.exec(file.getAbsolutePath()); } catch (IOException e) {
						 * 
						 * e.printStackTrace(); }
						 */
						List<String> args = new ArrayList<String>();
						args.add(file.getAbsolutePath());
						ProcessBuilder pb = new ProcessBuilder(args);
						pb.redirectErrorStream();
						try {
							pb.start();

						} catch (IOException e) {
							e.printStackTrace();

						}

					}
				});
			} catch (InvocationTargetException e) {

				e.printStackTrace();
			} catch (InterruptedException e) {

				e.printStackTrace();
			}

		}
		/* Executes a shell file(Linux)! */
		else if (fileextension.equals("sh")) {
			/*
			 * if (BatchModel.isDebug()) { a.setDefaultColor(); }
			 */

			final File file = new File(fileprop);
			// final Runtime rt;

			// rt = Runtime.getRuntime();

			IWorkbench wb = PlatformUI.getWorkbench();
			IProgressService ps = wb.getProgressService();

			try {
				ps.busyCursorWhile(new IRunnableWithProgress() {
					public void run(IProgressMonitor pm) {

						/*
						 * try { process = rt.exec(file.getAbsolutePath()); } catch (IOException e) {
						 * 
						 * e.printStackTrace(); }
						 */
						List<String> args = new ArrayList<String>();
						args.add(file.getAbsolutePath());
						ProcessBuilder pb = new ProcessBuilder(args);
						pb.redirectErrorStream();
						try {
							pb.start();

						} catch (IOException e) {
							e.printStackTrace();

						}

					}
				});
			} catch (InvocationTargetException e) {

				e.printStackTrace();
			} catch (InterruptedException e) {

				e.printStackTrace();
			}

			BatchModel.setArgument(null);
		}
		/* Assigns the text to the argument variable! */
		else if (fileextension.equals("argument")) {
			fileprop = fileroot + fileprop;
			BatchModel.setArgument(BatchModel.fileToString(fileprop));

		}
		/* Starts R with the specified RData file as argument(Windows!) */
		else if (fileextension.equals("rdata")) {
			fileprop = fileroot + fileprop;
			String result = fileprop;
			IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
			String path = store.getString(PreferenceConstants.PATH_R) + "\\bin\\Rgui";
			// Runtime rt;

			// Process proc = null;
			// rt = Runtime.getRuntime();

			// proc = rt.exec(store.getString(PreferenceConstants.PATH_R) + "\\bin\\Rgui " + result);

			List<String> args = new ArrayList<String>();
			args.add(path);
			args.add(result);

			ProcessBuilder pb = new ProcessBuilder(args);
			pb.redirectErrorStream();
			try {
				pb.start();

			} catch (IOException e) {
				e.printStackTrace();

			}

		}
		/*
		 * Starts R with the specified script in batch mode with the file as argument!
		 */
		else if (fileextension.equals("rbatch")) {
			fileprop = fileroot + fileprop;
			String result = fileprop;
			Runtime rt;
			IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
			String path = store.getString(PreferenceConstants.PATH_R) + "\\bin\\R";
			// Process proc = null;
			rt = Runtime.getRuntime();

			// proc = rt.exec(path+" CMD BATCH " + result);

			List<String> args = new ArrayList<String>();
			args.add(path);
			args.add("CMD");
			args.add("BATCH");
			args.add(result);

			ProcessBuilder pb = new ProcessBuilder(args);
			pb.redirectErrorStream();
			try {
				pb.start();

			} catch (IOException e) {
				e.printStackTrace();

			}

		}
		/* Starts the registered pdf application with the file! */
		else if (fileextension.equals("pdf")) {

			fileprop = fileroot + fileprop;

			try {
				Program.launch(fileprop);
			} catch (RuntimeException e) {

				e.printStackTrace();
			}

		}

		else if (fileextension.equals("rhtml") || fileextension.equals("Rhtml") || fileextension.equals("Rtex") || fileextension.equals("rtex") || fileextension.equals("Rmw")
				|| fileextension.equals("rmw") || fileextension.equals("Rmd") || fileextension.equals("rmd") || fileextension.equals("Rst") || fileextension.equals("rst")) {
			fileprop = fileroot + fileprop;

			String fileext = fileextension.replace("R", "");
			fileext = fileext.replace("r", "");

			fileroot = fileprop.replace("//", "////");// Conversion

			/* Get the parent directory! */
			File fi = new File(fileroot);
			String dir = fi.getParentFile().getPath().replace("\\", "/");
			String name = fi.getName();
			final String theName = name.replaceFirst("[.][^.]+$", "");

			if (RServe.isAlive()) {
				if (RState.isBusy() == false) {

					RConnection c = RServe.getConnection();

					try {
						REXPLogical rl = (REXPLogical) c.eval("require(knitr)");
						if (!(rl.isTRUE()[0])) {

							Bio7Dialog.message("Cannot load 'knitr' package!");
						}

						c.eval("try(library(knitr))");
						c.eval("setwd('" + dir + "')");

						RServe.print("try(knit('" + name + "','" + theName + "." + fileext + "'))");

					} catch (RserveException e1) {

						e1.printStackTrace();
					}

					if (fileext.equals("html")) {
						Work.openView("com.eco.bio7.browser.Browser");
						final String dir2 = dir;

						Display display = PlatformUI.getWorkbench().getDisplay();
						display.asyncExec(new Runnable() {

							public void run() {
								String temp = "file:///" + dir2 + "/" + theName + ".html";
								String url = temp.replace("\\", "/");
								System.out.println(url);
								BrowserView b = BrowserView.getBrowserInstance();
								b.setLocation(url);
							}
						});

					} else if (fileext.equals("tex")) {
						IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
						String pdfLatexPath = store.getString("pdfLatex");
						final BufferedReader input;
						OutputStream stdin;
						pdfLatexPath = pdfLatexPath.replace("\\", "/");
						final String tempDirLoc = dir;

						// Process proc = Runtime.getRuntime().exec(
						// pdfLatexPath+"/pdflatex -interaction=nonstopmode " + "-output-directory=" + tempDirLoc + " " + tempDirLoc + "/" + theName + ".tex");

						List<String> args = new ArrayList<String>();
						args.add(pdfLatexPath + "/pdflatex");
						args.add("-interaction=nonstopmode");
						args.add("-output-directory=" + tempDirLoc);
						args.add(tempDirLoc + "/" + theName + ".tex");

						Process proc = null;
						ProcessBuilder pb = new ProcessBuilder(args);
						pb.redirectErrorStream();
						try {
							proc = pb.start();

						} catch (IOException e) {

							e.printStackTrace();
						}
						input = new BufferedReader(new InputStreamReader(proc.getInputStream()));
						stdin = proc.getOutputStream();

						new Thread() {

							public void run() {
								setPriority(Thread.MAX_PRIORITY);
								String line;
								try {

									while ((line = input.readLine()) != null) {
										System.out.println(line);
									}
									File fil = new File(tempDirLoc + "/" + theName + ".pdf");
									if (fil.exists()) {
										Program.launch(tempDirLoc + "/" + theName + ".pdf");
									} else {
										Bio7Dialog.message("*.pdf file was not created.\nPlease check the error messages!");
									}

								} catch (IOException e) {

									e.printStackTrace();
								}
								/*
								 * IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot(); IProject proj = root.getProject(activeProject.getName()); try { proj.refreshLocal(IResource.DEPTH_INFINITE, null); } catch (CoreException e) { // TODO Auto-generated catch block e.printStackTrace(); }
								 */

							}
						}.start();

					}
				}

			}

			Work.refreshAllWorkspaces();
		}

		else {
			/* Opens a dragged folder if isOpenFolder()==true! */
			String result = fileprop;

			if (!BatchModel.isOpenFolder()) {
				BatchModel.setFolder(result);

			}

			else {
				BatchModel.setFolder(result);
				try {
					Program.launch(fileprop);
				} catch (RuntimeException e) {

					e.printStackTrace();
				}

			}
		}
	}

}
