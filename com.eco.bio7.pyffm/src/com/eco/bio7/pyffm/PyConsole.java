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
 * The output of Python (sys.stdout and sys.stderr, the prompts of the console).
 * Called in the thread which prints (also threads of Python): must not wait for
 * the UI thread.
 */
@FunctionalInterface
public interface PyConsole {

	/**
	 * @param text  the text
	 * @param error true for sys.stderr (errors, warnings, tracebacks)
	 */
	void write(String text, boolean error);
}
