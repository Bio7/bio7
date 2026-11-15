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
package com.eco.bio7.pyffm;

import java.nio.file.Path;

/**
 * The options to start the embedded Python.
 *
 * @param executable the Python executable (its shared library is loaded, its
 *                   paths of the modules are used, e.g., of a venv or conda)
 * @param console    the output (sys.stdout, sys.stderr) or null
 */
public record PyStartOptions(Path executable, PyConsole console) {

	public static PyStartOptions of(Path executable) {
		return new PyStartOptions(executable, null);
	}

	public PyStartOptions withConsole(PyConsole console) {
		return new PyStartOptions(executable, console);
	}
}
