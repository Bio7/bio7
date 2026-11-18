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
package org.gdal.gdal;

import static java.lang.foreign.ValueLayout.JAVA_DOUBLE;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_SHORT;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;

import com.eco.bio7.gdal.internal.GdalCompat;
import com.eco.bio7.gdal.internal.GdalNative;

/**
 * A band of a dataset (compatible with the Java bindings of GDAL). The raster
 * I/O with direct buffers passes their memory to GDAL (no copy).
 */
public class Band {

	private final MemorySegment handle;
	/* The dataset of the band (kept: the band is valid while its dataset is open) */
	private final Dataset dataset;

	Band(MemorySegment handle, Dataset dataset) {
		this.handle = handle;
		this.dataset = dataset;
	}

	private MemorySegment h() {
		if (dataset != null) {
			dataset.handle();
		}
		return handle;
	}

	public int getXSize() {
		try {
			return (int) GdalCompat.n().GDALGetRasterBandXSize.invokeExact(h());
		} catch (Throwable t) {
			throw GdalCompat.wrap(t);
		}
	}

	public int GetXSize() {
		return getXSize();
	}

	public int getYSize() {
		try {
			return (int) GdalCompat.n().GDALGetRasterBandYSize.invokeExact(h());
		} catch (Throwable t) {
			throw GdalCompat.wrap(t);
		}
	}

	public int GetYSize() {
		return getYSize();
	}

	public int getDataType() {
		try {
			return (int) GdalCompat.n().GDALGetRasterDataType.invokeExact(h());
		} catch (Throwable t) {
			throw GdalCompat.wrap(t);
		}
	}

	public int GetRasterDataType() {
		return getDataType();
	}

	public int GetColorInterpretation() {
		try {
			return (int) GdalCompat.n().GDALGetRasterColorInterpretation.invokeExact(h());
		} catch (Throwable t) {
			throw GdalCompat.wrap(t);
		}
	}

	public int GetRasterColorInterpretation() {
		return GetColorInterpretation();
	}

	public int SetColorInterpretation(int value) {
		try {
			return (int) GdalCompat.n().GDALSetRasterColorInterpretation.invokeExact(h(), value);
		} catch (Throwable t) {
			throw GdalCompat.wrap(t);
		}
	}

	public int SetRasterColorInterpretation(int value) {
		return SetColorInterpretation(value);
	}

	/** The color table (a copy of its entries), null if the band has none. */
	public ColorTable GetColorTable() {
		GdalNative g = GdalCompat.n();
		try {
			MemorySegment ct = (MemorySegment) g.GDALGetRasterColorTable.invokeExact(h());
			if (GdalNative.isNull(ct)) {
				return null;
			}
			int n = (int) g.GDALGetColorEntryCount.invokeExact(ct);
			int[] argb = new int[n];
			for (int i = 0; i < n; i++) {
				MemorySegment e = ((MemorySegment) g.GDALGetColorEntry.invokeExact(ct, i)).reinterpret(8);
				argb[i] = clamp(e.get(JAVA_SHORT, 6)) << 24 | clamp(e.get(JAVA_SHORT, 0)) << 16 | clamp(e.get(JAVA_SHORT, 2)) << 8
						| clamp(e.get(JAVA_SHORT, 4));
			}
			return new ColorTable(argb);
		} catch (Throwable t) {
			throw GdalCompat.wrap(t);
		}
	}

	public ColorTable GetRasterColorTable() {
		return GetColorTable();
	}

	private static int clamp(int v) {
		return Math.max(0, Math.min(255, v));
	}

	/** Sets the color table (copied by GDAL). */
	public int SetColorTable(ColorTable table) {
		GdalNative g = GdalCompat.n();
		try (Arena a = Arena.ofConfined()) {
			if (table == null) {
				return (int) g.GDALSetRasterColorTable.invokeExact(h(), MemorySegment.NULL);
			}
			/* GPI_RGB */
			MemorySegment ct = (MemorySegment) g.GDALCreateColorTable.invokeExact(1);
			try {
				MemorySegment e = a.allocate(JAVA_SHORT, 4);
				int[] argb = table.argb();
				for (int i = 0; i < argb.length; i++) {
					e.set(JAVA_SHORT, 0, (short) (argb[i] >> 16 & 0xff));
					e.set(JAVA_SHORT, 2, (short) (argb[i] >> 8 & 0xff));
					e.set(JAVA_SHORT, 4, (short) (argb[i] & 0xff));
					e.set(JAVA_SHORT, 6, (short) (argb[i] >>> 24));
					g.GDALSetColorEntry.invokeExact(ct, i, e);
				}
				return (int) g.GDALSetRasterColorTable.invokeExact(h(), ct);
			} finally {
				g.GDALDestroyColorTable.invokeExact(ct);
			}
		} catch (Throwable t) {
			throw GdalCompat.wrap(t);
		}
	}

	public int SetRasterColorTable(ColorTable table) {
		return SetColorTable(table);
	}

	/** val[0]: the no data value, null if not set. */
	public void GetNoDataValue(Double[] val) {
		try (Arena a = Arena.ofConfined()) {
			MemorySegment ok = a.allocate(JAVA_INT);
			double v = (double) GdalCompat.n().GDALGetRasterNoDataValue.invokeExact(h(), ok);
			val[0] = ok.get(JAVA_INT, 0) != 0 ? v : null;
		} catch (Throwable t) {
			throw GdalCompat.wrap(t);
		}
	}

	public int SetNoDataValue(double value) {
		try {
			return (int) GdalCompat.n().GDALSetRasterNoDataValue.invokeExact(h(), value);
		} catch (Throwable t) {
			throw GdalCompat.wrap(t);
		}
	}

	/** val[0]: the minimum of the metadata, null if not known. */
	public void GetMinimum(Double[] val) {
		try (Arena a = Arena.ofConfined()) {
			MemorySegment ok = a.allocate(JAVA_INT);
			double v = (double) GdalCompat.n().GDALGetRasterMinimum.invokeExact(h(), ok);
			val[0] = ok.get(JAVA_INT, 0) != 0 ? v : null;
		} catch (Throwable t) {
			throw GdalCompat.wrap(t);
		}
	}

	/** val[0]: the maximum of the metadata, null if not known. */
	public void GetMaximum(Double[] val) {
		try (Arena a = Arena.ofConfined()) {
			MemorySegment ok = a.allocate(JAVA_INT);
			double v = (double) GdalCompat.n().GDALGetRasterMaximum.invokeExact(h(), ok);
			val[0] = ok.get(JAVA_INT, 0) != 0 ? v : null;
		} catch (Throwable t) {
			throw GdalCompat.wrap(t);
		}
	}

	public void ComputeRasterMinMax(double[] minmax) {
		ComputeRasterMinMax(minmax, 0);
	}

	/** minmax[0], minmax[1]: the minimum and maximum of the pixels (approximate: approxOk != 0). */
	public void ComputeRasterMinMax(double[] minmax, int approxOk) {
		try (Arena a = Arena.ofConfined()) {
			MemorySegment s = a.allocate(JAVA_DOUBLE, 2);
			GdalCompat.n().GDALComputeRasterMinMax.invokeExact(h(), approxOk, s);
			MemorySegment.copy(s, JAVA_DOUBLE, 0, minmax, 0, Math.min(2, minmax.length));
		} catch (Throwable t) {
			throw GdalCompat.wrap(t);
		}
	}

	public int SetStatistics(double min, double max, double mean, double stddev) {
		try {
			return (int) GdalCompat.n().GDALSetRasterStatistics.invokeExact(h(), min, max, mean, stddev);
		} catch (Throwable t) {
			throw GdalCompat.wrap(t);
		}
	}

	public int Fill(double real) {
		return Fill(real, 0.0);
	}

	public int Fill(double real, double imaginary) {
		try {
			return (int) GdalCompat.n().GDALFillRaster.invokeExact(h(), real, imaginary);
		} catch (Throwable t) {
			throw GdalCompat.wrap(t);
		}
	}

	public int GetOverviewCount() {
		try {
			return (int) GdalCompat.n().GDALGetOverviewCount.invokeExact(h());
		} catch (Throwable t) {
			throw GdalCompat.wrap(t);
		}
	}

	public Band GetOverview(int index) {
		try {
			MemorySegment b = (MemorySegment) GdalCompat.n().GDALGetOverview.invokeExact(h(), index);
			return GdalNative.isNull(b) ? null : new Band(b, dataset);
		} catch (Throwable t) {
			throw GdalCompat.wrap(t);
		}
	}

	/* ---------------- Raster I/O with direct buffers ---------------- */

	public int ReadRaster_Direct(int xoff, int yoff, int xsize, int ysize, ByteBuffer buffer) {
		return ReadRaster_Direct(xoff, yoff, xsize, ysize, xsize, ysize, getDataType(), buffer, 0, 0);
	}

	public int ReadRaster_Direct(int xoff, int yoff, int xsize, int ysize, int bufXSize, int bufYSize, ByteBuffer buffer) {
		return ReadRaster_Direct(xoff, yoff, xsize, ysize, bufXSize, bufYSize, getDataType(), buffer, 0, 0);
	}

	public int ReadRaster_Direct(int xoff, int yoff, int xsize, int ysize, int bufXSize, int bufYSize, int bufType, ByteBuffer buffer) {
		return ReadRaster_Direct(xoff, yoff, xsize, ysize, bufXSize, bufYSize, bufType, buffer, 0, 0);
	}

	public int ReadRaster_Direct(int xoff, int yoff, int xsize, int ysize, int bufXSize, int bufYSize, int bufType, ByteBuffer buffer, int pixelSpace) {
		return ReadRaster_Direct(xoff, yoff, xsize, ysize, bufXSize, bufYSize, bufType, buffer, pixelSpace, 0);
	}

	/** Reads pixels into a direct buffer (GDALRasterIO): CE_None (0) or an error. */
	public int ReadRaster_Direct(int xoff, int yoff, int xsize, int ysize, int bufXSize, int bufYSize, int bufType, ByteBuffer buffer, int pixelSpace,
			int lineSpace) {
		return rasterIO(0, xoff, yoff, xsize, ysize, bufXSize, bufYSize, bufType, buffer, pixelSpace, lineSpace);
	}

	/** Reads pixels into a new direct buffer (native byte order), null if it fails. */
	public ByteBuffer ReadRaster_Direct(int xoff, int yoff, int xsize, int ysize, int bufXSize, int bufYSize, int bufType) {
		ByteBuffer b = ByteBuffer.allocateDirect(bufXSize * bufYSize * (gdal.GetDataTypeSize(bufType) / 8)).order(java.nio.ByteOrder.nativeOrder());
		return ReadRaster_Direct(xoff, yoff, xsize, ysize, bufXSize, bufYSize, bufType, b) == 0 ? b : null;
	}

	public ByteBuffer ReadRaster_Direct(int xoff, int yoff, int xsize, int ysize, int bufType) {
		return ReadRaster_Direct(xoff, yoff, xsize, ysize, xsize, ysize, bufType);
	}

	public ByteBuffer ReadRaster_Direct(int xoff, int yoff, int xsize, int ysize) {
		return ReadRaster_Direct(xoff, yoff, xsize, ysize, xsize, ysize, getDataType());
	}

	public int WriteRaster_Direct(int xoff, int yoff, int xsize, int ysize, ByteBuffer buffer) {
		return WriteRaster_Direct(xoff, yoff, xsize, ysize, xsize, ysize, getDataType(), buffer, 0, 0);
	}

	public int WriteRaster_Direct(int xoff, int yoff, int xsize, int ysize, int bufType, ByteBuffer buffer) {
		return WriteRaster_Direct(xoff, yoff, xsize, ysize, xsize, ysize, bufType, buffer, 0, 0);
	}

	public int WriteRaster_Direct(int xoff, int yoff, int xsize, int ysize, int bufXSize, int bufYSize, ByteBuffer buffer) {
		return WriteRaster_Direct(xoff, yoff, xsize, ysize, bufXSize, bufYSize, getDataType(), buffer, 0, 0);
	}

	public int WriteRaster_Direct(int xoff, int yoff, int xsize, int ysize, int bufXSize, int bufYSize, int bufType, ByteBuffer buffer) {
		return WriteRaster_Direct(xoff, yoff, xsize, ysize, bufXSize, bufYSize, bufType, buffer, 0, 0);
	}

	public int WriteRaster_Direct(int xoff, int yoff, int xsize, int ysize, int bufXSize, int bufYSize, int bufType, ByteBuffer buffer, int pixelSpace) {
		return WriteRaster_Direct(xoff, yoff, xsize, ysize, bufXSize, bufYSize, bufType, buffer, pixelSpace, 0);
	}

	/** Writes pixels of a direct buffer (GDALRasterIO): CE_None (0) or an error. */
	public int WriteRaster_Direct(int xoff, int yoff, int xsize, int ysize, int bufXSize, int bufYSize, int bufType, ByteBuffer buffer, int pixelSpace,
			int lineSpace) {
		return rasterIO(1, xoff, yoff, xsize, ysize, bufXSize, bufYSize, bufType, buffer, pixelSpace, lineSpace);
	}

	private int rasterIO(int rw, int xoff, int yoff, int xsize, int ysize, int bx, int by, int type, ByteBuffer buffer, int pixelSpace, int lineSpace) {
		if (!buffer.isDirect()) {
			throw new IllegalArgumentException("The buffer must be a direct buffer (ByteBuffer.allocateDirect)");
		}
		int pixel = pixelSpace > 0 ? pixelSpace : gdal.GetDataTypeSize(type) / 8;
		long needed = (long) (lineSpace > 0 ? lineSpace : (long) bx * pixel) * (by - 1) + (long) bx * pixel;
		if (buffer.capacity() < needed) {
			throw new IllegalArgumentException("The buffer is too small: " + buffer.capacity() + " < " + needed + " bytes");
		}
		/* The memory of the buffer (from its start, as the JNI bindings) */
		MemorySegment seg = MemorySegment.ofBuffer(buffer.duplicate().clear());
		try {
			return (int) GdalCompat.n().GDALRasterIO.invokeExact(h(), rw, xoff, yoff, xsize, ysize, seg, bx, by, type, pixelSpace, lineSpace);
		} catch (Throwable t) {
			throw GdalCompat.wrap(t);
		}
	}

	public void delete() {
	}
}
