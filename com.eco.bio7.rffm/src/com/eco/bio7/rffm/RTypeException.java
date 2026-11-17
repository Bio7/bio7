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

/**
 * Thrown when an R value is read as a type it can't be read as, e.g.,
 * {@code doubles()} of a function or {@code firstString()} of an empty vector.
 */
public class RTypeException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public RTypeException(String message) {
		super(message);
	}
}
