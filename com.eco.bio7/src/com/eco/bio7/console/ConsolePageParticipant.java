/*******************************************************************************
 * Copyright (c) 2007-2013 M. Austenfeld
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     M. Austenfeld
 *     
 * The EPL-Code for the caret corrections came from the ConsolePageParticipant class
 * of the Counterclockwise plugin:
 * http://code.google.com/p/counterclockwise/
 * (see information below)
 * 
 ********************************************************************************
 * Copyright (c) 2009 Laurent PETIT.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Laurent PETIT - initial API and implementation
 *    Gorsal - patch for correcting namespace browser initialisation issue
 *    Mark Feber - patch to correct caret position in process console
 *******************************************************************************/

package com.eco.bio7.console;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IRegion;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsolePageParticipant;
import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.console.IOConsoleInputStream;
import org.eclipse.ui.console.TextConsolePage;
import org.eclipse.ui.console.TextConsoleViewer;
import org.eclipse.ui.part.IPageBookViewPage;
import org.eclipse.ui.part.IPageSite;
import org.osgi.framework.Bundle;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.os.pid.Pid;
import com.eco.bio7.os.pid.UnixProcessManager;
import com.eco.bio7.popup.actions.RunJavaClassFile;
import com.eco.bio7.preferences.PreferenceConstants;
import com.eco.bio7.rbridge.RServe;
import com.eco.bio7.rbridge.RState;
import com.eco.bio7.rbridge.debug.DebugContinueAction;
import com.eco.bio7.rbridge.debug.DebugInfoAction;
import com.eco.bio7.rbridge.debug.DebugNextAction;
import com.eco.bio7.rbridge.debug.DebugRScript;
import com.eco.bio7.rbridge.debug.DebugStepFinishAction;
import com.eco.bio7.rbridge.debug.DebugStepIntoAction;
import com.eco.bio7.rbridge.debug.DebugStopAction;
import com.eco.bio7.rcp.ApplicationWorkbenchWindowAdvisor;
import com.eco.bio7.rcp.StartBio7Utils;
import com.eco.bio7.scriptengines.ScriptEngineConnection;
import com.eco.bio7.util.PlaceholderLabel;

public class ConsolePageParticipant implements IConsolePageParticipant {

	public IPageBookViewPage page;
	private static IOConsole ioc;
	private Thread initializationThread;
	public static String interpreterSelection = "-";
	private String input = null;
	private InputStreamReader isr;
	private BufferedReader in;
	public StyledText styledText;
	private ArrayList<String> list;
	public int commandBack = 0;
	protected int position = -1;
	public boolean ignore = false;
	private Process shellProcess;
	private Process pythonProcess;
	private Process RProcess;
	public Process nativeShellProcess;
	public String consoleEncoding = "UTF-8";
	public Thread processThread;
	public Thread RprocessThread;
	public Thread nativeShellprocessThread;
	public Thread pythonProcessThread;
	private IOConsoleInputStream iocinput;
	private boolean runThread;
	public ConsoleInterpreterAction ia;
	public IToolBarManager toolBarManager;
	public IActionBars actionBars;
	private Pid rPid;
	private Pid shellPid;
	private Pid pythonPid;
	private IContributionItem item;
	private static final char IAC = (char) 5;
	private static final char BRK = (char) 3;

	private static ConsolePageParticipant ConsolePageParticipantInstance;
	static boolean lineSeperatorConsole = true;
	static boolean addToHistoryConsole = true;
	public static String finalOutput;

	public ConsolePageParticipant() {
		ConsolePageParticipantInstance = this;
	}

	@Override
	public Object getAdapter(Class adapter) {
		// TODO Auto-generated method stub
		return null;
	}

	public static void pipeInputToConsole(String input, boolean lineSeperator, boolean addToHistory) {
		lineSeperatorConsole = lineSeperator;
		addToHistoryConsole = addToHistory;
		ioc.getInputStream().appendData(input);
		ioc.getInputStream().appendData(System.getProperty("line.separator"));

	}

	public static void pipeInputToConsole(String input) {
		lineSeperatorConsole = true;
		addToHistoryConsole = true;
		ioc.getInputStream().appendData(input);
		ioc.getInputStream().appendData(System.getProperty("line.separator"));

	}

	public static String pipeOutputToString() {
		finalOutput = ioc.getDocument().get();
		return finalOutput;
	}

	@Override
	public void init(IPageBookViewPage page, IConsole console) {

		this.page = page;

		if (console instanceof IOConsole) {
			ioc = (IOConsole) console;

		}
		iocinput = ioc.getInputStream();
		isr = new InputStreamReader(iocinput);
		initializationThread = new Thread(new Runnable() {

			public void run() {
				if (initializationThread.isInterrupted() == false) {
					interpret();
				}

			}

		});
		runThread = true;
		initializationThread.start();
		list = new ArrayList<String>();
		styledText = (StyledText) page.getControl();
		IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
		FontData shellFont = PreferenceConverter.getFontData(store, "Bio7ShellFonts");
		if (shellFont != null) {
			styledText.setFont(new Font(Display.getDefault(), shellFont));
		}
		styledText.setKeyBinding(SWT.ARROW_UP, SWT.NULL);
		styledText.setKeyBinding(SWT.ARROW_DOWN, SWT.NULL);

		// styledText.setKeyBinding(SWT.ARROW_LEFT, SWT.NULL);
		// styledText.setKeyBinding(SWT.ARROW_RIGHT, SWT.NULL);

		styledText.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent event) {

				if (event.keyCode == SWT.ARROW_UP) {
					styledText.setCaretOffset(styledText.getText().length());
					commandBack--;

					if (position == -1) {
						position = styledText.getCaretOffset();
						// commandBack = list.size() - 1;
					}

					if (commandBack == -1) {
						commandBack = 0;
					}

					int range = styledText.getText().length() - position;

					if (range > 0) {
						if (list.size() > 0) {
							styledText.replaceTextRange(position, range, list.get(commandBack));
						}
					} else if (range == 0) {
						if (list.size() > 0) {
							styledText.replaceTextRange(position, 0, list.get(commandBack));
						}
					} else {

					}

				} else if (event.keyCode == SWT.ARROW_DOWN) {
					styledText.setCaretOffset(styledText.getText().length());
					commandBack++;
					if (position == -1) {
						position = styledText.getCaretOffset();
						// commandBack=list.size()-1;
					}

					if (commandBack >= list.size() - 1) {
						commandBack = list.size() - 1;
					}

					int range = styledText.getText().length() - position;

					if (range > 0) {
						if (list.size() > 0) {
							styledText.replaceTextRange(position, range, list.get(commandBack));
						}
					} else if (range == 0) {
						if (list.size() > 0) {
							styledText.replaceTextRange(position, 0, list.get(commandBack));
						}
					} else {

					}

				}

				else if (event.keyCode == SWT.F1) {

					if (interpreterSelection.equals("R") && RServe.getConnection() != null) {
						fastSaveAndReloadRWorkspace();
						System.out.print("Rserve workspace data transferred to native R workspace!");
						System.out.println();
					} else {
						Bio7Dialog.message("Please start Rserve and the \"Native R\" shell in the Bio7 console!");
					}

				}
				/* CTRL+c key event! */
				else if (event.stateMask == SWT.CTRL && event.keyCode == 'c') {

					// Send OS signal
					if (Bio7Dialog.getOS().equals("Windows")) {
						Bundle bundle = Platform.getBundle("com.eco.bio7.os");

						URL locationUrl = FileLocator.find(bundle, new Path("/"), null);
						URL fileUrl = null;
						try {
							fileUrl = FileLocator.toFileURL(locationUrl);
						} catch (IOException e2) {

							e2.printStackTrace();
						}
						File fi = new File(fileUrl.getPath());
						String pathBundle = fi.toString() + "/win/64";
						if (interpreterSelection.equals("R")) {
							try {
								Process p = Runtime.getRuntime().exec(pathBundle + "/SendSignalCtrlC.exe " + rPid.getPidWindows(RProcess));

							} catch (IOException e) {
								e.printStackTrace();
							}
							// System.out.println(pathBundle +
							// "/SendSignalCtrlC.exe " +
							// rPid.getPidWindows(RProcess));
						} else if (interpreterSelection.equals("shell")) {
							// sendCtrlBreakThroughStream(nativeShellProcess);
							try {
								Process p = Runtime.getRuntime().exec(pathBundle + "/SendSignalCtrlC.exe " + shellPid.getPidWindows(nativeShellProcess));

							} catch (IOException e) {
								e.printStackTrace();
							}

						}

						else if (interpreterSelection.equals("python")) {
							try {
								Process p = Runtime.getRuntime().exec(pathBundle + "/SendSignalCtrlC.exe " + pythonPid.getPidWindows(pythonProcess));

							} catch (IOException e) {
								e.printStackTrace();
							}

						}
					} else {

						if (interpreterSelection.equals("R")) {

							// Process p =
							// Runtime.getRuntime().exec("kill -INT " +
							// rPid.getPidUnix(RProcess));
							if (RServe.getConnection() == null) {
								UnixProcessManager.sendSigIntToProcessTree(RProcess);
							} else {
								System.out.print("Please change to the native connection to interrupt a running R script!");
							}

							// System.out.println(rPid.getPidWindows(RProcess));
						} else if (interpreterSelection.equals("shell")) {

							// Process p =
							// Runtime.getRuntime().exec("kill -INT " +
							// shellPid.getPidUnix(nativeShellProcess));

							UnixProcessManager.sendSigIntToProcessTree(nativeShellProcess);

						}

						else if (interpreterSelection.equals("python")) {

							// Process p =
							// Runtime.getRuntime().exec("kill -INT " +
							// pythonPid.getPidUnix(pythonProcess));
							UnixProcessManager.sendSigIntToProcessTree(pythonProcess);

						}

					}

				}
				/* CTRL+x key event! */
				else if (event.stateMask == SWT.CTRL && event.keyCode == 'x') {
					if (interpreterSelection.equals("shell")) {

						// Process p =
						// Runtime.getRuntime().exec("kill -2 -$PGID " +
						// shellPid.getPidUnix(nativeShellProcess));
						UnixProcessManager.sendSigKillToProcessTree(nativeShellProcess);
						setNativeShellProcess(null);
					}

					else if (interpreterSelection.equals("python")) {

						// Process p = Runtime.getRuntime().exec("kill -TERM " +
						// pythonPid.getPidUnix(pythonProcess));
						UnixProcessManager.sendSigKillToProcessTree(pythonProcess);
						setPythonProcess(null);

					}
				}

				else {
					commandBack = list.size();
					position = -1;
				}
			}
		});

		IPageSite pageSite = page.getSite();
		IWorkbenchPage workbenchPage = pageSite.getPage();
		IViewPart viewPart = workbenchPage.findView(IConsoleConstants.ID_CONSOLE_VIEW);
		IViewSite viewSite = viewPart.getViewSite();
		actionBars = viewSite.getActionBars();

		toolBarManager = actionBars.getToolBarManager();

		// toolBarManager.removeAll();

		// At startup only output console is activated!
		styledText.setEditable(false);
		toolBarManager.add(new ConsoleRShellAction());
		toolBarManager.add(new ConsoleNativeShellAction());
		toolBarManager.add(new ConsolePythonShellAction());
		toolBarManager.add(new ConsoleCustomActions(this));
		item = new PlaceholderLabel().getPlaceholderLabel();
		ia = new ConsoleInterpreterAction(this);
		toolBarManager.add(ia);
		toolBarManager.add(item);

		in = new BufferedReader(isr);

		ioc.clearConsole();

	}

	public void processCommand() {

		IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
		consoleEncoding = store.getString("Console_Encoding");
		String shellArgs = store.getString("shell_arguments");
		try {
			if (ApplicationWorkbenchWindowAdvisor.getOS().equals("Windows")) {
				// process = Runtime.getRuntime().exec("cmd");
				/*
				 * ProcessBuilder can redirect the error stream! No second
				 * thread needed!
				 */
				ProcessBuilder builder = new ProcessBuilder("cmd");
				// System.out.println(builder.environment());
				builder.redirectErrorStream(true);
				nativeShellProcess = builder.start();
				nativeShellprocessThread = new Thread(new NativeProcessGrabber());
				nativeShellprocessThread.start();
				/* Start shell with arguments! */
				ConsolePageParticipant.pipeInputToConsole(shellArgs, true, true);
				shellPid = new Pid();
				// System.out.println("Process Id is: " +
				// shellPid.getPidWindows(nativeShellProcess));

			} else if (ApplicationWorkbenchWindowAdvisor.getOS().equals("Linux")) {
				// Some Useful commands: export TERM=xterm; top -b; ssh -tt
				// gksudo 'apt-get --yes install abiword'
				List<String> args = new ArrayList<String>();
				args.add("/bin/sh");
				args.add("-i");
				ProcessBuilder builder = new ProcessBuilder(args);
				builder.redirectErrorStream(true);
				nativeShellProcess = builder.start();
				nativeShellprocessThread = new Thread(new NativeProcessGrabber());
				nativeShellprocessThread.start();
				/* Start shell with arguments! */
				ConsolePageParticipant.pipeInputToConsole(shellArgs, true, true);
				shellPid = new Pid();
				// System.out.println("Process Id is: " +
				// shellPid.getPidUnix(nativeShellProcess));
			}

			else if (ApplicationWorkbenchWindowAdvisor.getOS().equals("Mac")) {
				List<String> args = new ArrayList<String>();
				args.add("/bin/sh");
				args.add("-i");
				ProcessBuilder builder = new ProcessBuilder(args);
				builder.redirectErrorStream(true);
				nativeShellProcess = builder.start();
				nativeShellprocessThread = new Thread(new NativeProcessGrabber());
				nativeShellprocessThread.start();
				/* Start shell with arguments! */
				ConsolePageParticipant.pipeInputToConsole(shellArgs, true, false);
				shellPid = new Pid();
				// System.out.println("Process Id is: " +
				// shellPid.getPidUnix(nativeShellProcess));
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void processPythonCommand() {

		IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
		consoleEncoding = store.getString("Console_Encoding");
		try {
			if (ApplicationWorkbenchWindowAdvisor.getOS().equals("Windows")) {
				// process = Runtime.getRuntime().exec("cmd");
				/*
				 * ProcessBuilder can redirect the error stream! No second
				 * thread needed!
				 */

				String cPython = store.getString("python_pipe_path");
				List<String> args = new ArrayList<String>();
				args.add(cPython + "/python");
				args.add("-i");
				ProcessBuilder builder = new ProcessBuilder(args);
				builder.environment().put("Path", builder.environment().get("Path") + cPython);
				builder.redirectErrorStream(true);
				pythonProcess = builder.start();
				pythonProcessThread = new Thread(new PythonProcessGrabber());
				pythonProcessThread.start();
				pythonPid = new Pid();
				// System.out.println("Process Id is: " +
				// pythonPid.getPidWindows(pythonProcess));

			} else if (ApplicationWorkbenchWindowAdvisor.getOS().equals("Linux")) {

				String cPython = store.getString("python_pipe_path");
				List<String> args = new ArrayList<String>();
				args.add(cPython + "/python");
				args.add("-i");
				ProcessBuilder builder = new ProcessBuilder(args);
				builder.redirectErrorStream(true);
				pythonProcess = builder.start();
				pythonProcessThread = new Thread(new PythonProcessGrabber());
				pythonProcessThread.start();
				pythonPid = new Pid();
				// System.out.println("Process Id is: " +
				// pythonPid.getPidUnix(pythonProcess));
			}

			else if (ApplicationWorkbenchWindowAdvisor.getOS().equals("Mac")) {
				String cPython = store.getString("python_pipe_path");
				List<String> args = new ArrayList<String>();
				args.add(cPython + "/python");
				args.add("-i");
				ProcessBuilder builder = new ProcessBuilder(args);
				builder.redirectErrorStream(true);
				pythonProcess = builder.start();
				pythonProcessThread = new Thread(new PythonProcessGrabber());
				pythonProcessThread.start();
				pythonPid = new Pid();
				// System.out.println("Process Id is: " +
				// pythonPid.getPidUnix(pythonProcess));
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
			Bio7Dialog.message("Interpreter not available!\n\nPlease adjust the path to the interpreter in the Bio7 preferences!");
		}

	}

	/*
	 * If R native is started first we have to make some default settings from
	 * the preferences!
	 */
	public void rOptions() {
		pipeInputToConsole("options(max.print=5000)");

		/*
		 * Set the default install location for the add on packages!
		 */
		IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
		String rPackages = store.getString("InstallLocation");

		if (ApplicationWorkbenchWindowAdvisor.getOS().equals("Windows")) {

			rPackages = rPackages.replace("\\", "/");

		}
		pipeInputToConsole(".libPaths(\"" + rPackages + "\")");
	}

	public void processRCommand() {

		IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
		consoleEncoding = store.getString("Console_Encoding");
		try {
			if (ApplicationWorkbenchWindowAdvisor.getOS().equals("Windows")) {
				// process = Runtime.getRuntime().exec("cmd");
				/*
				 * ProcessBuilder can redirect the error stream! No second
				 * thread needed!
				 */

				String rPath = store.getString(PreferenceConstants.PATH_R);

				List<String> args = new ArrayList<String>();
				args.add(rPath + "/bin/x64/rterm");
				args.add("--ess");
				ProcessBuilder builder = new ProcessBuilder(args);
				builder.redirectErrorStream(true);
				RProcess = builder.start();
				RprocessThread = new Thread(new RProcessGrabber());
				RprocessThread.start();
				rPid = new Pid();
				// System.out.println("Process Id is: " +
				// rPid.getPidWindows(RProcess));
				rOptions();

			} else if (ApplicationWorkbenchWindowAdvisor.getOS().equals("Linux")) {

				String rPath = store.getString(PreferenceConstants.PATH_R);

				List<String> args = new ArrayList<String>();
				args.add(rPath + "/bin/R");
				args.add("--interactive");

				ProcessBuilder builder = new ProcessBuilder(args);
				builder.redirectErrorStream(true);
				RProcess = builder.start();
				RprocessThread = new Thread(new RProcessGrabber());
				RprocessThread.start();
				rPid = new Pid();
				// System.out.println("Process Id is: " +
				// rPid.getPidUnix(RProcess));
				rOptions();
			}

			else if (ApplicationWorkbenchWindowAdvisor.getOS().equals("Mac")) {
				String rPath = store.getString(PreferenceConstants.PATH_R);

				List<String> args = new ArrayList<String>();
				args.add(rPath + "/bin/R");
				args.add("--interactive");

				ProcessBuilder builder = new ProcessBuilder(args);
				builder.redirectErrorStream(true);
				RProcess = builder.start();
				RprocessThread = new Thread(new RProcessGrabber());
				RprocessThread.start();
				rPid = new Pid();
				// System.out.println("Process Id is: " +
				// rPid.getPidUnix(RProcess));
				rOptions();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	class PythonProcessGrabber implements Runnable {
		final InputStream inp = pythonProcess.getInputStream();

		public void run() {
			// setPriority(Thread.MAX_PRIORITY);
			if (interpreterSelection.equals("Python")) {
				try {

					InputStreamReader inr = new InputStreamReader(inp, Charset.forName(consoleEncoding));

					int ch;
					while ((ch = inr.read()) != -1) {

						System.out.print((char) ch);

					}
					// consoleOutputChar.delete(0,
					// consoleOutputChar.length()-1);
				} catch (IOException e) {
					// e.printStackTrace();
					System.out.println("Stream closed!");
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
	}

	class NativeProcessGrabber implements Runnable {
		final InputStream inp = nativeShellProcess.getInputStream();

		public void run() {
			// setPriority(Thread.MAX_PRIORITY);
			if (interpreterSelection.equals("shell")) {
				try {

					InputStreamReader inr = new InputStreamReader(inp, Charset.forName(consoleEncoding));

					int ch;
					while ((ch = inr.read()) != -1) {

						System.out.print((char) ch);

					}
					// consoleOutputChar.delete(0,
					// consoleOutputChar.length()-1);
				} catch (IOException e) {
					// e.printStackTrace();
					System.out.println("Stream closed!");
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
	}

	class RProcessGrabber implements Runnable {
		final InputStream inp = RProcess.getInputStream();

		public void run() {
			// setPriority(Thread.MAX_PRIORITY);
			if (interpreterSelection.equals("R")) {
				try {

					InputStreamReader inr = new InputStreamReader(inp, Charset.forName(consoleEncoding));

					int ch;
					while ((ch = inr.read()) != -1) {

						if (Bio7Dialog.getOS().equals("Windows")) {

							System.out.print((char) ch);
						}
						/*
						 * Under Linux and MacOSX commands are echoed in ASCII
						 * and evtl. ANSI control characters.We cannot avoid the
						 * echo but can delete some ASCII characters for a
						 * better output!
						 */
						else {
							if (ch != 8 && ch != 12 && ch != 13) {
								System.out.print((char) ch);
							}
						}

					}

				} catch (IOException e) {
					// e.printStackTrace();
					System.out.println("Stream closed!");
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
	}

	class ProcessGrabber implements Runnable {
		final InputStream inp = shellProcess.getInputStream();

		public void run() {
			// setPriority(Thread.MAX_PRIORITY);

			try {

				InputStreamReader inr = new InputStreamReader(inp, Charset.forName(consoleEncoding));

				int ch;
				while ((ch = inr.read()) != -1) {

					System.out.print((char) ch);

				}
				// consoleOutputChar.delete(0, consoleOutputChar.length()-1);
			} catch (IOException e) {
				// e.printStackTrace();
				System.out.println("Stream closed!");
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

	@Override
	public void dispose() {
		ioc.getInputStream().appendData(System.getProperty("line.separator"));
		interpreterSelection = "-";
		/*
		 * Use a thread to ensure that the other threads will be destroyed -
		 * maybee not necessary!
		 */
		new Thread() {
			@Override
			public void run() {
				if (shellProcess != null) {
					shellProcess.destroy();
				}

				if (RProcess != null) {
					RProcess.destroy();
				}

				if (nativeShellProcess != null) {
					nativeShellProcess.destroy();
				}

				if (pythonProcess != null) {
					pythonProcess.destroy();
				}

				if (initializationThread != null) {
					initializationThread.interrupt();
				}
				if (processThread != null) {
					processThread.interrupt();
				}

				if (RprocessThread != null) {
					RprocessThread.interrupt();
				}

				if (nativeShellprocessThread != null) {
					nativeShellprocessThread.interrupt();
				}

				if (pythonProcessThread != null) {
					pythonProcessThread.interrupt();
				}
				in = null;

			}
		}.start();

	}

	@Override
	public void activated() {
		ioc.getDocument().addDocumentListener(eobHandler);

	}

	@Override
	public void deactivated() {
		ioc.getDocument().removeDocumentListener(eobHandler);

	}

	private void interpret() {

		while (!Thread.interrupted() && runThread == true) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e1) {
				runThread = false;

				e1.printStackTrace();
			}
			switch (interpreterSelection) {
			case "jython":

				try {
					if (in != null) {
						System.out.print(">>>");
						input = in.readLine();

					}
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				if (input == null)
					break;
				else {
					if (ignore == false) {
						if (input != null && input.equals("") == false) {
							list.add(input);
						}
					}

					ScriptEngine gs = ScriptEngineConnection.getScriptingEnginePython();

					try {
						gs.eval(input);
					} catch (ScriptException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					ignore = false;
					System.out.flush();

				}
				break;
			case "beanshell":

				try {
					if (in != null) {
						System.out.print("bsh %");
						input = in.readLine();
					}
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				if (input == null)
					break;
				else {
					if (ignore == false) {
						if (input != null && input.equals("") == false) {
							list.add(input);
						}
					}
					ScriptEngine gs = ScriptEngineConnection.getScriptingEngineBeanShell();

					try {
						gs.eval(input);
					} catch (ScriptException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					ignore = false;
					System.out.flush();

				}
				break;
			case "groovy":

				try {
					if (in != null) {
						System.out.print("groovy>");
						input = in.readLine();
					}
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				if (input == null)
					break;
				else {
					if (ignore == false) {
						if (input != null && input.equals("") == false) {
							list.add(input);
						}
					}
					ScriptEngine gs = ScriptEngineConnection.getScriptingEngineGroovy();

					try {
						gs.eval(input);
					} catch (ScriptException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					ignore = false;
					// System.out.println();
					System.out.flush();

				}
				break;
			/*
			 * case "r": System.out.print("System>");
			 * 
			 * input = in.readLine(); if (input == null||input.equals(""))
			 * break; else { if(RServe.isAliveDialog()){ RServe.printJob(input);
			 * 
			 * }
			 * 
			 * System.out.flush();
			 * 
			 * } break;
			 */
			case "shell":

				try {
					if (in != null) {
						// System.out.print("Shell>");
						if (ApplicationWorkbenchWindowAdvisor.getOS().equals("Windows")) {
							input = in.readLine();
						} else {
							// System.out.print("$ ");
							input = in.readLine();
						}
					}
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				if (input == null)
					break;
				else {
					if (ignore == false) {
						if (input != null && input.equals("") == false) {
							list.add(input);
						}
					}
					if (nativeShellProcess != null) {
						final OutputStream os = nativeShellProcess.getOutputStream();
						final OutputStreamWriter osw = new OutputStreamWriter(os);
						final BufferedWriter bw = new BufferedWriter(osw, 100);

						try {
							bw.write(input);

							bw.newLine();
							// If necessary: bw.write("\r\n");
							os.flush();
							bw.flush();
							// bw.close();
							System.out.flush();
						} catch (IOException e) {
							System.err.println("");
						}

					}

					/*
					 * Process p = RConnectionJob.getProc(); // Write to the
					 * output!
					 */

					ignore = false;
					System.out.flush();

				}

				break;
			case "java":

				try {
					if (in != null) {

						if (ApplicationWorkbenchWindowAdvisor.getOS().equals("Windows")) {
							input = in.readLine();
						} else {
							// System.out.print("$ ");
							input = in.readLine();
						}
					}
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				if (input == null)
					break;
				else {
					if (ignore == false) {
						if (input != null && input.equals("") == false) {
							list.add(input);
						}
					}
					Process jprocess = RunJavaClassFile.getJavaProcess();
					if (jprocess != null) {
						final OutputStream os = jprocess.getOutputStream();
						final OutputStreamWriter osw = new OutputStreamWriter(os);
						final BufferedWriter bw = new BufferedWriter(osw, 100);

						try {
							bw.write(input);

							bw.newLine();
							// If necessary: bw.write("\r\n");
							os.flush();
							bw.flush();
							// bw.close();
							System.out.flush();
						} catch (IOException e) {
							System.err.println("");
						}

					}

					/*
					 * Process p = RConnectionJob.getProc(); // Write to the
					 * output!
					 */

					ignore = false;
					System.out.flush();

				}

				break;
			case "Python":

				try {
					if (in != null) {
						// System.out.print("Shell>");
						input = in.readLine();
					}
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				if (input == null)
					break;
				else {
					if (ignore == false) {
						if (input != null && input.equals("") == false) {
							list.add(input);
						}
					}
					if (pythonProcess != null) {
						final OutputStream os = pythonProcess.getOutputStream();
						final OutputStreamWriter osw = new OutputStreamWriter(os);
						final BufferedWriter bw = new BufferedWriter(osw, 100);

						try {
							bw.write(input);

							bw.newLine();
							// If necessary: bw.write("\r\n");
							os.flush();
							bw.flush();
							// bw.close();
							System.out.flush();
						} catch (IOException e) {
							System.err.println("");
						}

					}

					/*
					 * Process p = RConnectionJob.getProc(); // Write to the
					 * output!
					 */

					ignore = false;
					System.out.flush();

				}

				break;
			case "R":

				try {
					if (in != null) {
						// System.out.print("Shell>");
						input = in.readLine();
					}
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				if (input == null)
					break;
				else {
					if (ignore == false) {
						if (input != null && input.equals("") == false) {
							if (addToHistoryConsole) {
								list.add(input);
							}

						}
					}
					if (RProcess != null) {
						final OutputStream os = RProcess.getOutputStream();
						final OutputStreamWriter osw = new OutputStreamWriter(os);
						final BufferedWriter bw = new BufferedWriter(osw, 100);

						try {
							// ConsolePageParticipant.getConsolePageParticipantInstance().ioc.get
							bw.write(input);

							if (lineSeperatorConsole) {
								bw.newLine();
							}
							// If necessary: bw.write("\r\n");
							os.flush();
							bw.flush();
							// bw.close();
							System.out.flush();
						} catch (IOException e) {
							System.err.println("");
						}

					}

					/*
					 * Process p = RConnectionJob.getProc(); // Write to the
					 * output!
					 */

					ignore = false;
					System.out.flush();

				}

				break;

			case "-":

				System.out.flush();

				break;

			default:
				System.out.flush();
				break;
			}

			// System.out.flush();

		}
	}

	private IDocumentListener eobHandler = new IDocumentListener() {

		public void documentAboutToBeChanged(DocumentEvent event) {
		}

		/**
		 * Ensure that caret moves to End of Buffer when the REPL prints it
		 * prompt
		 */
		public void documentChanged(DocumentEvent event) {
			IDocument doc = event.getDocument();
			TextConsoleViewer viewer = ((page instanceof TextConsolePage) ? ((TextConsolePage) page).getViewer() : null);
			if (doc != null) {
				try {
					int textLen = (event.getText() != null ? event.getText().length() : 0);
					int doclen = doc.getLength() - 1;
					// move cursor to end of repl-prompt when necessary
					if (textLen > 0 && viewer.getTextWidget().getCaretOffset() < doclen) {
						IRegion reg = doc.getLineInformationOfOffset(doclen);

						// use end offset excluding any eol characters
						viewer.getTextWidget().setCaretOffset(reg.getOffset() + reg.getLength());

						/*
						 * IRegion reg = doc.getLineInformationOfOffset(doclen);
						 * String selectionConsole =
						 * ConsolePageParticipant.getInterpreterSelection();
						 * 
						 * if (selectionConsole.equals("R")) { String line =
						 * doc.get(reg.getOffset(), reg.getLength());
						 * 
						 * if(line.contains("R#")){ int lineNumber
						 * =line.indexOf("#");
						 * 
						 * int
						 * number=Integer.parseInt(line.replaceAll("\\D+",""));
						 * System.out.println("Line Number: "+number);
						 * 
						 * }
						 * 
						 * }
						 */

					}
					// System.out.println(textLen);

				} catch (BadLocationException e) {
				}
			}

		}
	};
	private String selected;

	public IOConsole getIoc() {
		return ioc;
	}

	public static String getInterpreterSelection() {
		return interpreterSelection;
	}

	public static void setInterpreterSelection(String interpreterSelection) {
		interpreterSelection = interpreterSelection;
	}

	private void fastSaveAndReloadRWorkspace() {
		IPreferenceStore store = null;
		String path = null;
		try {
			store = Bio7Plugin.getDefault().getPreferenceStore();
		} catch (RuntimeException ex) {

			ex.printStackTrace();
		}
		if (store != null) {
			path = store.getString(PreferenceConstants.P_TEMP_R);
		}

		selected = path + "\\tempCurrent";

		File dir = new File(path);
		if (dir.canWrite()) {

			if (ApplicationWorkbenchWindowAdvisor.getOS().equals("Windows")) {
				selected = path + "\\tempCurrent";
				selected = selected.replace("\\", "\\\\");
			}
			/* For Linux and MacOSX! */
			else {
				selected = path + "/tempCurrent";
			}

			if (selected != null) {

				final String save = "try(save.image(file =\"" + selected + ".RData" + "\", version = NULL, ascii = FALSE))";
				if (RState.isBusy() == false) {
					RState.setBusy(true);

					Job job = new Job("Transfer data to native started R!") {
						@Override
						protected IStatus run(IProgressMonitor monitor) {
							monitor.beginTask("Transfer data to native started R ...", IProgressMonitor.UNKNOWN);
							RConnection con = RServe.getConnection();
							try {
								con.voidEval(save);
							} catch (RserveException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							String fileR = selected + ".RData";
							if (interpreterSelection.equals("R")) {

								ConsolePageParticipant.pipeInputToConsole("load(file =\"" + fileR + "\");", true, true);

							} else {
								Bio7Dialog.message("Please start the \"Native R\" shell in the Bio7 console!");
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
								RState.setBusy(false);
							}
						}
					});

					job.schedule();

					// LineFeed.

				} else {
					Bio7Dialog.message("Rserve is busy!");
				}
			}
		} else {
			Bio7Dialog.message("Directory is not writable!");
		}
	}

	public static ConsolePageParticipant getConsolePageParticipantInstance() {
		return ConsolePageParticipantInstance;
	}

	/* Add toolbar actions to the console view! */
	public void setRDebugToolbarActions() {
		StartBio7Utils utils = StartBio7Utils.getConsoleInstance();

		/* Add the debug actions dynamically! */
		IToolBarManager tm = toolBarManager;
		/* Remove the distance label! */
		tm.remove("PlaceholderLabel");
		IContributionItem[] its = toolBarManager.getItems();
		boolean exist = false;
		for (int i = 0; i < its.length; i++) {

			if (its[i].getId() != null) {
				/* Control if the items exists already! */
				if (its[i].getId().equals("Stop")) {
					tm.add(item);
					exist = true;
				}

			}

		}
		if (exist == false) {
			tm.add(new DebugRScript());
			tm.add(new DebugStopAction());
			tm.add(new DebugNextAction());
			tm.add(new DebugContinueAction());
			tm.add(new DebugStepIntoAction());
			tm.add(new DebugStepFinishAction());
			tm.add(new DebugInfoAction());
			/* Add the distance label again! */
			tm.add(item);
			actionBars.updateActionBars();
		}
		/* Remove all toolbar actions from the console view! */
		/*
		 * if (utils != null) { utils.cons.clear(); }
		 */
	}

	public void deleteDebugToolbarActions() {

		toolBarManager.remove("Debug");
		toolBarManager.remove("Stop");
		toolBarManager.remove("Next");
		toolBarManager.remove("Continue");
		toolBarManager.remove("StepInto");
		toolBarManager.remove("Finish");
		toolBarManager.remove("DebugInfo");

		actionBars.updateActionBars();

	}

	public Process getShellProcess() {
		return shellProcess;
	}

	public Process getNativeShellProcess() {
		return nativeShellProcess;
	}

	public void setNativeShellProcess(Process nativeShellProcess) {
		this.nativeShellProcess = nativeShellProcess;
	}

	public Process getRProcess() {
		return RProcess;
	}

	public void setRProcess(Process rProcess) {
		RProcess = rProcess;
	}

	public void setPythonProcess(Process pythonProcess) {
		this.pythonProcess = pythonProcess;
	}

	public Process getPythonProcess() {
		return pythonProcess;
	}

	public Pid getrPid() {
		return rPid;
	}

	public void pipeToRConsole(String command) {
		String selectionConsole = ConsolePageParticipant.getInterpreterSelection();

		if (selectionConsole.equals("R")) {
			ConsolePageParticipant con = ConsolePageParticipant.getConsolePageParticipantInstance();
			Process rProcess = con.getRProcess();
			if (rProcess != null) {
				final OutputStream os = rProcess.getOutputStream();
				final OutputStreamWriter osw = new OutputStreamWriter(os);
				final BufferedWriter bw = new BufferedWriter(osw, 100);

				try {
					bw.write(command);
					bw.newLine();

					// If necessary: bw.write("\r\n");
					os.flush();
					bw.flush();
					// bw.close();
					System.out.flush();
				} catch (IOException e) {
					System.err.println("");
				}
			}

		}
	}

	/**
	 * Sends sequence of two chars(codes 5 and 3) to a process output stream
	 * Source from:
	 * https://github.com/joewalnes/idea-community/blob/master/platform
	 * /platform-impl/src/com/intellij/execution/process/RunnerMediator.java
	 * 
	 * Copyright 2000-2010 JetBrains s.r.o.
	 *
	 * Licensed under the Apache License, Version 2.0 (the "License"); you may
	 * not use this file except in compliance with the License. You may obtain a
	 * copy of the License at
	 *
	 * http://www.apache.org/licenses/LICENSE-2.0
	 *
	 * Unless required by applicable law or agreed to in writing, software
	 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
	 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
	 * License for the specific language governing permissions and limitations
	 * under the License.
	 */

	private static void sendCtrlBreakThroughStream(Process process) {
		if (process != null) {
			OutputStream os = process.getOutputStream();
			PrintWriter pw = new PrintWriter(os);
			try {
				pw.print(IAC);
				pw.print(BRK);
				pw.flush();
			} finally {
				pw.close();
			}
		}
	}
}
