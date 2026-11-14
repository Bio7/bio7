/*******************************************************************************
 * Copyright (c) 2007-2026 M. Austenfeld
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
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
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.console.ConsolePageParticipant;
import com.eco.bio7.rbridge.backend.EmbeddedR;

/*
 * DebugProgress handles stepping through an R debugging session.
 *
 * The debug toolbar actions (Next, Continue, Step Into, Finish, Run to Cursor)
 * send a command (n/s/f/c) to R while it waits in Browse mode, then the stop is
 * shown: the line in the editor, the variables, the watch expressions and the
 * call stack (DebugVariablesView).
 *
 * Embedded R: the output comes from its console (EmbeddedR.consoleCapture); the
 * R helpers of DebugSession (.bio7dbg) give the variables (class, size, a short
 * value), the watch expressions and the call stack in one output, and stop at an
 * error (the frame of the error is browsed).
 *
 * The R console (without the bridge): a server socket in R receives the output
 * of the step (sink) and the variables (ls.str()).
 */
public class DebugProgress {

	private Socket debugSocket;
	/* Stores the debug stepping line containing .R#linenum: */
	private String debugStepLine = null;

	/* Session state flags — static so they are shared across all action instances */
	private static volatile boolean debugSessionActive = false;
	private static volatile boolean stepping = false;

	/* A step of embedded R waits at most this long (e.g., Continue through a long loop) */
	private static final long STEP_TIMEOUT = 24L * 3600 * 1000;
	private static final long STATE_TIMEOUT = 30000;
	private static final int MAX_RUN_TO_CURSOR_STEPS = 20000;

	private static final Pattern DEBUG_LINE = Pattern.compile("\\.R#(\\d+):\\s*(.*)");
	/* The line of the last stop (for the call stack when another frame is shown) */
	private static volatile int lastStopLine;

	private static final Pattern ERROR_LINE =Pattern.compile("@@E@@(\\S*)\\t(.*)");

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
			Bio7Dialog.message("No active debug session.\n\n" + "Set a breakpoint and click Debug to start.");
			return;
		}

		/* Guard: ignore if a step is already in progress */
		if (stepping) {
			return;
		}

		final String selectionConsole = ConsolePageParticipant.getInterpreterSelection();
		final IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
		final int port = store.getInt("R_DEBUG_PORT");
		/* The name of the debugged script (for the console instead of the temporary file) */
		final String fileName = activeEditorName();

		if (com.eco.bio7.rbridge.backend.Backends.isEmbeddedRunning()) {
			runEmbedded("R Debug Step", () -> {
				arm();
				String out = EmbeddedR.consoleCapture(command, STEP_TIMEOUT, true, text -> shortenDebugLines(text, fileName));
				afterEmbeddedStop(out);
			});
			return;
		}

		if (selectionConsole.equals("R")) {
			stepping = true;
			Job debugStepJob = new Job("R Debug Step") {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					try {
						debugStepLine = null;
						ConsolePageParticipant con = ConsolePageParticipant.getConsolePageParticipantInstance();
						/*
						 * Command order: the socket (R blocks at server=TRUE until Java connects), the
						 * sink, the debug command (captured), the variables, then sink and socket closed
						 */
						con.pipeToRConsole(".b7sc <- socketConnection(port=" + port + ",server=TRUE,timeout=10)");
						con.pipeToRConsole("sink(.b7sc)");
						con.pipeToRConsole(command);
						con.pipeToRConsole("writeLines(\"Bio7...Debug...Start...\")");
						con.pipeToRConsole("print(ls.str(), max.level = 0)");
						con.pipeToRConsole("sink()");
						con.pipeToRConsole("close(.b7sc)");
						con.pipeToRConsole("writeLines(\"\")");
						try {
							Thread.sleep(200);
						} catch (InterruptedException e) {
							Thread.currentThread().interrupt();
						}
						javaReadFromRSocket(port);
						final String stepLine = debugStepLine;
						Display.getDefault().asyncExec(() -> {
							if (stepLine != null) {
								Matcher m = DEBUG_LINE.matcher(stepLine);
								if (m.find()) {
									showLine(DebugSession.toSourceLine(Integer.parseInt(m.group(1))));
								}
							} else {
								sessionEnded(null);
							}
						});
					} finally {
						stepping = false;
					}
					return Status.OK_STATUS;
				}
			};
			debugStepJob.setSystem(true);
			debugStepJob.schedule();
		} else {
			Bio7Dialog.message("Please start the \"Native R\" shell in the Bio7 console!");
		}
	}

	/* ------------------------------------------------------------ embedded R */

	/* Runs a debug action of embedded R in a background job (one at a time) */
	/* One action of embedded R at a time (a step, a frame, a hover): false if one runs */
	private static synchronized boolean claim() {
		if (stepping) {
			return false;
		}
		stepping = true;
		return true;
	}

	/**
	 * The text of the hover of a variable in the R editor (see RDebugHover): its
	 * class, dimension and size (the shown variables) and a preview from R (the
	 * first rows of a table, the first values); null if no debug session runs or it
	 * is not a variable of the shown frame. Called in the thread of the hover.
	 */
	public static String hoverText(String name) {
		if (!debugSessionActive) {
			return null;
		}
		String[] v = DebugVariablesView.shownVariable(name);
		if (v == null) {
			return null;
		}
		int frame = DebugVariablesView.currentFrame();
		StringBuilder sb = new StringBuilder();
		sb.append(v[0]).append("  (").append(v[1]).append(", ").append(v[2]).append(", ").append(v[3]).append(")");
		if (frame > 0) {
			sb.append("  frame ").append(frame);
		}
		sb.append("\n\n");
		String preview = null;
		/* Not while a step runs (the hover never waits for R) */
		if (EmbeddedR.isBrowsing() && claim()) {
			try {
				String env = frame <= 0 ? "environment()" : "sys.frame(" + frame + ")";
				String out = EmbeddedR.consoleCapture(".bio7dbg$preview(get(" + DebugSession.rString(name) + ", envir = " + env + "))", 3000, false);
				if (out != null) {
					StringBuilder p = new StringBuilder();
					for (String line : out.split("\\r?\\n")) {
						if (line.startsWith("@@P@@")) {
							p.append(line.substring(5)).append('\n');
						}
					}
					preview = p.length() > 0 ? p.toString() : null;
				}
			} finally {
				stepping = false;
			}
		}
		sb.append(preview != null ? preview : v[4]);
		return sb.toString().stripTrailing();
	}

	private static void runEmbedded(String name, Runnable action) {
		if (!claim()) {
			return;
		}
		Job job = new Job(name) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					action.run();
				} catch (RuntimeException e) {
					e.printStackTrace();
				} finally {
					stepping = false;
				}
				return Status.OK_STATUS;
			}
		};
		job.setSystem(true);
		job.schedule();
	}

	/* The handler of errors stops at the next error (not at errors typed at the Browse prompt) */
	private static void arm() {
		EmbeddedR.consoleCapture(".bio7dbg$arm()", 10000, false);
	}

	/**
	 * Starts the debugged function of embedded R (the copy is sourced, the helpers
	 * defined) and shows the first stop.
	 */
	public static void startEmbedded(String fileName) {
		startSession();
		sessionFileName = fileName;
		EmbeddedR.setConsoleObserver(DebugProgress::consoleTyped);
		runEmbedded("R Debug", () -> {
			clearTextView();
			String out = EmbeddedR.consoleCapture("withCallingHandlers(.bio7debugfunc(), error = .bio7dbg$onError)", STEP_TIMEOUT, true,
					text -> shortenDebugLines(text, fileName));
			afterEmbeddedStop(out);
		});
	}

	/* The name of the debugged script (embedded R) */
	private static volatile String sessionFileName = "script";

	/*
	 * A line typed in the console during a debug session (e.g., n, s, c, f or an
	 * expression at the Browse prompt): the editor line and the variables follow
	 */
	private static void consoleTyped(String line, String out) {
		if (!debugSessionActive || !com.eco.bio7.rbridge.backend.Backends.isEmbeddedRunning()) {
			return;
		}
		/* Many steps (e.g., Return held down): one refresh with the output of the last one */
		consoleStepOutput = out;
		CONSOLE_STEP.schedule();
	}

	/* The output of the last step typed in the console (not shown yet) */
	private static volatile String consoleStepOutput;

	private static final Job CONSOLE_STEP = new Job("R Debug Console Step") {
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			if (!debugSessionActive) {
				return Status.OK_STATUS;
			}
			/* R evaluates the next step already (or more input is queued): its refresh follows */
			if (EmbeddedR.isBusy()) {
				return Status.OK_STATUS;
			}
			/* Another action (a step of the toolbar, a hover) runs: again a little later */
			if (!claim()) {
				schedule(200);
				return Status.OK_STATUS;
			}
			try {
				String out = consoleStepOutput;
				if (out != null) {
					consoleStepOutput = null;
					afterEmbeddedStop(out, true);
				}
			} catch (RuntimeException e) {
				e.printStackTrace();
			} finally {
				stepping = false;
			}
			return Status.OK_STATUS;
		}
	};

	static {
		CONSOLE_STEP.setSystem(true);
	}

	/**
	 * Steps (n) until R reaches the line of the editor (in the current function) or
	 * stops otherwise (an error, the end).
	 */
	public static void runToLine(int targetLine) {
		if (!debugSessionActive || !com.eco.bio7.rbridge.backend.Backends.isEmbeddedRunning()) {
			Bio7Dialog.message("Run to Cursor needs a debug session of embedded R.");
			return;
		}
		final String fileName = activeEditorName();
		runEmbedded("R Debug Run to Cursor", () -> {
			arm();
			StringBuilder all = new StringBuilder();
			for (int i = 0; i < MAX_RUN_TO_CURSOR_STEPS; i++) {
				String out = EmbeddedR.consoleCapture("n", STEP_TIMEOUT, false);
				if (out == null) {
					break;
				}
				all.setLength(0);
				all.append(out);
				if (out.contains("@@E@@") || !EmbeddedR.isBrowsing()) {
					break;
				}
				Matcher m = DEBUG_LINE.matcher(out);
				if (m.find() && DebugSession.toSourceLine(Integer.parseInt(m.group(1))) == targetLine) {
					break;
				}
			}
			printToTextView("▶ Run to line " + targetLine);
			afterEmbeddedStop(all.toString());
		});
	}

	/** Shows the variables of a frame of the call stack (0: the current frame). */
	public static void selectFrame(int frame) {
		if (!debugSessionActive || !EmbeddedR.isBrowsing()) {
			return;
		}
		runEmbedded("R Debug Frame", () -> {
			String env = frame <= 0 ? "environment()" : "sys.frame(" + frame + ")";
			String state = EmbeddedR.consoleCapture(".bio7dbg$state(" + env + ", " + DebugSession.rStrings(DebugSession.watchExpressions()) + ", " + frame + ")",
					STATE_TIMEOUT, false);
			DebugState s = DebugState.parse(state);
			s.stopLine = lastStopLine;
			Display.getDefault().asyncExec(() -> DebugVariablesView.show(s, false));
		});
	}

	/** Evaluates the watch expressions again (e.g., after an expression was added). */
	public static void refreshWatch() {
		selectFrame(DebugVariablesView.currentFrame());
	}

	/** Opens a variable of the shown frame in the table (a vector as one column, other objects as text). */
	public static void openVariable(String name) {
		openVariable(name, "exportTable");
	}

	/** Opens a variable of the shown frame in the table (the hover of the editor): a vector as one column. */
	public static void openVariableInTable(String name) {
		openVariable(name, "exportTable");
	}

	/** Opens a numeric matrix of the shown frame as an image (ImageJ). */
	public static void openVariableAsImage(String name) {
		openVariable(name, "exportImage");
	}

	/* export: the R function of .bio7dbg (exportTable, exportImage) */
	private static void openVariable(String name, String export) {
		if (!debugSessionActive || !EmbeddedR.isBrowsing()) {
			Bio7Dialog.message("A variable can be opened while R waits in a debug session (embedded R).");
			return;
		}
		int frame = DebugVariablesView.currentFrame();
		runEmbedded("R Debug Open Variable", () -> {
			try {
				Path file = Files.createTempFile("bio7var", ".dat");
				file.toFile().deleteOnExit();
				String env = frame <= 0 ? "environment()" : "sys.frame(" + frame + ")";
				String out = EmbeddedR.consoleCapture("cat('@@X@@', .bio7dbg$" + export + "(get(" + DebugSession.rString(name) + ", envir = " + env + "), "
						+ DebugSession.rString(file.toString()) + "), '\\n', sep = '')", STATE_TIMEOUT, false);
				Matcher m = Pattern.compile("@@X@@(\\w+)").matcher(out == null ? "" : out);
				if (m.find() && "none".equals(m.group(1))) {
					Display.getDefault().asyncExec(() -> Bio7Dialog.message("The variable " + name + " is not a numeric matrix (it can't be opened as an image)."));
				} else if (m.find(0)) {
					DebugVariableExport.open(name, m.group(1), file);
				} else {
					String detail = out != null && !out.isBlank() ? ":\n" + out.trim() : " (it may not exist in this frame yet).";
					Display.getDefault().asyncExec(() -> Bio7Dialog.message("The variable " + name + " could not be opened" + detail));
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}

	/*
	 * After a step (or the start) of embedded R: the line (the "debug at" line of
	 * the step, the breakpoint or the error), then the state of the frame
	 */
	private static void afterEmbeddedStop(String out) {
		afterEmbeddedStop(out, false);
	}

	/* quiet: no message if R waits without a new line (e.g., an expression typed at the Browse prompt) */
	private static void afterEmbeddedStop(String out, boolean quiet) {
		String text = out == null ? "" : out;
		boolean browsing = EmbeddedR.isBrowsing();
		int line = -1;
		String code = null;
		String error = null;
		Matcher e = ERROR_LINE.matcher(text);
		if (e.find()) {
			error = e.group(2).trim();
			try {
				line = DebugSession.toSourceLine(Integer.parseInt(e.group(1)));
			} catch (NumberFormatException x) {
				line = -1;
			}
		} else {
			/* The output of R (the debugged copy) as lines of the editor */
			Matcher m = Pattern.compile("(?:debug(?: at| bei)?) .*? line (\\d+):\\s*(.*)").matcher(shortenDebugLines(text, sessionFileName));
			if (m.find()) {
				line = Integer.parseInt(m.group(1));
				code = m.group(2).trim();
				/* An injected line of the debugged copy (not the code of the editor) */
				if (code.contains(".bio7bp") || code.contains(".bio7hit_") || (code.startsWith("cat(") && (code.contains("▶ line") || code.contains("\\u25B6 line")))) {
					code = "(breakpoint)";
				}
			}
		}
		DebugState s = null;
		if (browsing) {
			String state = EmbeddedR.consoleCapture(".bio7dbg$state(environment(), " + DebugSession.rStrings(DebugSession.watchExpressions()) + ", 0)",
					STATE_TIMEOUT, false);
			s = DebugState.parse(state);
			if (line < 0 && s.breakpoint > 0) {
				line = s.breakpoint;
			}
			/* An expression typed at the Browse prompt: R still waits at the last line */
			s.stopLine = line > 0 ? line : quiet ? lastStopLine : 0;
			lastStopLine = s.stopLine;
		}
		final int stopLine = line;
		final String stopCode = code;
		final String stopError = error;
		final DebugState stopState = s;
		/* A step typed in the console: R is not at the Browse prompt because it evaluates the next one (not the end) */
		if (quiet && !browsing && EmbeddedR.isBusy()) {
			return;
		}
		Display.getDefault().asyncExec(() -> {
			if (!browsing) {
				/* Stopped with the Stop action: no message */
				if (debugSessionActive) {
					sessionEnded(stopError);
				} else {
					DebugVariablesView.clear();
				}
				return;
			}
			if (stopError != null) {
				printToTextView("✖ Error" + (stopLine > 0 ? " in line " + stopLine : "") + ": " + stopError);
				printToTextView("  R stopped in the frame of the error: inspect the variables, Continue ends the session.");
			} else if (stopLine > 0) {
				printToTextView("▶ Line " + stopLine + (stopCode != null ? ": " + stopCode : ""));
			} else if (!quiet) {
				printToTextView("▶ R waits in Browse mode");
			}
			if (stopLine > 0) {
				showLine(stopLine);
			}
			if (stopState != null) {
				DebugVariablesView.show(stopState, true);
			}
		});
	}

	/* The session has ended (the script has finished or R has left Browse mode) */
	private static void sessionEnded(String error) {
		endSession();
		if (!com.eco.bio7.rbridge.backend.Backends.isEmbeddedRunning()) {
			ConsolePageParticipant c = ConsolePageParticipant.getConsolePageParticipantInstance();
			c.pipeToRConsole("options(prompt=\"> \")");
		}
		DebugVariablesView.clear();
		printToTextView("■ Debug session ended" + (error != null ? " (error: " + error + ")" : "."));
		Bio7Dialog.message("Debug session ended.\n\n" + (error != null ? "Error: " + error + "\n\n" : "")
				+ "The script has finished or R has left Browse mode\n(see the R console for details).");
		removeDebugMarker();
	}

	/* ------------------------------------------------------------ the editor */

	/** Shows the line of R in the active editor (the marker of the debugger). */
	static void showLine(int line) {
		IEditorPart edit = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		if (!(edit instanceof ITextEditor editor)) {
			return;
		}
		IDocumentProvider dp = editor.getDocumentProvider();
		IDocument doc = dp.getDocument(editor.getEditorInput());
		if (line < 1 || line > doc.getNumberOfLines()) {
			return;
		}
		IRegion reg;
		try {
			reg = doc.getLineInformation(line - 1);
		} catch (BadLocationException e) {
			return;
		}
		editor.selectAndReveal(reg.getOffset() + reg.getLength(), 0);
		IResource resource = editor.getEditorInput().getAdapter(IResource.class);
		if (resource == null) {
			return;
		}
		try {
			resource.deleteMarkers("com.eco.bio7.reditor.debugrulermark", false, IResource.DEPTH_ZERO);
			IMarker marker = resource.createMarker("com.eco.bio7.reditor.debugrulermark");
			marker.setAttribute(IMarker.CHAR_START, reg.getOffset());
			marker.setAttribute(IMarker.CHAR_END, reg.getOffset() + reg.getLength());
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	private static void removeDebugMarker() {
		try {
			IEditorPart edit = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
			IResource res = edit == null ? null : edit.getEditorInput().getAdapter(IResource.class);
			if (res != null) {
				res.deleteMarkers("com.eco.bio7.reditor.debugrulermark", false, IResource.DEPTH_ZERO);
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	/* ------------------------------------------------------------ the R console (socket) */

	private void javaReadFromRSocket(int port) {
		try {
			debugSocket = new Socket();
			debugSocket.connect(new java.net.InetSocketAddress("127.0.0.1", port), 2000);
			debugSocket.setTcpNoDelay(true);
			debugSocket.setSoTimeout(2000);
			try (BufferedReader input = new BufferedReader(new InputStreamReader(debugSocket.getInputStream()))) {
				processSocketOutput(input);
			}
			debugSocket.close();
		} catch (IOException e) {
			/* Connection failed — R may have exited Browse mode */
		}
	}

	/*
	 * The output of a debug step: the "debug at file.R#n" line (the position in the
	 * editor), after the line Bio7...Debug...Start... the variables (ls.str()). The
	 * rows are shown at once (one update of the table).
	 */
	private void processSocketOutput(BufferedReader input) {
		String line;
		boolean capture = false;
		List<String[]> rows = new ArrayList<>();
		clearTextView();
		try {
			while ((line = input.readLine()) != null) {
				Matcher dm = DEBUG_LINE.matcher(line);
				if (dm.find() && debugStepLine == null) {
					debugStepLine = line;
					try {
						printToTextView("▶ Line " + DebugSession.toSourceLine(Integer.parseInt(dm.group(1))) + ": " + dm.group(2).trim());
					} catch (NumberFormatException e) {
						/* Ignore */
					}
				}
				if (line.startsWith("Bio7...Debug...Start...")) {
					capture = true;
					continue;
				}
				if (capture) {
					/* "name :  type value" */
					String[] sp = line.split(":", 2);
					if (sp.length == 2) {
						String[] sp2 = sp[1].split("\\s", 4);
						rows.add(new String[] { sp[0].trim(), sp2.length > 3 ? sp2[2] : "", "", "", sp2.length > 3 ? sp2[3] : sp[1].trim() });
					}
				}
			}
		} catch (IOException e) {
			/* Socket timeout — normal, means R closed the connection */
		}
		DebugState s = new DebugState();
		s.variables.addAll(rows);
		Display.getDefault().syncExec(() -> DebugVariablesView.show(s, true));
	}

	/* ------------------------------------------------------------ texts */

	/*
	 * "debug at /tmp/.../bio7debug123.R#11: code" -> "debug at Script.R line 9:
	 * code", "@@E@@12\tmessage" -> "Error in Script.R line 10: message" (the lines of
	 * the editor, see DebugSession.toSourceLine)
	 */
	static String shortenDebugLines(String text, String fileName) {
		Matcher m = Pattern.compile("(debug(?: at| bei)?) \\S+?\\.R#(\\d+):").matcher(text);
		StringBuilder sb = new StringBuilder();
		while (m.find()) {
			int line = DebugSession.toSourceLine(Integer.parseInt(m.group(2)));
			m.appendReplacement(sb, Matcher.quoteReplacement(m.group(1) + " " + fileName + " line " + line + ":"));
		}
		m.appendTail(sb);
		Matcher e = Pattern.compile("@@E@@(\\S*)\\t([^\\n]*)").matcher(sb.toString());
		StringBuilder sb2 = new StringBuilder();
		while (e.find()) {
			String where = "";
			try {
				where = " in " + fileName + " line " + DebugSession.toSourceLine(Integer.parseInt(e.group(1)));
			} catch (NumberFormatException x) {
				/* NA */
			}
			e.appendReplacement(sb2, Matcher.quoteReplacement("Error" + where + ": " + e.group(2) + "\n(Bio7 debugger: R stopped in the frame of the error)"));
		}
		e.appendTail(sb2);
		return sb2.toString();
	}

	/* The name of the file of the active editor (UI thread) or "script" */
	static String activeEditorName() {
		String[] name = { "script" };
		Runnable r = () -> {
			try {
				IEditorPart edit = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
				if (edit != null) {
					name[0] = edit.getEditorInput().getName();
				}
			} catch (RuntimeException e) {
				/* No editor */
			}
		};
		if (Display.getCurrent() != null) {
			r.run();
		} else {
			Display.getDefault().syncExec(r);
		}
		return name[0];
	}

	static void printToTextView(String line) {
		Display.getDefault().syncExec(() -> {
			StyledText styledTxT = DebugTextView.getStyledText();
			if (styledTxT != null && !styledTxT.isDisposed()) {
				styledTxT.append(line);
				styledTxT.append("\n");
			}
		});
	}

	static void clearTextView() {
		Display.getDefault().syncExec(() -> {
			StyledText styledTxT = DebugTextView.getStyledText();
			if (styledTxT != null && !styledTxT.isDisposed()) {
				styledTxT.setText("");
			}
		});
	}
}
