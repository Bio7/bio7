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
package com.eco.bio7.gdal.internal;

/**
 * Helpers of the classes of org.gdal (compatibility with the Java bindings of
 * GDAL, built on the FFM bridge). Internal.
 */
public final class GdalCompat {

	private GdalCompat() {
	}

	/** The loaded GDAL; an UnsatisfiedLinkError if GDAL is not loaded (as a missing JNI library). */
	public static GdalNative n() {
		GdalNative g = GdalNative.loaded;
		if (g == null) {
			throw new UnsatisfiedLinkError("GDAL is not loaded (activate GDAL in the preferences)");
		}
		return g;
	}

	/** A native error of a call as unchecked exception. */
	public static RuntimeException wrap(Throwable t) {
		if (t instanceof RuntimeException r) {
			return r;
		}
		if (t instanceof Error e) {
			throw e;
		}
		return new RuntimeException(t);
	}
}
