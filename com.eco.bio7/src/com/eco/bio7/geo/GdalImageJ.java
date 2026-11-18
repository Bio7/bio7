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
package com.eco.bio7.geo;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import com.eco.bio7.gdal.CoordinateTransform;
import com.eco.bio7.gdal.Feature;
import com.eco.bio7.gdal.Gdal;
import com.eco.bio7.gdal.GdalBand;
import com.eco.bio7.gdal.GdalDataType;
import com.eco.bio7.gdal.GdalDataset;
import com.eco.bio7.gdal.GdalException;
import com.eco.bio7.gdal.Geometry;
import com.eco.bio7.gdal.SpatialReference;
import com.eco.bio7.gdal.VectorLayer;

import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.PointRoi;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.measure.Calibration;
import ij.plugin.frame.RoiManager;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.LUT;
import ij.process.ShortProcessor;

/**
 * GDAL and ImageJ: rasters of all formats of GDAL as ImageJ images (with the
 * georeferencing as calibration and properties), ImageJ images as GeoTIFF, the
 * vectors of GDAL as ROIs of an image. GDAL must be loaded (see
 * com.eco.bio7.gdal.ui.GdalActivation).
 */
public final class GdalImageJ {

	/** The properties of the image with the georeferencing of GDAL (for the export). */
	public static final String GEOTRANSFORM = "GDAL.GeoTransform", PROJECTION = "GDAL.Projection", NODATA = "GDAL.NoData", SOURCE = "GDAL.Source";

	/* Larger rasters are read as overview (pixels per image) */
	private static final long MAX_PIXELS = 120_000_000L;

	private GdalImageJ() {
	}

	/**
	 * Reads a raster as ImageJ image (not shown): 8 bit and 16 bit (unsigned) keep
	 * their type, other types are 32 bit (no data as NaN), RGB(A) bands of 8 bit an
	 * RGB image, a palette band an 8 bit image with the colors. Several bands are a
	 * stack. Very large rasters are read as overview.
	 */
	public static ImagePlus open(Path file) {
		try (GdalDataset ds = Gdal.open(file.toString())) {
			int bands = ds.bandCount();
			if (bands == 0) {
				throw new GdalException("The file has no raster (a vector file?): " + file);
			}
			int w = ds.width(), h = ds.height();
			/* An overview of a very large raster (GDAL uses the overviews of the file) */
			double factor = Math.max(1, Math.sqrt((double) w * h / MAX_PIXELS));
			int bw = (int) Math.max(1, Math.round(w / factor)), bh = (int) Math.max(1, Math.round(h / factor));
			String title = file.getFileName().toString();
			ImagePlus imp;
			if (isRgb(ds)) {
				imp = new ImagePlus(title, rgb(ds, bw, bh));
			} else {
				ImageStack stack = new ImageStack(bw, bh);
				for (int i = 1; i <= bands; i++) {
					GdalBand b = ds.band(i);
					String label = "Band " + i + ("Undefined".equals(b.colorInterpretation()) ? "" : " (" + b.colorInterpretation() + ")");
					stack.addSlice(label, processor(b, w, h, bw, bh));
				}
				imp = new ImagePlus(title, stack);
			}
			georeference(imp, ds, (double) w / bw, (double) h / bh);
			imp.setProperty(SOURCE, file.toString());
			imp.setProperty("Info", ds.info() + (factor > 1 ? "\nRead as overview: " + bw + " x " + bh + " of " + w + " x " + h + " pixels\n" : ""));
			return imp;
		}
	}

	private static boolean isRgb(GdalDataset ds) {
		int n = ds.bandCount();
		if ((n != 3 && n != 4) || ds.band(1).dataType() != GdalDataType.BYTE) {
			return false;
		}
		return "Red".equals(ds.band(1).colorInterpretation()) && "Green".equals(ds.band(2).colorInterpretation())
				&& "Blue".equals(ds.band(3).colorInterpretation());
	}

	private static ColorProcessor rgb(GdalDataset ds, int bw, int bh) {
		int w = ds.width(), h = ds.height();
		byte[] r = read(ds.band(1), w, h, bw, bh), g = read(ds.band(2), w, h, bw, bh), b = read(ds.band(3), w, h, bw, bh);
		int[] px = new int[bw * bh];
		for (int i = 0; i < px.length; i++) {
			px[i] = 0xff000000 | (r[i] & 0xff) << 16 | (g[i] & 0xff) << 8 | (b[i] & 0xff);
		}
		return new ColorProcessor(bw, bh, px);
	}

	private static byte[] read(GdalBand b, int w, int h, int bw, int bh) {
		if (bw == w && bh == h) {
			return b.readBytes();
		}
		float[] f = b.readFloats(0, 0, w, h, bw, bh);
		byte[] out = new byte[f.length];
		for (int i = 0; i < f.length; i++) {
			out[i] = (byte) Math.max(0, Math.min(255, Math.round(f[i])));
		}
		return out;
	}

	private static ImageProcessor processor(GdalBand b, int w, int h, int bw, int bh) {
		GdalDataType t = b.dataType();
		boolean full = bw == w && bh == h;
		Double noData = b.noData();
		if (full && t == GdalDataType.BYTE) {
			ByteProcessor ip = new ByteProcessor(bw, bh, b.readBytes());
			int[] table = b.colorTable();
			if (table != null) {
				ip.setLut(lut(table));
			}
			return ip;
		}
		if (full && t == GdalDataType.UINT16) {
			return new ShortProcessor(bw, bh, b.readShorts(), null);
		}
		float[] px = full ? b.readFloats() : b.readFloats(0, 0, w, h, bw, bh);
		if (noData != null) {
			float nd = noData.floatValue();
			for (int i = 0; i < px.length; i++) {
				if (px[i] == nd) {
					px[i] = Float.NaN;
				}
			}
		}
		FloatProcessor ip = new FloatProcessor(bw, bh, px);
		ip.resetMinAndMax();
		return ip;
	}

	private static LUT lut(int[] argb) {
		byte[] r = new byte[256], g = new byte[256], b = new byte[256];
		for (int i = 0; i < Math.min(256, argb.length); i++) {
			r[i] = (byte) (argb[i] >> 16);
			g[i] = (byte) (argb[i] >> 8);
			b[i] = (byte) argb[i];
		}
		return new LUT(r, g, b);
	}

	/* The georeferencing as calibration (a north-up raster) and as properties */
	private static void georeference(ImagePlus imp, GdalDataset ds, double sx, double sy) {
		double[] gt = ds.geoTransform();
		String wkt = ds.projection();
		if (gt == null) {
			return;
		}
		/* The geotransform of the (possibly resampled) image */
		double[] g = { gt[0], gt[1] * sx, gt[2] * sy, gt[3], gt[4] * sx, gt[5] * sy };
		imp.setProperty(GEOTRANSFORM, join(g));
		imp.setProperty(PROJECTION, wkt);
		Double nd = ds.bandCount() > 0 ? ds.band(1).noData() : null;
		if (nd != null) {
			imp.setProperty(NODATA, String.valueOf(nd));
		}
		if (g[2] == 0 && g[4] == 0 && g[1] != 0 && g[5] != 0) {
			Calibration cal = imp.getCalibration();
			cal.pixelWidth = Math.abs(g[1]);
			cal.pixelHeight = Math.abs(g[5]);
			cal.xOrigin = -g[0] / g[1];
			if (g[5] < 0) {
				/* North up: y grows upwards */
				cal.setInvertY(true);
				cal.yOrigin = g[3] / cal.pixelHeight;
			} else {
				cal.yOrigin = -g[3] / g[5];
			}
			cal.setUnit(unit(wkt));
		}
	}

	private static String unit(String wkt) {
		if (wkt == null || wkt.isBlank()) {
			return "pixel";
		}
		try (SpatialReference srs = SpatialReference.fromWkt(wkt)) {
			return srs.isGeographic() ? "degree" : wkt.toLowerCase(Locale.ROOT).contains("foot") ? "ft" : "m";
		} catch (GdalException e) {
			return "pixel";
		}
	}

	/**
	 * Writes an ImageJ image as GeoTIFF (compressed): 8 bit as Byte, 16 bit as
	 * UInt16, 32 bit as Float32 (NaN as no data), RGB as 3 bands; a stack as bands.
	 * The georeferencing of an image read with GDAL is written.
	 */
	public static void exportGeoTiff(ImagePlus imp, Path file) {
		int w = imp.getWidth(), h = imp.getHeight();
		ImageStack stack = imp.getStack();
		boolean rgb = imp.getType() == ImagePlus.COLOR_RGB;
		int bands = rgb ? 3 * stack.getSize() : stack.getSize();
		GdalDataType type = switch (imp.getBitDepth()) {
		case 8, 24 -> GdalDataType.BYTE;
		case 16 -> GdalDataType.UINT16;
		default -> GdalDataType.FLOAT32;
		};
		try (GdalDataset ds = Gdal.create(file.toString(), "GTiff", w, h, bands, type, "COMPRESS=DEFLATE", "BIGTIFF=IF_SAFER")) {
			Object gt = imp.getProperty(GEOTRANSFORM);
			if (gt instanceof String s && !s.isBlank()) {
				ds.setGeoTransform(split(s));
			}
			Object wkt = imp.getProperty(PROJECTION);
			if (wkt instanceof String s && !s.isBlank()) {
				ds.setProjection(s);
			}
			int band = 1;
			for (int i = 1; i <= stack.getSize(); i++) {
				ImageProcessor ip = stack.getProcessor(i);
				if (rgb) {
					int[] px = (int[]) ip.getPixels();
					byte[][] c = new byte[3][px.length];
					for (int k = 0; k < px.length; k++) {
						c[0][k] = (byte) (px[k] >> 16);
						c[1][k] = (byte) (px[k] >> 8);
						c[2][k] = (byte) px[k];
					}
					for (byte[] ch : c) {
						ds.band(band++).write(ch);
					}
				} else if (type == GdalDataType.BYTE) {
					ds.band(band++).write((byte[]) ip.getPixels());
				} else if (type == GdalDataType.UINT16) {
					ds.band(band++).write((short[]) ip.getPixels());
				} else {
					GdalBand b = ds.band(band++);
					b.write((float[]) ip.convertToFloat().getPixels());
					b.setNoData(Double.NaN);
				}
			}
		}
	}

	/**
	 * The features of a vector file as ROIs of the image (in the ROI Manager): the
	 * coordinates are transformed into the coordinate system of the image (read
	 * with GDAL) and into pixels. Points as point ROIs, lines as polylines,
	 * polygons (with holes) as polygon or composite ROIs.
	 *
	 * @return the number of ROIs
	 */
	public static int addVectorRois(Path file, ImagePlus imp, RoiManager manager) {
		Object gtp = imp.getProperty(GEOTRANSFORM);
		if (!(gtp instanceof String gts) || gts.isBlank()) {
			throw new GdalException("The image has no georeferencing of GDAL (open the raster with GDAL first)");
		}
		double[] gt = split(gts);
		Object wkt = imp.getProperty(PROJECTION);
		int count = 0;
		try (GdalDataset ds = Gdal.open(file.toString())) {
			for (VectorLayer layer : ds.layers()) {
				CoordinateTransform ct = null;
				SpatialReference from = layer.spatialReference();
				SpatialReference to = wkt instanceof String s && !s.isBlank() ? SpatialReference.fromWkt(s) : null;
				try {
					if (from != null && to != null) {
						ct = from.transformTo(to);
					}
					List<Feature> features = layer.features();
					for (Feature f : features) {
						Geometry g = f.geometry();
						if (g == null) {
							continue;
						}
						Roi roi = roi(g, ct, gt);
						if (roi != null) {
							roi.setName(layer.name() + " " + f.fid() + label(f));
							manager.addRoi(roi);
							count++;
						}
					}
				} finally {
					if (ct != null) {
						ct.close();
					}
					if (from != null) {
						from.close();
					}
					if (to != null) {
						to.close();
					}
				}
			}
		}
		return count;
	}

	private static String label(Feature f) {
		for (Object v : f.attributes().values()) {
			if (v instanceof String s && !s.isBlank()) {
				return " " + s;
			}
		}
		return "";
	}

	private static Roi roi(Geometry g, CoordinateTransform ct, double[] gt) {
		if ("COLLECTION".equals(g.type())) {
			ShapeRoi all = null;
			for (Geometry m : g.members()) {
				Roi r = roi(m, ct, gt);
				if (r != null) {
					ShapeRoi s = new ShapeRoi(r);
					all = all == null ? s : all.or(s);
				}
			}
			return all;
		}
		switch (g.type()) {
		case "POINT": {
			PointRoi points = null;
			for (List<double[][]> part : g.parts()) {
				double[] p = pixel(part.get(0)[0], ct, gt);
				if (points == null) {
					points = new PointRoi(p[0], p[1]);
				} else {
					points.addPoint(p[0], p[1]);
				}
			}
			return points;
		}
		case "LINESTRING": {
			ShapeRoi all = null;
			Roi single = null;
			for (List<double[][]> part : g.parts()) {
				Roi r = polygon(part.get(0), ct, gt, Roi.POLYLINE);
				if (g.parts().size() == 1) {
					single = r;
				} else {
					all = all == null ? new ShapeRoi(r) : all.or(new ShapeRoi(r));
				}
			}
			return single != null ? single : all;
		}
		case "POLYGON": {
			ShapeRoi all = null;
			Roi single = null;
			for (List<double[][]> part : g.parts()) {
				Roi outer = polygon(part.get(0), ct, gt, Roi.POLYGON);
				if (part.size() == 1 && g.parts().size() == 1) {
					single = outer;
					break;
				}
				ShapeRoi s = new ShapeRoi(outer);
				for (int k = 1; k < part.size(); k++) {
					s = s.not(new ShapeRoi(polygon(part.get(k), ct, gt, Roi.POLYGON)));
				}
				all = all == null ? s : all.or(s);
			}
			return single != null ? single : all;
		}
		default:
			return null;
		}
	}

	private static PolygonRoi polygon(double[][] path, CoordinateTransform ct, double[] gt, int type) {
		float[] x = new float[path.length], y = new float[path.length];
		for (int i = 0; i < path.length; i++) {
			double[] p = pixel(path[i], ct, gt);
			x[i] = (float) p[0];
			y[i] = (float) p[1];
		}
		return new PolygonRoi(x, y, path.length, type);
	}

	/* World coordinates (of the vector) -> pixels of the image */
	private static double[] pixel(double[] c, CoordinateTransform ct, double[] gt) {
		double[] w = ct != null ? ct.transform(c[0], c[1]) : c;
		double det = gt[1] * gt[5] - gt[2] * gt[4];
		double dx = w[0] - gt[0], dy = w[1] - gt[3];
		return new double[] { (gt[5] * dx - gt[2] * dy) / det, (-gt[4] * dx + gt[1] * dy) / det };
	}

	private static String join(double[] v) {
		StringBuilder s = new StringBuilder();
		for (double d : v) {
			s.append(s.length() > 0 ? " " : "").append(d);
		}
		return s.toString();
	}

	private static double[] split(String s) {
		String[] p = s.trim().split("\\s+");
		double[] v = new double[p.length];
		for (int i = 0; i < p.length; i++) {
			v[i] = Double.parseDouble(p[i]);
		}
		return v;
	}
}
