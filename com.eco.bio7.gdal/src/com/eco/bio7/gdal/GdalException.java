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
package com.eco.bio7.gdal;

/** An error of GDAL (the message of GDAL) or of the bridge (e.g., GDAL is not loaded). */
public class GdalException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public GdalException(String message) {
		super(message);
	}

	public GdalException(String message, Throwable cause) {
		super(message, cause);
	}
}
