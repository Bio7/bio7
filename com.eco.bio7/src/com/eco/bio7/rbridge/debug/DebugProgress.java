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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.ide.IDE;
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
	/* The global variable of R which enables the temporary breakpoint of Run to Cursor */
	private static final String RUN_TO_FLAG = ".bio7runto";

	/*
	 * A line of R "debug at <file>#<line>: <code>" (in any language): the file (an
	 * absolute path, also with blanks, or a name), its line and the code
	 */
	private static final Pattern DEBUG_LINE = Pattern.compile("((?:/|[A-Za-z]:[/\\\\])[^\\n]*?\\.[Rr]|[^\\s/\\\\]\\S*?\\.[Rr])#(\\d+):[ \\t]*([^\\n]*)");
	/* The last stop was a breakpoint (R waits before its line) */
	private static volatile boolean lastStopAtBreakpoint;
	/* A step in a function without a file: "debug: <code>" */
	private static final Pattern NO_FILE_LINE = Pattern.compile("(?m)^debug(?: at| bei)?: ([^\\n]*)");
	/* The position of the last stop (for the call stack when another frame is shown) */
	private static volatile DebugSession.Location lastStop;

	private static final Pattern ERROR_LINE =Pattern.compile("@@E@@(\\S*)\\t(.*)");

	/* Called by DebugRScript when a debug session starts */
	public static void startSession() {
		debugSessionActive = true;
		stepping = false;
		DebugLiveUpdate.start();
		DebugVariablesView.updateActions();
	}

	/* Called to end the debug session */
	public static void endSession() {
		debugSessionActive = false;
		stepping = false;
		awaitingCall = false;
		DebugLiveUpdate.stop();
		DebugVariablesView.updateActions();
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
				/* Step Into of a method call: into the method (see .bio7dbg$stepInto) */
				boolean targeted = "s".equals(command) && markStepTargets();
				String out;
				if ((targeted || "s".equals(command) || "n".equals(command)) && lastStopAtBreakpoint && EmbeddedR.isBrowsing()) {
					/*
					 * At a breakpoint R waits before the line (in the browser() of the breakpoint):
					 * to the line first, then the step (Next executes the line, Step Into enters its
					 * call; else the first click would only reach the line)
					 */
					EmbeddedR.consoleCapture("n", STEP_TIMEOUT, false);
				}
				out = EmbeddedR.consoleCapture(targeted ? "n" : command, STEP_TIMEOUT, true, text -> shortenDebugLines(text, fileName));
				if (targeted && EmbeddedR.isBrowsing()) {
					EmbeddedR.consoleCapture(".bio7dbg$untarget()", STATE_TIMEOUT, false);
				}
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
									showLine(DebugSession.location(m.group(1), Integer.parseInt(m.group(2))));
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
		startEmbedded(fileName, null, null);
	}

	/*
	 * Has the session stopped (a breakpoint, a step, an error)? A hint for the end
	 * of a session without a stop (e.g., a file which only defines classes)
	 */
	private static volatile boolean stoppedInSession;
	private static volatile String endHint;

	/**
	 * As above; note: a line for the Debug Text view (e.g., the script which is
	 * debugged for the active file), hint: the message if the session ends without
	 * a stop.
	 */
	public static void startEmbedded(String fileName, String note, String hint) {
		startSession();
		sessionFileName = fileName;
		stoppedInSession = false;
		endHint = hint;
		EmbeddedR.setConsoleObserver(DebugProgress::consoleTyped);
		runEmbedded("R Debug", () -> {
			clearTextView();
			if (note != null) {
				printToTextView(note);
			}
			String lower = fileName.toLowerCase(java.util.Locale.ROOT);
			if (lower.endsWith(".rmd") || lower.endsWith(".qmd") || lower.endsWith(".rmarkdown")) {
				printToTextView("ℹ " + fileName + ": the R chunks are debugged as a script (not knitted: inline R code, child documents and the cache are not used)");
			}
			String out = EmbeddedR.consoleCapture("withCallingHandlers(.bio7debugfunc(), error = .bio7dbg$onError)", STEP_TIMEOUT, true,
					text -> shortenDebugLines(text, fileName));
			afterEmbeddedStop(out);
		});
	}

	/* Debug Function: the session waits until R calls the function (other console input doesn't end it) */
	private static volatile boolean awaitingCall;
	/* Debug Function: the function (undebug at the end) */
	private static volatile String debuggedFunction;

	/**
	 * Debug Function: R stops at the next call of a function (debugonce), also of
	 * a package or without a file. The selection is a name (R stops when the
	 * function is called, e.g., in the console or a script) or a call (f(3): it is
	 * run at once).
	 */
	public static void debugFunction(String selection) {
		if (!com.eco.bio7.rbridge.backend.Backends.isEmbeddedRunning()) {
			Bio7Dialog.message("Debug Function needs embedded R.");
			return;
		}
		String sel = selection == null ? "" : selection.strip();
		boolean call = sel.matches("(?s)[A-Za-z.][\\w.]*(:::?[A-Za-z.][\\w.]*)?\\s*\\(.*\\)");
		String name = call ? sel.substring(0, sel.indexOf('(')).strip() : sel;
		if (!name.matches("[A-Za-z.][\\w.]*(:::?[A-Za-z.][\\w.]*)?")) {
			Bio7Dialog.message("Please select the name of a function (e.g., myFunction or stats::sd) or a call (e.g., myFunction(3)).");
			return;
		}
		if (debugSessionActive && EmbeddedR.isBrowsing()) {
			/* In a session: R stops at the next call (e.g., Continue) */
			runEmbedded("R Debug Function", () -> {
				String out = EmbeddedR.consoleCapture("tryCatch({ debugonce(" + name + "); cat('@@OK@@\\n') }, error = function(e) cat('@@ERR@@', conditionMessage(e), '\\n'))",
						STATE_TIMEOUT, false);
				printToTextView(out != null && out.contains("@@OK@@") ? "▶ " + name + ": R stops at its next call" : "✖ " + name + " is not a function");
			});
			return;
		}
		if (debugSessionActive || EmbeddedR.isBusy() || EmbeddedR.isBrowsing()) {
			Bio7Dialog.message("R is busy or another debug session runs.");
			return;
		}
		com.eco.bio7.rbridge.backend.RBackend b = com.eco.bio7.rbridge.RBridge.getBackend();
		try {
			b.voidEval(DebugSession.HELPERS);
			b.voidEval("debugonce(" + name + ")");
		} catch (Exception e) {
			Bio7Dialog.message(name + " is not a function of R:\n" + e.getMessage());
			return;
		}
		debuggedFunction = name;
		Display.getDefault().syncExec(() -> {
			ConsolePageParticipant inst = ConsolePageParticipant.getConsolePageParticipantInstance();
			if (inst != null) {
				inst.setRDebugToolbarActions();
			}
			DebugVariablesView.showDebugView();
		});
		startSession();
		EmbeddedR.setConsoleObserver(DebugProgress::consoleTyped);
		clearTextView();
		if (!call) {
			awaitingCall = true;
			printToTextView("▶ Debug " + name + ": R stops at its next call (e.g., call it in the console or run a script).");
			return;
		}
		runEmbedded("R Debug Function", () -> {
			String out = EmbeddedR.consoleCapture("withCallingHandlers(" + sel + ", error = .bio7dbg$onError)", STEP_TIMEOUT, true, text -> shortenDebugLines(text, name));
			afterEmbeddedStop(out);
		});
	}

	/*
	 * Changes of files during the session (breakpoints or code, see
	 * DebugLiveUpdate): applied when R waits in the browser (now or at the next
	 * stop); the running code keeps the old version
	 */
	static void liveUpdateLater() {
		if (!debugSessionActive || !EmbeddedR.isBrowsing() || EmbeddedR.isBusy()) {
			/* At the next stop */
			return;
		}
		runEmbedded("R Debug Update", DebugProgress::applyLiveUpdatesNow);
	}

	/*
	 * Step Into: the methods which the current line calls (a reference class, R6
	 * or S7 method) get debugonce; true if there are some (the step is then n: R
	 * stops in the method, not in R's code of $ or the dispatch)
	 */
	private static boolean markStepTargets() {
		DebugSession.Location at = lastStop;
		if (at == null || !Files.isRegularFile(at.file())) {
			return false;
		}
		try {
			List<String> lines = Files.readAllLines(at.file(), java.nio.charset.StandardCharsets.UTF_8);
			List<String> code = new ArrayList<>();
			for (int i = at.line() - 1; i >= 0 && i < lines.size() && code.size() < 8; i++) {
				code.add(lines.get(i));
			}
			if (code.isEmpty()) {
				return false;
			}
			String out = EmbeddedR.consoleCapture(".bio7dbg$stepInto(" + DebugSession.rStrings(code) + ", environment())", STATE_TIMEOUT, false);
			Matcher m = Pattern.compile("@@T@@(-?\\d+)").matcher(out == null ? "" : out);
			return m.find() && Integer.parseInt(m.group(1)) > 0;
		} catch (java.io.IOException | RuntimeException e) {
			return false;
		}
	}

	/* In the thread of a debug action while R waits in the browser */
	private static void applyLiveUpdatesNow() {
		if (!com.eco.bio7.rbridge.backend.Backends.isEmbeddedRunning() || !EmbeddedR.isBrowsing()) {
			return;
		}
		for (Path source : DebugLiveUpdate.take()) {
			boolean inSession = DebugSession.files().contains(source.toAbsolutePath());
			java.util.Map<Integer, RBreakpoint> bps = DebugLiveUpdate.breakpoints(source);
			if (!inSession && bps.isEmpty()) {
				continue;
			}
			try {
				Path copy = DebugSession.rewriteCopy(source, bps);
				String out = EmbeddedR.consoleCapture(DebugSession.updateR(source, copy), STATE_TIMEOUT, false);
				Matcher u = Pattern.compile("@@U@@([^\\n]*)").matcher(out == null ? "" : out);
				String names = u.find() ? u.group(1).trim() : "";
				String file = source.getFileName().toString();
				if (!names.isEmpty()) {
					printToTextView("↻ " + file + ": updated " + names + " (applies at the next call)");
				} else if (DebugSession.isScript(source)) {
					printToTextView("↻ " + file + ": changes of the running script code apply at the next start of the debugger");
				} else {
					printToTextView("↻ " + file + ": applies when the file is loaded again (source)");
				}
			} catch (java.io.IOException e) {
				printToTextView("✖ " + source.getFileName() + " could not be updated: " + e.getMessage());
			}
		}
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
		final Path targetFile = activeEditorFile();
		if (!debugSessionActive || !com.eco.bio7.rbridge.backend.Backends.isEmbeddedRunning()) {
			Bio7Dialog.message("Run to Cursor needs a debug session of embedded R.");
			return;
		}
		final String fileName = activeEditorName();
		runEmbedded("R Debug Run to Cursor", () -> {
			/*
			 * A temporary breakpoint at the line: n steps over calls, so R would not stop
			 * in a function (or method, or another file) which the code calls later. The
			 * functions of the file are defined again with it (the running code keeps its
			 * version: there the steps reach the line).
			 */
			/* 1: applied, 0: pending (the definitions don't exist yet, e.g., before the class definition), -1: none */
			int state = targetFile == null ? TEMPORARY_NONE : redefineWithBreakpoints(targetFile, targetLine);
			boolean temporary = state != TEMPORARY_NONE;
			if (temporary) {
				/* The temporary breakpoint stops only once (also in a function which runs with it, e.g., in a loop) */
				EmbeddedR.consoleCapture("assign('" + RUN_TO_FLAG + "', TRUE, envir = globalenv())", STATE_TIMEOUT, false);
			}
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
				DebugSession.Location at = lastLocation(out);
				if (at != null && at.line() == targetLine && (targetFile == null || at.file().equals(targetFile))) {
					break;
				}
				if (state == TEMPORARY_PENDING) {
					/* After the step the definitions may exist (e.g., the class of the file) */
					String p = EmbeddedR.consoleCapture(".bio7dbg$applyPending()", STATE_TIMEOUT, false);
					if (p != null && p.contains("@@P@@0")) {
						state = TEMPORARY_APPLIED;
					}
				}
			}
			if (temporary && EmbeddedR.isBrowsing()) {
				EmbeddedR.consoleCapture("assign('" + RUN_TO_FLAG + "', FALSE, envir = globalenv()); .bio7dbg$clearPending()", STATE_TIMEOUT, false);
				/* The functions without the temporary breakpoint (at their next call) */
				redefineWithBreakpoints(targetFile, 0);
			}
			printToTextView("▶ Run to line " + targetLine);
			afterEmbeddedStop(all.toString());
		});
	}

	/*
	 * Run to Cursor: a new copy of the file with the breakpoints of its markers and
	 * a temporary breakpoint at a line (0: none); its functions and classes are
	 * defined again (as for a change during the session). R waits in the browser.
	 * TEMPORARY_APPLIED if R has defined functions or classes again;
	 * TEMPORARY_PENDING if they don't exist yet (with a temporary breakpoint: R
	 * uses the copy when they exist, see .bio7dbg$later); else TEMPORARY_NONE.
	 */
	private static final int TEMPORARY_NONE = -1, TEMPORARY_PENDING = 0, TEMPORARY_APPLIED = 1;

	private static int redefineWithBreakpoints(Path source, int temporaryLine) {
		if (!EmbeddedR.isBrowsing() || !Files.isRegularFile(source)) {
			return TEMPORARY_NONE;
		}
		java.util.Map<Integer, RBreakpoint> bps = new java.util.TreeMap<>(DebugLiveUpdate.breakpoints(source));
		if (temporaryLine > 0) {
			bps.putIfAbsent(temporaryLine, new RBreakpoint("isTRUE(get0('" + RUN_TO_FLAG + "', envir = globalenv()))", 0, "", null));
		}
		try {
			Path copy = DebugSession.rewriteCopy(source, bps);
			String out = EmbeddedR.consoleCapture(DebugSession.updateR(source, copy), STATE_TIMEOUT, false);
			Matcher u = Pattern.compile("@@U@@([^\\n]*)").matcher(out == null ? "" : out);
			if (u.find() && !u.group(1).isBlank()) {
				return TEMPORARY_APPLIED;
			}
			if (temporaryLine > 0) {
				/* Not defined yet: applied by R when the definitions exist */
				EmbeddedR.consoleCapture(".bio7dbg$later(" + DebugSession.rString(source.toAbsolutePath().toString().replace("\\", "/")) + ", "
						+ DebugSession.rString(copy.toAbsolutePath().toString().replace("\\", "/")) + ", " + (DebugSession.isScript(source) ? "TRUE" : "FALSE") + ")",
						STATE_TIMEOUT, false);
				return TEMPORARY_PENDING;
			}
			return TEMPORARY_NONE;
		} catch (java.io.IOException | RuntimeException e) {
			return TEMPORARY_NONE;
		}
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
			s.stop = lastStop;
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
		/* Also the prompt of browser() (e.g., a stop in R's own code without a file) */
		boolean browsing = EmbeddedR.isBrowsing() || EmbeddedR.isAtBrowsePrompt();
		DebugSession.Location line = null;
		String code = null;
		String error = null;
		/* A stop without a line of a file (shown as text) */
		String note = null;
		Matcher e = ERROR_LINE.matcher(text);
		if (e.find()) {
			error = e.group(2).trim();
			line = DebugSession.location(e.group(1));
		} else {
			/* The last position of R (a debugged copy or another file) as file and line of the editor */
			Matcher m = DEBUG_LINE.matcher(text);
			while (m.find()) {
				DebugSession.Location at = DebugSession.location(m.group(1), Integer.parseInt(m.group(2)));
				if (at != null && at.line() < 1) {
					/* The line of the wrapper: the end of the script or file (its variables are kept) */
					line = null;
					code = null;
					note = "End of " + at.name() + " (its variables are kept)";
				} else if (at != null) {
					line = at;
					code = m.group(3).trim();
					note = null;
				}
			}
			if (line == null && note == null) {
				/* A function without a file (e.g., Debug Function of a package function): its code */
				Matcher d = NO_FILE_LINE.matcher(text);
				while (d.find()) {
					note = d.group(1).trim();
				}
				Matcher in = Pattern.compile("(?m)^debugging in: ([^\\n]*)").matcher(text);
				String fn = null;
				while (in.find()) {
					fn = in.group(1).trim();
				}
				if (fn != null) {
					note = "In " + fn + " (R code without a file; Step Out returns)" + (note != null && !note.equals("{") ? ": " + note : "");
				}
			}
			/* An injected line of the debugged copy (not the code of the editor) */
			if (code != null && (code.contains(".bio7bp") || code.contains(".bio7hit_") || (code.startsWith("cat(") && (code.contains("▶ ") || code.contains("\\u25B6 "))))) {
				code = "(breakpoint)";
			}
		}
		DebugState s = null;
		if (browsing) {
			String state = EmbeddedR.consoleCapture(".bio7dbg$state(environment(), " + DebugSession.rStrings(DebugSession.watchExpressions()) + ", 0)",
					STATE_TIMEOUT, false);
			s = DebugState.parse(state);
			if (line == null && s.breakpoint != null) {
				line = s.breakpoint;
			}
			/* An expression typed at the Browse prompt: R still waits at the last line */
			s.stop = line != null ? line : quiet ? lastStop : null;
			lastStop = s.stop;
		}
		/* R waits at the stop of a breakpoint (before the line of the breakpoint) */
		lastStopAtBreakpoint = "(breakpoint)".equals(code) || (code == null && error == null && s != null && s.breakpoint != null && line == s.breakpoint);
		if (!browsing && error == null) {
			/* An error which has ended the session (without the browser of the debugger) */
			Matcher x = Pattern.compile("(?m)^(Error[^\\n]*(?:\\n(?![>+] )[^\\n@]+)?)").matcher(text);
			if (x.find()) {
				error = x.group(1).trim();
			}
		}
		final DebugSession.Location stopLine = line;
		final String stopCode = code;
		final String stopError = error;
		final DebugState stopState = s;
		final String stopNote = note;
		/* A step typed in the console: R is not at the Browse prompt because it evaluates the next one (not the end) */
		if (quiet && !browsing && EmbeddedR.isBusy()) {
			return;
		}
		if (!browsing && awaitingCall) {
			/* Debug Function: R has not called the function yet (other input in the console) */
			return;
		}
		if (browsing) {
			stoppedInSession = true;
			awaitingCall = false;
			/* Changes of files during the session (breakpoints, code): now that R waits */
			applyLiveUpdatesNow();
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
				printToTextView("✖ Error" + (stopLine != null ? " in " + where(stopLine).replace("Line", "line") : "") + ": " + stopError);
				printToTextView("  R stopped in the frame of the error: inspect the variables, Continue ends the session.");
			} else if (stopLine != null) {
				printToTextView("▶ " + where(stopLine) + (stopCode != null ? ": " + stopCode : ""));
			} else if (stopNote != null) {
				printToTextView("▶ " + stopNote);
			} else if (!quiet) {
				printToTextView("▶ R waits in Browse mode");
			}
			if (stopLine != null) {
				showLine(stopLine);
			}
			if (stopState != null) {
				DebugVariablesView.show(stopState, true);
			}
		});
	}

	/* The session has ended (the script has finished or R has left Browse mode) */
	/**
	 * Ends a layout of several plots which the debugged script has set (e.g.,
	 * par(mfrow = c(2, 3)), maybe half filled after Stop): the next plot starts
	 * on a new page with one plot. As soon as R is free (after Q or an interrupt).
	 */
	public static void resetPlotLayoutWhenFree() {
		Thread t = new Thread(() -> {
			try {
				long end = System.currentTimeMillis() + 30000;
				while ((com.eco.bio7.rbridge.backend.EmbeddedR.isBrowsing() || com.eco.bio7.rbridge.backend.EmbeddedR.isBusy())
						&& System.currentTimeMillis() < end) {
					Thread.sleep(100);
				}
				com.eco.bio7.rbridge.backend.RBackend r = com.eco.bio7.rbridge.RBridge.getBackend();
				if (r != null && !com.eco.bio7.rbridge.backend.EmbeddedR.isBrowsing()) {
					/* The source() of the session is removed first (see .bio7dbg$install) */
					r.voidEval("if (exists('.bio7dbg', envir = .GlobalEnv, inherits = FALSE)) .bio7dbg$cleanup()");
					/* Debug Function: the function was maybe not called (debugonce would stop later) */
					String fn = debuggedFunction;
					debuggedFunction = null;
					if (fn != null) {
						r.voidEval("try(if (isdebugged(" + fn + ")) undebug(" + fn + "), silent = TRUE)");
					}
					r.voidEval("if (dev.cur() > 1 && any(par('mfrow') != 1)) par(mfrow = c(1, 1))");
				}
			} catch (Exception e) {
				/* R ended or waits again */
			}
		}, "R Debug plot layout");
		t.setDaemon(true);
		t.start();
	}

	private static void sessionEnded(String error) {
		endSession();
		resetPlotLayoutWhenFree();
		if (!com.eco.bio7.rbridge.backend.Backends.isEmbeddedRunning()) {
			ConsolePageParticipant c = ConsolePageParticipant.getConsolePageParticipantInstance();
			c.pipeToRConsole("options(prompt=\"> \")");
		}
		DebugVariablesView.clear();
		printToTextView("■ Debug session ended" + (error != null ? " (error: " + error + ")" : "."));
		String hint = endHint;
		if (!stoppedInSession && hint != null) {
			/* Without a stop: e.g., a file which only defines classes */
			printToTextView("ℹ " + hint.replace('\n', ' '));
			Bio7Dialog.message("Debug session ended without a stop.\n\n" + (error != null ? "Error: " + error + "\n\n" : "") + hint);
		} else if (!stoppedInSession && error != null) {
			Bio7Dialog.message("Debug session ended without a stop.\n\nError: " + error + "\n\n(see the R console for details)");
		} else {
			Bio7Dialog.message("Debug session ended.\n\n" + (error != null ? "Error: " + error + "\n\n" : "")
					+ "The script has finished or R has left Browse mode\n(see the R console for details).");
		}
		removeDebugMarker();
	}

	/* ------------------------------------------------------------ the editor */

	/**
	 * Shows a position of R (the marker of the debugger): the editor of the file is
	 * opened (or brought to the top) if it is not the active one, without taking
	 * the focus (e.g., from the console).
	 */
	static void showLine(DebugSession.Location loc) {
		if (loc == null || loc.line() < 1) {
			return;
		}
		ITextEditor editor = editorOf(loc.file());
		if (editor == null) {
			return;
		}
		IDocumentProvider dp = editor.getDocumentProvider();
		IDocument doc = dp.getDocument(editor.getEditorInput());
		if (doc == null || loc.line() > doc.getNumberOfLines()) {
			return;
		}
		IRegion reg;
		try {
			reg = doc.getLineInformation(loc.line() - 1);
		} catch (BadLocationException e) {
			return;
		}
		editor.selectAndReveal(reg.getOffset() + reg.getLength(), 0);
		/* One marker of the debugger (also if the last stop was in another file) */
		removeDebugMarker();
		IResource resource = editor.getEditorInput().getAdapter(IResource.class);
		if (resource == null) {
			return;
		}
		try {
			IMarker marker = resource.createMarker("com.eco.bio7.reditor.debugrulermark");
			marker.setAttribute(IMarker.CHAR_START, reg.getOffset());
			marker.setAttribute(IMarker.CHAR_END, reg.getOffset() + reg.getLength());
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	/*
	 * The text editor of a file: the active one if it shows the file, else the file
	 * is opened in the workspace (or as a file outside of it) and brought to the top
	 */
	private static ITextEditor editorOf(Path file) {
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow() == null ? null
				: PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		if (page == null) {
			return null;
		}
		IEditorPart active = page.getActiveEditor();
		if (active instanceof ITextEditor t && file.equals(fileOf(active))) {
			return t;
		}
		try {
			IEditorPart part = null;
			IFile[] files = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocationURI(file.toUri());
			for (IFile f : files) {
				if (f.exists()) {
					part = IDE.openEditor(page, f, false);
					break;
				}
			}
			if (part == null) {
				part = IDE.openEditorOnFileStore(page, EFS.getLocalFileSystem().getStore(file.toUri()));
			}
			if (part != null) {
				page.bringToTop(part);
			}
			return part instanceof ITextEditor t ? t : part == null ? null : part.getAdapter(ITextEditor.class);
		} catch (PartInitException e) {
			return null;
		}
	}

	/* The file of an editor (null if unknown) */
	private static Path fileOf(IEditorPart editor) {
		IResource r = editor.getEditorInput().getAdapter(IResource.class);
		if (r != null && r.getLocation() != null) {
			return r.getLocation().toFile().toPath().toAbsolutePath();
		}
		if (editor.getEditorInput() instanceof IURIEditorInput u && u.getURI() != null && "file".equals(u.getURI().getScheme())) {
			return Path.of(u.getURI()).toAbsolutePath();
		}
		return null;
	}

	/* The file of the active editor (UI thread), null if unknown */
	static Path activeEditorFile() {
		Path[] file = { null };
		Runnable r = () -> {
			IEditorPart edit = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
			if (edit != null) {
				file[0] = fileOf(edit);
			}
		};
		if (Display.getCurrent() != null) {
			r.run();
		} else {
			Display.getDefault().syncExec(r);
		}
		return file[0];
	}

	/* "Line 12" in the debugged script, "animal.R line 12" in another file */
	static String where(DebugSession.Location loc) {
		return loc.file().equals(DebugSession.script()) ? "Line " + loc.line() : loc.name() + " line " + loc.line();
	}

	/* The last known position of R in an output, null if none */
	private static DebugSession.Location lastLocation(String out) {
		DebugSession.Location last = null;
		Matcher m = DEBUG_LINE.matcher(out == null ? "" : out);
		while (m.find()) {
			DebugSession.Location at = DebugSession.location(m.group(1), Integer.parseInt(m.group(2)));
			if (at != null) {
				last = at;
			}
		}
		return last;
	}

	/* The markers of the debugger in all files (the session may have several) */
	private static void removeDebugMarker() {
		try {
			ResourcesPlugin.getWorkspace().getRoot().deleteMarkers("com.eco.bio7.reditor.debugrulermark", false, IResource.DEPTH_INFINITE);
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
					DebugSession.Location at = DebugSession.location(dm.group(1), Integer.parseInt(dm.group(2)));
					if (at != null) {
						printToTextView("▶ " + where(at) + ": " + dm.group(3).trim());
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
		/* The position of R as file and line of the editor (a debugged copy: its source) */
		Matcher m = DEBUG_LINE.matcher(text);
		StringBuilder sb = new StringBuilder();
		while (m.find()) {
			DebugSession.Location at = DebugSession.location(m.group(1), Integer.parseInt(m.group(2)));
			String shown = at == null ? m.group(1) + "#" + m.group(2) : at.name() + " line " + at.line();
			m.appendReplacement(sb, Matcher.quoteReplacement(shown + ": " + m.group(3)));
		}
		m.appendTail(sb);
		Matcher e = Pattern.compile("@@E@@(\\S*)\\t([^\\n]*)").matcher(sb.toString());
		StringBuilder sb2 = new StringBuilder();
		while (e.find()) {
			DebugSession.Location at = DebugSession.location(e.group(1));
			String where = at == null ? "" : " in " + at.name() + " line " + at.line();
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
