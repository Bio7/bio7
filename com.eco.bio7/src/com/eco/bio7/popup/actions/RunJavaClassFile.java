/*******************************************************************************
 * Copyright (c) 2007-2013 M. Austenfeld
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/

package com.eco.bio7.popup.actions;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

import com.eco.bio7.compile.utils.ScanClassPath;
import com.eco.bio7.console.ConsoleInterpreterAction;
import com.eco.bio7.console.ConsolePageParticipant;
import com.eco.bio7.rcp.ApplicationWorkbenchWindowAdvisor;

public class RunJavaClassFile implements IObjectActionDelegate {

	private static Process shellProcess;
	private Thread processThread;

	public RunJavaClassFile() {
		super();
	}

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
	}

	public void run(IAction action) {
		if (shellProcess != null) {
			shellProcess.destroy();
		}
		String project = null;
		ISelection selection = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().getSelection();
		IStructuredSelection strucSelection = null;
		if (selection instanceof IStructuredSelection) {
			strucSelection = (IStructuredSelection) selection;
			if (strucSelection.size() == 0) {

			} else if (strucSelection.size() == 1) {
				String nameofiofile;
				Object selectedObj = strucSelection.getFirstElement();
				if (selectedObj instanceof IFile) {
					IFile selectedFile = (IFile) selectedObj;
					nameofiofile = getFileName(selectedFile.getName());
					project = selectedFile.getLocation().toString();
					File file = selectedFile.getLocation().toFile();
					String dir = file.getParent();
					String selectionConsole = ConsolePageParticipant.getInterpreterSelection();

					String temp = new ScanClassPath().scan().replace(";;", ";");
					ConsolePageParticipant cpp = ConsolePageParticipant.getConsolePageParticipantInstance();
					ConsoleInterpreterAction iaction = cpp.ia;
					iaction.javaProcess();
					if (ApplicationWorkbenchWindowAdvisor.getOS().equals("Windows")) {
						// String cp = temp.replace("\\", "////");
						// Bio7Console.write("java -cp \"" + temp + ";" + dir + "\"/" +" " + nameofiofile);
						// Bio7Console.write("start java -cp \"" + cp + ";" + "\"" + dir + " " + nameofiofile);

						List<String> args = new ArrayList<String>();
						args.add("java");
						args.add("-cp");
						args.add("\"" + temp + ";" + dir + "\"");
						args.add(nameofiofile);
						ProcessBuilder builder = new ProcessBuilder(args);
						builder.redirectErrorStream(true);
						try {
							shellProcess = builder.start();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						processThread = new Thread(new ProcessGrabber());
						processThread.start();

					} else if (ApplicationWorkbenchWindowAdvisor.getOS().equals("Linux")) {
						List<String> args = new ArrayList<String>();
						args.add("java");
						args.add("-cp");
						args.add("" + temp + ";" + dir);
						args.add(nameofiofile);
						ProcessBuilder builder = new ProcessBuilder(args);
						builder.redirectErrorStream(true);
						try {
							shellProcess = builder.start();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						processThread = new Thread(new ProcessGrabber());
						processThread.start();
					} else if (ApplicationWorkbenchWindowAdvisor.getOS().equals("Mac")) {
						List<String> args = new ArrayList<String>();
						args.add("java");
						args.add("-cp");
						args.add("" + temp + ";" + dir);
						args.add(nameofiofile);
						ProcessBuilder builder = new ProcessBuilder(args);
						builder.redirectErrorStream(true);
						try {
							shellProcess = builder.start();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						processThread = new Thread(new ProcessGrabber());
						processThread.start();
					}

				}
			}

		}

	}

	class ProcessGrabber implements Runnable {
		final InputStream inp = shellProcess.getInputStream();

		public void run() {
			

			try {

				InputStreamReader inr = new InputStreamReader(inp);

				int ch;
				while ((ch = inr.read()) != -1) {

					System.out.print((char) ch);

				}
				
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (inp != null) {
					try {
						inp.close();

					} catch (IOException e) {

					}
				}
			}

		}
	}

	public static String getFileName(String path) {

		String fileName = null;
		String separator = File.separator;

		int pos = path.lastIndexOf(separator);
		int pos2 = path.lastIndexOf(".");

		if (pos2 > -1)
			fileName = path.substring(pos + 1, pos2);
		else
			fileName = path.substring(pos + 1);

		return fileName;
	}

	public void selectionChanged(IAction action, ISelection selection) {
	}

	public static Process getJavaProcess() {
		return shellProcess;
	}

}
