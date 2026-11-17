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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;

import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.console.ConsoleInterpreterAction;
import com.eco.bio7.pybridge.EmbeddedPython;
import com.eco.bio7.pybridge.PythonCalls;
import com.eco.bio7.pybridge.PythonImageTransfer;
import com.eco.bio7.pyffm.PySession;
import com.eco.bio7.pyffm.PyValue;
import com.eco.bio7.pythonedit.analysis.PythonProfileMarks;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The profiler of the Python editor: runs the script (or the selection) in the
 * embedded Python with a sampling profiler and cProfile (python_profiler.py),
 * shows the report in the browser view and the times of the lines as marks in
 * the editor.
 */
public final class PythonProfiler {

	private static boolean registered;

	private PythonProfiler() {
	}

	/** Profiles the script of the active Python editor (selection: only the selected lines). UI thread. */
	public static void profileActiveEditor(boolean selection) {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		IEditorPart editor = window == null ? null : window.getActivePage().getActiveEditor();
		IFile file = editor == null ? null : editor.getEditorInput().getAdapter(IFile.class);
		if (file == null || !"py".equalsIgnoreCase(file.getFileExtension()) || file.getLocation() == null) {
			Bio7Dialog.message("Please open a Python script (.py) in the editor.");
			return;
		}
		if (PythonDebugger.isRunning()) {
			Bio7Dialog.message("A Python script is debugged (stop it first).");
			return;
		}
		String code = null;
		int firstLine = 1, lastLine = 0;
		if (selection) {
			if (!(editor instanceof ITextEditor te) || !(te.getSelectionProvider().getSelection() instanceof ITextSelection sel) || sel.getLength() == 0) {
				Bio7Dialog.message("Please select the code to profile.");
				return;
			}
			IDocument doc = te.getDocumentProvider().getDocument(te.getEditorInput());
			try {
				/* Complete lines (an indented selection would be a syntax error) */
				int start = doc.getLineOffset(sel.getStartLine());
				int end = doc.getLineOffset(sel.getEndLine()) + doc.getLineLength(sel.getEndLine());
				code = doc.get(start, end - start);
				firstLine = sel.getStartLine() + 1;
				lastLine = sel.getEndLine() + 1;
				code = dedent(code);
			} catch (BadLocationException e) {
				return;
			}
		}
		/* The report shows the file: the lines of the editor */
		if (editor.isDirty()) {
			editor.doSave(null);
		}
		if (!PythonImageTransfer.ensurePython()) {
			return;
		}
		PySession py = EmbeddedPython.session();
		PythonProfileMarks.clear(file);
		ConsoleInterpreterAction.getInstance().startPython();
		String path = file.getLocation().toOSString().replace("\\", "\\\\").replace("'", "\\'");
		String name = file.getName(), profiled = code;
		int first = firstLine;
		EmbeddedPython.print("\nProfiling " + name + (selection ? " (lines " + firstLine + " to " + lastLine + ")" : "")
				+ (py.isBusy() ? " - Python is busy (e.g., starting): the profile starts when it is free." : " ...") + "\n", false);
		/*
		 * In a thread: the calls wait for Python (e.g., its startup) without blocking
		 * the user interface; the run is a job of Python (time and stop button)
		 */
		Thread starter = new Thread(() -> {
			try {
				register();
				loadModule(py);
				py.assign("_bio7_prof_code", profiled == null ? new PyValue.None() : PyValue.of(profiled));
				py.runAsync("__import__('_bio7prof').run('" + path + "', globals().pop('_bio7_prof_code', None), " + first + ")", "Profile " + name);
			} catch (Exception e) {
				Bio7Dialog.message("The profiler could not be started: " + e.getMessage());
			}
		}, "Bio7 profiler start");
		starter.setDaemon(true);
		starter.start();
	}

	/* The lines without their common indentation (a selection in a function or a block) */
	private static String dedent(String code) {
		String[] lines = code.split("\n", -1);
		int min = Integer.MAX_VALUE;
		for (String l : lines) {
			if (!l.isBlank()) {
				int n = 0;
				while (n < l.length() && (l.charAt(n) == ' ' || l.charAt(n) == '\t')) {
					n++;
				}
				min = Math.min(min, n);
			}
		}
		if (min == 0 || min == Integer.MAX_VALUE) {
			return code;
		}
		StringBuilder b = new StringBuilder();
		for (int i = 0; i < lines.length; i++) {
			b.append(lines[i].isBlank() ? lines[i].strip() : lines[i].substring(min));
			if (i < lines.length - 1) {
				b.append('\n');
			}
		}
		return b.toString();
	}

	/* The result of a profile (thread of Python): the marks in the editor */
	private static synchronized void register() {
		if (registered) {
			return;
		}
		PythonCalls.register("profile_result", (args, value) -> {
			if (args.length < 2) {
				return null;
			}
			JsonNode n = new ObjectMapper().readTree(args[1]);
			double total = n.path("total").asDouble() * 1000;
			List<PythonProfileMarks.Line> lines = new ArrayList<>();
			for (JsonNode l : n.path("lines")) {
				lines.add(new PythonProfileMarks.Line(l.get(0).asInt(), l.get(1).asDouble(), l.get(2).asDouble()));
			}
			IFile[] files = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocationURI(new File(args[0]).toURI());
			if (files.length > 0) {
				IFile f = files[0];
				Display.getDefault().asyncExec(() -> PythonProfileMarks.show(f, lines, total));
			}
			return null;
		});
		registered = true;
	}

	/** The HTML template of the profile reports of Python and R (__TITLE__, __DATA__: the JSON of the data). */
	public static String reportTemplate() throws java.io.IOException {
		try (InputStream in = PythonProfiler.class.getResourceAsStream("profile_report.html")) {
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	private static void loadModule(PySession py) throws Exception {
		PyValue loaded = py.eval("'_bio7prof' in __import__('sys').modules");
		if (!(loaded instanceof PyValue.Array a && a.getDouble(0) != 0)) {
			load(py);
		}
		/* The template of the report (also for a module of an older Bio7) */
		py.assign("_bio7_src", PyValue.of(reportTemplate()));
		py.exec("import sys as _s\n_s.modules['_bio7prof']._TEMPLATE = _bio7_src\ndel _s, _bio7_src");
	}

	private static void load(PySession py) throws Exception {
		String source;
		try (InputStream in = PythonProfiler.class.getResourceAsStream("python_profiler.py")) {
			source = new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
		py.assign("_bio7_src", PyValue.of(source));
		py.exec("import types as _t, sys as _s\n_m = _t.ModuleType('_bio7prof')\nexec(compile(_bio7_src, '<_bio7prof>', 'exec'), _m.__dict__)\n"
				+ "_s.modules['_bio7prof'] = _m\ndel _t, _s, _m, _bio7_src");
	}
}
