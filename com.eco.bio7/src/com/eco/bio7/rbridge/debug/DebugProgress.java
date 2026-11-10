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
package com.eco.bio7.rbridge.debug;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.nebula.widgets.grid.Grid;
import org.eclipse.nebula.widgets.grid.GridItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.console.ConsolePageParticipant;

/*
 * DebugProgress handles stepping through an R debugging session.
 * 
 * This class is used by the debug toolbar actions (Next, Continue,
 * Step Into, Finish) to send a debug command (n/s/f/c) to R while
 * it is paused in Browse mode, and then capture the resulting output
 * to determine which line R has stepped to.
 * 
 * The communication with R uses a socket-based capture mechanism:
 * 
 * 1. A server socket is opened in R (.b7sc) and output is redirected
 *    to it via sink(). The variable name is kept short to stay under
 *    the 80-character rterm console buffer width limit.
 * 2. The debug command (e.g. "n") is sent while sink is active, so
 *    R's debug stepping output (containing the .R#linenum: pattern)
 *    is captured to the socket instead of the console.
 * 3. After the command, variable state is captured via print(ls.str()),
 *    separated by a "Bio7...Debug...Start..." marker line.
 * 4. Sink and socket are closed, and R returns to Browse mode.
 * 
 * On the Java side:
 * 
 * 1. A client socket connects to R's server socket.
 * 2. All lines are read and scanned for the .R#linenum: pattern
 *    which indicates the line R has stepped to in the temp file.
 * 3. The line number is adjusted by -2 to map back to the original
 *    source (compensating for the function wrapper and browser()
 *    injection done by DebugRScript).
 * 4. The editor cursor and debug marker are moved to the new line.
 * 5. Variable data after the separator is parsed and displayed in
 *    the debug variables grid (name, type, value).
 * 6. The current stepping position is shown in the debug text view
 *    as a clean "Line N: <code>" message.
 * 
 * All work runs on a background Job thread to avoid blocking the UI.
 * SWT widget updates (grid items, text view, editor markers) are
 * dispatched to the UI thread via Display.syncExec/asyncExec.
 * 
 * Command order is critical: socket must be created BEFORE sink,
 * and sink must be active WHEN the debug command runs, otherwise
 * the stepping output is lost to the console instead of captured.
 * R blocks at socketConnection(server=TRUE) until Java connects,
 * so all subsequent commands queue in stdin and execute in order
 * once the socket handshake completes.
 */
public class DebugProgress {

	private int line = 1;
	private Socket debugSocket;
	protected Grid debugGrid;
	protected StyledText styledTxT;
	private String outputLine;
	/* Stores the debug stepping line containing .R#linenum: */
	private String debugStepLine = null;

	/* Embedded R: R is still in Browse mode after the step, and it stopped at browser() again */
	private volatile boolean stillBrowsing;
	private volatile boolean calledFrom;

	/* Session state flags — static so they are shared across all action instances */
	private static boolean debugSessionActive = false;
	private static boolean stepping = false;

	/* Called by DebugRScript when a debug session starts */
	public static void startSession() {
		debugSessionActive = true;
		stepping = false;
	}

	/* Called to end the debug session */
	public static void endSession() {
		debugSessionActive = false;
		stepping = false;
	}

	/* Check if a debug session is active */
	public static boolean isSessionActive() {
		return debugSessionActive;
	}

	public void progress(final String command) {

		/* Guard: ignore if no active debug session */
		if (!debugSessionActive) {
			Bio7Dialog.message("No active debug session.\n\n"
					+ "Set a breakpoint and click Debug to start.");
			return;
		}

		/* Guard: ignore if a step is already in progress */
		if (stepping) {
			return;
		}

		stepping = true;

		final String selectionConsole = ConsolePageParticipant.getInterpreterSelection();
		final IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
		final int port = store.getInt("R_DEBUG_PORT");
		/* The name of the debugged script (for the console instead of the temporary file) */
		final String fileName = activeEditorName();

		if (selectionConsole.equals("R")) {

			Job debugStepJob = new Job("R Debug Step") {
				@Override
				protected IStatus run(IProgressMonitor monitor) {

					try {
						ConsolePageParticipant con = ConsolePageParticipant.getConsolePageParticipantInstance();

						/* Reset the debug step line */
						debugStepLine = null;
						stillBrowsing = false;
						calledFrom = false;

						/*
						 * Command order:
						 * 1. Socket (R blocks at server=TRUE until Java connects)
						 * 2. Sink (queued, runs after socket connects)
						 * 3. Debug command (n/s/f/c — captured by sink)
						 * 4. Capture variables
						 * 5. Close sink + socket
						 */

						if (com.eco.bio7.rbridge.backend.Backends.isEmbeddedRunning()) {
							/*
							 * Embedded R: the output of R comes directly from its console (no socket,
							 * no sink): the step (shown in the console), then the variables
							 */
							String stepOutput = com.eco.bio7.rbridge.backend.EmbeddedR.consoleCapture(command, 120000, true,
									text -> shortenDebugLines(text, fileName));
							String variables = com.eco.bio7.rbridge.backend.EmbeddedR.isBrowsing()
									? com.eco.bio7.rbridge.backend.EmbeddedR.consoleCapture("print(ls.str(), max.level = 0)", 20000, false)
									: "";
							deleteSpreadSheet();
							stillBrowsing = com.eco.bio7.rbridge.backend.EmbeddedR.isBrowsing();
							calledFrom = stepOutput != null && stepOutput.contains("Called from:");
							processDebugOutput(new BufferedReader(new java.io.StringReader(
									(stepOutput == null ? "" : stepOutput) + "\nBio7...Debug...Start...\n" + (variables == null ? "" : variables))));
						} else {
							/* 1. Create socket */
							con.pipeToRConsole(".b7sc <- socketConnection(port=" + port + ",server=TRUE,timeout=10)");

							/* 2. Sink */
							con.pipeToRConsole("sink(.b7sc)");

							/* 3. Debug command */
							con.pipeToRConsole(command);

							/* 4. Separator */
							con.pipeToRConsole("writeLines(\"Bio7...Debug...Start...\")");

							/* 5. Variables */
							con.pipeToRConsole("print(ls.str(), max.level = 0)");

							/* 6. Close sink */
							con.pipeToRConsole("sink()");

							/* 7. Close socket */
							con.pipeToRConsole("close(.b7sc)");

							/* 8. Newline */
							con.pipeToRConsole("writeLines(\"\")");

							try {
								Thread.sleep(200);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}

							/* Clear the debug spreadsheet! */
							deleteSpreadSheet();

							/* Read the R socket data with Java! */
							javaReadFromRSocket(port, command);
						}

						/* Update editor on UI thread */
						Display.getDefault().asyncExec(new Runnable() {
							public void run() {
								if (debugStepLine != null) {
									updateEditor(debugStepLine);
								} else if (stillBrowsing) {
									/*
									 * Embedded R stopped again at a breakpoint (browser(): "Called from:", e.g., the
									 * next round of a loop) or waits for input: the session continues
									 */
									printToDebugTextView(calledFrom ? "\u25B6 Breakpoint reached again" : "\u25B6 R waits in Browse mode");
								} else {
									/*
									 * No debug stepping line received.
									 * R likely hit an error and exited Browse mode,
									 * or the script has finished executing.
									 */
									endSession();
									ConsolePageParticipant c = ConsolePageParticipant.getConsolePageParticipantInstance();
									c.pipeToRConsole("options(prompt=\"> \")");
									Bio7Dialog.message("Debug session ended.\n\n"
											+ "R may have exited Browse mode due to an error\n"
											+ "or the script has finished executing.\n\n"
											+ "Check the R console for details.");

									/* Clean up the debug marker in the editor */
									try {
										IEditorPart edit = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
										IResource res = (IResource) edit.getEditorInput().getAdapter(IResource.class);
										if (res != null) {
											res.deleteMarkers("com.eco.bio7.reditor.debugrulermark", false, IResource.DEPTH_ZERO);
										}
									} catch (CoreException e) {
										e.printStackTrace();
									}
								}
							}
						});

					} finally {
						/* Always allow the next step, even if something went wrong */
						stepping = false;
					}

					return Status.OK_STATUS;
				}
			};
			debugStepJob.setUser(false);
			debugStepJob.setSystem(true);
			debugStepJob.schedule();

		} else {
			stepping = false;
			Bio7Dialog.message("Please start the \"Native R\" shell in the Bio7 console!");
		}
	}

	private void updateEditor(String debugLine) {

		IEditorPart edit = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();

		ITextEditor editor = (ITextEditor) edit;
		IDocumentProvider dp = editor.getDocumentProvider();
		IDocument doc = dp.getDocument(editor.getEditorInput());

		if (debugLine != null && debugLine.length() > 0) {

			/* Extract the line number between .R# and : */
			Pattern p = Pattern.compile(".R#(.*?):");

			Matcher m = p.matcher(debugLine);
			if (m.find()) {
				int temp = 0;
				try {
					temp = Integer.parseInt(m.group(1));
				} catch (NumberFormatException e1) {
					/* Ignore parse errors */
				}

				/*
				 * Offset: temp file has 2 extra lines vs original:
				 *   +1 function wrapper
				 *   +1 injected browser()
				 */
				if (temp > 2) {
					temp = temp - 2;
				}

				int lines = doc.getNumberOfLines();
				if (temp > 0 && temp <= lines) {
					line = temp;
					IRegion reg = null;
					try {
						reg = doc.getLineInformation(line - 1);
					} catch (BadLocationException e) {
						e.printStackTrace();
					}

					editor.selectAndReveal(reg.getOffset() + reg.getLength(), 0);

					IResource resource = (IResource) editor.getEditorInput().getAdapter(IResource.class);
					try {
						resource.deleteMarkers("com.eco.bio7.reditor.debugrulermark", false, IResource.DEPTH_ZERO);
					} catch (CoreException e1) {
						e1.printStackTrace();
					}

					IMarker marker;
					try {
						marker = resource.createMarker("com.eco.bio7.reditor.debugrulermark");
						marker.setAttribute(IMarker.CHAR_START, reg.getOffset());
						marker.setAttribute(IMarker.CHAR_END, reg.getOffset() + reg.getLength());
					} catch (CoreException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	private void javaReadFromRSocket(int port, String command) {
		try {

			debugSocket = new Socket();
			debugSocket.connect(new java.net.InetSocketAddress("127.0.0.1", port), 2000);
			debugSocket.setTcpNoDelay(true);
			debugSocket.setSoTimeout(2000);
			BufferedReader input = null;
			try {
				input = new BufferedReader(new InputStreamReader(debugSocket.getInputStream()));
			} catch (IOException e1) {
				e1.printStackTrace();
			}

			processDebugOutput(input);

			input.close();
			debugSocket.close();

		} catch (IOException e) {
			/* Connection failed — R may have exited Browse mode */
		}
	}

	/*
	 * The output of a debug step: the "debug at file.R#n" line (the position in the
	 * editor), after the line Bio7...Debug...Start... the variables (ls.str()).
	 */
	private void processDebugOutput(BufferedReader input) {
		String line;
		int count = 0;
		boolean capture = false;
		boolean printConsole = false;
		deleteDebugTextView();

		/* Pattern to find the debug stepping line: .R#linenum: */
		Pattern debugLinePattern = Pattern.compile(".R#\\d+:");

		/*
		 * Pattern to extract a clean description from the debug line.
		 * e.g. "debug bei /path/file.R#6: cat(A)" -> "Line 4: cat(A)"
		 */
		Pattern codePattern = Pattern.compile(".R#(\\d+):\\s*(.*)");

		try {

			while ((line = input.readLine()) != null) {

				/* Find the debug stepping line */
				Matcher dm = debugLinePattern.matcher(line);
				if (dm.find() && debugStepLine == null) {
					debugStepLine = line;

					/* Print a clean line to the debug text view */
					Matcher cm = codePattern.matcher(line);
					if (cm.find()) {
						try {
							int rawLine = Integer.parseInt(cm.group(1));
							int adjustedLine = (rawLine > 2) ? rawLine - 2 : rawLine;
							String code = cm.group(2).trim();
							printToDebugTextView("\u25B6 Line " + adjustedLine + ": " + code);
						} catch (NumberFormatException e) {
							/* Ignore */
						}
					}
				}

				if (line.startsWith("Bio7...Debug...Start...")) {
					capture = true;
					printConsole = false;
					/* Clear the debug spreadsheet! */
					deleteSpreadSheet();
				}

				if (line.startsWith("debug:") || line.startsWith("debug bei") || line.startsWith("debug at") || line.startsWith("debugging in")) {
					printConsole = true;
				}

				if (capture) {
					/* find ':' at first occurrence! */
					final String sp[] = line.split(":", 2);

					if (sp.length == 2) {
						/* Find whitespace at 4th position! */
						final String sp2[] = sp[1].split("\\s", 4);
						final int idx = count;
						if (debugGrid != null) {
							Display.getDefault().syncExec(new Runnable() {
								public void run() {
									if (debugGrid != null && debugGrid.isDisposed() == false) {
										GridItem it = new GridItem(debugGrid, SWT.NONE, idx);
										it.setText(0, sp[0]);
										if (sp2.length > 3) {
											it.setText(1, sp2[2]);
											it.setText(2, sp2[3]);
										}
									}
								}
							});
						}
					}
				}

				count++;

			}
		} catch (IOException e) {
			/* Socket timeout — normal, means R closed the connection */
		}
	}

	/*
	 * "debug at /tmp/.../file123.R#11: code" -> "debug at Script.R line 9: code": the
	 * script of the editor and its line (as the marker in the editor: the debugged
	 * copy has two more lines, the function wrapper and the browser() call)
	 */
	static String shortenDebugLines(String text, String fileName) {
		Matcher m = Pattern.compile("(debug(?: at| bei)?) \\S+?\\.R#(\\d+):").matcher(text);
		StringBuilder sb = new StringBuilder();
		while (m.find()) {
			int raw = Integer.parseInt(m.group(2));
			int line = raw > 2 ? raw - 2 : raw;
			m.appendReplacement(sb, Matcher.quoteReplacement(m.group(1) + " " + fileName + " line " + line + ":"));
		}
		m.appendTail(sb);
		return sb.toString();
	}

	/* The name of the file of the active editor (UI thread) or "script" */
	private static String activeEditorName() {
		try {
			IEditorPart edit = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
			if (edit != null) {
				return edit.getEditorInput().getName();
			}
		} catch (RuntimeException e) {
			/* No editor */
		}
		return "script";
	}

	private void printToDebugTextView(String line) {
		outputLine = line;
		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {
			public void run() {
				styledTxT = DebugTextView.getStyledText();

				if (styledTxT != null && styledTxT.isDisposed() == false) {
					styledTxT.append(outputLine);
					styledTxT.append("\n");
				}
			}
		});
	}

	private void deleteDebugTextView() {
		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {
			public void run() {
				styledTxT = DebugTextView.getStyledText();

				if (styledTxT != null && styledTxT.isDisposed() == false) {
					styledTxT.setText("");
				}
			}
		});
	}

	private void deleteSpreadSheet() {
		/* Delete all items in the debug spreadsheet! */
		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {
			public void run() {
				debugGrid = DebugVariablesView.getDebugVariablesGrid();

				if (debugGrid != null && debugGrid.isDisposed() == false) {
					int itemCount = debugGrid.getItemCount();
					if (itemCount > 0) {
						debugGrid.remove(0, itemCount - 1);
					}
				}
			}
		});
	}

}