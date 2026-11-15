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

/** An error of Python (the message is the traceback) or of the embedding. */
public class PyException extends Exception {

	private static final long serialVersionUID = 1L;

	public PyException(String message) {
		super(message);
	}

	public PyException(String message, Throwable cause) {
		super(message, cause);
	}
}
