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

import static java.lang.foreign.ValueLayout.JAVA_DOUBLE;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

/** A transformation of coordinates between two coordinate systems (OGRCoordinateTransformation). Close it after use. */
public final class CoordinateTransform implements AutoCloseable {

	private MemorySegment handle;

	CoordinateTransform(MemorySegment handle) {
		this.handle = handle;
	}

	/** Transforms one point: {x, y} in the target system. */
	public double[] transform(double x, double y) {
		double[] xs = { x }, ys = { y };
		transform(xs, ys);
		return new double[] { xs[0], ys[0] };
	}

	/** Transforms the points in place (x and y of the same length). */
	public void transform(double[] x, double[] y) {
		if (x.length != y.length) {
			throw new GdalException("x and y have different lengths");
		}
		if (x.length == 0) {
			return;
		}
		if (handle == null) {
			throw new GdalException("The transformation is closed");
		}
		try (Arena a = Arena.ofConfined()) {
			MemorySegment xs = a.allocateFrom(JAVA_DOUBLE, x), ys = a.allocateFrom(JAVA_DOUBLE, y);
			int ok = (int) Gdal.native_().OCTTransform.invokeExact(handle, x.length, xs, ys, MemorySegment.NULL);
			if (ok == 0) {
				throw Gdal.error("The coordinates could not be transformed");
			}
			MemorySegment.copy(xs, JAVA_DOUBLE, 0, x, 0, x.length);
			MemorySegment.copy(ys, JAVA_DOUBLE, 0, y, 0, y.length);
		} catch (GdalException e) {
			throw e;
		} catch (Throwable t) {
			throw Gdal.wrap(t);
		}
	}

	@Override
	public synchronized void close() {
		if (handle != null) {
			MemorySegment h = handle;
			handle = null;
			try {
				Gdal.native_().OCTDestroyCoordinateTransformation.invokeExact(h);
			} catch (Throwable t) {
				throw Gdal.wrap(t);
			}
		}
	}
}
