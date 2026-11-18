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

/** The types of the attribute fields of vector layers (OGRFieldType). */
public enum FieldType {
	INTEGER(0), REAL(2), STRING(4), DATE(9), TIME(10), DATETIME(11), INTEGER64(12);

	public final int code;

	FieldType(int code) {
		this.code = code;
	}
}
