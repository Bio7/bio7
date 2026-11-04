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

package com.eco.bio7.spatial;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.util.gl2.GLUT;

/**
 * A simple API to create 3D plots in the 3D view (the space view) without
 * OpenGL code: the data of one or more plot layers (scatter, line, filled
 * line, surface, wireframe, bars, contour, pie and the charts line graph, box
 * plot, violin plot, histogram, area, radar, rose and treemap, see
 * PlotCharts) with a title, axis titles, log
 * axes, number formats, a legend and a color bar. The data is fitted into the
 * plot box (the axis numbers show the values of the data) and the plot is
 * drawn in the 3D view (also without a running model). A plot can be exported
 * to an HTML page with WebGL (see exportHtml).
 * <p>
 * Example (Java or Groovy):
 *
 * <pre>
 * double[] x = ..., y = ..., z = ...;
 * new Plot3D().title("Measurements").axisTitles("Length [cm]", "Width [cm]", "Height [cm]")
 * 		.add(Plot3D.scatter(x, y, z).label("Species A"))
 * 		.show();
 *
 * new Plot3D().title("z = sin(x) cos(y)").add(Plot3D.surface(xs, ys, zGrid).colorMap(PlotLegend.VIRIDIS))
 * 		.colorBarTitle("z").show();
 *
 * Plot3D.clear(); // removes the plot
 * </pre>
 *
 * The texts can contain the math markup of the plot view (x^2, x^{-0.5},
 * N_0).
 */
public class Plot3D {

	/** The colors of the layers without a color (like the categories of R or matplotlib). */
	public static final int[] CATEGORY_COLORS = { 0x1F77B4, 0xFF7F0E, 0x2CA02C, 0xD62728, 0x9467BD, 0x8C564B, 0xE377C2, 0x7F7F7F,
			0xBCBD22, 0x17BECF };

	/* The displayed plot (drawn in the 3D view) */
	private static volatile Plot3D current;
	/* The grid, the axes and the quad of the 3D view before a pie chart (restored after the pie) */
	private static boolean[] sceneFlags;

	private final List<Layer<?>> layers = new ArrayList<>();
	private String title;
	private final String[] axisTitles = new String[3];
	private final boolean[] logAxes = new boolean[3];
	private final String[] numberFormats = new String[3];
	/* The ranges of the axes set by the user (data values, null = the range of the data) */
	private final double[][] userRanges = new double[3][];
	/* The size of the box (x, y, z): the largest side has the size 10 */
	private final double[] aspect = { 1, 1, 0.75 };
	private final boolean[] grids = { true, true, true };
	private String colorBarTitle;
	private boolean legend = true;
	/* The colors of the texts (0xRRGGBB, -1 = the color of the lines): title, axis numbers, axis titles, legend */
	private final int[] textColors = { -1, -1, -1, -1 };
	/* The font families of the texts (null = Verdana): title, axis numbers, axis titles, legend */
	private final String[] fonts = new String[4];
	/* The first display (show): the former settings of the plot view are removed */
	private volatile boolean fullApply = true;
	private boolean aspectSet;
	/* The categories of the axes (of the user and of the layers) */
	private final String[][] userCategories = new String[3][];
	private String[][] categories = new String[3][];
	/* Shows the export action of the 3D view with the plot (null = unchanged) */
	private Boolean exportAction;

	/* The computed plot: the ranges (transformed values), the box and the geometry */
	private double[][] ranges;
	private double[] box;
	private PlotGeometry geometry;
	private volatile boolean applied;

	/* ----------------------------- The settings ----------------------------- */

	/** The title of the plot (lines separated with \n). */
	public Plot3D title(String title) {
		this.title = title;
		return this;
	}

	/** The titles of the axes (null = X, Y, Z). */
	public Plot3D axisTitles(String x, String y, String z) {
		axisTitles[0] = x;
		axisTitles[1] = y;
		axisTitles[2] = z;
		return this;
	}

	/**
	 * Logarithmic axes: the data of the axes are displayed logarithmic (values <=
	 * 0 are not displayed). The data is given in the original values.
	 */
	public Plot3D logAxes(boolean x, boolean y, boolean z) {
		logAxes[0] = x;
		logAxes[1] = y;
		logAxes[2] = z;
		return this;
	}

	/**
	 * The formats of the axis numbers (java.util.Formatter, e.g., "%.1f", "10^" for
	 * powers of ten, null = automatic).
	 */
	public Plot3D numberFormats(String x, String y, String z) {
		numberFormats[0] = x;
		numberFormats[1] = y;
		numberFormats[2] = z;
		return this;
	}

	/** The range of the x axis (instead of the range of the data). */
	public Plot3D xRange(double min, double max) {
		return range(0, min, max);
	}

	/** The range of the y axis (instead of the range of the data). */
	public Plot3D yRange(double min, double max) {
		return range(1, min, max);
	}

	/** The range of the z axis (instead of the range of the data). */
	public Plot3D zRange(double min, double max) {
		return range(2, min, max);
	}

	private Plot3D range(int axis, double min, double max) {
		if (!(max > min)) {
			throw new IllegalArgumentException("The maximum of the range must be larger than the minimum");
		}
		userRanges[axis] = new double[] { min, max };
		return this;
	}

	/**
	 * The proportions of the plot box (default 1, 1, 0.75), e.g., 1, 1, 1 for a
	 * cube or 2, 1, 1 for a long x axis.
	 */
	public Plot3D aspect(double x, double y, double z) {
		if (!(x > 0 && y > 0 && z > 0)) {
			throw new IllegalArgumentException("The proportions must be positive");
		}
		aspect[0] = x;
		aspect[1] = y;
		aspect[2] = z;
		aspectSet = true;
		return this;
	}

	/**
	 * The categories of an axis (e.g., the names of groups): the numbers of the
	 * axis are the names at the positions 0, 1, 2, ... (the box plot, the violin
	 * plot and the histogram set their categories).
	 * 
	 * @param axis   0 = x, 1 = y, 2 = z
	 * @param labels the names of the categories
	 * @return the plot
	 */
	public Plot3D categories(int axis, String... labels) {
		userCategories[axis] = labels != null ? labels.clone() : null;
		return this;
	}

	/** The grids behind the data: the floor (XY) and the walls (XZ, YZ). */
	public Plot3D grids(boolean xy, boolean xz, boolean yz) {
		grids[0] = xy;
		grids[1] = xz;
		grids[2] = yz;
		return this;
	}

	/** The title of the color bar (default: the title of the z axis). */
	public Plot3D colorBarTitle(String title) {
		this.colorBarTitle = title;
		return this;
	}

	/**
	 * The colors of the texts (0xRRGGBB, -1 = the color of the lines of the 3D
	 * view): the title, the numbers of the axes (and the labels of the data, e.g.,
	 * the percentages of a pie chart), the titles of the axes and the legend with
	 * the color bar.
	 */
	public Plot3D textColors(int title, int numbers, int axisTitles, int legend) {
		textColors[0] = title;
		textColors[1] = numbers;
		textColors[2] = axisTitles;
		textColors[3] = legend;
		return this;
	}

	/**
	 * The font families of the texts (e.g., "Arial", "Georgia", null = Verdana):
	 * the title, the numbers of the axes (and the labels of the data), the titles
	 * of the axes and the legend with the color bar. The fonts are used in the 3D
	 * view, in the HTML export (CSS font families) and in the Blender export (the
	 * font files of the families, if found in the font folders of the system).
	 * See availableFonts.
	 */
	public Plot3D fonts(String title, String numbers, String axisTitles, String legend) {
		fonts[0] = title;
		fonts[1] = numbers;
		fonts[2] = axisTitles;
		fonts[3] = legend;
		return this;
	}

	/** The font family of all texts (e.g., "Arial", null = Verdana, see fonts). */
	public Plot3D font(String family) {
		return fonts(family, family, family, family);
	}

	/** Returns the names of the font families which are installed (for font and fonts). */
	public static String[] availableFonts() {
		return java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
	}

	/* The font families of the texts (for the exports, null = the default) */
	String[] fontsOf() {
		return fonts.clone();
	}

	/** The color of all texts (0xRRGGBB, -1 = the color of the lines of the 3D view). */
	public Plot3D textColor(int rgb) {
		return textColors(rgb, rgb, rgb, rgb);
	}

	/* The colors of the texts (for the exports) */
	int[] textColorsOf() {
		return textColors.clone();
	}

	/** Removes all layers (e.g., to add the layers of the next step of a simulation, see update). */
	public Plot3D clearLayers() {
		layers.clear();
		changed = true;
		return this;
	}

	/** Replaces a layer (the index of add). */
	public Plot3D set(int index, Layer<?> layer) {
		layers.set(index, layer);
		changed = true;
		return this;
	}

	/** Removes a layer (the index of add). */
	public Plot3D remove(int index) {
		layers.remove(index);
		changed = true;
		return this;
	}

	/** Returns the number of the layers. */
	public int layerCount() {
		return layers.size();
	}

	/** Displays the legend of the layers with a label (default true). */
	public Plot3D legend(boolean legend) {
		this.legend = legend;
		return this;
	}

	/** Adds a layer (e.g., Plot3D.scatter(x, y, z)). */
	public Plot3D add(Layer<?> layer) {
		layers.add(layer);
		changed = true;
		return this;
	}

	/* ----------------------------- The layers ----------------------------- */

	/** A scatter plot (spheres) of the points (x[i], y[i], z[i]). */
	public static Scatter scatter(double[] x, double[] y, double[] z) {
		return new Scatter(x, y, z);
	}

	/** A line through the points (x[i], y[i], z[i]). */
	public static Line line(double[] x, double[] y, double[] z) {
		return new Line(x, y, z);
	}

	/** A line through the points filled down to the floor (e.g., several series at different y). */
	public static FilledLine filledLine(double[] x, double[] y, double[] z) {
		return new FilledLine(x, y, z);
	}

	/** A surface of the grid z[i][j] at x[i], y[j]. */
	public static Surface surface(double[] x, double[] y, double[][] z) {
		return new Surface(x, y, z);
	}

	/** A wireframe of the grid z[i][j] at x[i], y[j]. */
	public static Wireframe wireframe(double[] x, double[] y, double[][] z) {
		return new Wireframe(x, y, z);
	}

	/** Bars of the grid z[i][j] at x[i], y[j] (e.g., a 2D histogram). */
	public static Bars bars(double[] x, double[] y, double[][] z) {
		return new Bars(x, y, z);
	}

	/** Bars at the points (x[i], y[i]) with the heights z[i]. */
	public static Bars bars(double[] x, double[] y, double[] z) {
		return new Bars(x, y, z);
	}

	/** The contour lines of the grid z[i][j] at x[i], y[j]. */
	public static Contour contour(double[] x, double[] y, double[][] z) {
		return new Contour(x, y, z);
	}

	/** A 3D pie chart of the values (the labels are displayed in the legend). */
	public static Pie pie(double[] values, String[] labels) {
		return new Pie(values, labels);
	}

	/**
	 * A line graph: the series of values over x (series[k][i] at x[i]), each
	 * series a line with markers at its own position in the depth (see
	 * PlotCharts.LineGraph).
	 */
	public static PlotCharts.LineGraph lineGraph(double[] x, double[][] series) {
		return new PlotCharts.LineGraph(x, series);
	}

	/** A box plot of groups (the values of each group, the names on the x axis, see PlotCharts.BoxPlot). */
	public static PlotCharts.BoxPlot boxplot(double[][] groups, String[] names) {
		return new PlotCharts.BoxPlot(groups, names);
	}

	/** A violin plot of groups (the kernel density of the values of each group, see PlotCharts.Violin). */
	public static PlotCharts.Violin violin(double[][] groups, String[] names) {
		return new PlotCharts.Violin(groups, names);
	}

	/** A histogram of values (see PlotCharts.Histogram). */
	public static PlotCharts.Histogram histogram(double[] values) {
		return new PlotCharts.Histogram(new double[][] { values }, null);
	}

	/** Histograms of groups (in the depth with the names on the y axis, see PlotCharts.Histogram). */
	public static PlotCharts.Histogram histogram(double[][] groups, String[] names) {
		return new PlotCharts.Histogram(groups, names);
	}

	/** An area chart of series over x (stacked, see PlotCharts.Area). */
	public static PlotCharts.Area area(double[] x, double[][] series) {
		return new PlotCharts.Area(x, series);
	}

	/** A radar chart: the values of the series (series[k][axis]) on the axes (without the axes of the box, see PlotCharts.Radar). */
	public static PlotCharts.Radar radar(double[][] series, String[] axes) {
		return new PlotCharts.Radar(series, axes);
	}

	/** A rose chart: a value for each sector (e.g., the directions of a wind rose, see PlotCharts.Rose). */
	public static PlotCharts.Rose rose(double[] values, String[] sectors) {
		double[][] v = new double[values.length][];
		for (int i = 0; i < values.length; i++) {
			v[i] = new double[] { values[i] };
		}
		return new PlotCharts.Rose(v, sectors);
	}

	/** A rose chart with classes: values[sector][class] stacked from the center (e.g., the wind speeds of a wind rose). */
	public static PlotCharts.Rose rose(double[][] values, String[] sectors) {
		return new PlotCharts.Rose(values, sectors);
	}

	/** A treemap of the values (rectangles with the area of the values, see PlotCharts.Treemap). */
	public static PlotCharts.Treemap treemap(double[] values, String[] labels) {
		return new PlotCharts.Treemap(values, labels);
	}

	/* ----------------------------- Show, clear, export ----------------------------- */

	/**
	 * Displays the plot in the 3D view (opens the 3D perspective if necessary). The
	 * plot replaces a displayed plot.
	 *
	 * @return the plot
	 */
	public Plot3D show() {
		build();
		fullApply = true;
		applied = false;
		current = this;
		if (SpatialStructure.getSpatialStructureInstance() == null) {
			com.eco.bio7.collection.Work.openPerspective("com.eco.bio7.perspective_3d");
		}
		SpatialStructure struc = SpatialStructure.getSpatialStructureInstance();
		if (struc != null) {
			apply(struc);
		}
		/* The 3D view in front (e.g., if the Bio7 table is in the same place) */
		org.eclipse.swt.widgets.Display display = org.eclipse.ui.PlatformUI.getWorkbench().getDisplay();
		display.syncExec(() -> {
			try {
				org.eclipse.ui.IWorkbenchWindow window = org.eclipse.ui.PlatformUI.getWorkbench().getActiveWorkbenchWindow();
				org.eclipse.ui.IWorkbenchPage page = window != null ? window.getActivePage() : null;
				if (page != null) {
					/* The Bio7 table in the stack of the 3D view: the table is moved below the 3D view */
					org.eclipse.ui.IViewPart table = page.findView(com.eco.bio7.rbridge.RTable.ID);
					if (table != null) {
						Measurement3d.keep3dViewVisible(window, page, table);
					}
					org.eclipse.ui.IViewPart view = page.showView(SpatialView.ID, null, org.eclipse.ui.IWorkbenchPage.VIEW_VISIBLE);
					if (view != null) {
						page.bringToTop(view);
					}
				}
			} catch (org.eclipse.ui.PartInitException e) {
				e.printStackTrace();
			}
		});
		if (exportAction != null) {
			setExportAction(exportAction);
		}
		return this;
	}

	/**
	 * Updates the displayed plot with the current layers (e.g., in each step of a
	 * simulation, also from the OpenGL thread of a model): the plot is computed
	 * again and the 3D view displays it in the next frame. The camera, the font
	 * sizes and the view are kept (the 3D view is not opened or brought to the
	 * front, see show). Fixed ranges (xRange, yRange, zRange) avoid that the axes
	 * change with the data.
	 * 
	 * @return the plot
	 */
	public Plot3D update() {
		build();
		applied = false;
		if (current != this) {
			fullApply = true;
			current = this;
		}
		return this;
	}

	/**
	 * Removes the displayed plot from the 3D view (and the title, the axes and the
	 * legends of the plot).
	 */
	public static void clear() {
		current = null;
		SpatialStructure struc = SpatialStructure.getSpatialStructureInstance();
		if (struc != null) {
			struc.resetPlotLabels();
			struc.setDataBounds(null);
			struc.plotView = false;
			restoreScene(struc);
		}
	}

	/**
	 * Shows or hides the action "Export Plot (WebGL, Blender)" in the toolbar of the 3D view
	 * (hidden by default). The action exports the displayed plot to an HTML page
	 * with WebGL or for Blender (glTF and a Python script).
	 * 
	 * @param visible true to show the action
	 */
	public static void setExportAction(boolean visible) {
		SpatialView.setExportActionVisible(visible);
	}

	/**
	 * Shows the action "Export Plot (WebGL, Blender)" in the toolbar of the 3D view when the
	 * plot is displayed (see setExportAction).
	 * 
	 * @param visible true to show the action
	 * @return the plot
	 */
	public Plot3D exportAction(boolean visible) {
		this.exportAction = visible;
		return this;
	}

	/** Returns the displayed plot or null. */
	public static Plot3D getCurrent() {
		return current;
	}

	/**
	 * Exports the plot to an HTML page with WebGL (a single file without
	 * dependencies): the plot can be rotated with the mouse and zoomed with the
	 * mouse wheel in a web browser.
	 *
	 * @param file the HTML file
	 * @throws IOException if the file cannot be written
	 */
	public void exportHtml(String file) throws IOException {
		build();
		Files.writeString(Path.of(file), PlotHtml.create(this), StandardCharsets.UTF_8);
	}

	/**
	 * Exports the plot to a glTF binary file (*.glb), e.g., for Blender (File -
	 * Import - glTF 2.0): an object for each layer (named after the label) with
	 * the vertex colors, the points as spheres, the lines as tubes and the box,
	 * the grids and the axes as an object "Frame" (not for a pie chart). The texts
	 * are not part of the file (see exportBlender).
	 * 
	 * @param file the glTF file (*.glb)
	 * @throws IOException if the file cannot be written
	 */
	public void exportGltf(String file) throws IOException {
		build();
		PlotGltf.writeGlb(this, Path.of(file));
	}

	/**
	 * Exports the plot for Blender: the glTF file (see exportGltf) and a Python
	 * script with the same name (*.py) which imports the file in Blender and adds
	 * the title, the axis titles and the numbers as text objects, a camera and the
	 * lights (in Blender: Scripting - Open - Run Script).
	 * 
	 * @param file the glTF file (*.glb), the script is written next to it
	 * @return the path of the Python script
	 * @throws IOException if a file cannot be written
	 */
	public String exportBlender(String file) throws IOException {
		build();
		Path glb = Path.of(file);
		PlotGltf.writeGlb(this, glb);
		String name = glb.getFileName().toString();
		String base = name.contains(".") ? name.substring(0, name.lastIndexOf('.')) : name;
		Path script = glb.resolveSibling(base + ".py");
		Files.writeString(script, PlotGltf.blenderScript(this, glb), StandardCharsets.UTF_8);
		return script.toString();
	}

	/* Called in the OpenGL thread of the 3D view (in the coordinates of the data bounds) */
	/* The geometry of the last frame (its display list is deleted if the plot is updated) */
	private static PlotGeometry drawn;

	static void render(GL2 gl, GLUT glut, SpatialStructure struc) {
		Plot3D plot = current;
		PlotGeometry geometry = plot != null ? plot.geometry : null;
		if (drawn != null && drawn != geometry) {
			drawn.dispose(gl);
			drawn = null;
		}
		if (plot == null || geometry == null) {
			return;
		}
		if (!plot.applied) {
			plot.apply(struc);
		}
		/* A texture, volume or iso surface defines the box: the plot is not displayed */
		if (struc.getTextureExtent() != null) {
			return;
		}
		geometry.draw(gl, glut);
		drawn = geometry;
		SpatialScene scene = struc.getScene();
		if (scene != null && !geometry.labels.isEmpty()) {
			float factor = AxisLabelsRenderer.getSizeFactor(struc);
			float[] color = scene.textColor(struc.plotTextColors, 1);
			String family = SpatialScene.family(struc.plotFonts, 1);
			for (PlotGeometry.Label label : geometry.labels) {
				scene.drawPlotText(gl, label.text(), label.x(), label.y(), label.z(), 0.5f * factor * label.size(), color, family);
			}
		}
	}

	/* The settings of the 3D view (the box, the axes, the labels and the legends) */
	private void apply(SpatialStructure struc) {
		applied = true;
		/* The first display removes the former settings, an update keeps the camera and the font sizes */
		if (fullApply) {
			struc.resetPlotLabels();
			fullApply = false;
		}
		struc.plotLegend = null;
		struc.plotColorBar = null;
		double[] bounds = { 0, box[0], 0, box[1], 0, box[2] };
		if (!java.util.Arrays.equals(bounds, struc.getDataBounds())) {
			struc.setDataBounds(bounds);
		}
		struc.plotTextColors = textColors.clone();
		struc.plotFonts = fonts.clone();
		if (isPie()) {
			/* No axes for a pie chart */
			struc.plotView = false;
			AxisLabelsRenderer numbers = struc.getAxisLabelsRenderer();
			if (sceneFlags == null) {
				sceneFlags = new boolean[] { struc.showGrid, struc.showAxes, struc.showQuad, numbers != null && numbers.isEnabled() };
			}
			struc.showGrid = false;
			struc.showAxes = false;
			struc.showQuad = false;
			if (numbers != null) {
				numbers.setEnabled(false);
			}
		} else {
			restoreScene(struc);
			struc.plotAxisRanges = new double[] { ranges[0][0], ranges[0][1], ranges[1][0], ranges[1][1], ranges[2][0], ranges[2][1] };
			struc.plotLogAxes = new boolean[] { logAxes[0] && categories[0] == null, logAxes[1] && categories[1] == null,
					logAxes[2] && categories[2] == null };
			struc.plotNumberFormats = numberFormats.clone();
			/* An axis without a meaning (one empty category, e.g., the depth of a box plot) has no title */
			String[] titles = axisTitles.clone();
			for (int k = 0; k < 3; k++) {
				if (hiddenAxis(k)) {
					titles[k] = "";
				}
			}
			struc.plotAxisTitles = titles;
			struc.plotAxisCategories = categories.clone();
			struc.plotGridXY = grids[0];
			struc.plotGridXZ = grids[1];
			struc.plotGridYZ = grids[2];
			struc.showGrid = true;
			struc.plotView = true;
			if (struc.getAxisLabelsRenderer() != null) {
				struc.getAxisLabelsRenderer().setEnabled(true);
			}
			Options3d.updatePlotGrids();
		}
		struc.plotTitle = title;
		LegendData legendData = legendData();
		if (legendData.labels.length > 0) {
			struc.plotLegend = PlotLegend.categories(legendData.title, legendData.labels, legendData.colors);
		}
		ColorBarData bar = colorBarData();
		if (bar != null) {
			struc.plotColorBar = PlotLegend.colorBar(bar.title, bar.min, bar.max, bar.colors, bar.log, bar.format);
		}
	}

	private static void restoreScene(SpatialStructure struc) {
		if (sceneFlags != null) {
			struc.showGrid = sceneFlags[0];
			struc.showAxes = sceneFlags[1];
			struc.showQuad = sceneFlags[2];
			if (struc.getAxisLabelsRenderer() != null) {
				struc.getAxisLabelsRenderer().setEnabled(sceneFlags[3]);
			}
			sceneFlags = null;
		}
	}

	/* True for a chart without axes (e.g., a pie, radar, rose chart or treemap) */
	boolean isPie() {
		return layers.stream().anyMatch(l -> !l.hasAxes());
	}

	/* The categories of the axes (null for an axis with numbers) */
	String[][] categories() {
		return categories;
	}

	/* ----------------------------- The computation ----------------------------- */

	/* Computes the ranges, the box and the geometry */
	private synchronized void build() {
		if (layers.isEmpty()) {
			throw new IllegalStateException("The plot has no layers (see add)");
		}
		/* The proportions of the box: of the user or of a layer (e.g., a box plot) */
		double[] a = aspect;
		if (!aspectSet) {
			for (Layer<?> layer : layers) {
				if (layer.preferredAspect() != null) {
					a = layer.preferredAspect();
					break;
				}
			}
		}
		double max = Math.max(a[0], Math.max(a[1], a[2]));
		box = new double[] { 10 * a[0] / max, 10 * a[1] / max, 10 * a[2] / max };
		/* The categories of the axes (the user or the first layer with categories) */
		categories = new String[3][];
		for (int k = 0; k < 3; k++) {
			categories[k] = userCategories[k];
			for (Layer<?> layer : layers) {
				if (categories[k] == null && layer.categories(k) != null) {
					categories[k] = layer.categories(k);
				}
			}
		}
		if (isPie()) {
			box = new double[] { 10, 10, 3 };
			ranges = new double[][] { { 0, 10 }, { 0, 10 }, { 0, 3 } };
			categories = new String[3][];
		} else {
			/* The ranges of the data (transformed values, e.g., log10) */
			double[][] extent = { { Double.MAX_VALUE, -Double.MAX_VALUE }, { Double.MAX_VALUE, -Double.MAX_VALUE },
					{ Double.MAX_VALUE, -Double.MAX_VALUE } };
			for (Layer<?> layer : layers) {
				layer.extent(this, extent);
			}
			ranges = new double[3][];
			for (int k = 0; k < 3; k++) {
				double lo, hi;
				if (userRanges[k] != null) {
					lo = value(k, userRanges[k][0]);
					hi = value(k, userRanges[k][1]);
				} else if (categories[k] != null && categories[k].length > 0) {
					/* The categories at 0, 1, 2, ... */
					lo = -0.5;
					hi = categories[k].length - 0.5;
				} else {
					lo = extent[k][0];
					hi = extent[k][1];
				}
				if (!(lo <= hi) || Double.isInfinite(lo) || Double.isInfinite(hi)) {
					lo = 0;
					hi = 1;
				}
				if (hi == lo) {
					double d = lo == 0 ? 1 : Math.abs(lo) * 0.1;
					lo -= d;
					hi += d;
				}
				ranges[k] = new double[] { lo, hi };
			}
		}
		/* The geometry of each layer (e.g., the objects of a glTF export) and of all layers */
		PlotGeometry g = new PlotGeometry();
		List<PlotGeometry> perLayer = new ArrayList<>();
		for (int i = 0; i < layers.size(); i++) {
			PlotGeometry lg = new PlotGeometry();
			layers.get(i).build(lg, this, i);
			perLayer.add(lg);
			g.append(lg);
		}
		layerGeometries = perLayer;
		geometry = g;
		changed = false;
	}

	/* True if the layers changed after the last computation */
	private volatile boolean changed = true;

	/*
	 * A copy of the computed plot (e.g., a frame of a recording for Blender): the
	 * settings, the layers and the geometry (the geometry is not changed by a
	 * later computation, it is replaced).
	 */
	synchronized Plot3D snapshot() {
		if (geometry == null || changed) {
			build();
		}
		Plot3D p = new Plot3D();
		p.title = title;
		System.arraycopy(axisTitles, 0, p.axisTitles, 0, 3);
		System.arraycopy(logAxes, 0, p.logAxes, 0, 3);
		System.arraycopy(numberFormats, 0, p.numberFormats, 0, 3);
		System.arraycopy(userRanges, 0, p.userRanges, 0, 3);
		System.arraycopy(aspect, 0, p.aspect, 0, 3);
		System.arraycopy(grids, 0, p.grids, 0, 3);
		System.arraycopy(textColors, 0, p.textColors, 0, 4);
		System.arraycopy(fonts, 0, p.fonts, 0, 4);
		p.colorBarTitle = colorBarTitle;
		p.legend = legend;
		p.aspectSet = aspectSet;
		p.layers.addAll(layers);
		p.ranges = ranges;
		p.box = box;
		p.categories = categories;
		p.geometry = geometry;
		p.layerGeometries = layerGeometries;
		p.changed = false;
		return p;
	}

	/* The plot as a frame of an HTML page (see PlotRecorder): computed if the layers changed */
	synchronized String frameJson() {
		if (geometry == null || changed) {
			build();
		}
		return PlotHtml.frame(this);
	}

	/* The geometry of the layers (the same order as the layers) */
	private List<PlotGeometry> layerGeometries;

	List<PlotGeometry> layerGeometries() {
		return layerGeometries;
	}

	/* The name of a layer (its label or the type and the number) */
	String layerName(int index) {
		Layer<?> layer = layers.get(index);
		return layer.label != null && !layer.label.isBlank() ? layer.label : layer.getClass().getSimpleName() + " " + (index + 1);
	}

	/* The value of an axis (log10 for a log axis, NaN for values <= 0) */
	double value(int axis, double v) {
		if (logAxes[axis] && (categories == null || categories[axis] == null)) {
			return v > 0 ? Math.log10(v) : Double.NaN;
		}
		return v;
	}

	/* The coordinate of the box of a transformed value */
	double box(int axis, double t) {
		return (t - ranges[axis][0]) / (ranges[axis][1] - ranges[axis][0]) * box[axis];
	}

	/* The position in the box of the data values (null if a value cannot be displayed) */
	double[] position(double x, double y, double z) {
		double tx = value(0, x), ty = value(1, y), tz = value(2, z);
		if (!Double.isFinite(tx) || !Double.isFinite(ty) || !Double.isFinite(tz)) {
			return null;
		}
		return new double[] { box(0, tx), box(1, ty), box(2, tz) };
	}

	/* The z of the floor (the bottom of the box) */
	double floor() {
		return 0;
	}

	/* The color of a layer (its color or a color of the categories) */
	int layerColor(Layer<?> layer, int index) {
		if (layer.color != null) {
			return layer.color;
		}
		return CATEGORY_COLORS[index % CATEGORY_COLORS.length];
	}

	/* The color of a transformed z value of a color map (the range of the z axis) */
	float[] zColor(Layer<?> layer, double tz) {
		return PlotGeometry.rgba(PlotLegend.rampColor(layer.colorMap, ranges[2][0], ranges[2][1], tz), layer.alpha);
	}

	/* ----------------------------- Legends ----------------------------- */

	record LegendData(String title, String[] labels, int[] colors) {
	}

	record ColorBarData(String title, double min, double max, int[] colors, boolean log, String format) {
	}

	LegendData legendData() {
		List<String> labels = new ArrayList<>();
		List<Integer> colors = new ArrayList<>();
		if (legend) {
			for (int i = 0; i < layers.size(); i++) {
				layers.get(i).legend(this, i, labels, colors);
			}
		}
		return new LegendData(null, labels.toArray(new String[0]), colors.stream().mapToInt(Integer::intValue).toArray());
	}

	ColorBarData colorBarData() {
		for (Layer<?> layer : layers) {
			if (layer.colorMap != null && !(layer instanceof Pie)) {
				double[] values = layer.colorValueRange();
				String barTitle = colorBarTitle != null ? colorBarTitle : axisTitles[2] != null ? axisTitles[2] : "z";
				if (values != null) {
					return new ColorBarData(barTitle, values[0], values[1], layer.colorMap, false, null);
				}
				return new ColorBarData(barTitle, ranges[2][0], ranges[2][1], layer.colorMap, logAxes[2], numberFormats[2]);
			}
		}
		return null;
	}

	/* For the HTML export */
	List<Layer<?>> layers() {
		return layers;
	}

	String title() {
		return title;
	}

	String[] axisTitlesOrDefault() {
		String[] names = { "X", "Y", "Z" };
		String[] t = new String[3];
		for (int k = 0; k < 3; k++) {
			t[k] = hiddenAxis(k) ? "" : axisTitles[k] != null && !axisTitles[k].isBlank() ? axisTitles[k] : names[k];
		}
		return t;
	}

	/* True for an axis with one empty category (e.g., the depth of a box plot) */
	boolean hiddenAxis(int k) {
		return categories != null && categories[k] != null && categories[k].length == 1 && categories[k][0].isEmpty();
	}

	boolean[] logAxes() {
		return logAxes;
	}

	String[] numberFormats() {
		return numberFormats;
	}

	double[][] ranges() {
		return ranges;
	}

	double[] boxSize() {
		return box;
	}

	boolean[] grids() {
		return grids;
	}

	PlotGeometry geometry() {
		return geometry;
	}

	/* ----------------------------- The layer classes ----------------------------- */

	/**
	 * A layer of a plot with the common settings: a label (for the legend), a
	 * color, a color map and the transparency.
	 */
	public abstract static class Layer<T extends Layer<T>> {
		String label;
		Integer color;
		int[] colorMap;
		float alpha = 1;

		@SuppressWarnings("unchecked")
		T self() {
			return (T) this;
		}

		/** The label of the layer in the legend. */
		public T label(String label) {
			this.label = label;
			return self();
		}

		/** One color (0xRRGGBB) for the layer (instead of a color map). */
		public T color(int rgb) {
			this.color = rgb;
			this.colorMap = null;
			return self();
		}

		/**
		 * The colors of the z values (e.g., PlotLegend.VIRIDIS) with a color bar
		 * (null = one color).
		 */
		public T colorMap(int[] ramp) {
			this.colorMap = ramp;
			return self();
		}

		/** The opacity (0 transparent - 1 opaque). */
		public T alpha(double alpha) {
			this.alpha = (float) Math.max(0, Math.min(1, alpha));
			return self();
		}

		/* Adds the transformed values of the data to the extent */
		abstract void extent(Plot3D plot, double[][] extent);

		/* False for a chart without axes (e.g., a pie chart) */
		boolean hasAxes() {
			return true;
		}

		/* The categories of an axis (e.g., the groups of a box plot) or null */
		String[] categories(int axis) {
			return null;
		}

		/* The proportions of the box for the layer (if the user sets none) or null */
		double[] preferredAspect() {
			return null;
		}

		/* The entries of the legend (the label of the layer with its color) */
		void legend(Plot3D plot, int index, List<String> labels, List<Integer> colors) {
			if (label != null && colorMap == null) {
				labels.add(label);
				colors.add(plot.layerColor(this, index));
			}
		}

		/* Creates the geometry of the layer */
		abstract void build(PlotGeometry g, Plot3D plot, int index);

		/* The range of the values of the colors if not the z values (e.g., the values of a scatter plot) */
		double[] colorValueRange() {
			return null;
		}

		/* The color of a transformed z value or the color of the layer */
		float[] color(Plot3D plot, int index, double tz) {
			return colorMap != null ? plot.zColor(this, tz) : PlotGeometry.rgba(plot.layerColor(this, index), alpha);
		}

		static void add(Plot3D plot, double[][] extent, int axis, double v) {
			double t = plot.value(axis, v);
			if (Double.isFinite(t)) {
				extent[axis][0] = Math.min(extent[axis][0], t);
				extent[axis][1] = Math.max(extent[axis][1], t);
			}
		}

		static void check(double[] x, double[] y, double[] z) {
			if (x == null || y == null || z == null || x.length != y.length || x.length != z.length) {
				throw new IllegalArgumentException("x, y and z need the same number of values");
			}
		}

		static void checkGrid(double[] x, double[] y, double[][] z) {
			if (x == null || y == null || z == null || z.length != x.length) {
				throw new IllegalArgumentException("The grid z[i][j] needs a row for each x value");
			}
			for (double[] row : z) {
				if (row == null || row.length != y.length) {
					throw new IllegalArgumentException("The grid z[i][j] needs a value for each y value in each row");
				}
			}
		}
	}

	/** A scatter plot: spheres at the points. */
	public static final class Scatter extends Layer<Scatter> {
		final double[] x, y, z;
		double size = 0.12;
		double[] values;
		int[] colors;
		boolean dropLines;

		Scatter(double[] x, double[] y, double[] z) {
			check(x, y, z);
			this.x = x.clone();
			this.y = y.clone();
			this.z = z.clone();
		}

		/** The radius of the spheres (the box has the size 10, default 0.12). */
		public Scatter size(double radius) {
			this.size = radius;
			return this;
		}

		/** The colors of the points by values (with the color map, default VIRIDIS). */
		public Scatter values(double[] values) {
			if (values.length != x.length) {
				throw new IllegalArgumentException("A value for each point is needed");
			}
			this.values = values.clone();
			if (colorMap == null) {
				colorMap = PlotLegend.VIRIDIS;
			}
			return this;
		}

		/** A color (0xRRGGBB) for each point. */
		public Scatter colors(int[] colors) {
			if (colors.length != x.length) {
				throw new IllegalArgumentException("A color for each point is needed");
			}
			this.colors = colors.clone();
			return this;
		}

		/** Lines from the points to the floor. */
		public Scatter dropLines(boolean dropLines) {
			this.dropLines = dropLines;
			return this;
		}

		@Override
		void extent(Plot3D plot, double[][] extent) {
			for (int i = 0; i < x.length; i++) {
				add(plot, extent, 0, x[i]);
				add(plot, extent, 1, y[i]);
				add(plot, extent, 2, z[i]);
			}
		}

		@Override
		double[] colorValueRange() {
			if (values == null) {
				return null;
			}
			double min = Arrays.stream(values).filter(Double::isFinite).min().orElse(0);
			double max = Arrays.stream(values).filter(Double::isFinite).max().orElse(1);
			return max > min ? new double[] { min, max } : new double[] { min, min + 1 };
		}

		@Override
		void build(PlotGeometry g, Plot3D plot, int index) {
			double[] valueRange = colorValueRange();
			float[] lineColor = { 0.3f, 0.3f, 0.3f, 0.6f };
			for (int i = 0; i < x.length; i++) {
				double[] p = plot.position(x[i], y[i], z[i]);
				if (p == null) {
					continue;
				}
				float[] c;
				if (colors != null) {
					c = PlotGeometry.rgba(colors[i], alpha);
				} else if (values != null && colorMap != null) {
					c = PlotGeometry.rgba(PlotLegend.rampColor(colorMap, valueRange[0], valueRange[1], values[i]), alpha);
				} else {
					c = color(plot, index, plot.value(2, z[i]));
				}
				g.point(p, c, size);
				if (dropLines) {
					g.line(p, new double[] { p[0], p[1], plot.floor() }, lineColor);
				}
			}
		}
	}

	/** A line through the points. */
	public static class Line extends Layer<Line> {
		final double[] x, y, z;

		Line(double[] x, double[] y, double[] z) {
			check(x, y, z);
			this.x = x.clone();
			this.y = y.clone();
			this.z = z.clone();
		}

		@Override
		void extent(Plot3D plot, double[][] extent) {
			for (int i = 0; i < x.length; i++) {
				add(plot, extent, 0, x[i]);
				add(plot, extent, 1, y[i]);
				add(plot, extent, 2, z[i]);
			}
		}

		@Override
		void build(PlotGeometry g, Plot3D plot, int index) {
			double[] last = null;
			float[] lastColor = null;
			for (int i = 0; i < x.length; i++) {
				double[] p = plot.position(x[i], y[i], z[i]);
				if (p == null) {
					last = null;
					continue;
				}
				float[] c = color(plot, index, plot.value(2, z[i]));
				if (last != null) {
					g.line(last, lastColor, p, c);
				}
				last = p;
				lastColor = c;
			}
		}
	}

	/** A line filled down to the floor (a ribbon). */
	public static final class FilledLine extends Layer<FilledLine> {
		final double[] x, y, z;

		FilledLine(double[] x, double[] y, double[] z) {
			check(x, y, z);
			this.x = x.clone();
			this.y = y.clone();
			this.z = z.clone();
			alpha = 0.8f;
		}

		@Override
		void extent(Plot3D plot, double[][] extent) {
			for (int i = 0; i < x.length; i++) {
				add(plot, extent, 0, x[i]);
				add(plot, extent, 1, y[i]);
				add(plot, extent, 2, z[i]);
			}
		}

		@Override
		void build(PlotGeometry g, Plot3D plot, int index) {
			double[] last = null;
			float[] lastColor = null;
			float[] edge = PlotGeometry.rgba(0x333333, 0.9f);
			for (int i = 0; i < x.length; i++) {
				double[] p = plot.position(x[i], y[i], z[i]);
				if (p == null) {
					last = null;
					continue;
				}
				float[] c = color(plot, index, plot.value(2, z[i]));
				if (last != null) {
					double[] a0 = { last[0], last[1], plot.floor() }, b0 = { p[0], p[1], plot.floor() };
					double[] n = PlotGeometry.normal(a0, b0, p);
					g.triangle(a0, n, lastColor, b0, n, c, p, n, c);
					g.triangle(a0, n, lastColor, p, n, c, last, n, lastColor);
					g.line(last, edge, p, edge);
				}
				last = p;
				lastColor = c;
			}
		}
	}

	/* The common data of the grid layers */
	abstract static class GridLayer<T extends GridLayer<T>> extends Layer<T> {
		final double[] x, y;
		final double[][] z;

		GridLayer(double[] x, double[] y, double[][] z) {
			checkGrid(x, y, z);
			this.x = x.clone();
			this.y = y.clone();
			this.z = new double[z.length][];
			for (int i = 0; i < z.length; i++) {
				this.z[i] = z[i].clone();
			}
		}

		@Override
		void extent(Plot3D plot, double[][] extent) {
			for (double v : x) {
				add(plot, extent, 0, v);
			}
			for (double v : y) {
				add(plot, extent, 1, v);
			}
			for (double[] row : z) {
				for (double v : row) {
					add(plot, extent, 2, v);
				}
			}
		}

		/* The positions of the grid in the box (null if a value cannot be displayed) */
		double[][][] positions(Plot3D plot) {
			double[][][] p = new double[x.length][y.length][];
			for (int i = 0; i < x.length; i++) {
				for (int j = 0; j < y.length; j++) {
					p[i][j] = plot.position(x[i], y[j], z[i][j]);
				}
			}
			return p;
		}
	}

	/** A surface of a grid (smooth shading, optional with the lines of the grid). */
	public static final class Surface extends GridLayer<Surface> {
		boolean mesh = true;
		int meshStep = 0;

		Surface(double[] x, double[] y, double[][] z) {
			super(x, y, z);
			colorMap = PlotLegend.VIRIDIS;
		}

		/** Draws the lines of the grid on the surface (default true). */
		public Surface mesh(boolean mesh) {
			this.mesh = mesh;
			return this;
		}

		/** The step of the lines of the grid (e.g., 5 for every 5th line, 0 = automatic). */
		public Surface meshStep(int step) {
			this.meshStep = step;
			return this;
		}

		@Override
		void build(PlotGeometry g, Plot3D plot, int index) {
			double[][][] p = positions(plot);
			int nx = x.length, ny = y.length;
			/* The normals of the vertices from the neighbors */
			double[][][] n = new double[nx][ny][];
			float[][][] c = new float[nx][ny][];
			for (int i = 0; i < nx; i++) {
				for (int j = 0; j < ny; j++) {
					if (p[i][j] == null) {
						continue;
					}
					double[] dx = difference(p[Math.min(nx - 1, i + 1)][j], p[Math.max(0, i - 1)][j]);
					double[] dy = difference(p[i][Math.min(ny - 1, j + 1)], p[i][Math.max(0, j - 1)]);
					double[] nn = dx != null && dy != null ? cross(dx, dy) : new double[] { 0, 0, 1 };
					n[i][j] = nn;
					c[i][j] = color(plot, index, plot.value(2, z[i][j]));
				}
			}
			for (int i = 0; i < nx - 1; i++) {
				for (int j = 0; j < ny - 1; j++) {
					double[] a = p[i][j], b = p[i + 1][j], d = p[i + 1][j + 1], e = p[i][j + 1];
					if (a == null || b == null || d == null || e == null) {
						continue;
					}
					g.triangle(a, n[i][j], c[i][j], b, n[i + 1][j], c[i + 1][j], d, n[i + 1][j + 1], c[i + 1][j + 1]);
					g.triangle(a, n[i][j], c[i][j], d, n[i + 1][j + 1], c[i + 1][j + 1], e, n[i][j + 1], c[i][j + 1]);
				}
			}
			if (mesh) {
				int step = meshStep > 0 ? meshStep : Math.max(1, Math.max(nx, ny) / 16);
				float[] lineColor = { 0.1f, 0.1f, 0.1f, 0.45f };
				gridLines(g, p, step, lineColor);
			}
		}

		private static double[] difference(double[] a, double[] b) {
			return a != null && b != null ? new double[] { a[0] - b[0], a[1] - b[1], a[2] - b[2] } : null;
		}

		private static double[] cross(double[] u, double[] v) {
			double[] n = { u[1] * v[2] - u[2] * v[1], u[2] * v[0] - u[0] * v[2], u[0] * v[1] - u[1] * v[0] };
			double length = Math.sqrt(n[0] * n[0] + n[1] * n[1] + n[2] * n[2]);
			return length > 0 ? new double[] { n[0] / length, n[1] / length, n[2] / length } : new double[] { 0, 0, 1 };
		}
	}

	/* The lines of a grid (every step-th line in both directions) */
	static void gridLines(PlotGeometry g, double[][][] p, int step, float[] color) {
		int nx = p.length, ny = p[0].length;
		for (int i = 0; i < nx; i++) {
			for (int j = 0; j < ny; j++) {
				if (p[i][j] == null) {
					continue;
				}
				if (i % step == 0 && j + 1 < ny && p[i][j + 1] != null) {
					g.line(p[i][j], p[i][j + 1], color);
				}
				if (j % step == 0 && i + 1 < nx && p[i + 1][j] != null) {
					g.line(p[i][j], p[i + 1][j], color);
				}
			}
		}
	}

	/** A wireframe of a grid. */
	public static final class Wireframe extends GridLayer<Wireframe> {
		int step = 1;

		Wireframe(double[] x, double[] y, double[][] z) {
			super(x, y, z);
		}

		/** The step of the lines (e.g., 2 for every 2nd line, default 1). */
		public Wireframe step(int step) {
			this.step = Math.max(1, step);
			return this;
		}

		@Override
		void build(PlotGeometry g, Plot3D plot, int index) {
			double[][][] p = positions(plot);
			int nx = x.length, ny = y.length;
			for (int i = 0; i < nx; i++) {
				for (int j = 0; j < ny; j++) {
					if (p[i][j] == null) {
						continue;
					}
					float[] c = color(plot, index, plot.value(2, z[i][j]));
					if (i % step == 0 && j + 1 < ny && p[i][j + 1] != null) {
						g.line(p[i][j], c, p[i][j + 1], color(plot, index, plot.value(2, z[i][j + 1])));
					}
					if (j % step == 0 && i + 1 < nx && p[i + 1][j] != null) {
						g.line(p[i][j], c, p[i + 1][j], color(plot, index, plot.value(2, z[i + 1][j])));
					}
				}
			}
		}
	}

	/** Bars (boxes) from the floor (or 0) to the values. */
	public static final class Bars extends Layer<Bars> {
		/* x, y, height and the cell sizes (data units) of the bars */
		final double[] bx, by, bz;
		final double cellX, cellY;
		double width = 0.8;

		Bars(double[] x, double[] y, double[][] z) {
			checkGrid(x, y, z);
			int n = x.length * y.length;
			bx = new double[n];
			by = new double[n];
			bz = new double[n];
			int k = 0;
			for (int i = 0; i < x.length; i++) {
				for (int j = 0; j < y.length; j++) {
					bx[k] = x[i];
					by[k] = y[j];
					bz[k++] = z[i][j];
				}
			}
			cellX = spacing(x);
			cellY = spacing(y);
			colorMap = PlotLegend.VIRIDIS;
		}

		Bars(double[] x, double[] y, double[] z) {
			check(x, y, z);
			bx = x.clone();
			by = y.clone();
			bz = z.clone();
			cellX = spacing(x);
			cellY = spacing(y);
			colorMap = PlotLegend.VIRIDIS;
		}

		/** The width of the bars as a fraction of the distance of the values (default 0.8). */
		public Bars width(double fraction) {
			this.width = Math.max(0.05, Math.min(1, fraction));
			return this;
		}

		/* The smallest distance of the different values (or 1) */
		private static double spacing(double[] v) {
			double[] s = Arrays.stream(v).filter(Double::isFinite).sorted().distinct().toArray();
			double d = Double.MAX_VALUE;
			for (int i = 1; i < s.length; i++) {
				d = Math.min(d, s[i] - s[i - 1]);
			}
			if (d == Double.MAX_VALUE) {
				return s.length > 0 && s[0] != 0 ? Math.abs(s[0]) * 0.2 : 1;
			}
			return d;
		}

		@Override
		void extent(Plot3D plot, double[][] extent) {
			for (int i = 0; i < bx.length; i++) {
				add(plot, extent, 0, bx[i] - cellX / 2);
				add(plot, extent, 0, bx[i] + cellX / 2);
				add(plot, extent, 1, by[i] - cellY / 2);
				add(plot, extent, 1, by[i] + cellY / 2);
				add(plot, extent, 2, bz[i]);
			}
			/* The bars start at 0 (not for a log axis) */
			if (!plot.logAxes[2]) {
				add(plot, extent, 2, 0);
			}
		}

		@Override
		void build(PlotGeometry g, Plot3D plot, int index) {
			/* The base: 0 in the range or the bottom of the box */
			double base = plot.logAxes[2] ? plot.ranges[2][0] : Math.max(plot.ranges[2][0], Math.min(plot.ranges[2][1], 0));
			double z0 = plot.box(2, base);
			for (int i = 0; i < bx.length; i++) {
				double tz = plot.value(2, bz[i]);
				double tx0 = plot.value(0, bx[i] - cellX * width / 2), tx1 = plot.value(0, bx[i] + cellX * width / 2);
				double ty0 = plot.value(1, by[i] - cellY * width / 2), ty1 = plot.value(1, by[i] + cellY * width / 2);
				if (!Double.isFinite(tz) || !Double.isFinite(tx0) || !Double.isFinite(tx1) || !Double.isFinite(ty0)
						|| !Double.isFinite(ty1) || tz == base) {
					continue;
				}
				double x0 = plot.box(0, tx0), x1 = plot.box(0, tx1), y0 = plot.box(1, ty0), y1 = plot.box(1, ty1);
				double z1 = plot.box(2, tz);
				box(g, x0, x1, y0, y1, Math.min(z0, z1), Math.max(z0, z1), color(plot, index, tz));
			}
		}

		static void box(PlotGeometry g, double x0, double x1, double y0, double y1, double z0, double z1, float[] c) {
			double[] a = { x0, y0, z0 }, b = { x1, y0, z0 }, d = { x1, y1, z0 }, e = { x0, y1, z0 };
			double[] a1 = { x0, y0, z1 }, b1 = { x1, y0, z1 }, d1 = { x1, y1, z1 }, e1 = { x0, y1, z1 };
			g.quad(a1, b1, d1, e1, c);
			g.quad(a, e, d, b, c);
			g.quad(a, b, b1, a1, c);
			g.quad(b, d, d1, b1, c);
			g.quad(d, e, e1, d1, c);
			g.quad(e, a, a1, e1, c);
		}
	}

	/** The contour lines of a grid (at their heights or on the floor). */
	public static final class Contour extends GridLayer<Contour> {
		int count = 10;
		double[] levels;
		boolean onFloor;

		Contour(double[] x, double[] y, double[][] z) {
			super(x, y, z);
			colorMap = PlotLegend.VIRIDIS;
		}

		/** The number of the contour levels (evenly distributed, default 10). */
		public Contour levels(int count) {
			this.count = Math.max(1, count);
			this.levels = null;
			return this;
		}

		/** The values of the contour levels. */
		public Contour levels(double[] levels) {
			this.levels = levels.clone();
			return this;
		}

		/** Draws the lines on the floor (e.g., below a surface) instead of their heights. */
		public Contour onFloor(boolean onFloor) {
			this.onFloor = onFloor;
			return this;
		}

		@Override
		void build(PlotGeometry g, Plot3D plot, int index) {
			int nx = x.length, ny = y.length;
			/* The transformed values of the grid */
			double[][] t = new double[nx][ny];
			double min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
			for (int i = 0; i < nx; i++) {
				for (int j = 0; j < ny; j++) {
					t[i][j] = plot.value(2, z[i][j]);
					if (Double.isFinite(t[i][j])) {
						min = Math.min(min, t[i][j]);
						max = Math.max(max, t[i][j]);
					}
				}
			}
			if (!(max > min)) {
				return;
			}
			double[] lv;
			if (levels != null) {
				lv = Arrays.stream(levels).map(v -> plot.value(2, v)).filter(Double::isFinite).toArray();
			} else {
				lv = new double[count];
				for (int k = 0; k < count; k++) {
					lv[k] = min + (max - min) * (k + 0.5) / count;
				}
			}
			double[] bxs = new double[nx], bys = new double[ny];
			for (int i = 0; i < nx; i++) {
				bxs[i] = plot.box(0, plot.value(0, x[i]));
			}
			for (int j = 0; j < ny; j++) {
				bys[j] = plot.box(1, plot.value(1, y[j]));
			}
			for (double level : lv) {
				float[] c = color(plot, index, level);
				double height = onFloor ? plot.floor() + 0.01 : plot.box(2, level);
				for (int i = 0; i < nx - 1; i++) {
					for (int j = 0; j < ny - 1; j++) {
						double v00 = t[i][j], v10 = t[i + 1][j], v11 = t[i + 1][j + 1], v01 = t[i][j + 1];
						if (!Double.isFinite(v00) || !Double.isFinite(v10) || !Double.isFinite(v11) || !Double.isFinite(v01)) {
							continue;
						}
						/* The crossings of the edges: bottom, right, top, left */
						double[][] e = new double[4][];
						e[0] = crossing(bxs[i], bys[j], v00, bxs[i + 1], bys[j], v10, level, height);
						e[1] = crossing(bxs[i + 1], bys[j], v10, bxs[i + 1], bys[j + 1], v11, level, height);
						e[2] = crossing(bxs[i + 1], bys[j + 1], v11, bxs[i], bys[j + 1], v01, level, height);
						e[3] = crossing(bxs[i], bys[j + 1], v01, bxs[i], bys[j], v00, level, height);
						List<double[]> found = new ArrayList<>();
						for (double[] p : e) {
							if (p != null) {
								found.add(p);
							}
						}
						if (found.size() == 2) {
							g.line(found.get(0), found.get(1), c);
						} else if (found.size() == 4) {
							/* A saddle: the center decides which corners are connected */
							double center = (v00 + v10 + v11 + v01) / 4;
							if ((v00 > level) == (center > level)) {
								g.line(e[0], e[1], c);
								g.line(e[2], e[3], c);
							} else {
								g.line(e[0], e[3], c);
								g.line(e[1], e[2], c);
							}
						}
					}
				}
			}
		}

		/* The position of the level on an edge or null */
		private static double[] crossing(double xa, double ya, double va, double xb, double yb, double vb, double level, double height) {
			if ((va > level) == (vb > level)) {
				return null;
			}
			double f = (level - va) / (vb - va);
			return new double[] { xa + f * (xb - xa), ya + f * (yb - ya), height };
		}
	}

	/** A 3D pie chart (without axes, the labels in the legend and the percentages at the slices). */
	public static final class Pie extends Layer<Pie> {
		final double[] values;
		final String[] labels;
		int[] colors;
		double explode = 0;
		double height = 1.2;
		boolean percentages = true;

		Pie(double[] values, String[] labels) {
			if (values == null || values.length == 0) {
				throw new IllegalArgumentException("The pie chart needs values");
			}
			for (double v : values) {
				if (!(v >= 0)) {
					throw new IllegalArgumentException("The values of a pie chart must be >= 0");
				}
			}
			this.values = values.clone();
			this.labels = labels != null ? labels.clone() : null;
		}

		/** The colors of the slices (0xRRGGBB, default the colors of the categories). */
		public Pie colors(int[] colors) {
			this.colors = colors.clone();
			return this;
		}

		/** The distance of the slices from the center (0 - 1, default 0). */
		public Pie explode(double explode) {
			this.explode = Math.max(0, Math.min(1, explode));
			return this;
		}

		/** The height of the pie (the radius is 4, default 1.2). */
		public Pie height(double height) {
			this.height = Math.max(0.05, height);
			return this;
		}

		/** Displays the percentages at the slices (default true). */
		public Pie percentages(boolean percentages) {
			this.percentages = percentages;
			return this;
		}

		@Override
		boolean hasAxes() {
			return false;
		}

		@Override
		void legend(Plot3D plot, int index, List<String> labels, List<Integer> colors) {
			for (int s = 0; s < values.length; s++) {
				labels.add(this.labels != null && s < this.labels.length ? this.labels[s] : "Slice " + (s + 1));
				colors.add(sliceColor(s));
			}
		}

		int sliceColor(int s) {
			if (colors != null && s < colors.length) {
				return colors[s];
			}
			return CATEGORY_COLORS[s % CATEGORY_COLORS.length];
		}

		@Override
		void extent(Plot3D plot, double[][] extent) {
		}

		@Override
		void build(PlotGeometry g, Plot3D plot, int index) {
			double total = Arrays.stream(values).sum();
			if (total <= 0) {
				return;
			}
			double cx = 5, cy = 5, radius = 4;
			double start = Math.PI / 2;
			for (int s = 0; s < values.length; s++) {
				double angle = values[s] / total * 2 * Math.PI;
				if (angle <= 0) {
					continue;
				}
				double mid = start - angle / 2;
				double ox = cx + Math.cos(mid) * explode * radius * 0.25, oy = cy + Math.sin(mid) * explode * radius * 0.25;
				float[] c = PlotGeometry.rgba(sliceColor(s), alpha);
				int steps = Math.max(2, (int) Math.ceil(angle / (2 * Math.PI) * 96));
				double[] top = { ox, oy, height }, bottom = { ox, oy, 0 };
				for (int k = 0; k < steps; k++) {
					double a0 = start - angle * k / steps, a1 = start - angle * (k + 1) / steps;
					double[] p0 = { ox + radius * Math.cos(a0), oy + radius * Math.sin(a0), 0 };
					double[] p1 = { ox + radius * Math.cos(a1), oy + radius * Math.sin(a1), 0 };
					double[] q0 = { p0[0], p0[1], height }, q1 = { p1[0], p1[1], height };
					g.triangle(top, q1, q0, new double[] { 0, 0, 1 }, c);
					g.triangle(bottom, p0, p1, new double[] { 0, 0, -1 }, c);
					/* The outer wall with the normals of the circle */
					double[] n0 = { Math.cos(a0), Math.sin(a0), 0 }, n1 = { Math.cos(a1), Math.sin(a1), 0 };
					g.triangle(p0, n0, c, q1, n1, c, p1, n1, c);
					g.triangle(p0, n0, c, q0, n0, c, q1, n1, c);
				}
				/* The side walls of the slice */
				double[] s0 = { ox + radius * Math.cos(start), oy + radius * Math.sin(start), 0 };
				double[] s1 = { ox + radius * Math.cos(start - angle), oy + radius * Math.sin(start - angle), 0 };
				g.quad(bottom, top, new double[] { s0[0], s0[1], height }, s0, c);
				g.quad(bottom, s1, new double[] { s1[0], s1[1], height }, top, c);
				if (percentages) {
					double lr = radius * 0.68;
					g.label(new double[] { ox + Math.cos(mid) * lr, oy + Math.sin(mid) * lr, height + 0.25 },
							String.format(java.util.Locale.ROOT, "%.1f %%", values[s] / total * 100), 1.1f);
				}
				start -= angle;
			}
		}
	}
}
