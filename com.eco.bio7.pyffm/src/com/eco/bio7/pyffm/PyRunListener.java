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
 * Notified when user code of the embedded Python starts and ends (a console
 * line, a script): e.g., to show that Python runs. Called in the thread of
 * Python without the GIL; the methods must return at once.
 */
public interface PyRunListener {

	/** User code starts (label: e.g., "Console" or the name of the script). */
	void started(String label);

	/** The user code has ended (also after an error or an interruption). */
	void finished();
}
