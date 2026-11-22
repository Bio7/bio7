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
package com.eco.bio7.rbridge;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.widgets.Display;

import com.eco.bio7.pybridge.EmbeddedPython;
import com.eco.bio7.pybridge.PythonCalls;
import com.eco.bio7.pybridge.PythonConvert;
import com.eco.bio7.pybridge.PythonImageTransfer;
import com.eco.bio7.pyffm.PySession;
import com.eco.bio7.pyffm.PyValue;
import com.eco.bio7.rffm.RException;
import com.eco.bio7.rffm.RSession;
import com.eco.bio7.rffm.RValue;

/**
 * The API of Bio7 for the embedded R (as the module bio7 of the embedded
 * Python): the data of ImageJ, the embedded Python and the Java API of Bio7,
 * ImageJ and Java (see bio7_r_api.R for the functions of R). R calls the
 * function .bio7_call(function, args, value) which runs the functions of
 * {@link PythonCalls} (the same as for Python) with the values converted.
 */
public final class RCalls {

	private static final PythonCalls CALLS = new PythonCalls();
	private static volatile RSession installed;

	private RCalls() {
	}

	/** Installs the functions of the API in the embedded R (once per session). */
	public static synchronized void install(RSession session) {
		if (session == null || session == installed) {
			return;
		}
		try (InputStream in = RCalls.class.getResourceAsStream("bio7_r_api.R")) {
			session.registerFunction(".bio7_call", RCalls::call);
			session.voidEval(new String(in.readAllBytes(), StandardCharsets.UTF_8));
			/* The simulation API (grid_model(), simulation(), ...) */
			com.eco.bio7.simulation.ScriptSimulation.installR(session);
			installed = session;
		} catch (RException | IOException | RuntimeException e) {
			System.err.println("The Bio7 API for R could not be installed: " + e.getMessage());
		}
	}

	/* .bio7_call(function, args (character), value): runs in a thread of Java while R waits */
	private static RValue call(List<RValue> a) throws Exception {
		String function = a.get(0).strings()[0];
		String[] args = a.size() > 1 && !a.get(1).isNull() ? a.get(1).strings() : new String[0];
		RValue value = a.size() > 2 ? a.get(2) : RValue.NULL;
		switch (function) {
		case "python":
			return python(args[0]);
		case "to_python":
			python().assign(args[0], PythonConvert.fromR(value));
			return RValue.NULL;
		default:
			break;
		}
		/* The Java API: scalars as scalars (an R vector of length 1), the proxies as handles */
		boolean java = function.startsWith("java_");
		PyValue v = value == null || value.isNull() ? null : java ? toPython(value) : PythonConvert.fromR(value);
		PyValue result = CALLS.call(function, args, v);
		return result == null ? RValue.NULL : PythonConvert.toR(result);
	}

	/* ---------------- The embedded Python ---------------- */

	/* The session of Python (started if necessary) */
	private static PySession python() throws Exception {
		PySession py = EmbeddedPython.session();
		if (py == null) {
			boolean[] ok = new boolean[1];
			Display.getDefault().syncExec(() -> ok[0] = PythonImageTransfer.ensurePython());
			py = EmbeddedPython.session();
			if (!ok[0] || py == null) {
				throw new IllegalStateException("Python is not running (please start Python)");
			}
		}
		return py;
	}

	/* Runs Python code (as the console): the value of a last expression for R */
	private static RValue python(String code) throws Exception {
		PySession py = python();
		py.assign("_bio7_rcode", PyValue.of(code));
		py.exec("""
				def _bio7_rrun():
				    import ast, sys
				    main = sys.modules["__main__"].__dict__
				    tree = ast.parse(main.pop("_bio7_rcode"), "<R>", "exec")
				    last = None
				    if tree.body and isinstance(tree.body[-1], ast.Expr):
				        last = ast.Expression(tree.body.pop().value)
				    exec(compile(tree, "<R>", "exec"), main)
				    return None if last is None else eval(compile(last, "<R>", "eval"), main)
				_bio7_rresult = _bio7_rrun()
				del _bio7_rrun
				""");
		try {
			return PythonConvert.toR(py.eval("_bio7_rresult"));
		} finally {
			py.exec("del _bio7_rresult");
		}
	}

	/* ---------------- R -> Python values for the Java API ---------------- */

	/*
	 * As PythonConvert.fromR, but a vector of length 1 (without dim) as scalar (an
	 * integral number as integer: R writes 5 for an int argument), a named list as
	 * dict (the handles of the Java objects: list(`__java__` = id))
	 */
	static PyValue toPython(RValue v) {
		if (v == null || v.isNull()) {
			return PyValue.None.INSTANCE;
		}
		if (v instanceof RValue.List l && !l.isDataFrame()) {
			String[] names = l.names();
			boolean named = names != null && java.util.Arrays.stream(names).anyMatch(n -> n != null && !n.isEmpty());
			if (named) {
				Map<String, PyValue> m = new LinkedHashMap<>();
				for (int i = 0; i < l.length(); i++) {
					m.put(names[i], toPython(l.values().get(i)));
				}
				return new PyValue.PyDict(m);
			}
			List<PyValue> items = new java.util.ArrayList<>();
			for (RValue e : l.values()) {
				items.add(toPython(e));
			}
			return new PyValue.PyList(items);
		}
		if (v.length() == 1 && v.dim() == null && !v.isFactor()) {
			switch (v) {
			case RValue.Num n: {
				double d = n.values()[0];
				if (!Double.isNaN(d) && d == Math.rint(d) && Math.abs(d) <= 9.007199254740992E15) {
					return new PyValue.Array(new long[] { (long) d }, "i8", new int[0]);
				}
				return PyValue.scalar(d);
			}
			case RValue.Int i:
				return new PyValue.Array(new long[] { i.values()[0] }, "i8", new int[0]);
			case RValue.Logical b:
				return new PyValue.Array(new boolean[] { b.values()[0] != 0 }, "b1", new int[0]);
			case RValue.Str s:
				return PyValue.of(s.values()[0]);
			default:
				break;
			}
		}
		return PythonConvert.fromR(v);
	}
}
