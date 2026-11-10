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

package com.eco.bio7.rffm;

/** An error of R (the message of R) or of the embedded session. */
public class RException extends Exception {

	private static final long serialVersionUID = 1L;

	public RException(String message) {
		super(message);
	}

	public RException(String message, Throwable cause) {
		super(message, cause);
	}
}
