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
package com.eco.bio7.pybridge;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import com.eco.bio7.pyffm.PyException;
import com.eco.bio7.pyffm.PySession;
import com.eco.bio7.pyffm.PyValue;

/**
 * The arguments of a call of a Bio7 helper function in Python. The assignment
 * and the call are two calls of the session, so another thread (e.g., the
 * Python-Shell view refreshing its variables while the chunks of a document
 * run) can call Python in between. Each call therefore gets a variable of its
 * own (_bio7_args_N), which the call removes again.
 */
final class PythonArgs {

	private static final AtomicLong COUNT = new AtomicLong();

	private PythonArgs() {
	}

	/**
	 * Assigns the arguments and returns the Python expression which takes them
	 * (and removes the variable), e.g., for f(*expression).
	 */
	static String assign(PySession py, List<PyValue> args) throws PyException {
		String name = "_bio7_args_" + COUNT.incrementAndGet();
		py.assign(name, new PyValue.PyList(args));
		return "__import__('__main__').__dict__.pop('" + name + "')";
	}
}
