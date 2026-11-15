/*******************************************************************************
 * Copyright (c) 2007-2026 M. Austenfeld
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/
package com.eco.bio7.pybridge.debug;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.ITextEditor;

import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.console.ConsoleInterpreterAction;
import com.eco.bio7.pybridge.EmbeddedPython;
import com.eco.bio7.pybridge.PythonCalls;
import com.eco.bio7.pybridge.PythonImageTransfer;
import com.eco.bio7.pyffm.PySession;
import com.eco.bio7.pyffm.PyValue;
import com.eco.bio7.pythonedit.analysis.PythonBreakpoints;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The debugger of the embedded Python: runs a script with bdb (module
 * _bio7dbg). At a stop the thread of Python waits here for the next command; the
 * queries of the views (variables, watch, call stack, console lines) run in that
 * thread too (the frame of the stop).
 */
public final class PythonDebugger {

	public static final String PERSPECTIVE = "com.eco.bio7.pybridge.debug.PythonDebugPerspective";
	private static final long QUERY_TIMEOUT_MS = 10000;

	/** A stop: the file, line, function, reason and the call stack (index, function, file name, line, path). */
	public record Stop(String file, int line, String function, String reason, String message, List<String[]> stack) {
	}

	private static final BlockingQueue<Object> commands = new LinkedBlockingQueue<>();
	private static volatile boolean running;
	private static volatile Stop stop;
	private static volatile IFile scriptFile;
	private static volatile boolean registered;

	private PythonDebugger() {
	}

	public static boolean isRunning() {
		return running;
	}

	/** Returns true if the debugger stopped (the commands and queries are possible). */
	public static boolean isPaused() {
		return stop != null;
	}

	/** The current stop or null. */
	public static Stop stop() {
		return stop;
	}

	/* ---------------- Start ---------------- */

	/** Debugs the script of the active Python editor (UI thread). */
	public static void startActiveEditor(boolean stopAtFirstLine) {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		IEditorPart editor = window == null ? null : window.getActivePage().getActiveEditor();
		IFile file = editor == null ? null : editor.getEditorInput().getAdapter(IFile.class);
		if (file == null || !"py".equalsIgnoreCase(file.getFileExtension())) {
			Bio7Dialog.message("Please open a Python script (.py) in the editor.");
			return;
		}
		if (editor.isDirty()) {
			editor.doSave(null);
		}
		start(file, stopAtFirstLine);
	}

	/** Debugs a script: stops at the first breakpoint (or the first line). UI thread. */
	public static void start(IFile file, boolean stopAtFirstLine) {
		if (running) {
			Bio7Dialog.message("A Python script is debugged (stop it first).");
			return;
		}
		if (!PythonImageTransfer.ensurePython() || file.getLocation() == null) {
			return;
		}
		PySession py = EmbeddedPython.session();
		if (py.isBusy()) {
			Bio7Dialog.message("Python is busy.");
			return;
		}
		try {
			register();
			loadModule(py);
		} catch (Exception e) {
			Bio7Dialog.message("The debugger could not be started: " + e.getMessage());
			return;
		}
		scriptFile = file;
		running = true;
		List<Integer> lines = PythonBreakpoints.lines(file);
		/* The perspective of the debugger, the console in Python mode */
		try {
			PlatformUI.getWorkbench().showPerspective(PERSPECTIVE, PlatformUI.getWorkbench().getActiveWorkbenchWindow());
		} catch (Exception e) {
			/* The current perspective */
		}
		ConsoleInterpreterAction.getInstance().startPython();
		EmbeddedPython.print("Debugging " + file.getName() + (lines.isEmpty() ? " (no breakpoints: stops at the first line)" : " (breakpoints: lines " + lines + ")")
				+ "\nWhile stopped, the console evaluates in the frame; n (next), s (step), r (return), c (continue), q (quit).\n", false);
		PythonDebugView.refreshAll();
		String path = file.getLocation().toOSString().replace("\\", "\\\\").replace("'", "\\'");
		/* Not shown as running job (the debug session waits at breakpoints) */
		py.runAsync("__import__('_bio7dbg').run('" + path + "', " + lines + ", " + (stopAtFirstLine ? "True" : "False") + ")", null);
	}

	private static void register() {
		if (registered) {
			return;
		}
		PythonCalls.register("debug_pause", (args, value) -> pause(args.length > 0 ? args[0] : "{}"));
		PythonCalls.register("debug_end", (args, value) -> {
			ended();
			return null;
		});
		registered = true;
	}

	private static void loadModule(PySession py) throws Exception {
		PyValue loaded = py.eval("'_bio7dbg' in __import__('sys').modules");
		if (loaded instanceof PyValue.Array a && a.getDouble(0) != 0) {
			return;
		}
		String source;
		try (InputStream in = PythonDebugger.class.getResourceAsStream("python_debugger.py")) {
			source = new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
		py.assign("_bio7_src", PyValue.of(source));
		py.exec("import types as _t, sys as _s\n_m = _t.ModuleType('_bio7dbg')\nexec(compile(_bio7_src, '<_bio7dbg>', 'exec'), _m.__dict__)\n"
				+ "_s.modules['_bio7dbg'] = _m\ndel _t, _s, _m, _bio7_src");
	}

	/* ---------------- The stop (thread of Python) ---------------- */

	private static PyValue pause(String json) throws Exception {
		JsonNode n = new ObjectMapper().readTree(json);
		List<String[]> stack = new ArrayList<>();
		for (JsonNode r : n.path("stack")) {
			stack.add(new String[] { r.get(0).asText(), r.get(1).asText(), r.get(2).asText(), r.get(3).asText(), r.get(4).asText() });
		}
		commands.clear();
		stop = new Stop(n.path("file").asText(), n.path("line").asInt(), n.path("function").asText(), n.path("reason").asText(),
				n.path("message").isNull() ? null : n.path("message").asText(), stack);
		/* Breakpoints changed in the editor since the start */
		IFile f = scriptFile;
		if (f != null) {
			EmbeddedPython.session().exec("__import__('_bio7dbg').set_breaks(" + PythonBreakpoints.lines(f) + ")");
		}
		Stop s = stop;
		if (s.message() != null) {
			EmbeddedPython.print("Exception: " + s.message() + "\n", true);
		}
		Display.getDefault().asyncExec(() -> {
			showLine(s.file(), s.line());
			PythonDebugView.refreshAll();
		});
		EmbeddedPython.print("> " + new File(s.file()).getName() + ":" + s.line() + " (" + s.function() + ")\n(debug) ", false);
		/* The commands of the user; the queries of the views run here (in the frame of the stop) */
		while (true) {
			Object c = commands.take();
			if (c instanceof FutureTask<?> query) {
				query.run();
				continue;
			}
			stop = null;
			Display.getDefault().asyncExec(PythonDebugView::refreshAll);
			return PyValue.of((String) c);
		}
	}

	private static void ended() {
		running = false;
		stop = null;
		commands.clear();
		IFile f = scriptFile;
		Display.getDefault().asyncExec(() -> {
			if (f != null && f.exists()) {
				PythonBreakpoints.showDebugLine(f, null, 0);
			}
			PythonDebugView.refreshAll();
			EmbeddedPython.prompt();
		});
	}

	/* ---------------- Commands (any thread) ---------------- */

	private static void command(String c) {
		if (stop != null) {
			commands.offer(c);
		}
	}

	public static void next() {
		command("next");
	}

	public static void stepInto() {
		command("step");
	}

	public static void stepReturn() {
		command("return");
	}

	public static void resume() {
		command("continue");
	}

	/** Continues to a line of the script (without a breakpoint). */
	public static void runToLine(int line) {
		command("line " + line);
	}

	/** Stops the debugging (also while the script runs: KeyboardInterrupt). */
	public static void terminate() {
		if (stop != null) {
			command("quit");
		} else if (running) {
			EmbeddedPython.interrupt();
		}
	}

	/**
	 * A line of the console while stopped: n (next), s (step), r (return), c
	 * (continue), q (quit) or Python code in the frame (the result is printed).
	 */
	public static void consoleLine(String line) {
		switch (line.strip()) {
		case "n", "next" -> next();
		case "s", "step" -> stepInto();
		case "r", "return" -> stepReturn();
		case "c", "cont", "continue" -> resume();
		case "q", "quit" -> terminate();
		case "" -> EmbeddedPython.print("(debug) ", false);
		default -> {
			Thread t = new Thread(() -> {
				try {
					String result = console(line);
					EmbeddedPython.print((result.isEmpty() ? "" : result + "\n") + "(debug) ", false);
				} catch (Exception e) {
					EmbeddedPython.print(e.getMessage() + "\n(debug) ", true);
				}
				PythonDebugView.refreshAll();
			}, "Python debug console");
			t.setDaemon(true);
			t.start();
		}
		}
	}

	/* ---------------- Queries (in the frame of the stop) ---------------- */

	/** Runs a query in the thread of Python while stopped (waits for the result). */
	public static <T> T query(Callable<T> task) throws Exception {
		if (stop == null) {
			throw new IllegalStateException("The debugger is not stopped");
		}
		FutureTask<T> f = new FutureTask<>(task);
		commands.offer(f);
		return f.get(QUERY_TIMEOUT_MS, TimeUnit.MILLISECONDS);
	}

	private static PySession py() {
		return EmbeddedPython.session();
	}

	/** The variables of the selected frame: name, type, size, value. */
	public static List<String[]> variables() throws Exception {
		return query(() -> rows(py().eval("__import__('_bio7dbg').variables()"), 4));
	}

	/** The value of an expression in the selected frame. */
	public static String evaluate(String expression) throws Exception {
		return query(() -> {
			py().assign("_bio7_dbg_arg", PyValue.of(expression));
			return text(py().eval("__import__('_bio7dbg').evaluate(_bio7_dbg_arg)"));
		});
	}

	/** A line of the console in the selected frame (the value of an expression or a statement). */
	public static String console(String line) throws Exception {
		return query(() -> {
			py().assign("_bio7_dbg_arg", PyValue.of(line));
			return text(py().eval("__import__('_bio7dbg').console(_bio7_dbg_arg)"));
		});
	}

	/** Selects a frame of the call stack: its variables; the editor shows its line. */
	public static void selectFrame(int index) throws Exception {
		String[] fl = query(() -> rowOf(py().eval("__import__('_bio7dbg').select(" + index + ")")));
		Display.getDefault().asyncExec(() -> showLine(fl[0], Integer.parseInt(fl[1])));
	}

	/** Copies a variable of the frame to the workspace of Python (Python-Shell, table, ImageJ). */
	public static void toWorkspace(String name) throws Exception {
		query(() -> py().eval("__import__('_bio7dbg').to_main('" + name.replaceAll("[^A-Za-z0-9_]", "") + "')"));
	}

	private static String text(PyValue v) {
		return v instanceof PyValue.Str s ? s.asString() : String.valueOf(v);
	}

	private static String[] rowOf(PyValue v) {
		if (v instanceof PyValue.PyList l) {
			String[] r = new String[l.length()];
			for (int i = 0; i < r.length; i++) {
				PyValue e = l.values().get(i);
				r[i] = e instanceof PyValue.Array a ? String.valueOf((long) a.asDouble()) : text(e);
			}
			return r;
		}
		return v instanceof PyValue.Str s ? s.values() : new String[0];
	}

	private static List<String[]> rows(PyValue v, int columns) {
		List<String[]> out = new ArrayList<>();
		if (v instanceof PyValue.PyList l) {
			for (PyValue e : l.values()) {
				String[] r = rowOf(e);
				if (r.length == columns) {
					out.add(r);
				}
			}
		}
		return out;
	}

	/* ---------------- The editor ---------------- */

	/* Opens the file of a stop (if it is in the workspace) and marks the line */
	static void showLine(String path, int line) {
		IFile[] files = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocationURI(new File(path).toURI());
		if (files.length == 0) {
			return;
		}
		IFile file = files[0];
		try {
			IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
			IEditorPart part = IDE.openEditor(page, file, false);
			if (part instanceof ITextEditor editor) {
				IDocument doc = editor.getDocumentProvider().getDocument(editor.getEditorInput());
				PythonBreakpoints.showDebugLine(file, doc, line);
				if (doc != null && line > 0 && line <= doc.getNumberOfLines()) {
					editor.selectAndReveal(doc.getLineOffset(line - 1), 0);
				}
			}
		} catch (Exception e) {
			/* Not shown */
		}
	}
}
