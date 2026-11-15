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

/**
 * Handles the calls of Python code to the application (the module bio7 of the
 * embedded Python: bio7.call(name, *args)). Called in the thread of Python
 * while Python waits for the result.
 */
@FunctionalInterface
public interface PyCallHandler {

	/**
	 * @param function the name of the function
	 * @param args     the string arguments
	 * @param value    the value argument (e.g., an array to transfer) or null
	 * @return the result (null for None)
	 * @throws Exception an error raised in Python (RuntimeError with the message)
	 */
	PyValue call(String function, String[] args, PyValue value) throws Exception;
}
