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

/** The data types of the pixels of GDAL (GDALDataType). */
public enum GdalDataType {
	UNKNOWN(0, 0), BYTE(1, 1), UINT16(2, 2), INT16(3, 2), UINT32(4, 4), INT32(5, 4), FLOAT32(6, 4), FLOAT64(7, 8), CINT16(8, 4), CINT32(9, 8),
	CFLOAT32(10, 8), CFLOAT64(11, 16), UINT64(12, 8), INT64(13, 8), INT8(14, 1);

	/** The value of GDALDataType. */
	public final int code;
	/** The size of a value in bytes. */
	public final int bytes;

	GdalDataType(int code, int bytes) {
		this.code = code;
		this.bytes = bytes;
	}

	public static GdalDataType of(int code) {
		for (GdalDataType t : values()) {
			if (t.code == code) {
				return t;
			}
		}
		return UNKNOWN;
	}

	/** True for complex numbers (read as their real part). */
	public boolean isComplex() {
		return this == CINT16 || this == CINT32 || this == CFLOAT32 || this == CFLOAT64;
	}

	/** True for the floating point types. */
	public boolean isFloat() {
		return this == FLOAT32 || this == FLOAT64 || this == CFLOAT32 || this == CFLOAT64;
	}
}
