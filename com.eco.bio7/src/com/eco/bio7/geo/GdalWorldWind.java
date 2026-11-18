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

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;

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
import com.eco.bio7.worldwind.LayerComposite;
import com.eco.bio7.worldwind.WorldWindOptionsView;
import com.eco.bio7.worldwind.WorldWindView;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.BasicShapeAttributes;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.PointPlacemark;
import gov.nasa.worldwind.render.PointPlacemarkAttributes;
import gov.nasa.worldwind.render.SurfaceImage;
import gov.nasa.worldwind.render.SurfacePolygon;
import gov.nasa.worldwind.render.SurfacePolyline;

/**
 * GDAL and WorldWind: rasters of all formats of GDAL as image layers
 * (reprojected to WGS84 by GDAL, the area outside of the data transparent),
 * vectors as surface shapes and placemarks. GDAL must be loaded.
 */
public final class GdalWorldWind {

	/* The largest side of the image of a layer (texture of OpenGL) */
	private static final int MAX_SIDE = 4096;
	/* More features are not shown (a layer stays usable) */
	private static final int MAX_FEATURES = 50_000;

	private GdalWorldWind() {
	}

	/** The image and its sector (WGS84) of a raster. */
	public record Raster(BufferedImage image, Sector sector) {
	}

	/**
	 * A raster as image in WGS84 (not shown): reprojected by GDAL (at most 4096
	 * pixels), RGB(A) and palettes with their colors, one band as gray values
	 * (minimum to maximum); no data and the area outside transparent.
	 */
	public static Raster raster(Path file) {
		try (GdalDataset src = Gdal.open(file.toString())) {
			if (src.bandCount() == 0) {
				throw new GdalException("The file has no raster (a vector file?): " + file);
			}
			if (src.projection().isBlank() && src.geoTransform() == null) {
				throw new GdalException("The raster has no georeferencing: " + file);
			}
			/* The size in WGS84 (a virtual raster: no pixels are computed) */
			int w, h;
			try (GdalDataset vrt = Gdal.warp(src, "", "-of", "VRT", "-t_srs", "EPSG:4326")) {
				w = vrt.width();
				h = vrt.height();
			}
			List<String> opts = new ArrayList<>(List.of("-of", "MEM", "-t_srs", "EPSG:4326", "-dstalpha", "-r", "bilinear"));
			if (Math.max(w, h) > MAX_SIDE) {
				opts.addAll(w >= h ? List.of("-ts", String.valueOf(MAX_SIDE), "0") : List.of("-ts", "0", String.valueOf(MAX_SIDE)));
			}
			try (GdalDataset ds = Gdal.warp(src, "", opts.toArray(String[]::new))) {
				double[] b = ds.bounds();
				Sector sector = Sector.fromDegrees(b[1], b[3], b[0], b[2]);
				return new Raster(image(ds, src), sector);
			}
		}
	}

	private static BufferedImage image(GdalDataset ds, GdalDataset src) {
		int w = ds.width(), h = ds.height();
		int n = ds.bandCount();
		/* The last band is the alpha band of -dstalpha */
		byte[] alpha = ds.band(n).readBytes();
		int dataBands = n - 1;
		int[] argb = new int[w * h];
		GdalBand first = ds.band(1);
		boolean rgb = dataBands >= 3 && first.dataType() == GdalDataType.BYTE && "Red".equals(src.band(1).colorInterpretation());
		int[] palette = src.band(1).colorTable();
		if (rgb) {
			byte[] r = ds.band(1).readBytes(), g = ds.band(2).readBytes(), bl = ds.band(3).readBytes();
			for (int i = 0; i < argb.length; i++) {
				argb[i] = (alpha[i] & 0xff) << 24 | (r[i] & 0xff) << 16 | (g[i] & 0xff) << 8 | (bl[i] & 0xff);
			}
		} else if (palette != null) {
			byte[] idx = first.readBytes();
			for (int i = 0; i < argb.length; i++) {
				int c = palette[Math.min(idx[i] & 0xff, palette.length - 1)];
				argb[i] = (alpha[i] & 0xff) << 24 | (c & 0xffffff);
			}
		} else {
			float[] v = first.readFloats();
			Double noData = src.band(1).noData();
			float min = Float.POSITIVE_INFINITY, max = Float.NEGATIVE_INFINITY;
			for (int i = 0; i < v.length; i++) {
				if (alpha[i] != 0 && !Float.isNaN(v[i]) && (noData == null || v[i] != noData.floatValue())) {
					min = Math.min(min, v[i]);
					max = Math.max(max, v[i]);
				}
			}
			float range = max > min ? max - min : 1;
			for (int i = 0; i < argb.length; i++) {
				boolean valid = alpha[i] != 0 && !Float.isNaN(v[i]) && (noData == null || v[i] != noData.floatValue());
				int gray = valid ? Math.round((v[i] - min) / range * 255) : 0;
				argb[i] = (valid ? 0xff : 0) << 24 | gray << 16 | gray << 8 | gray;
			}
		}
		BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		image.setRGB(0, 0, w, h, argb, 0, w);
		return image;
	}

	/** Adds a raster as layer to WorldWind and moves the view to it (not in the UI thread). */
	public static void addRaster(Path file) {
		Raster r = raster(file);
		SurfaceImage si = new SurfaceImage(r.image(), r.sector());
		RenderableLayer layer = new RenderableLayer();
		layer.setName(file.getFileName().toString());
		layer.setPickEnabled(false);
		layer.addRenderable(si);
		add(layer, si, r.sector());
	}

	/**
	 * Adds the features of a vector file as layer to WorldWind (in WGS84):
	 * polygons (with holes) and lines as surface shapes, points as placemarks.
	 *
	 * @return the number of features shown
	 */
	public static int addVector(Path file) {
		RenderableLayer layer = new RenderableLayer();
		layer.setName(file.getFileName().toString());
		BasicShapeAttributes attrs = new BasicShapeAttributes();
		attrs.setOutlineMaterial(new Material(new java.awt.Color(255, 200, 0)));
		attrs.setInteriorMaterial(new Material(new java.awt.Color(255, 200, 0)));
		attrs.setInteriorOpacity(0.35);
		attrs.setOutlineWidth(2);
		PointPlacemarkAttributes pointAttrs = new PointPlacemarkAttributes();
		pointAttrs.setScale(0.6);
		pointAttrs.setLabelScale(0.8);
		double[] extent = { Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE };
		int count = 0;
		try (GdalDataset ds = Gdal.open(file.toString()); SpatialReference wgs84 = SpatialReference.fromEpsg(4326)) {
			if (ds.layerCount() == 0) {
				throw new GdalException("The file has no vector layer: " + file);
			}
			for (VectorLayer vl : ds.layers()) {
				try (SpatialReference srs = vl.spatialReference(); CoordinateTransform ct = srs == null ? null : srs.transformTo(wgs84)) {
					List<Feature> features = vl.features();
					for (Feature f : features) {
						if (count >= MAX_FEATURES) {
							break;
						}
						Geometry g = f.geometry();
						if (g == null) {
							continue;
						}
						if (addGeometry(layer, g, ct, attrs, pointAttrs, label(f), extent)) {
							count++;
						}
					}
				}
			}
		}
		Sector sector = extent[0] <= extent[2] ? Sector.fromDegrees(extent[1], extent[3], extent[0], extent[2]) : null;
		add(layer, null, sector);
		return count;
	}

	private static boolean addGeometry(RenderableLayer layer, Geometry g, CoordinateTransform ct, BasicShapeAttributes attrs, PointPlacemarkAttributes pointAttrs,
			String label, double[] extent) {
		boolean added = false;
		if ("COLLECTION".equals(g.type())) {
			for (Geometry m : g.members()) {
				added |= addGeometry(layer, m, ct, attrs, pointAttrs, label, extent);
			}
			return added;
		}
		for (List<double[][]> part : g.parts()) {
			switch (g.type()) {
			case "POINT": {
				LatLon p = transform(part.get(0), ct, extent)[0];
				PointPlacemark pm = new PointPlacemark(new Position(p, 0));
				pm.setAltitudeMode(gov.nasa.worldwind.WorldWind.CLAMP_TO_GROUND);
				pm.setAttributes(pointAttrs);
				if (label != null) {
					pm.setLabelText(label);
				}
				layer.addRenderable(pm);
				added = true;
				break;
			}
			case "LINESTRING": {
				List<LatLon> line = List.of(transform(part.get(0), ct, extent));
				SurfacePolyline pl = new SurfacePolyline(attrs, line);
				layer.addRenderable(pl);
				added = true;
				break;
			}
			case "POLYGON": {
				SurfacePolygon poly = new SurfacePolygon(attrs, List.of(transform(part.get(0), ct, extent)));
				for (int k = 1; k < part.size(); k++) {
					poly.addInnerBoundary(List.of(transform(part.get(k), ct, null)));
				}
				if (label != null) {
					poly.setValue(gov.nasa.worldwind.avlist.AVKey.DISPLAY_NAME, label);
				}
				layer.addRenderable(poly);
				added = true;
				break;
			}
			default:
				break;
			}
		}
		return added;
	}

	/* The coordinates of a path in WGS84 (transformed if the layer has another coordinate system) */
	private static LatLon[] transform(double[][] path, CoordinateTransform ct, double[] extent) {
		double[] x = new double[path.length], y = new double[path.length];
		for (int i = 0; i < path.length; i++) {
			x[i] = path[i][0];
			y[i] = path[i][1];
		}
		if (ct != null) {
			ct.transform(x, y);
		}
		LatLon[] out = new LatLon[path.length];
		for (int i = 0; i < path.length; i++) {
			out[i] = LatLon.fromDegrees(y[i], x[i]);
			if (extent != null) {
				grow(extent, out[i]);
			}
		}
		return out;
	}

	private static void grow(double[] e, LatLon p) {
		e[0] = Math.min(e[0], p.getLongitude().degrees);
		e[1] = Math.min(e[1], p.getLatitude().degrees);
		e[2] = Math.max(e[2], p.getLongitude().degrees);
		e[3] = Math.max(e[3], p.getLatitude().degrees);
	}

	private static String label(Feature f) {
		for (Object v : f.attributes().values()) {
			if (v instanceof String s && !s.isBlank()) {
				return s;
			}
		}
		return null;
	}

	/* Adds the layer to WorldWind (before the place names), to the layer list of the options and moves the view */
	private static void add(RenderableLayer layer, SurfaceImage si, Sector sector) {
		WorldWindow wwd = WorldWindView.getWwd();
		if (wwd == null) {
			throw new GdalException("Please open the WorldWind view (WorldWind perspective) first");
		}
		WorldWindOptionsView.insertBeforePlacenames(wwd, layer);
		Display display = Display.getDefault();
		display.asyncExec(() -> {
			if (WorldWindOptionsView.compositeLayers != null && !WorldWindOptionsView.compositeLayers.isDisposed()) {
				LayerComposite lc = new LayerComposite(WorldWindOptionsView.compositeLayers, SWT.NONE, si, layer, sector);
				lc.setBounds(10, 10, 300, 60);
				WorldWindOptionsView.computeScrolledSize();
			}
			if (sector != null) {
				double size = Math.max(sector.getDeltaLatDegrees(), sector.getDeltaLonDegrees());
				/* The distance: the whole sector is visible (about 111 km per degree) */
				double distance = Math.max(2_000, size * 111_000 * 1.8);
				wwd.getView().goTo(new Position(sector.getCentroid(), 0), distance);
			}
			wwd.redraw();
		});
	}
}
