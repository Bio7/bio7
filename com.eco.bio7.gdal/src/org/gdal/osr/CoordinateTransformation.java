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
package org.gdal.osr;

import static java.lang.foreign.ValueLayout.JAVA_DOUBLE;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import com.eco.bio7.gdal.internal.GdalCompat;
import com.eco.bio7.gdal.internal.GdalNative;

/** A transformation of coordinates (compatible with the Java bindings of GDAL). Release it with delete(). */
public class CoordinateTransformation {

	private MemorySegment handle;

	public CoordinateTransformation(SpatialReference src, SpatialReference dst) {
		try {
			handle = (MemorySegment) GdalCompat.n().OCTNewCoordinateTransformation.invokeExact(src.handle(), dst.handle());
		} catch (Throwable t) {
			throw GdalCompat.wrap(t);
		}
		if (GdalNative.isNull(handle)) {
			throw new RuntimeException("No coordinate transformation: " + com.eco.bio7.gdal.internal.GdalCompat.n().library);
		}
	}

	public synchronized void delete() {
		if (handle != null) {
			MemorySegment h = handle;
			handle = null;
			try {
				GdalCompat.n().OCTDestroyCoordinateTransformation.invokeExact(h);
			} catch (Throwable t) {
				throw GdalCompat.wrap(t);
			}
		}
	}

	public double[] TransformPoint(double x, double y) {
		return TransformPoint(x, y, 0);
	}

	/** {x, y, z} in the target system (NaN values if the point can't be transformed). */
	public double[] TransformPoint(double x, double y, double z) {
		try (Arena a = Arena.ofConfined()) {
			MemorySegment xs = a.allocateFrom(JAVA_DOUBLE, x), ys = a.allocateFrom(JAVA_DOUBLE, y), zs = a.allocateFrom(JAVA_DOUBLE, z);
			int ok = (int) GdalCompat.n().OCTTransform.invokeExact(handle, 1, xs, ys, zs);
			if (ok == 0) {
				return new double[] { Double.NaN, Double.NaN, Double.NaN };
			}
			return new double[] { xs.get(JAVA_DOUBLE, 0), ys.get(JAVA_DOUBLE, 0), zs.get(JAVA_DOUBLE, 0) };
		} catch (Throwable t) {
			throw GdalCompat.wrap(t);
		}
	}

	public void TransformPoint(double[] point) {
		double[] r = TransformPoint(point[0], point[1], point.length > 2 ? point[2] : 0);
		System.arraycopy(r, 0, point, 0, Math.min(point.length, 3));
	}
}
