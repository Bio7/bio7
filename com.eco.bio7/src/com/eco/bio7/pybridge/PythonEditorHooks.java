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
package com.eco.bio7.pybridge;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.swt.widgets.Display;

import com.eco.bio7.pyffm.PySession;
import com.eco.bio7.pyffm.PyValue;
import com.eco.bio7.pythonedit.analysis.PythonEditorSupport;

/**
 * Connects the Python editor to Bio7: the Python of the preferences for the
 * code analysis, the variables of the running embedded Python (no warnings,
 * completion, hover) and the installation of packages.
 */
public final class PythonEditorHooks {

	private static final Pattern DOTTED = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)*");

	/* The names of the running Python (updated when Python ran code) */
	private static volatile Set<String> names = Set.of();
	private static volatile long namesActivity = Long.MIN_VALUE;

	private PythonEditorHooks() {
	}

	/** Sets the hooks (at the start of Bio7). */
	public static void install() {
		try {
			PythonEditorSupport.executable = PythonEnvironment::executable;
			PythonEditorSupport.liveNames = PythonEditorHooks::liveNames;
			PythonEditorSupport.liveCompletion = PythonEditorHooks::liveCompletion;
			PythonEditorSupport.liveHover = PythonEditorHooks::liveHover;
			PythonEditorSupport.debugHover = PythonEditorHooks::debugHover;
			PythonEditorSupport.debugAction = PythonEditorHooks::debugAction;
			PythonEditorSupport.installPackage = pkg -> Display.getDefault().asyncExec(() -> PythonPackagesView.prepareInstall(pkg));
			PythonEditorSupport.debugScript = () -> com.eco.bio7.pybridge.debug.PythonDebugger.startActiveEditor(false);
			PythonEditorSupport.profileScript = selection -> com.eco.bio7.pybridge.debug.PythonProfiler.profileActiveEditor(selection);
		} catch (LinkageError e) {
			/* The Python editor is not installed */
		}
	}

	/* The running Python if it is idle (the editor never waits for running code) */
	private static PySession idle() {
		PySession s = EmbeddedPython.session();
		return s != null && !s.isBusy() ? s : null;
	}

	private static Collection<String> liveNames() {
		PySession s = idle();
		if (s == null) {
			return EmbeddedPython.isRunning() ? names : Set.of();
		}
		if (s.getActivity() == namesActivity) {
			return names;
		}
		try {
			PyValue v = s.eval("[n for n in globals() if not n.startswith('__')]");
			namesActivity = s.getActivity();
			if (v instanceof PyValue.Str st) {
				names = Set.of(java.util.Arrays.stream(st.values()).filter(java.util.Objects::nonNull).distinct().toArray(String[]::new));
			} else {
				names = Set.of();
			}
		} catch (Exception e) {
			/* Busy: the last names */
		}
		return names;
	}

	/* An expression of a variable (attributes only: nothing is called) */
	private static String resolve(String dotted) {
		String[] parts = dotted.split("\\.");
		StringBuilder attrs = new StringBuilder("[");
		for (int i = 1; i < parts.length; i++) {
			attrs.append('\'').append(parts[i]).append("',");
		}
		attrs.append(']');
		return "__import__('functools').reduce(getattr, " + attrs + ", globals()['" + parts[0] + "'])";
	}

	private static List<String[]> liveCompletion(String base) {
		PySession s = idle();
		if (s == null) {
			return null;
		}
		try {
			PyValue v;
			if (base.isEmpty()) {
				v = s.eval("[[n, type(o).__name__] for n, o in list(globals().items()) if not n.startswith('_') and type(o).__name__ != 'module']");
			} else {
				if (!DOTTED.matcher(base).matches() || !liveNames().contains(base.split("\\.")[0])) {
					return null;
				}
				v = s.eval("[[n, type(getattr(_o, n, None)).__name__] for _o in [" + resolve(base) + "] for n in dir(_o)]");
			}
			List<String[]> out = new ArrayList<>();
			if (v instanceof PyValue.PyList l) {
				for (PyValue e : l.values()) {
					if (e instanceof PyValue.Str st && st.length() == 2) {
						out.add(st.values());
					}
				}
			}
			return out;
		} catch (Exception e) {
			return null;
		}
	}

	/* An expression for the hover while debugging: no calls, statements or assignments (no side effects) */
	private static final Pattern UNSAFE = Pattern.compile("[(;\\n]|(?<![=!<>])=(?!=)|:=|\\b(lambda|import|del|yield|await)\\b");

	/* The debugger stopped: type, size and value of the expression in the frame of the stop */
	private static String debugHover(String expression) {
		if (!com.eco.bio7.pybridge.debug.PythonDebugger.isPaused() || expression.length() > 300 || UNSAFE.matcher(expression).find()) {
			return null;
		}
		try {
			String v = com.eco.bio7.pybridge.debug.PythonDebugger.describe(expression);
			return v == null || v.isBlank() ? null : v;
		} catch (Exception e) {
			return null;
		}
	}

	/*
	 * The actions of the hover while debugging (a background thread): the value of
	 * the expression to the workspace of Python, the table or ImageJ
	 */
	private static void debugAction(String action, String expression) {
		String target = expression.matches("[A-Za-z_][A-Za-z0-9_]*") ? expression : "debug_value";
		Thread t = new Thread(() -> {
			try {
				switch (action) {
				case "table" -> {
					java.util.List<String[]> rows = com.eco.bio7.pybridge.debug.PythonDebugger.toWorkspace(target, expression, "auto");
					Display.getDefault().asyncExec(() -> PythonShellView.fillTable(target, rows));
				}
				case "imagej" -> {
					com.eco.bio7.pybridge.debug.PythonDebugger.toWorkspace(target, expression, null);
					com.eco.bio7.pybridge.debug.PythonDebugger.query(() -> PythonUtil.imageFromPython(target, target));
				}
				default -> {
					com.eco.bio7.pybridge.debug.PythonDebugger.toWorkspace(target, expression, null);
					EmbeddedPython.print("\nCopied to the Python workspace: " + target + "\n", false);
				}
				}
			} catch (Exception e) {
				String m = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
				EmbeddedPython.print("\n" + expression + ": " + m + "\n", true);
			}
		}, "Bio7 Python debug hover");
		t.setDaemon(true);
		t.start();
	}

	private static String liveHover(String word) {
		/* The debugger stopped: the hover of the debugger */
		if (com.eco.bio7.pybridge.debug.PythonDebugger.isPaused()) {
			return null;
		}
		PySession s = idle();
		if (s == null || !DOTTED.matcher(word).matches() || !liveNames().contains(word.split("\\.")[0])) {
			return null;
		}
		try {
			PyValue v = s.eval("(lambda o: 'Running Python: ' + type(o).__name__ + (' ' + str(o.dtype) + ' ' + 'x'.join(map(str, o.shape)) if hasattr(o, 'shape') and hasattr(o, 'dtype')"
					+ " else (' ' + 'x'.join(map(str, o.shape)) if hasattr(o, 'shape') else (' [' + str(len(o)) + ']' if hasattr(o, '__len__') and not isinstance(o, str) else '')))"
					+ " + '\\n' + (lambda r: r if len(r) < 800 else r[:800] + ' ...')(repr(o)))(" + resolve(word) + ")");
			return v instanceof PyValue.Str st ? st.asString() : null;
		} catch (Exception e) {
			return null;
		}
	}
}
