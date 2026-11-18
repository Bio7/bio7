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
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_SHORT;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

import com.eco.bio7.gdal.internal.GdalNative;

/**
 * A band of a raster dataset: its pixels as Java arrays (row by row, the first
 * row is the top), the no data value, the color table and the statistics.
 * GDAL converts the pixels to the type of the array.
 */
public final class GdalBand {

	private static final int GF_READ = 0, GF_WRITE = 1;
	/* GDALColorInterp */
	private static final String[] COLOR = { "Undefined", "Gray", "Palette", "Red", "Green", "Blue", "Alpha", "Hue", "Saturation", "Lightness", "Cyan",
			"Magenta", "Yellow", "Black", "YCbCr_Y", "YCbCr_Cb", "YCbCr_Cr" };

	private final GdalDataset dataset;
	private final MemorySegment handle;
	private final int index;

	GdalBand(GdalDataset dataset, MemorySegment handle, int index) {
		this.dataset = dataset;
		this.handle = handle;
		this.index = index;
	}

	private MemorySegment h() {
		dataset.handle();
		return handle;
	}

	public int index() {
		return index;
	}

	public int width() {
		return dataset.width();
	}

	public int height() {
		return dataset.height();
	}

	public GdalDataType dataType() {
		try {
			return GdalDataType.of((int) Gdal.native_().GDALGetRasterDataType.invokeExact(h()));
		} catch (Throwable t) {
			throw Gdal.wrap(t);
		}
	}

	/** The value of the pixels without data (null if not set). */
	public Double noData() {
		try (Arena a = Arena.ofConfined()) {
			MemorySegment ok = a.allocate(JAVA_INT);
			double v = (double) Gdal.native_().GDALGetRasterNoDataValue.invokeExact(h(), ok);
			return ok.get(JAVA_INT, 0) != 0 ? v : null;
		} catch (Throwable t) {
			throw Gdal.wrap(t);
		}
	}

	public void setNoData(double value) {
		try {
			int err = (int) Gdal.native_().GDALSetRasterNoDataValue.invokeExact(h(), value);
			if (err != 0) {
				throw Gdal.error("The no data value could not be set");
			}
		} catch (GdalException e) {
			throw e;
		} catch (Throwable t) {
			throw Gdal.wrap(t);
		}
	}

	/** The color interpretation (e.g., "Gray", "Red", "Palette", "Alpha"). */
	public String colorInterpretation() {
		try {
			int c = (int) Gdal.native_().GDALGetRasterColorInterpretation.invokeExact(h());
			return c >= 0 && c < COLOR.length ? COLOR[c] : "Undefined";
		} catch (Throwable t) {
			throw Gdal.wrap(t);
		}
	}

	/** The color table as ARGB values (index -> color), null if the band has none. */
	public int[] colorTable() {
		try {
			MemorySegment ct = (MemorySegment) Gdal.native_().GDALGetRasterColorTable.invokeExact(h());
			if (GdalNative.isNull(ct)) {
				return null;
			}
			int n = (int) Gdal.native_().GDALGetColorEntryCount.invokeExact(ct);
			int[] argb = new int[n];
			for (int i = 0; i < n; i++) {
				/* GDALColorEntry: four shorts c1..c4 (RGBA) */
				MemorySegment e = ((MemorySegment) Gdal.native_().GDALGetColorEntry.invokeExact(ct, i)).reinterpret(8);
				int r = e.get(JAVA_SHORT, 0), g = e.get(JAVA_SHORT, 2), b = e.get(JAVA_SHORT, 4), alpha = e.get(JAVA_SHORT, 6);
				argb[i] = (clamp(alpha) << 24) | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
			}
			return argb;
		} catch (Throwable t) {
			throw Gdal.wrap(t);
		}
	}

	private static int clamp(int v) {
		return Math.max(0, Math.min(255, v));
	}

	/** The scale of the values (value = raw * scale + offset), 1 if not set. */
	public double scale() {
		try (Arena a = Arena.ofConfined()) {
			return (double) Gdal.native_().GDALGetRasterScale.invokeExact(h(), a.allocate(JAVA_INT));
		} catch (Throwable t) {
			throw Gdal.wrap(t);
		}
	}

	/** The offset of the values (value = raw * scale + offset), 0 if not set. */
	public double offset() {
		try (Arena a = Arena.ofConfined()) {
			return (double) Gdal.native_().GDALGetRasterOffset.invokeExact(h(), a.allocate(JAVA_INT));
		} catch (Throwable t) {
			throw Gdal.wrap(t);
		}
	}

	/** {min, max} of the values (without the no data value; approximate: faster for large rasters). */
	public double[] minMax(boolean approximate) {
		try (Arena a = Arena.ofConfined()) {
			MemorySegment mm = a.allocate(JAVA_DOUBLE, 2);
			mm.set(JAVA_DOUBLE, 0, Double.NaN);
			mm.set(JAVA_DOUBLE, 8, Double.NaN);
			Gdal.native_().CPLErrorReset.invokeExact();
			Gdal.native_().GDALComputeRasterMinMax.invokeExact(h(), approximate ? 1 : 0, mm);
			return mm.toArray(JAVA_DOUBLE);
		} catch (Throwable t) {
			throw Gdal.wrap(t);
		}
	}

	/* ---------------- Reading ---------------- */

	public double[] readDoubles() {
		return readDoubles(0, 0, width(), height());
	}

	/** The pixels of a window as double values (row by row). */
	public double[] readDoubles(int x, int y, int w, int h) {
		return (double[]) read(x, y, w, h, GdalDataType.FLOAT64, JAVA_DOUBLE, new double[count(w, h)]);
	}

	public float[] readFloats() {
		return readFloats(0, 0, width(), height());
	}

	public float[] readFloats(int x, int y, int w, int h) {
		return (float[]) read(x, y, w, h, GdalDataType.FLOAT32, ValueLayout.JAVA_FLOAT, new float[count(w, h)]);
	}

	public int[] readInts() {
		return readInts(0, 0, width(), height());
	}

	public int[] readInts(int x, int y, int w, int h) {
		return (int[]) read(x, y, w, h, GdalDataType.INT32, JAVA_INT, new int[count(w, h)]);
	}

	/** The pixels as 16 bit values (UInt16 or Int16 rasters; unsigned: & 0xffff). */
	public short[] readShorts() {
		return readShorts(0, 0, width(), height());
	}

	public short[] readShorts(int x, int y, int w, int h) {
		GdalDataType t = dataType() == GdalDataType.INT16 ? GdalDataType.INT16 : GdalDataType.UINT16;
		return (short[]) read(x, y, w, h, t, JAVA_SHORT, new short[count(w, h)]);
	}

	/** The pixels as 8 bit values (unsigned: & 0xff). */
	public byte[] readBytes() {
		return readBytes(0, 0, width(), height());
	}

	public byte[] readBytes(int x, int y, int w, int h) {
		return (byte[]) read(x, y, w, h, GdalDataType.BYTE, ValueLayout.JAVA_BYTE, new byte[count(w, h)]);
	}

	/**
	 * The pixels of a window resampled to the size of the buffer (e.g., an
	 * overview of a large raster: GDAL uses the overviews of the file).
	 */
	public float[] readFloats(int x, int y, int w, int h, int bufferWidth, int bufferHeight) {
		return (float[]) read(x, y, w, h, bufferWidth, bufferHeight, GdalDataType.FLOAT32, ValueLayout.JAVA_FLOAT, new float[count(bufferWidth, bufferHeight)]);
	}

	private static int count(int w, int h) {
		long n = (long) w * h;
		if (w < 0 || h < 0 || n > Integer.MAX_VALUE - 8) {
			throw new GdalException("The window is too large for a Java array: " + w + " x " + h + " (read it in parts)");
		}
		return (int) n;
	}

	private Object read(int x, int y, int w, int h, GdalDataType type, ValueLayout layout, Object array) {
		return read(x, y, w, h, w, h, type, layout, array);
	}

	private Object read(int x, int y, int w, int h, int bw, int bh, GdalDataType type, ValueLayout layout, Object array) {
		try (Arena a = Arena.ofConfined()) {
			MemorySegment buf = a.allocate(layout, (long) bw * bh);
			Gdal.native_().CPLErrorReset.invokeExact();
			int err = (int) Gdal.native_().GDALRasterIO.invokeExact(h(), GF_READ, x, y, w, h, buf, bw, bh, type.code, 0, 0);
			if (err != 0) {
				throw Gdal.error("The pixels could not be read (band " + index + ")");
			}
			MemorySegment.copy(buf, layout, 0, array, 0, bw * bh);
			return array;
		} catch (GdalException e) {
			throw e;
		} catch (Throwable t) {
			throw Gdal.wrap(t);
		}
	}

	/* ---------------- Writing ---------------- */

	public void write(double[] values) {
		write(0, 0, width(), height(), values);
	}

	public void write(int x, int y, int w, int h, double[] values) {
		write(x, y, w, h, GdalDataType.FLOAT64, JAVA_DOUBLE, values, values.length);
	}

	public void write(float[] values) {
		write(0, 0, width(), height(), values);
	}

	public void write(int x, int y, int w, int h, float[] values) {
		write(x, y, w, h, GdalDataType.FLOAT32, ValueLayout.JAVA_FLOAT, values, values.length);
	}

	public void write(int[] values) {
		write(0, 0, width(), height(), values);
	}

	public void write(int x, int y, int w, int h, int[] values) {
		write(x, y, w, h, GdalDataType.INT32, JAVA_INT, values, values.length);
	}

	/** Writes 16 bit values (as UInt16, or Int16 for an Int16 band). */
	public void write(short[] values) {
		write(0, 0, width(), height(), values);
	}

	public void write(int x, int y, int w, int h, short[] values) {
		GdalDataType t = dataType() == GdalDataType.INT16 ? GdalDataType.INT16 : GdalDataType.UINT16;
		write(x, y, w, h, t, JAVA_SHORT, values, values.length);
	}

	/** Writes 8 bit values (unsigned). */
	public void write(byte[] values) {
		write(0, 0, width(), height(), values);
	}

	public void write(int x, int y, int w, int h, byte[] values) {
		write(x, y, w, h, GdalDataType.BYTE, ValueLayout.JAVA_BYTE, values, values.length);
	}

	private void write(int x, int y, int w, int h, GdalDataType type, ValueLayout layout, Object array, int length) {
		if (length < (long) w * h) {
			throw new GdalException("The array has " + length + " values for " + w + " x " + h + " pixels");
		}
		try (Arena a = Arena.ofConfined()) {
			MemorySegment buf = a.allocate(layout, (long) w * h);
			MemorySegment.copy(array, 0, buf, layout, 0, w * h);
			Gdal.native_().CPLErrorReset.invokeExact();
			int err = (int) Gdal.native_().GDALRasterIO.invokeExact(h(), GF_WRITE, x, y, w, h, buf, w, h, type.code, 0, 0);
			if (err != 0) {
				throw Gdal.error("The pixels could not be written (band " + index + ")");
			}
		} catch (GdalException e) {
			throw e;
		} catch (Throwable t) {
			throw Gdal.wrap(t);
		}
	}

	/* ---------------- Contours and polygons (gdal_alg.h) ---------------- */

	/**
	 * Contour lines of the band into a vector layer (gdal_contour): the fields "ID"
	 * and "ELEV" are created if they don't exist; the no data value of the band is
	 * used. Further options of GDALContourGenerateEx, e.g. "LEVEL_BASE=100",
	 * "FIXED_LEVELS=500,1000", "POLYGONIZE=YES" (with "ELEV_FIELD_MIN=..." and
	 * "ELEV_FIELD_MAX=...").
	 */
	public void contours(VectorLayer target, double interval, String... options) {
		GdalNative g = Gdal.native_();
		if (g.GDALContourGenerateEx == null) {
			throw new GdalException("Contours need GDAL 2.4 or later");
		}
		java.util.List<String> opts = new java.util.ArrayList<>();
		opts.add("LEVEL_INTERVAL=" + interval);
		if (target.fieldIndex("ID") < 0) {
			target.createField("ID", FieldType.INTEGER);
		}
		boolean polygons = java.util.Arrays.stream(options).anyMatch(o -> o.equalsIgnoreCase("POLYGONIZE=YES"));
		if (!polygons) {
			if (target.fieldIndex("ELEV") < 0) {
				target.createField("ELEV", FieldType.REAL);
			}
			opts.add("ELEV_FIELD=" + target.fieldIndex("ELEV"));
		}
		opts.add("ID_FIELD=" + target.fieldIndex("ID"));
		Double nd = noData();
		if (nd != null) {
			opts.add("NODATA=" + nd);
		}
		opts.addAll(java.util.Arrays.asList(options));
		try (Arena a = Arena.ofConfined()) {
			g.CPLErrorReset.invokeExact();
			int err = (int) g.GDALContourGenerateEx.invokeExact(h(), target.h(), GdalNative.stringList(a, opts), MemorySegment.NULL, MemorySegment.NULL);
			if (err != 0) {
				throw Gdal.error("The contours could not be computed");
			}
		} catch (GdalException e) {
			throw e;
		} catch (Throwable t) {
			throw Gdal.wrap(t);
		}
	}

	/**
	 * Polygons of the areas of equal values (gdal_polygonize) into a vector layer:
	 * the value of each polygon in the field (created if it doesn't exist: integer,
	 * or real for float bands). useMask: the pixels without data (the mask of the
	 * band) get no polygons. Options of GDALPolygonize, e.g. "8CONNECTED=8".
	 */
	public void polygonize(VectorLayer target, String field, boolean useMask, String... options) {
		GdalNative g = Gdal.native_();
		boolean real = dataType().isFloat();
		if (target.fieldIndex(field) < 0) {
			target.createField(field, real ? FieldType.REAL : FieldType.INTEGER);
		}
		int idx = target.fieldIndex(field);
		try (Arena a = Arena.ofConfined()) {
			MemorySegment mask = useMask ? (MemorySegment) g.GDALGetMaskBand.invokeExact(h()) : MemorySegment.NULL;
			MemorySegment opts = GdalNative.stringList(a, java.util.Arrays.asList(options));
			g.CPLErrorReset.invokeExact();
			int err = real ? (int) g.GDALFPolygonize.invokeExact(h(), mask, target.h(), idx, opts, MemorySegment.NULL, MemorySegment.NULL)
					: (int) g.GDALPolygonize.invokeExact(h(), mask, target.h(), idx, opts, MemorySegment.NULL, MemorySegment.NULL);
			if (err != 0) {
				throw Gdal.error("The polygons could not be computed");
			}
		} catch (GdalException e) {
			throw e;
		} catch (Throwable t) {
			throw Gdal.wrap(t);
		}
	}

	@Override
	public String toString() {
		return "GdalBand(" + index + ", " + dataType() + ", " + colorInterpretation() + ")";
	}
}
