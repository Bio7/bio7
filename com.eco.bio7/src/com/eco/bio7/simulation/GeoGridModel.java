/*******************************************************************************
 * Copyright (c) 2007-2026 M. Austenfeld
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * Contributors:
 * Marcel Austenfeld - initial API and implementation
 *******************************************************************************/
package com.eco.bio7.simulation;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;

import com.eco.bio7.gdal.CoordinateTransform;
import com.eco.bio7.gdal.Gdal;
import com.eco.bio7.gdal.GdalDataset;
import com.eco.bio7.gdal.SpatialReference;

/**
 * A simulation on a georeferenced grid (a geospatial model, e.g., a wildfire
 * on a terrain, a runoff or a species dispersal): the grid and its data layers
 * are read with GDAL in Bio7 (rasters and vectors in any format of GDAL, e.g.,
 * GeoTIFF, GeoPackage, Shapefile, GeoJSON) and resampled or rasterized to the
 * cells. The cells have states (as a {@link GridModel}) and the model can show
 * a layer of values with a color map ({@link #showValues}).
 * <p>
 * The data is loaded in {@link #load()} (once, before the first init):
 *
 * <pre>
 * protected void load() {
 * 	grid("data/geo/dem.tif", 200); // the grid: the extent and the coordinate system of the raster, 200 columns
 * 	addLayer("elevation", raster("data/geo/dem.tif")); // a raster on the cells (bilinear)
 * 	addLayer("lakes", vector("data/geo/lakes.geojson", 1)); // vectors on the cells (1 inside, 0 outside)
 * }
 * </pre>
 *
 * init() and step() use the layers with {@link #layer(String)} (a copy for each
 * setup, the model can change it). Relative paths are found in the project of
 * the model.
 * <p>
 * The HTML export embeds the loaded layers (the page needs no GDAL) and shows
 * the grid on a map (OpenStreetMap and other base maps, online). load() is not
 * exported: compile and run the model in Bio7 before the export.
 */
public abstract class GeoGridModel extends GridModel {

	private static final double MERCATOR = 20037508.342789244;

	/** The projected coordinates of the upper left corner of the grid and the size of a cell (cellHeight is negative for north up). */
	public double originX, originY, cellWidth = 1, cellHeight = -1;

	/** The size of a cell on the ground in meters (approximately, at the center of the grid). */
	public double cellSizeMeters = 1;

	/** The geographic bounds of the grid (WGS84 degrees). */
	public double south, west, north, east;

	/** The coordinate system of the grid (WKT). */
	public String crs = "";

	/* "mercator" (EPSG:3857), "geographic" (degrees) or "other" (the bounds are interpolated) */
	private String mode = "other";

	private final Map<String, double[]> data = new LinkedHashMap<>();
	private final Map<String, double[]> working = new LinkedHashMap<>();
	private boolean loaded;
	private final List<String> sources = new ArrayList<>();
	private String attribution;

	private boolean onGlobe;
	private boolean flown;
	private final GlobeDisplay globe = new GlobeDisplay();

	private double[] shownValues;
	private double shownMin, shownMax;
	private String[] shownColors;

	/**
	 * Loads the grid and the layers with GDAL (in Bio7, once before the first
	 * init; not exported: the page gets the loaded layers). Call
	 * {@link #grid(String, int)} first, then {@link #addLayer(String, double[])}
	 * with {@link #raster(String)} or {@link #vector(String, double)}.
	 */
	protected abstract void load();

	/* The kind of the coordinates: "mercator", "geographic" or "other" */
	String mode() {
		return mode;
	}

	/* ===================== Loading (GDAL) ===================== */

	/**
	 * Defines the grid by a raster: its extent and coordinate system, the number of
	 * the columns (the rows from the aspect of the raster).
	 */
	protected void grid(String file, int columns) {
		ensureGdal();
		try (GdalDataset d = Gdal.open(resolve(file))) {
			double[] gt = d.geoTransform();
			double w = gt[1] * d.width(), h = gt[5] * d.height();
			int rows = Math.max(1, (int) Math.round(columns * Math.abs(h / w)));
			originX = gt[0];
			originY = gt[3];
			cellWidth = w / columns;
			cellHeight = h / rows;
			crs = d.projection();
			width = columns;
			height = rows;
			SpatialReference srs = d.spatialReference();
			String auth = srs == null ? null : srs.authority();
			mode = "EPSG:3857".equals(auth) || "EPSG:900913".equals(auth) ? "mercator" : srs != null && srs.isGeographic() ? "geographic" : "other";
			/* The geographic bounds (the corners in WGS84) */
			try (SpatialReference wgs = SpatialReference.fromEpsg(4326); CoordinateTransform ct = srs.transformTo(wgs)) {
				double[] xs = { originX, originX + w, originX, originX + w }, ys = { originY, originY, originY + h, originY + h };
				ct.transform(xs, ys);
				west = Math.min(xs[0], xs[2]);
				east = Math.max(xs[1], xs[3]);
				north = Math.max(ys[0], ys[1]);
				south = Math.min(ys[2], ys[3]);
			}
			double latC = (south + north) / 2;
			if (mode.equals("mercator")) {
				cellSizeMeters = Math.abs(cellWidth) * Math.cos(Math.toRadians(latC));
			} else if (mode.equals("geographic")) {
				cellSizeMeters = Math.abs(cellWidth) * 111320 * Math.cos(Math.toRadians(latC));
			} else {
				cellSizeMeters = Math.abs(cellWidth);
			}
			sources.add(new File(file).getName());
		}
		cells = new int[width * height];
		next = new int[width * height];
	}

	/** The first band of a raster on the cells (bilinear resampling, no data: NaN). */
	protected double[] raster(String file) {
		return raster(file, 1, "bilinear");
	}

	/**
	 * A band of a raster on the cells (reprojected to the grid if needed).
	 *
	 * @param resampling "near", "bilinear", "cubic", "average", "mode", ... (e.g.,
	 *                   "near" or "mode" for classes like land use)
	 */
	protected double[] raster(String file, int band, String resampling) {
		checkGrid();
		ensureGdal();
		try (GdalDataset d = Gdal.open(resolve(file));
				GdalDataset w = Gdal.warp(d, "", "-of", "MEM", "-t_srs", crs, "-te", num(Math.min(originX, originX + cellWidth * width)), num(Math.min(originY, originY + cellHeight * height)),
						num(Math.max(originX, originX + cellWidth * width)), num(Math.max(originY, originY + cellHeight * height)), "-ts", String.valueOf(width), String.valueOf(height), "-r", resampling,
						"-ot", "Float64", "-dstnodata", "nan")) {
			double[] v = w.band(band).readDoubles();
			if (cellHeight > 0) {
				flipRows(v);
			}
			sources.add(new File(file).getName());
			return v;
		}
	}

	/** Vectors (points, lines, polygons) on the cells: the value inside, 0 outside. */
	protected double[] vector(String file, double value) {
		return rasterize(file, "-burn", num(value));
	}

	/** Vectors on the cells with the value of an attribute (a number field), 0 outside. */
	protected double[] vector(String file, String attribute) {
		return rasterize(file, "-a", attribute);
	}

	private double[] rasterize(String file, String kind, String what) {
		checkGrid();
		ensureGdal();
		String tmp = "/vsimem/bio7_sim_" + System.nanoTime() + ".gpkg";
		try (GdalDataset src = Gdal.open(resolve(file)); GdalDataset vec = Gdal.vectorTranslate(src, tmp, "-f", "GPKG", "-t_srs", crs);
				GdalDataset r = Gdal.rasterize(vec, "", "-of", "MEM", "-te", num(Math.min(originX, originX + cellWidth * width)), num(Math.min(originY, originY + cellHeight * height)),
						num(Math.max(originX, originX + cellWidth * width)), num(Math.max(originY, originY + cellHeight * height)), "-ts", String.valueOf(width), String.valueOf(height), "-ot",
						"Float64", "-init", "0", kind, what)) {
			double[] v = r.band(1).readDoubles();
			if (cellHeight > 0) {
				flipRows(v);
			}
			sources.add(new File(file).getName());
			return v;
		}
	}

	/**
	 * The sources of the data (shown on the map of the HTML export, e.g.,
	 * "Elevation: SRTM"): the attribution which the licenses of the data require
	 * (the export embeds the data in the page). Call it in load(); without it the
	 * names of the files are shown.
	 */
	protected void attribution(String text) {
		attribution = text;
	}

	/** Adds a layer (e.g., of raster or vector) for init() and step(). */
	protected void addLayer(String name, double[] values) {
		if (values.length != width * height) {
			throw new IllegalArgumentException("The layer " + name + " has " + values.length + " values, the grid " + width * height);
		}
		data.put(name, values);
	}

	private void checkGrid() {
		if (crs == null || crs.isEmpty()) {
			throw new IllegalStateException("Define the grid first: grid(file, columns)");
		}
	}

	private void flipRows(double[] v) {
		for (int y = 0; y < height / 2; y++) {
			for (int x = 0; x < width; x++) {
				double t = v[y * width + x];
				v[y * width + x] = v[(height - 1 - y) * width + x];
				v[(height - 1 - y) * width + x] = t;
			}
		}
	}

	private static String num(double v) {
		return Double.toString(v);
	}

	private static void ensureGdal() {
		if (!Gdal.isLoaded()) {
			try {
				Gdal.load();
			} catch (RuntimeException e) {
				throw new IllegalStateException("GDAL is needed for a GeoGridModel: " + e.getMessage(), e);
			}
		}
	}

	/**
	 * Shows the grid also on the WorldWind globe of Bio7 (call it in load(); the
	 * WorldWind perspective is opened and the globe flies to the area). The
	 * colors with an alpha (#rrggbbaa) show the globe below the grid.
	 */
	protected void showOnGlobe() {
		onGlobe = true;
	}

	@Override
	String[] displayViews() {
		return onGlobe ? new String[] { GlobeDisplay.VIEW, "com.eco.bio7.imagej" } : new String[] { "com.eco.bio7.imagej" };
	}

	@Override
	String[] optionsView() {
		return onGlobe ? new String[] { GlobeDisplay.OPTIONS_VIEW, "Options WorldWind" } : null;
	}

	/* ===================== The model ===================== */

	@Override
	protected void prepareDisplay() {
		super.prepareDisplay();
		if (!loaded) {
			data.clear();
			sources.clear();
			load();
			loaded = true;
			/* showOnGlobe() in load(): the globe and its options in the Custom perspective */
			if (onGlobe && arrangeViews && Simulation.hasDisplay() && !headless) {
				if (org.eclipse.swt.widgets.Display.getCurrent() != null) {
					SimulationLayout.arrange(this);
				} else {
					org.eclipse.ui.PlatformUI.getWorkbench().getDisplay().syncExec(() -> SimulationLayout.arrange(this));
				}
			}
		}
		cells = new int[width * height];
		next = new int[width * height];
		if (onGlobe) {
			if (!arrangeViews && !headless) {
				GlobeDisplay.open();
			}
			flown = false;
		}
		/* A copy of the layers for each setup (the model can change them) */
		working.clear();
		for (Map.Entry<String, double[]> e : data.entrySet()) {
			working.put(e.getKey(), e.getValue().clone());
		}
		shownValues = null;
	}

	/** The layer (a copy of the loaded data for this setup, index y * width + x), null if unknown. */
	public double[] layer(String name) {
		return working.get(name);
	}

	/** Shows the values (e.g., a layer or a field of the model) with a color map instead of the states. */
	public void showValues(double[] values, double min, double max, String... colors) {
		shownValues = values;
		shownMin = min;
		shownMax = max;
		shownColors = colors.length >= 2 ? colors : ValueGridModel.VIRIDIS;
	}

	/** Shows the states with the palette (the default). */
	public void showStates() {
		shownValues = null;
	}

	/* The longitude of a cell coordinate (0 the west border, fractions between the cells) */
	double cellLon(double x) {
		return lon(0) + (x - 0.5) * (width > 1 ? lon(1) - lon(0) : (east - west) / Math.max(1, width));
	}

	/* The latitude of a cell coordinate (0 the north border) */
	double cellLat(double y) {
		return lat(0) + (y - 0.5) * (height > 1 ? lat(1) - lat(0) : (south - north) / Math.max(1, height));
	}

	/** The column of the cell at the longitude and latitude (WGS84), -1 outside. */
	public int colAt(double lon, double lat) {
		double x;
		if (mode.equals("mercator")) {
			x = lon * MERCATOR / 180;
		} else if (mode.equals("geographic")) {
			x = lon;
		} else {
			return inside((int) Math.floor((lon - west) / (east - west) * width), width);
		}
		return inside((int) Math.floor((x - originX) / cellWidth), width);
	}

	/** The row of the cell at the longitude and latitude (WGS84), -1 outside. */
	public int rowAt(double lon, double lat) {
		double y;
		if (mode.equals("mercator")) {
			y = StrictMath.log(StrictMath.tan((90 + lat) * Math.PI / 360)) / (Math.PI / 180) * MERCATOR / 180;
		} else if (mode.equals("geographic")) {
			y = lat;
		} else {
			return inside((int) Math.floor((north - lat) / (north - south) * height), height);
		}
		return inside((int) Math.floor((y - originY) / cellHeight), height);
	}

	private static int inside(int v, int n) {
		return v >= 0 && v < n ? v : -1;
	}

	/** The longitude of the center of the column (WGS84). */
	public double lon(int x) {
		if (mode.equals("mercator")) {
			return (originX + (x + 0.5) * cellWidth) / MERCATOR * 180;
		}
		if (mode.equals("geographic")) {
			return originX + (x + 0.5) * cellWidth;
		}
		return west + (x + 0.5) / width * (east - west);
	}

	/** The latitude of the center of the row (WGS84). */
	public double lat(int y) {
		if (mode.equals("mercator")) {
			double my = (originY + (y + 0.5) * cellHeight) / MERCATOR * 180;
			return 180 / Math.PI * (2 * StrictMath.atan(StrictMath.exp(my * Math.PI / 180)) - Math.PI / 2);
		}
		if (mode.equals("geographic")) {
			return originY + (y + 0.5) * cellHeight;
		}
		return north - (y + 0.5) / height * (north - south);
	}

	/**
	 * The gradient (rise / run, e.g., 0.1 = 10 %) of a layer (e.g., the
	 * elevation in meters) at the cell (central differences, the borders are
	 * repeated).
	 */
	public double gradient(double[] layer, int x, int y) {
		double dzdx = (value(layer, x + 1, y) - value(layer, x - 1, y)) / (2 * cellSizeMeters);
		double dzdy = (value(layer, x, y + 1) - value(layer, x, y - 1)) / (2 * cellSizeMeters);
		return Math.sqrt(dzdx * dzdx + dzdy * dzdy);
	}

	/** The slope of a layer at the cell in degrees. */
	public double slopeDegrees(double[] layer, int x, int y) {
		return StrictMath.atan(gradient(layer, x, y)) * 180 / Math.PI;
	}

	/** The value of a layer at the cell (the nearest cell at the borders). */
	public double value(double[] layer, int x, int y) {
		x = Math.max(0, Math.min(width - 1, x));
		y = Math.max(0, Math.min(height - 1, y));
		return layer[y * width + x];
	}

	@Override
	protected void display() {
		showAgents();
		double[] v = shownValues;
		if (onGlobe) {
			displayOnGlobe(v);
		}
		if (v == null) {
			super.display();
			return;
		}
		int[][] ramp = GridDisplay.ramp(shownColors);
		double lo = shownMin, span = shownMax - shownMin == 0 ? 1 : shownMax - shownMin;
		grid.show(width, height, i -> GridDisplay.rampColor(ramp, v[i], lo, span), this::display, this);
	}

	/* The grid on the globe (ARGB with the alpha of the colors) */
	private void displayOnGlobe(double[] v) {
		if (!flown) {
			flown = true;
			globe.flyTo(south, west, north, east);
		}
		int n = width * height;
		int[] argb = new int[n];
		if (v != null) {
			double span = shownMax - shownMin == 0 ? 1 : shownMax - shownMin;
			for (int i = 0; i < n; i++) {
				argb[i] = GlobeDisplay.rampArgb(shownColors, v[i], shownMin, span);
			}
		} else {
			String[] pal = paletteColors();
			int[] colors = new int[pal.length];
			for (int k = 0; k < colors.length; k++) {
				colors[k] = GlobeDisplay.argb(pal[k]);
			}
			int[] c = cells;
			for (int i = 0; i < n; i++) {
				argb[i] = colors.length == 0 ? 0 : colors[Math.max(0, Math.min(colors.length - 1, c[i]))];
			}
		}
		/* The shapes in cells: the longitude and latitude of the cell coordinates, the radius in cells */
		GlobeDisplay.Drawing drawing = new GlobeDisplay.Drawing(getDrawing(), getImageFiles(), (x, y) -> new double[] { cellLon(x), cellLat(y) }, cellSizeMeters);
		globe.showGrid(width, height, argb, south, west, north, east, () -> displayOnGlobe(shownValues), drawing);
	}

	/* ===================== The data of the HTML export ===================== */

	/** The loaded data for the HTML export: the grid, the bounds and the layers. */
	public Map<String, Object> exportData() {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("width", width);
		m.put("height", height);
		m.put("originX", originX);
		m.put("originY", originY);
		m.put("cellWidth", cellWidth);
		m.put("cellHeight", cellHeight);
		m.put("cellSizeMeters", cellSizeMeters);
		m.put("south", south);
		m.put("west", west);
		m.put("north", north);
		m.put("east", east);
		m.put("mode", mode);
		m.put("layers", data);
		m.put("sources", attribution != null ? attribution : String.join(", ", sources.stream().distinct().toList()));
		return m;
	}
}
