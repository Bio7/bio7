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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.e4.ui.model.application.ui.MElementContainer;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.model.application.ui.advanced.MPlaceholder;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MPartSashContainerElement;
import org.eclipse.e4.ui.model.application.ui.basic.MPartStack;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.nebula.widgets.grid.Grid;
import org.eclipse.nebula.widgets.grid.GridColumn;
import org.eclipse.nebula.widgets.grid.GridItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.eco.bio7.rbridge.Bio7Grid;
import com.eco.bio7.rbridge.RTable;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLContext;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import ij.process.ImageProcessor;

/**
 * Measurements of the iso surface and the volume in the 3D view. The results
 * are displayed in new sheets of the Bio7 table:
 * <ul>
 * <li>The objects of the iso surface (connected parts of the mesh): volume,
 * surface area, sphericity, centroid, ellipsoid axes, Feret diameter, holes,
 * bounding box.</li>
 * <li>The statistics of the voxels in the region of the clipping (box, plane
 * and mask) and the volume above a threshold.</li>
 * <li>Points picked with the mouse on the displayed data (the position, the
 * value and the distances like a ruler, see Clipping.TOOL_PICK).</li>
 * </ul>
 * The values are calibrated (the unit of the image).
 */
public class Measurement3d {

	private final IsoSurface iso;
	private final VolumeRenderer volume;
	private final Clipping clipping;
	/*
	 * The picked points (relative positions 0 - 1, y = 0 is the first row) and the
	 * position of the value (inside of the surface)
	 */
	private final List<double[]> points = new ArrayList<>();
	/* The highlighted triangles of the points: {mesh, triangles} (null for the volume) */
	private final List<int[][]> pointTriangles = new ArrayList<>();
	private Grid pointGrid;
	private int minObjectSize = 10;
	/* 0 = no maximum */
	private int maxObjectSize = 0;

	/* The measured objects: the mesh, the object number of the vertices and the labels */
	private boolean highlightObjects = true;
	private int[] objectMesh;
	private int[] vertexObject;
	/* The measured surface (full resolution) and the object numbers of its vertices */
	private IsoSurface.MeasureData measuredData;
	private int[] measuredObject;
	/* rx, ry, rz (relative centroid), number */
	private double[][] objectLabels;
	/* The colors of the vertices (not dimmed), the selected object (0 = none) and its table */
	private float[] objectColors;
	private int selectedObject;
	private int objectCount;
	private Grid objectGrid;
	/* The rectangles of the displayed labels (window pixels from the bottom): x0, y0, x1, y1, number */
	private final List<int[]> labelBounds = new ArrayList<>();
	/* The lines between the nearest surface points (relative x, y, z of both points) */
	private List<double[]> distanceLines;
	private int[] distanceMesh;
	private DigitAtlas atlas;
	private GLContext atlasContext;
	private int labelViewportHeight;
	private static final float[] PICK_COLOR = { 1f, 0.15f, 0.55f };
	private static final float[] OUTLINE_COLOR = { 0.15f, 0f, 0.1f };
	private static final float[] NEIGHBOR_COLOR = { 0.95f, 0.95f, 0.95f };

	Measurement3d(IsoSurface iso, VolumeRenderer volume, Clipping clipping) {
		this.iso = iso;
		this.volume = volume;
		this.clipping = clipping;
		clipping.setPickListener(this::pick);
	}

	/** Returns the minimum size of a measured object (voxels of the data). */
	public synchronized int getMinObjectSize() {
		return minObjectSize;
	}

	/**
	 * Sets the minimum size of a measured object (voxels of the data), smaller
	 * objects (e.g., noise) are not measured.
	 */
	public synchronized void setMinObjectSize(int voxels) {
		minObjectSize = Math.max(0, voxels);
	}

	/** Returns the maximum size of a measured object (voxels of the data, 0 = no maximum). */
	public synchronized int getMaxObjectSize() {
		return maxObjectSize;
	}

	/**
	 * Sets the maximum size of a measured object (voxels of the data), larger
	 * objects are not measured (e.g., a background). 0 measures all objects (the
	 * default).
	 */
	public synchronized void setMaxObjectSize(int voxels) {
		maxObjectSize = Math.max(0, voxels);
	}

	/* The calibrated extent and the unit of the data */
	private static class Scale {
		double sizeX, sizeY, sizeZ; // the size of the data
		double voxelVolume; // the volume of a voxel of the source
		String unit;
	}

	/* ---------------------------- Iso surface ---------------------------- */

	/** The columns of the iso surface measurement (see measureIsoSurface). */
	public static final String[] ISO_COLUMNS = { "Volume", "Surface Area", "Sphericity", "Centroid X", "Centroid Y",
			"Centroid Z", "Axis 1", "Axis 2", "Axis 3", "Elongation", "Flatness", "Feret Diameter", "Holes",
			"Mean Inside Value", "Min X", "Min Y", "Min Z", "Max X", "Max Y", "Max Z", "Vertices", "Triangles" };

	/**
	 * Measures the objects (connected parts) of the iso surface mesh, sorted by
	 * the volume. The axes are the lengths of the axes of the ellipsoid with the
	 * same moments of inertia. Holes is the number of tunnels (genus). Mean Inside
	 * Value is the mean of the data one voxel inside of the surface.
	 *
	 * @param showTable true displays the results in a new sheet of the Bio7 table
	 * @return a row for each object with the values of ISO_COLUMNS or null if no
	 *         surface is available
	 */
	public double[][] measureIsoSurface(boolean showTable) {
		/* The full resolution (also if the display is reduced) */
		IsoSurface.MeasureData m = iso.getMeasurementData();
		IsoSurface.MeasureData display = iso.getMeasureData();
		if (m == null || m.vertices == null || m.triangles == null || m.triangles.length == 0 || display == null
				|| display.vertices == null) {
			return null;
		}
		float[] v = m.vertices;
		int[] t = m.triangles;
		int nv = v.length / 3;
		double sx = m.stepX * m.pixelSize, sy = m.stepY * m.pixelSize, sz = m.stepZ * m.pixelSize;
		/* The calibrated positions of the vertices (the center of a voxel) */
		double[] p = new double[nv * 3];
		for (int i = 0; i < nv; i++) {
			p[i * 3] = (v[i * 3] + 0.5) * sx;
			p[i * 3 + 1] = (v[i * 3 + 1] + 0.5) * sy;
			p[i * 3 + 2] = (v[i * 3 + 2] + 0.5) * sz;
		}
		/* The connected parts (union find) */
		int[] parent = new int[nv];
		for (int i = 0; i < nv; i++) {
			parent[i] = i;
		}
		for (int i = 0; i < t.length; i += 3) {
			union(parent, t[i], t[i + 1]);
			union(parent, t[i], t[i + 2]);
		}
		int[] component = new int[nv];
		int[] index = new int[nv];
		java.util.Arrays.fill(index, -1);
		int count = 0;
		for (int i = 0; i < nv; i++) {
			int root = find(parent, i);
			if (index[root] < 0) {
				index[root] = count++;
			}
			component[i] = index[root];
		}
		double[] vol = new double[count], area = new double[count];
		double[][] first = new double[count][3], second = new double[count][6];
		double[][] min = new double[count][3], max = new double[count][3];
		int[] vertices = new int[count], triangles = new int[count];
		double[] valueSum = new double[count];
		for (int c = 0; c < count; c++) {
			java.util.Arrays.fill(min[c], Double.MAX_VALUE);
			java.util.Arrays.fill(max[c], -Double.MAX_VALUE);
		}
		/* The values one voxel inside of the surface (the normals point outside) */
		float[] normals = IsoSurface.computeNormals(v, t);
		for (int i = 0; i < nv; i++) {
			int c = component[i];
			vertices[c]++;
			for (int k = 0; k < 3; k++) {
				min[c][k] = Math.min(min[c][k], p[i * 3 + k]);
				max[c][k] = Math.max(max[c][k], p[i * 3 + k]);
			}
			valueSum[c] += IsoSurface.sample(m.data, m.nx, m.ny, m.nz, v[i * 3] - normals[i * 3],
					v[i * 3 + 1] - normals[i * 3 + 1], v[i * 3 + 2] - normals[i * 3 + 2]);
		}
		/* Volume, moments (signed tetrahedra with the origin) and area of the triangles */
		for (int i = 0; i < t.length; i += 3) {
			int a = t[i] * 3, b = t[i + 1] * 3, d = t[i + 2] * 3;
			int c = component[t[i]];
			triangles[c]++;
			double ax = p[a], ay = p[a + 1], az = p[a + 2];
			double bx = p[b], by = p[b + 1], bz = p[b + 2];
			double cx = p[d], cy = p[d + 1], cz = p[d + 2];
			double tv = (ax * (by * cz - bz * cy) - ay * (bx * cz - bz * cx) + az * (bx * cy - by * cx)) / 6.0;
			vol[c] += tv;
			double sxs = ax + bx + cx, sys = ay + by + cy, szs = az + bz + cz;
			first[c][0] += tv * sxs / 4;
			first[c][1] += tv * sys / 4;
			first[c][2] += tv * szs / 4;
			double f = tv / 20.0;
			second[c][0] += f * (ax * ax + bx * bx + cx * cx + sxs * sxs);
			second[c][1] += f * (ay * ay + by * by + cy * cy + sys * sys);
			second[c][2] += f * (az * az + bz * bz + cz * cz + szs * szs);
			second[c][3] += f * (ax * ay + bx * by + cx * cy + sxs * sys);
			second[c][4] += f * (ax * az + bx * bz + cx * cz + sxs * szs);
			second[c][5] += f * (ay * az + by * bz + cy * cz + sys * szs);
			double ux = bx - ax, uy = by - ay, uz = bz - az, wx = cx - ax, wy = cy - ay, wz = cz - az;
			double nx = uy * wz - uz * wy, ny = uz * wx - ux * wz, nz = ux * wy - uy * wx;
			area[c] += 0.5 * Math.sqrt(nx * nx + ny * ny + nz * nz);
		}
		/* The vertices of the parts (sorted by the part) */
		int[] memberStart = new int[count + 1];
		for (int i = 0; i < nv; i++) {
			memberStart[component[i] + 1]++;
		}
		for (int c = 0; c < count; c++) {
			memberStart[c + 1] += memberStart[c];
		}
		int[] memberList = new int[nv];
		int[] memberFill = memberStart.clone();
		for (int i = 0; i < nv; i++) {
			memberList[memberFill[component[i]]++] = i;
		}
		double minVolume = getMinObjectSize() * sx * sy * sz;
		double maxVolume = getMaxObjectSize() > 0 ? getMaxObjectSize() * sx * sy * sz : Double.MAX_VALUE;
		List<double[]> rows = new ArrayList<>();
		for (int c = 0; c < count; c++) {
			double V = Math.abs(vol[c]);
			if (V < minVolume || V > maxVolume || V == 0) {
				continue;
			}
			double[] centroid = { first[c][0] / vol[c], first[c][1] / vol[c], first[c][2] / vol[c] };
			double[][] cov = new double[3][3];
			cov[0][0] = second[c][0] / vol[c] - centroid[0] * centroid[0];
			cov[1][1] = second[c][1] / vol[c] - centroid[1] * centroid[1];
			cov[2][2] = second[c][2] / vol[c] - centroid[2] * centroid[2];
			cov[0][1] = cov[1][0] = second[c][3] / vol[c] - centroid[0] * centroid[1];
			cov[0][2] = cov[2][0] = second[c][4] / vol[c] - centroid[0] * centroid[2];
			cov[1][2] = cov[2][1] = second[c][5] / vol[c] - centroid[1] * centroid[2];
			double[] eigen = eigenvalues(cov);
			/* A solid ellipsoid with the semi-axis a has the variance a^2 / 5 */
			double[] axes = new double[3];
			for (int k = 0; k < 3; k++) {
				axes[k] = 2 * Math.sqrt(5 * Math.max(0, eigen[k]));
			}
			double sphericity = Math.cbrt(Math.PI) * Math.pow(6 * V, 2.0 / 3.0) / area[c];
			/* Closed surface: Euler characteristic V - E + F with E = 3F / 2 */
			double euler = vertices[c] - triangles[c] / 2.0;
			double holes = Math.max(0, Math.round((2 - euler) / 2));
			/* The last value is the part (removed below) */
			rows.add(new double[] { V, area[c], sphericity, centroid[0], centroid[1], centroid[2], axes[0], axes[1],
					axes[2], axes[1] > 0 ? axes[0] / axes[1] : Double.NaN, axes[2] > 0 ? axes[1] / axes[2] : Double.NaN,
					feret(p, memberList, memberStart[c], memberStart[c + 1]), holes, valueSum[c] / vertices[c], min[c][0], min[c][1], min[c][2],
					max[c][0], max[c][1], max[c][2], vertices[c], triangles[c], c });
		}
		rows.sort((r1, r2) -> Double.compare(r2[0], r1[0]));
		/* The object numbers (1 - n, sorted by the volume), the colors and the labels */
		int[] number = new int[count];
		double[][] labels = new double[rows.size()][];
		double sizeX = m.nx * sx, sizeY = m.ny * sy, sizeZ = m.nz * sz;
		double[][] result = new double[rows.size()][];
		for (int i = 0; i < rows.size(); i++) {
			double[] row = rows.get(i);
			number[(int) row[ISO_COLUMNS.length]] = i + 1;
			labels[i] = new double[] { row[3] / sizeX, row[4] / sizeY, row[5] / sizeZ, i + 1 };
			result[i] = java.util.Arrays.copyOf(row, ISO_COLUMNS.length);
		}
		int[] objectOfVertex = new int[nv];
		for (int i = 0; i < nv; i++) {
			objectOfVertex[i] = number[component[i]];
		}
		/* The objects of the displayed vertices (the nearest measured vertex if the display is reduced) */
		int[] displayObject = display.triangles == t ? objectOfVertex
				: mapToDisplay(m, objectOfVertex, display);
		int nd = displayObject.length;
		float[] colors = new float[nd * 3];
		for (int i = 0; i < nd; i++) {
			int object = displayObject[i];
			int rgb = object > 0 ? objectColor(object) : 0x808080;
			colors[i * 3] = ((rgb >> 16) & 0xff) / 255f;
			colors[i * 3 + 1] = ((rgb >> 8) & 0xff) / 255f;
			colors[i * 3 + 2] = (rgb & 0xff) / 255f;
		}
		synchronized (this) {
			objectMesh = display.triangles;
			vertexObject = displayObject;
			measuredData = m;
			measuredObject = objectOfVertex;
			objectLabels = labels;
			objectColors = colors;
			objectCount = result.length;
			selectedObject = 0;
			if (highlightObjects) {
				iso.setObjectColors(colors, display.triangles);
			}
		}
		if (showTable) {
			String u = m.unit != null ? m.unit : "pixel";
			String[] header = new String[ISO_COLUMNS.length + 1];
			header[0] = "Object";
			String[] units = { u + "^3", u + "^2", "", u, u, u, u, u, u, "", "", u, "", "", u, u, u, u, u, u, "", "" };
			for (int k = 0; k < ISO_COLUMNS.length; k++) {
				header[k + 1] = ISO_COLUMNS[k] + (units[k].isEmpty() ? "" : " [" + units[k] + "]");
			}
			List<String[]> table = new ArrayList<>();
			int[] rowColors = new int[result.length + 1];
			double totalVolume = 0, totalArea = 0;
			for (int i = 0; i < result.length; i++) {
				String[] row = new String[header.length];
				row[0] = String.valueOf(i + 1);
				for (int k = 0; k < result[i].length; k++) {
					row[k + 1] = k == 12 || k >= 20 ? String.valueOf((long) result[i][k]) : format(result[i][k]);
				}
				totalVolume += result[i][0];
				totalArea += result[i][1];
				rowColors[i] = objectColor(i + 1);
				table.add(row);
			}
			String[] total = new String[header.length];
			java.util.Arrays.fill(total, "");
			total[0] = "Total";
			total[1] = format(totalVolume);
			total[2] = format(totalArea);
			table.add(total);
			rowColors[result.length] = -1;
			String title = m.image != null ? m.image.getTitle() : "Model";
			Grid grid = showSheet("Iso Objects " + title, header, table, rowColors);
			if (grid != null) {
				synchronized (this) {
					objectGrid = grid;
				}
				addObjectSelection(grid);
			}
		}
		return result;
	}

	/**
	 * Returns the color (0xRRGGBB) of an object number (the colors of the objects
	 * in the 3D view and in the table).
	 */
	public static int objectColor(int number) {
		/* Golden angle hues (neighboring numbers have different colors) */
		float hue = (float) ((number * 0.618033988749895) % 1.0);
		float saturation = number % 2 == 0 ? 0.65f : 0.85f;
		return java.awt.Color.HSBtoRGB(hue, saturation, 0.95f) & 0xffffff;
	}

	/** Returns true if the measured objects are highlighted (colors and numbers). */
	public synchronized boolean isHighlightObjects() {
		return highlightObjects;
	}

	/**
	 * Highlights the measured objects with colors and numbers (the object numbers
	 * of the table) in the 3D view.
	 */
	public void setHighlightObjects(boolean highlight) {
		synchronized (this) {
			highlightObjects = highlight;
		}
		if (!highlight) {
			iso.setObjectColors(null, null);
		} else {
			/* Measured again for the colors */
			IsoSurface.MeasureData m = iso.getMeasureData();
			if (m != null && m.triangles != null && m.triangles == objectMesh) {
				measureIsoSurface(false);
			}
		}
	}

	/**
	 * Selects a measured object (highlighted in the 3D view, the other objects are
	 * dimmed) and the row of the object in the table.
	 * 
	 * @param number the object number (1 - n, 0 removes the selection)
	 */
	public void selectObject(int number) {
		selectObject(number, true);
	}

	/** Returns the selected object (0 = none). */
	public synchronized int getSelectedObject() {
		return selectedObject;
	}

	private void selectObject(int number, boolean selectRow) {
		float[] colors;
		int[] mesh;
		Grid grid;
		synchronized (this) {
			if (objectColors == null || vertexObject == null || number < 0 || number > objectCount) {
				return;
			}
			selectedObject = number;
			mesh = objectMesh;
			grid = objectGrid;
			colors = objectColors;
			if (number > 0) {
				/* The other objects are dimmed (mixed with gray) */
				colors = objectColors.clone();
				for (int i = 0; i < vertexObject.length; i++) {
					if (vertexObject[i] != number) {
						for (int k = 0; k < 3; k++) {
							colors[i * 3 + k] = 0.3f * colors[i * 3 + k] + 0.35f;
						}
					}
				}
			}
			if (!highlightObjects) {
				return;
			}
		}
		iso.setObjectColors(colors, mesh);
		if (selectRow && grid != null) {
			Display.getDefault().asyncExec(() -> {
				if (grid.isDisposed()) {
					return;
				}
				if (number == 0) {
					grid.deselectAll();
					return;
				}
				int row = number - 1;
				org.eclipse.swt.graphics.Point[] cells = new org.eclipse.swt.graphics.Point[grid.getColumnCount()];
				for (int c = 0; c < cells.length; c++) {
					cells[c] = new org.eclipse.swt.graphics.Point(c, row);
				}
				grid.setCellSelection(cells);
				grid.showItem(grid.getItem(row));
				/* The sheet of the objects in front */
				CTabFolder folder = RTable.getTabFolder();
				if (folder != null && !folder.isDisposed()) {
					for (org.eclipse.swt.custom.CTabItem item : folder.getItems()) {
						if (item.getControl() == grid.getParent()) {
							folder.setSelection(item);
							RTable.setGrid(grid);
						}
					}
				}
			});
		}
	}

	/**
	 * Selects the object of the number label at the mouse position (in the 3D
	 * view, pixels from the top left). A click on the selected label removes the
	 * selection.
	 * 
	 * @return true if a label has been hit
	 */
	public boolean selectLabelAt(double x, double y) {
		int number;
		synchronized (this) {
			number = getLabelAt(x, y);
			if (number == 0) {
				return false;
			}
			if (number == selectedObject) {
				number = 0;
			}
		}
		selectObject(number, true);
		return true;
	}

	/**
	 * Returns the object number of the displayed label at the mouse position (in
	 * the 3D view, pixels from the top left) or 0.
	 */
	public synchronized int getLabelAt(double x, double y) {
		if (!highlightObjects || labelBounds.isEmpty() || labelViewportHeight <= 0) {
			return 0;
		}
		double wy = labelViewportHeight - y;
		/* The label with the nearest center (neighboring labels can touch) */
		int number = 0;
		double best = Double.MAX_VALUE;
		for (int[] r : labelBounds) {
			if (x >= r[0] && x <= r[2] && wy >= r[1] && wy <= r[3]) {
				double dx = x - (r[0] + r[2]) / 2.0, dy = wy - (r[1] + r[3]) / 2.0;
				if (dx * dx + dy * dy < best) {
					best = dx * dx + dy * dy;
					number = r[4];
				}
			}
		}
		return number;
	}

	/*
	 * The object numbers of the vertices of the reduced display: the object of
	 * the nearest vertex of the measured surface (full resolution)!
	 */
	private static int[] mapToDisplay(IsoSurface.MeasureData m, int[] objectOfVertex, IsoSurface.MeasureData display) {
		float[] v = m.vertices;
		int nv = v.length / 3;
		double[] p = new double[nv * 3];
		int[] all = new int[nv];
		for (int i = 0; i < nv; i++) {
			/* Pixel coordinates of the voxel centers */
			p[i * 3] = (v[i * 3] + 0.5) * m.stepX;
			p[i * 3 + 1] = (v[i * 3 + 1] + 0.5) * m.stepY;
			p[i * 3 + 2] = (v[i * 3 + 2] + 0.5) * m.stepZ;
			all[i] = i;
		}
		KdTree tree = new KdTree(p, all);
		float[] dv = display.vertices;
		int nd = dv.length / 3;
		int[] result = new int[nd];
		double[] best = new double[2];
		for (int i = 0; i < nd; i++) {
			best[0] = Double.MAX_VALUE;
			best[1] = -1;
			tree.nearest((dv[i * 3] + 0.5) * display.stepX, (dv[i * 3 + 1] + 0.5) * display.stepY,
					(dv[i * 3 + 2] + 0.5) * display.stepZ, best);
			result[i] = best[1] >= 0 ? objectOfVertex[(int) best[1]] : 0;
		}
		return result;
	}

	/* A selected row (row i = object i + 1) highlights the object in the 3D view */
	private void addObjectSelection(Grid grid) {
		int[] mesh;
		synchronized (this) {
			mesh = objectMesh;
		}
		Display.getDefault().syncExec(() -> grid.addListener(SWT.Selection, _ -> {
			org.eclipse.swt.graphics.Point[] cells = grid.getCellSelection();
			if (cells.length > 0 && mesh == objectMesh) {
				int row = cells[0].y;
				selectObject(row < objectCount ? row + 1 : 0, false);
			}
		}));
	}

	/* ----------------------------- Distances ----------------------------- */

	/** The columns of the nearest neighbor distances (see measureDistances). */
	public static final String[] DISTANCE_COLUMNS = { "Nearest (Centroid)", "Centroid Distance", "Nearest (Surface)",
			"Surface Distance", "Mean Centroid Distance" };

	/* The matrices are displayed for up to this number of objects */
	private static final int MAX_CENTROID_MATRIX = 500;
	private static final int MAX_SURFACE_MATRIX = 150;

	/**
	 * Measures the distances between the objects of the iso surface (the objects
	 * are measured again, see measureIsoSurface): for every object the nearest
	 * neighbor of the centroids and of the surfaces (edge to edge: the shortest
	 * distance between the triangles), the matrices of all centroid and surface
	 * distances and the Clark-Evans index R of the centroids (3D, R < 1 clustered,
	 * R = 1 random, R > 1 regular, without an edge correction). The nearest
	 * surface points are connected with lines in the 3D view.
	 * 
	 * @param showTable true displays the results in new sheets of the Bio7 table
	 * @return {nearest[n][DISTANCE_COLUMNS], centroid matrix[n][n] (null for more
	 *         than 500 objects), surface matrix[n][n] (null for more than 150
	 *         objects)} or null if no surface is available
	 */
	public double[][][] measureDistances(boolean showTable) {
		double[][] objects = measureIsoSurface(false);
		IsoSurface.MeasureData m;
		int[] objectOfVertex;
		int[] displayMesh;
		synchronized (this) {
			m = measuredData;
			objectOfVertex = measuredObject;
			displayMesh = objectMesh;
		}
		if (objects == null || m == null || objectOfVertex == null) {
			return null;
		}
		int n = objects.length;
		float[] v = m.vertices;
		int[] t = m.triangles;
		int nv = v.length / 3;
		double sx = m.stepX * m.pixelSize, sy = m.stepY * m.pixelSize, sz = m.stepZ * m.pixelSize;
		double[] p = new double[nv * 3];
		for (int i = 0; i < nv; i++) {
			p[i * 3] = (v[i * 3] + 0.5) * sx;
			p[i * 3 + 1] = (v[i * 3 + 1] + 0.5) * sy;
			p[i * 3 + 2] = (v[i * 3 + 2] + 0.5) * sz;
		}
		/* The vertices, bounding boxes and k-d trees of the objects */
		int[][] members = new int[n][];
		int[] counts = new int[n];
		for (int i = 0; i < nv; i++) {
			if (objectOfVertex[i] > 0) {
				counts[objectOfVertex[i] - 1]++;
			}
		}
		for (int o = 0; o < n; o++) {
			members[o] = new int[counts[o]];
			counts[o] = 0;
		}
		for (int i = 0; i < nv; i++) {
			int o = objectOfVertex[i] - 1;
			if (o >= 0) {
				members[o][counts[o]++] = i;
			}
		}
		double[][] box = new double[n][];
		KdTree[] trees = new KdTree[n];
		for (int o = 0; o < n; o++) {
			box[o] = bounds(p, members[o]);
			trees[o] = new KdTree(p, members[o]);
		}
		/* The triangles of the vertices (for the edge to edge distance) */
		int[] start = new int[nv + 1];
		for (int k : t) {
			start[k + 1]++;
		}
		for (int i = 0; i < nv; i++) {
			start[i + 1] += start[i];
		}
		int[] fill = start.clone();
		int[] incident = new int[t.length];
		for (int i = 0; i < t.length; i++) {
			incident[fill[t[i]]++] = i / 3;
		}
		/* The centroid distances: the nearest neighbor and the mean (the matrix for up to 500 objects) */
		double[][] centroid = n <= MAX_CENTROID_MATRIX ? new double[n][n] : null;
		int[] nearestCentroid = new int[n];
		double[] nearestCentroidDistance = new double[n];
		double[] meanCentroidDistance = new double[n];
		java.util.Arrays.fill(nearestCentroid, -1);
		java.util.Arrays.fill(nearestCentroidDistance, Double.MAX_VALUE);
		for (int a = 0; a < n; a++) {
			for (int b = a + 1; b < n; b++) {
				double dx = objects[a][3] - objects[b][3], dy = objects[a][4] - objects[b][4],
						dz = objects[a][5] - objects[b][5];
				double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
				if (centroid != null) {
					centroid[a][b] = centroid[b][a] = d;
				}
				meanCentroidDistance[a] += d;
				meanCentroidDistance[b] += d;
				if (d < nearestCentroidDistance[a]) {
					nearestCentroidDistance[a] = d;
					nearestCentroid[a] = b;
				}
				if (d < nearestCentroidDistance[b]) {
					nearestCentroidDistance[b] = d;
					nearestCentroid[b] = a;
				}
			}
		}
		/* The surface distances: all pairs for the matrix, else only the nearest neighbors */
		boolean allPairs = n <= MAX_SURFACE_MATRIX;
		double[][] surface = allPairs ? new double[n][n] : null;
		double[][][] closest = new double[n][][];
		/* The nearest points of the pairs (all pairs) */
		double[][][][] pairPoints = allPairs ? new double[n][n][][] : null;
		int[] nearestSurface = new int[n];
		double[] nearestSurfaceDistance = new double[n];
		java.util.Arrays.fill(nearestSurfaceDistance, Double.MAX_VALUE);
		java.util.Arrays.fill(nearestSurface, -1);
		if (allPairs) {
			for (double[] row : surface) {
				java.util.Arrays.fill(row, Double.NaN);
			}
			for (int a = 0; a < n; a++) {
				surface[a][a] = 0;
				for (int b = a + 1; b < n; b++) {
					double[][] points = new double[2][];
					double d = surfaceDistance(p, t, start, incident, members[a], trees[b], box[b], Double.MAX_VALUE,
							points);
					if (Double.isNaN(d)) {
						continue;
					}
					surface[a][b] = surface[b][a] = d;
					pairPoints[a][b] = points;
					pairPoints[b][a] = new double[][] { points[1], points[0] };
				}
			}
			for (int a = 0; a < n; a++) {
				for (int b = 0; b < n; b++) {
					if (b != a && !Double.isNaN(surface[a][b]) && surface[a][b] < nearestSurfaceDistance[a]) {
						nearestSurfaceDistance[a] = surface[a][b];
						nearestSurface[a] = b;
						closest[a] = pairPoints[a][b];
					}
				}
			}
		} else {
			/*
			 * Many objects: the distance to the nearest centroid neighbor as the start,
			 * then only the objects with a nearer bounding box
			 */
			for (int a = 0; a < n; a++) {
				int seed = nearestCentroid[a];
				if (seed < 0) {
					continue;
				}
				double[][] points = new double[2][];
				double d = surfaceDistance(p, t, start, incident, members[a], trees[seed], box[seed],
						Double.MAX_VALUE, points);
				if (!Double.isNaN(d)) {
					nearestSurfaceDistance[a] = d;
					nearestSurface[a] = seed;
					closest[a] = points;
				}
				for (int b = 0; b < n; b++) {
					if (b == a || b == seed || boxDistance(box[a], box[b]) >= nearestSurfaceDistance[a]) {
						continue;
					}
					points = new double[2][];
					d = surfaceDistance(p, t, start, incident, members[a], trees[b], box[b], nearestSurfaceDistance[a],
							points);
					if (!Double.isNaN(d) && d < nearestSurfaceDistance[a]) {
						nearestSurfaceDistance[a] = d;
						nearestSurface[a] = b;
						closest[a] = points;
					}
				}
			}
		}
		/* The nearest neighbors */
		double[][] nearest = new double[n][];
		double sumNearest = 0;
		for (int a = 0; a < n; a++) {
			if (nearestCentroid[a] >= 0) {
				sumNearest += nearestCentroidDistance[a];
			}
			nearest[a] = new double[] { nearestCentroid[a] + 1,
					nearestCentroid[a] >= 0 ? nearestCentroidDistance[a] : Double.NaN, nearestSurface[a] + 1,
					nearestSurface[a] >= 0 ? nearestSurfaceDistance[a] : Double.NaN,
					n > 1 ? meanCentroidDistance[a] / (n - 1) : Double.NaN };
		}
		/* The Clark-Evans index (3D Poisson: mean nearest distance 0.554 * density^(-1/3)) */
		double volumeOfData = m.nx * sx * m.ny * sy * m.nz * sz;
		double clarkEvans = n > 1 ? (sumNearest / n) / (0.55396 * Math.cbrt(volumeOfData / n)) : Double.NaN;
		/* The lines between the nearest surface points (relative positions) */
		double sizeX = m.nx * sx, sizeY = m.ny * sy, sizeZ = m.nz * sz;
		List<double[]> lines = new ArrayList<>();
		for (int a = 0; a < n; a++) {
			if (closest[a] != null && closest[a][0] != null && closest[a][1] != null) {
				double[] c0 = closest[a][0], c1 = closest[a][1];
				lines.add(new double[] { c0[0] / sizeX, c0[1] / sizeY, c0[2] / sizeZ, c1[0] / sizeX, c1[1] / sizeY,
						c1[2] / sizeZ });
			}
		}
		synchronized (this) {
			distanceLines = lines;
			distanceMesh = displayMesh;
		}
		if (showTable) {
			showDistanceTables(m, nearest, centroid, surface, clarkEvans);
		}
		return new double[][][] { nearest, centroid, surface };
	}

	private void showDistanceTables(IsoSurface.MeasureData m, double[][] nearest, double[][] centroid,
			double[][] surface, double clarkEvans) {
		int n = nearest.length;
		String u = m.unit != null ? m.unit : "pixel";
		String title = m.image != null ? m.image.getTitle() : "Model";
		String[] header = new String[DISTANCE_COLUMNS.length + 1];
		header[0] = "Object";
		for (int k = 0; k < DISTANCE_COLUMNS.length; k++) {
			header[k + 1] = DISTANCE_COLUMNS[k] + (k == 0 || k == 2 ? "" : " [" + u + "]");
		}
		List<String[]> table = new ArrayList<>();
		int[] colors = new int[n + 4];
		double[] sum = new double[DISTANCE_COLUMNS.length], sum2 = new double[DISTANCE_COLUMNS.length];
		for (int a = 0; a < n; a++) {
			String[] row = new String[header.length];
			row[0] = String.valueOf(a + 1);
			for (int k = 0; k < DISTANCE_COLUMNS.length; k++) {
				double value = nearest[a][k];
				row[k + 1] = k == 0 || k == 2 ? (value > 0 ? String.valueOf((int) value) : "") : format(value);
				if (!Double.isNaN(value)) {
					sum[k] += value;
					sum2[k] += value * value;
				}
			}
			colors[a] = objectColor(a + 1);
			table.add(row);
		}
		String[] mean = new String[header.length], sd = new String[header.length], ce = new String[header.length];
		java.util.Arrays.fill(mean, "");
		java.util.Arrays.fill(sd, "");
		java.util.Arrays.fill(ce, "");
		mean[0] = "Mean";
		sd[0] = "SD";
		for (int k : new int[] { 1, 3, 4 }) {
			mean[k + 1] = n > 0 ? format(sum[k] / n) : "";
			sd[k + 1] = n > 1 ? format(Math.sqrt(Math.max(0, (sum2[k] - sum[k] * sum[k] / n) / (n - 1)))) : "";
		}
		ce[0] = "Clark-Evans R";
		ce[2] = format(clarkEvans);
		table.add(mean);
		table.add(sd);
		table.add(ce);
		for (int i = n; i < colors.length; i++) {
			colors[i] = -1;
		}
		Grid grid = showSheet("Distances " + title, header, table, colors);
		if (grid != null) {
			addObjectSelection(grid);
		}
		if (centroid != null) {
			Grid g = showSheet("Centroid Distances " + title, matrixHeader(n), matrixRows(centroid), objectColors(n));
			if (g != null) {
				addObjectSelection(g);
			}
		}
		if (surface != null) {
			Grid g = showSheet("Surface Distances " + title, matrixHeader(n), matrixRows(surface), objectColors(n));
			if (g != null) {
				addObjectSelection(g);
			}
		}
		/* The nearest neighbor sheet in front */
		if (grid != null) {
			Display.getDefault().asyncExec(() -> {
				CTabFolder folder = RTable.getTabFolder();
				if (folder != null && !folder.isDisposed() && !grid.isDisposed()) {
					for (org.eclipse.swt.custom.CTabItem item : folder.getItems()) {
						if (item.getControl() == grid.getParent()) {
							folder.setSelection(item);
							RTable.setGrid(grid);
						}
					}
				}
			});
		}
	}

	private static String[] matrixHeader(int n) {
		String[] header = new String[n + 1];
		header[0] = "Object";
		for (int i = 0; i < n; i++) {
			header[i + 1] = String.valueOf(i + 1);
		}
		return header;
	}

	private static List<String[]> matrixRows(double[][] matrix) {
		List<String[]> rows = new ArrayList<>();
		for (int a = 0; a < matrix.length; a++) {
			String[] row = new String[matrix.length + 1];
			row[0] = String.valueOf(a + 1);
			for (int b = 0; b < matrix.length; b++) {
				row[b + 1] = format(matrix[a][b]);
			}
			rows.add(row);
		}
		return rows;
	}

	private static int[] objectColors(int n) {
		int[] colors = new int[n];
		for (int i = 0; i < n; i++) {
			colors[i] = objectColor(i + 1);
		}
		return colors;
	}

	/*
	 * The shortest distance between the surface of object a (the vertices) and the
	 * surface of object b (the k-d tree of the vertices): the nearest vertices,
	 * refined with the triangles around them. NaN if not closer than the limit!
	 */
	private static double surfaceDistance(double[] p, int[] t, int[] start, int[] incident, int[] verticesA,
			KdTree treeB, double[] boxB, double limit, double[][] points) {
		double[] best = { limit == Double.MAX_VALUE ? Double.MAX_VALUE : limit * limit, -1 };
		int bestA = -1;
		for (int i : verticesA) {
			double x = p[i * 3], y = p[i * 3 + 1], z = p[i * 3 + 2];
			double dx = Math.max(0, Math.max(boxB[0] - x, x - boxB[3]));
			double dy = Math.max(0, Math.max(boxB[1] - y, y - boxB[4]));
			double dz = Math.max(0, Math.max(boxB[2] - z, z - boxB[5]));
			if (dx * dx + dy * dy + dz * dz >= best[0]) {
				continue;
			}
			int before = (int) best[1];
			treeB.nearest(x, y, z, best);
			if ((int) best[1] != before) {
				bestA = i;
			}
		}
		if (bestA < 0 || best[1] < 0) {
			return Double.NaN;
		}
		int bestB = (int) best[1];
		/* Refined: the vertex of a to the triangles of b and the vertex of b to the triangles of a */
		double distance = Math.sqrt(best[0]);
		double[] pa = { p[bestA * 3], p[bestA * 3 + 1], p[bestA * 3 + 2] };
		double[] pb = { p[bestB * 3], p[bestB * 3 + 1], p[bestB * 3 + 2] };
		points[0] = pa;
		points[1] = pb;
		for (int side = 0; side < 2; side++) {
			double[] q = side == 0 ? pa : pb;
			int vertex = side == 0 ? bestB : bestA;
			for (int k = start[vertex]; k < start[vertex + 1]; k++) {
				int tri = incident[k] * 3;
				double[] c = closestPointOnTriangle(q, p, t[tri], t[tri + 1], t[tri + 2]);
				double d = Math.sqrt((c[0] - q[0]) * (c[0] - q[0]) + (c[1] - q[1]) * (c[1] - q[1])
						+ (c[2] - q[2]) * (c[2] - q[2]));
				if (d < distance) {
					distance = d;
					points[0] = side == 0 ? pa : c;
					points[1] = side == 0 ? c : pb;
				}
			}
		}
		return distance;
	}

	/* The closest point on the triangle (a, b, c) to q (Ericson, Real-Time Collision Detection) */
	private static double[] closestPointOnTriangle(double[] q, double[] p, int ia, int ib, int ic) {
		double ax = p[ia * 3], ay = p[ia * 3 + 1], az = p[ia * 3 + 2];
		double abx = p[ib * 3] - ax, aby = p[ib * 3 + 1] - ay, abz = p[ib * 3 + 2] - az;
		double acx = p[ic * 3] - ax, acy = p[ic * 3 + 1] - ay, acz = p[ic * 3 + 2] - az;
		double apx = q[0] - ax, apy = q[1] - ay, apz = q[2] - az;
		double d1 = abx * apx + aby * apy + abz * apz, d2 = acx * apx + acy * apy + acz * apz;
		if (d1 <= 0 && d2 <= 0) {
			return new double[] { ax, ay, az };
		}
		double bpx = q[0] - p[ib * 3], bpy = q[1] - p[ib * 3 + 1], bpz = q[2] - p[ib * 3 + 2];
		double d3 = abx * bpx + aby * bpy + abz * bpz, d4 = acx * bpx + acy * bpy + acz * bpz;
		if (d3 >= 0 && d4 <= d3) {
			return new double[] { p[ib * 3], p[ib * 3 + 1], p[ib * 3 + 2] };
		}
		double vc = d1 * d4 - d3 * d2;
		if (vc <= 0 && d1 >= 0 && d3 <= 0) {
			double w = d1 / (d1 - d3);
			return new double[] { ax + w * abx, ay + w * aby, az + w * abz };
		}
		double cpx = q[0] - p[ic * 3], cpy = q[1] - p[ic * 3 + 1], cpz = q[2] - p[ic * 3 + 2];
		double d5 = abx * cpx + aby * cpy + abz * cpz, d6 = acx * cpx + acy * cpy + acz * cpz;
		if (d6 >= 0 && d5 <= d6) {
			return new double[] { p[ic * 3], p[ic * 3 + 1], p[ic * 3 + 2] };
		}
		double vb = d5 * d2 - d1 * d6;
		if (vb <= 0 && d2 >= 0 && d6 <= 0) {
			double w = d2 / (d2 - d6);
			return new double[] { ax + w * acx, ay + w * acy, az + w * acz };
		}
		double va = d3 * d6 - d5 * d4;
		if (va <= 0 && (d4 - d3) >= 0 && (d5 - d6) >= 0) {
			double w = (d4 - d3) / ((d4 - d3) + (d5 - d6));
			double bx = p[ib * 3], by = p[ib * 3 + 1], bz = p[ib * 3 + 2];
			return new double[] { bx + w * (p[ic * 3] - bx), by + w * (p[ic * 3 + 1] - by),
					bz + w * (p[ic * 3 + 2] - bz) };
		}
		double denom = 1 / (va + vb + vc);
		double v = vb * denom, w = vc * denom;
		return new double[] { ax + abx * v + acx * w, ay + aby * v + acy * w, az + abz * v + acz * w };
	}

	/* The bounding box (min x, y, z, max x, y, z) of vertices */
	private static double[] bounds(double[] p, int[] vertices) {
		double[] b = { Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE,
				-Double.MAX_VALUE };
		for (int i : vertices) {
			for (int k = 0; k < 3; k++) {
				b[k] = Math.min(b[k], p[i * 3 + k]);
				b[k + 3] = Math.max(b[k + 3], p[i * 3 + k]);
			}
		}
		return b;
	}

	/* The distance between two bounding boxes (0 if they overlap) */
	private static double boxDistance(double[] a, double[] b) {
		double sum = 0;
		for (int k = 0; k < 3; k++) {
			double gap = Math.max(0, Math.max(a[k] - b[k + 3], b[k] - a[k + 3]));
			sum += gap * gap;
		}
		return Math.sqrt(sum);
	}

	/* A k-d tree of vertices (the nearest vertex of a point) */
	private static final class KdTree {
		private final double[] p;
		private final int[] index;

		KdTree(double[] p, int[] vertices) {
			this.p = p;
			this.index = vertices.clone();
			build(0, index.length, 0);
		}

		private void build(int lo, int hi, int depth) {
			if (hi - lo <= 1) {
				return;
			}
			int axis = depth % 3, mid = (lo + hi) >>> 1;
			select(lo, hi - 1, mid, axis);
			build(lo, mid, depth + 1);
			build(mid + 1, hi, depth + 1);
		}

		/* Quickselect: the element mid at its sorted position (by the axis) */
		private void select(int lo, int hi, int k, int axis) {
			while (hi > lo) {
				double pivot = p[index[(lo + hi) >>> 1] * 3 + axis];
				int i = lo, j = hi;
				while (i <= j) {
					while (p[index[i] * 3 + axis] < pivot) {
						i++;
					}
					while (p[index[j] * 3 + axis] > pivot) {
						j--;
					}
					if (i <= j) {
						int tmp = index[i];
						index[i] = index[j];
						index[j] = tmp;
						i++;
						j--;
					}
				}
				if (k <= j) {
					hi = j;
				} else if (k >= i) {
					lo = i;
				} else {
					return;
				}
			}
		}

		/* best = {squared distance, vertex}: updated if a nearer vertex is found */
		void nearest(double x, double y, double z, double[] best) {
			search(0, index.length, 0, x, y, z, best);
		}

		private void search(int lo, int hi, int depth, double x, double y, double z, double[] best) {
			if (lo >= hi) {
				return;
			}
			int mid = (lo + hi) >>> 1, vertex = index[mid];
			double dx = p[vertex * 3] - x, dy = p[vertex * 3 + 1] - y, dz = p[vertex * 3 + 2] - z;
			double d2 = dx * dx + dy * dy + dz * dz;
			if (d2 < best[0]) {
				best[0] = d2;
				best[1] = vertex;
			}
			int axis = depth % 3;
			double diff = (axis == 0 ? x : axis == 1 ? y : z) - p[vertex * 3 + axis];
			if (diff < 0) {
				search(lo, mid, depth + 1, x, y, z, best);
				if (diff * diff < best[0]) {
					search(mid + 1, hi, depth + 1, x, y, z, best);
				}
			} else {
				search(mid + 1, hi, depth + 1, x, y, z, best);
				if (diff * diff < best[0]) {
					search(lo, mid, depth + 1, x, y, z, best);
				}
			}
		}
	}

	/**
	 * Clears the measurements in the 3D view: the colors, numbers and selection of
	 * the objects and the picked points (the sheets of the table are kept).
	 */
	public void clear() {
		clearObjects();
		clearPoints();
	}

	/** Removes the highlight of the measured objects. */
	public void clearObjects() {
		synchronized (this) {
			objectMesh = null;
			vertexObject = null;
			measuredData = null;
			measuredObject = null;
			objectLabels = null;
			objectColors = null;
			selectedObject = 0;
			labelBounds.clear();
			distanceLines = null;
			distanceMesh = null;
		}
		iso.setObjectColors(null, null);
	}

	/* The largest distance between the vertices of a part (members from - to, at most 3000 vertices) */
	private static double feret(double[] p, int[] members, int from, int to) {
		int size = to - from;
		int stride = Math.max(1, size / 3000);
		double max = 0;
		for (int i = from; i < to; i += stride) {
			int a = members[i] * 3;
			for (int j = i + stride; j < to; j += stride) {
				int b = members[j] * 3;
				double dx = p[a] - p[b], dy = p[a + 1] - p[b + 1], dz = p[a + 2] - p[b + 2];
				max = Math.max(max, dx * dx + dy * dy + dz * dz);
			}
		}
		return Math.sqrt(max);
	}

	private static int find(int[] parent, int i) {
		while (parent[i] != i) {
			parent[i] = parent[parent[i]];
			i = parent[i];
		}
		return i;
	}

	private static void union(int[] parent, int a, int b) {
		int ra = find(parent, a), rb = find(parent, b);
		if (ra != rb) {
			parent[ra] = rb;
		}
	}

	/* The eigenvalues of a symmetric 3 x 3 matrix (Jacobi), sorted descending */
	private static double[] eigenvalues(double[][] matrix) {
		double[][] a = { matrix[0].clone(), matrix[1].clone(), matrix[2].clone() };
		for (int sweep = 0; sweep < 50; sweep++) {
			double off = a[0][1] * a[0][1] + a[0][2] * a[0][2] + a[1][2] * a[1][2];
			if (off < 1e-24) {
				break;
			}
			for (int pi = 0; pi < 2; pi++) {
				for (int qi = pi + 1; qi < 3; qi++) {
					if (Math.abs(a[pi][qi]) < 1e-30) {
						continue;
					}
					double theta = (a[qi][qi] - a[pi][pi]) / (2 * a[pi][qi]);
					double tt = Math.signum(theta) / (Math.abs(theta) + Math.sqrt(theta * theta + 1));
					if (theta == 0) {
						tt = 1;
					}
					double cs = 1 / Math.sqrt(tt * tt + 1), sn = tt * cs;
					for (int k = 0; k < 3; k++) {
						double akp = a[k][pi], akq = a[k][qi];
						a[k][pi] = cs * akp - sn * akq;
						a[k][qi] = sn * akp + cs * akq;
					}
					for (int k = 0; k < 3; k++) {
						double apk = a[pi][k], aqk = a[qi][k];
						a[pi][k] = cs * apk - sn * aqk;
						a[qi][k] = sn * apk + cs * aqk;
					}
				}
			}
		}
		double[] e = { a[0][0], a[1][1], a[2][2] };
		java.util.Arrays.sort(e);
		return new double[] { e[2], e[1], e[0] };
	}

	/* ------------------------------ Region ------------------------------ */

	/** The columns of the region measurement (see measureRegion). */
	public static final String[] REGION_COLUMNS = { "Voxels", "Volume", "Mean", "SD", "Min", "Max", "Integrated",
			"Threshold", "Voxels >= Threshold", "Volume >= Threshold", "Volume Fraction" };

	/**
	 * Measures the voxels of the displayed stack (the full resolution of the
	 * ImageJ image or the data of a model) in the region of the clipping (the box,
	 * the cutting plane and the mask, the whole stack without a clipping). The
	 * threshold is the iso value (if an iso surface of the data is available) or
	 * the threshold of the image (else NaN).
	 *
	 * @param showTable true displays the results in a new sheet of the Bio7 table
	 * @return the values of REGION_COLUMNS or null if no data is available
	 */
	public double[] measureRegion(boolean showTable) {
		IsoSurface.MeasureData m = iso.getMeasureData();
		ImagePlus imp = m != null ? m.image : volume.getImage();
		double[] extent = displayedExtent();
		if ((imp == null && m == null) || extent == null) {
			return null;
		}
		Clipping.Region region = clipping.getRegion(extent, HeightMaps.isFlipVertically());
		double threshold = Double.NaN;
		if (m != null) {
			threshold = m.isoValue;
		} else if (imp.getProcessor().getMinThreshold() != ImageProcessor.NO_THRESHOLD) {
			threshold = imp.getProcessor().getMinThreshold();
		}
		long n = 0, above = 0;
		double sum = 0, sum2 = 0, min = Double.MAX_VALUE, max = -Double.MAX_VALUE;
		double voxelVolume;
		String unit;
		if (imp != null) {
			int w = imp.getWidth(), h = imp.getHeight(), d = imp.getNSlices();
			ImageStack stack = imp.getStack();
			for (int z = 0; z < d; z++) {
				ImageProcessor ip = stack.getProcessor(imp.getStackIndex(imp.getChannel(), z + 1, imp.getFrame()));
				ip.setCalibrationTable(imp.getProcessor().getCalibrationTable());
				for (int y = 0; y < h; y++) {
					for (int x = 0; x < w; x++) {
						if (!region.isAll() && !region.contains((x + 0.5) / w, (y + 0.5) / h, (z + 0.5) / d)) {
							continue;
						}
						double value = ip.getPixelValue(x, y);
						n++;
						sum += value;
						sum2 += value * value;
						min = Math.min(min, value);
						max = Math.max(max, value);
						if (value >= threshold) {
							above++;
						}
					}
				}
			}
			Calibration cal = imp.getCalibration();
			voxelVolume = cal.pixelWidth * cal.pixelHeight * cal.pixelDepth;
			unit = cal.getUnit();
		} else {
			for (int z = 0; z < m.nz; z++) {
				for (int y = 0; y < m.ny; y++) {
					for (int x = 0; x < m.nx; x++) {
						if (!region.isAll()
								&& !region.contains((x + 0.5) / m.nx, (y + 0.5) / m.ny, (z + 0.5) / m.nz)) {
							continue;
						}
						double value = m.data[(z * m.ny + y) * m.nx + x];
						n++;
						sum += value;
						sum2 += value * value;
						min = Math.min(min, value);
						max = Math.max(max, value);
						if (value >= threshold) {
							above++;
						}
					}
				}
			}
			voxelVolume = m.stepX * m.stepY * m.stepZ * Math.pow(m.pixelSize, 3);
			unit = m.unit;
		}
		if (n == 0) {
			min = max = Double.NaN;
		}
		double mean = n > 0 ? sum / n : Double.NaN;
		double sd = n > 1 ? Math.sqrt(Math.max(0, (sum2 - sum * sum / n) / (n - 1))) : Double.NaN;
		double[] result = { n, n * voxelVolume, mean, sd, min, max, sum, threshold,
				Double.isNaN(threshold) ? Double.NaN : above, Double.isNaN(threshold) ? Double.NaN : above * voxelVolume,
				Double.isNaN(threshold) || n == 0 ? Double.NaN : above / (double) n };
		if (showTable) {
			String[] header = new String[REGION_COLUMNS.length + 1];
			header[0] = "Region";
			for (int k = 0; k < REGION_COLUMNS.length; k++) {
				header[k + 1] = REGION_COLUMNS[k] + (k == 1 || k == 9 ? " [" + unit + "^3]" : "");
			}
			String[] row = new String[header.length];
			row[0] = describeRegion();
			for (int k = 0; k < result.length; k++) {
				row[k + 1] = k == 0 ? String.valueOf((long) result[k]) : format(result[k]);
			}
			List<String[]> table = new ArrayList<>();
			table.add(row);
			showSheet("Region " + (imp != null ? imp.getTitle() : "Model"), header, table, null);
		}
		return result;
	}

	private String describeRegion() {
		List<String> parts = new ArrayList<>();
		if (clipping.isBoxEnabled()) {
			parts.add("Box");
		}
		if (clipping.isPlaneEnabled()) {
			parts.add("Plane");
		}
		if (clipping.getMask() != null) {
			parts.add("Mask");
		}
		return parts.isEmpty() ? "All" : String.join(" + ", parts);
	}

	/* The displayed size (width, height, depth with the z scale) of the iso surface or the volume */
	private double[] displayedExtent() {
		if (iso.getMeasureData() != null) {
			return new double[] { iso.getWidth(), iso.getHeight(), iso.getDepth() };
		}
		if (volume.isLoaded()) {
			return new double[] { volume.getWidth(), volume.getHeight(), volume.getDepth() };
		}
		return null;
	}

	/* ------------------------------ Points ------------------------------ */

	/** The columns of the picked points (see getPoints). */
	public static final String[] POINT_COLUMNS = { "X", "Y", "Z", "Value", "Object", "Distance", "Length" };

	/*
	 * Picks the first visible voxel along the view ray (the iso surface or the
	 * volume, inside of the clipping region)!
	 */
	private void pick(double[][] ray) {
		IsoSurface.MeasureData m = iso.isDisplayed() ? iso.getMeasureData() : null;
		if (m != null && m.vertices != null && m.triangles != null && m.triangles.length > 0) {
			pickMesh(m, ray);
			return;
		}
		boolean useVolume = m == null && volume.isDisplayed();
		double[] extent = displayedExtent();
		if ((m == null && !useVolume) || extent == null) {
			return;
		}
		Clipping.Region region = clipping.getRegion(extent, HeightMaps.isFlipVertically());
		int[] size = m != null ? new int[] { m.nx, m.ny, m.nz } : volume.getDataSize();
		if (size == null) {
			return;
		}
		double[] a = ray[0], b = ray[1];
		double dx = (b[0] - a[0]) * size[0], dy = (b[1] - a[1]) * size[1], dz = (b[2] - a[2]) * size[2];
		int steps = (int) Math.ceil(Math.sqrt(dx * dx + dy * dy + dz * dz) * 4) + 1;
		float threshold = Math.max(0.02f, volume.getThreshold());
		double previous = Double.NaN;
		for (int i = 0; i <= steps; i++) {
			double s = i / (double) steps;
			double rx = a[0] + (b[0] - a[0]) * s, ry = a[1] + (b[1] - a[1]) * s, rz = a[2] + (b[2] - a[2]) * s;
			if (!region.contains(rx, ry, rz)) {
				previous = Double.NaN;
				continue;
			}
			double value;
			double level;
			if (m != null) {
				value = IsoSurface.sample(m.data, m.nx, m.ny, m.nz, (float) (rx * m.nx - 0.5),
						(float) (ry * m.ny - 0.5), (float) (rz * m.nz - 0.5));
				level = m.isoValue;
			} else {
				value = volume.getOpacityAt(rx, ry, rz);
				level = threshold;
			}
			if (value >= level) {
				/* The crossing between the last two samples */
				double f = 0;
				if (!Double.isNaN(previous) && value != previous) {
					f = (level - previous) / (value - previous);
				}
				double back = (1 - f) / steps;
				/* The position of the surface and a position half a voxel inside (for the value) */
				double inside = 2.0 / steps - back;
				double[] point = { rx - (b[0] - a[0]) * back, ry - (b[1] - a[1]) * back, rz - (b[2] - a[2]) * back,
						rx + (b[0] - a[0]) * inside, ry + (b[1] - a[1]) * inside, rz + (b[2] - a[2]) * inside, 0 };
				addPoint(point, null);
				return;
			}
			previous = value;
		}
	}

	/*
	 * Picks the first triangle along the view ray (inside of the clipping region):
	 * the position on the measured surface (full resolution) and the highlighted
	 * triangle of the displayed surface!
	 */
	private void pickMesh(IsoSurface.MeasureData display, double[][] ray) {
		double[] extent = displayedExtent();
		if (extent == null) {
			return;
		}
		Clipping.Region region = clipping.getRegion(extent, HeightMaps.isFlipVertically());
		double[] displayHit = intersect(display, ray, region);
		IsoSurface.MeasureData full = iso.getMeasurementData();
		double[] hit = full == null || full.triangles == display.triangles ? displayHit : intersect(full, ray, region);
		if (hit == null) {
			return;
		}
		IsoSurface.MeasureData m = full != null ? full : display;
		/* The ray in voxel coordinates of the measured surface */
		double ox = ray[0][0] * m.nx - 0.5, oy = ray[0][1] * m.ny - 0.5, oz = ray[0][2] * m.nz - 0.5;
		double dx = (ray[1][0] - ray[0][0]) * m.nx, dy = (ray[1][1] - ray[0][1]) * m.ny,
				dz = (ray[1][2] - ray[0][2]) * m.nz;
		double bestT = hit[1];
		/* The hit and a position half a voxel inside (for the value) */
		double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
		double inside = bestT + (length > 0 ? 0.5 / length : 0);
		double[] point = { (ox + dx * bestT + 0.5) / m.nx, (oy + dy * bestT + 0.5) / m.ny,
				(oz + dz * bestT + 0.5) / m.nz, (ox + dx * inside + 0.5) / m.nx, (oy + dy * inside + 0.5) / m.ny,
				(oz + dz * inside + 0.5) / m.nz, 0 };
		synchronized (this) {
			if (measuredObject != null && measuredData != null && measuredData.triangles == m.triangles) {
				point[6] = measuredObject[m.triangles[(int) hit[0] * 3]];
			}
		}
		/* The highlighted triangle of the display and its neighbors (the triangles with a vertex of it) */
		int[][] highlight = null;
		if (displayHit != null) {
			int[] t = display.triangles;
			int triangle = (int) displayHit[0];
			int va = t[triangle * 3], vb = t[triangle * 3 + 1], vc = t[triangle * 3 + 2];
			List<Integer> list = new ArrayList<>();
			for (int i = 0; i < t.length; i += 3) {
				if (i / 3 == triangle) {
					continue;
				}
				for (int k = 0; k < 3; k++) {
					if (t[i + k] == va || t[i + k] == vb || t[i + k] == vc) {
						list.add(i / 3);
						break;
					}
				}
			}
			int[] neighbors = new int[list.size()];
			for (int i = 0; i < neighbors.length; i++) {
				neighbors[i] = list.get(i);
			}
			highlight = new int[][] { t, { triangle }, neighbors };
		}
		addPoint(point, highlight);
	}

	/*
	 * The first triangle of a surface along the ray inside of the region
	 * (Moeller-Trumbore): {triangle, ray parameter} or null!
	 */
	private static double[] intersect(IsoSurface.MeasureData m, double[][] ray, Clipping.Region region) {
		if (m == null || m.vertices == null || m.triangles == null) {
			return null;
		}
		/* The ray in voxel coordinates of the mesh */
		double ox = ray[0][0] * m.nx - 0.5, oy = ray[0][1] * m.ny - 0.5, oz = ray[0][2] * m.nz - 0.5;
		double dx = (ray[1][0] - ray[0][0]) * m.nx, dy = (ray[1][1] - ray[0][1]) * m.ny,
				dz = (ray[1][2] - ray[0][2]) * m.nz;
		float[] v = m.vertices;
		int[] t = m.triangles;
		double bestT = Double.MAX_VALUE;
		int bestTriangle = -1;
		for (int i = 0; i < t.length; i += 3) {
			int a = t[i] * 3, b = t[i + 1] * 3, c = t[i + 2] * 3;
			double e1x = v[b] - v[a], e1y = v[b + 1] - v[a + 1], e1z = v[b + 2] - v[a + 2];
			double e2x = v[c] - v[a], e2y = v[c + 1] - v[a + 1], e2z = v[c + 2] - v[a + 2];
			double px = dy * e2z - dz * e2y, py = dz * e2x - dx * e2z, pz = dx * e2y - dy * e2x;
			double det = e1x * px + e1y * py + e1z * pz;
			if (Math.abs(det) < 1e-12) {
				continue;
			}
			double inv = 1 / det;
			double sx = ox - v[a], sy = oy - v[a + 1], sz = oz - v[a + 2];
			double u = (sx * px + sy * py + sz * pz) * inv;
			if (u < 0 || u > 1) {
				continue;
			}
			double qx = sy * e1z - sz * e1y, qy = sz * e1x - sx * e1z, qz = sx * e1y - sy * e1x;
			double w = (dx * qx + dy * qy + dz * qz) * inv;
			if (w < 0 || u + w > 1) {
				continue;
			}
			double hit = (e2x * qx + e2y * qy + e2z * qz) * inv;
			if (hit < 0 || hit >= bestT) {
				continue;
			}
			/* Only the displayed part (the clipping region) */
			if (!region.isAll() && !region.contains((ox + dx * hit + 0.5) / m.nx, (oy + dy * hit + 0.5) / m.ny,
					(oz + dz * hit + 0.5) / m.nz)) {
				continue;
			}
			bestT = hit;
			bestTriangle = i / 3;
		}
		return bestTriangle < 0 ? null : new double[] { bestTriangle, bestT };
	}

	/* Adds a picked point and displays it in the table! */
	private void addPoint(double[] relative, int[][] highlight) {
		double[] row;
		synchronized (this) {
			points.add(relative);
			pointTriangles.add(highlight);
			row = pointRow(points.size() - 1);
		}
		String[] text = pointText(points.size(), row);
		Display display = PlatformUI.getWorkbench().getDisplay();
		display.asyncExec(() -> {
			if (pointGrid == null || pointGrid.isDisposed()) {
				List<double[]> all = getPoints();
				List<String[]> table = new ArrayList<>();
				for (int i = 0; i < all.size(); i++) {
					table.add(pointText(i + 1, all.get(i)));
				}
				pointGrid = showSheet("3D Points", pointHeader(), table, null);
			} else {
				GridItem item = new GridItem(pointGrid, SWT.NONE);
				for (int k = 0; k < text.length; k++) {
					item.setText(k, text[k]);
				}
				pointGrid.showItem(item);
			}
		});
	}

	/* The text of a point (the object number as an integer, empty if none) */
	private static String[] pointText(int number, double[] row) {
		String[] text = new String[row.length + 1];
		text[0] = String.valueOf(number);
		for (int k = 0; k < row.length; k++) {
			text[k + 1] = k == 4 ? (row[k] > 0 ? String.valueOf((int) row[k]) : "") : format(row[k]);
		}
		return text;
	}

	private String[] pointHeader() {
		String unit = scale().unit;
		String[] header = new String[POINT_COLUMNS.length + 1];
		header[0] = "Point";
		for (int k = 0; k < POINT_COLUMNS.length; k++) {
			header[k + 1] = POINT_COLUMNS[k] + (k == 3 || k == 4 ? "" : " [" + unit + "]");
		}
		return header;
	}

	/* The calibrated values of a point: x, y, z, value, distance to the previous point, length */
	private double[] pointRow(int i) {
		Scale s = scale();
		double[] r = points.get(i);
		double x = r[0] * s.sizeX, y = r[1] * s.sizeY, z = r[2] * s.sizeZ;
		double distance = 0, length = 0;
		for (int j = 1; j <= i; j++) {
			double[] p0 = points.get(j - 1), p1 = points.get(j);
			double ddx = (p1[0] - p0[0]) * s.sizeX, ddy = (p1[1] - p0[1]) * s.sizeY, ddz = (p1[2] - p0[2]) * s.sizeZ;
			distance = Math.sqrt(ddx * ddx + ddy * ddy + ddz * ddz);
			length += distance;
		}
		/* The value of the data half a voxel inside of the surface */
		double[] inside = r.length >= 6 ? new double[] { r[3], r[4], r[5] } : r;
		double object = r.length >= 7 ? r[6] : 0;
		return new double[] { x, y, z, valueAt(inside), object, distance, length };
	}

	/* The value of the stack (full resolution) or of the data at a relative position */
	private double valueAt(double[] r) {
		IsoSurface.MeasureData m = iso.getMeasureData();
		ImagePlus imp = m != null ? m.image : volume.getImage();
		if (imp != null) {
			int w = imp.getWidth(), h = imp.getHeight(), d = imp.getNSlices();
			int x = clamp((int) (r[0] * w), w), y = clamp((int) (r[1] * h), h), z = clamp((int) (r[2] * d), d);
			ImageProcessor ip = imp.getStack().getProcessor(imp.getStackIndex(imp.getChannel(), z + 1, imp.getFrame()));
			ip.setCalibrationTable(imp.getProcessor().getCalibrationTable());
			return ip.getPixelValue(x, y);
		}
		if (m != null) {
			return IsoSurface.sample(m.data, m.nx, m.ny, m.nz, (float) (r[0] * m.nx - 0.5),
					(float) (r[1] * m.ny - 0.5), (float) (r[2] * m.nz - 0.5));
		}
		return Double.NaN;
	}

	private Scale scale() {
		Scale s = new Scale();
		IsoSurface.MeasureData m = iso.getMeasureData();
		ImagePlus imp = m != null ? m.image : volume.getImage();
		if (imp != null) {
			Calibration cal = imp.getCalibration();
			s.sizeX = imp.getWidth() * cal.pixelWidth;
			s.sizeY = imp.getHeight() * cal.pixelHeight;
			s.sizeZ = imp.getNSlices() * cal.pixelDepth;
			s.voxelVolume = cal.pixelWidth * cal.pixelHeight * cal.pixelDepth;
			s.unit = cal.getUnit();
		} else if (m != null) {
			s.sizeX = m.nx * m.stepX * m.pixelSize;
			s.sizeY = m.ny * m.stepY * m.pixelSize;
			s.sizeZ = m.nz * m.stepZ * m.pixelSize;
			s.voxelVolume = m.stepX * m.stepY * m.stepZ * Math.pow(m.pixelSize, 3);
			s.unit = m.unit;
		} else {
			s.sizeX = s.sizeY = s.sizeZ = 1;
			s.voxelVolume = 1;
			s.unit = "pixel";
		}
		return s;
	}

	private static int clamp(int v, int size) {
		return Math.max(0, Math.min(size - 1, v));
	}

	/**
	 * Returns the picked points (calibrated x, y, z, the value, the distance to
	 * the previous point and the length of the path, see POINT_COLUMNS).
	 */
	public synchronized List<double[]> getPoints() {
		List<double[]> list = new ArrayList<>();
		for (int i = 0; i < points.size(); i++) {
			list.add(pointRow(i));
		}
		return list;
	}

	/** Removes the picked points (a new sheet is created for the next point). */
	public void clearPoints() {
		synchronized (this) {
			points.clear();
			pointTriangles.clear();
		}
		Display display = PlatformUI.getWorkbench().getDisplay();
		display.asyncExec(() -> pointGrid = null);
	}

	/**
	 * Draws the picked points and the lines between them (the scene coordinates of
	 * the current model view matrix).
	 */
	public synchronized void render(GL2 gl, double[] bounds, boolean flip, boolean cartesian, float pixelScale) {
		if (bounds == null) {
			return;
		}
		renderDistanceLines(gl, bounds, flip);
		renderObjectLabels(gl, bounds, flip, pixelScale);
		if (points.isEmpty()) {
			return;
		}
		/* The triangles of the picked points */
		for (int[][] highlight : pointTriangles) {
			if (highlight != null) {
				/* The hit triangle (filled, dark outline) and the outlines of the neighbors */
				if (highlight.length > 2) {
					iso.renderTriangles(gl, cartesian, highlight[0], highlight[2], NEIGHBOR_COLOR, false, 1);
				}
				iso.renderTriangles(gl, cartesian, highlight[0], highlight[1], PICK_COLOR, true, 0);
				iso.renderTriangles(gl, cartesian, highlight[0], highlight[1], OUTLINE_COLOR, false, 1);
			}
		}
		gl.glPushAttrib(GL2.GL_ENABLE_BIT | GL2.GL_CURRENT_BIT | GL2.GL_LINE_BIT | GL2.GL_POINT_BIT);
		gl.glDisable(GL2.GL_LIGHTING);
		gl.glDisable(GL2.GL_TEXTURE_2D);
		gl.glDisable(GL2.GL_TEXTURE_3D);
		gl.glDisable(GL2.GL_DEPTH_TEST);
		gl.glColor3f(1f, 0.25f, 0.6f);
		gl.glLineWidth(2);
		gl.glBegin(GL2.GL_LINE_STRIP);
		for (double[] r : points) {
			vertex(gl, bounds, flip, r);
		}
		gl.glEnd();
		/* A marker only for the points without a triangle (volume) */
		gl.glPointSize(6);
		gl.glBegin(GL2.GL_POINTS);
		for (int i = 0; i < points.size(); i++) {
			if (pointTriangles.get(i) == null) {
				vertex(gl, bounds, flip, points.get(i));
			}
		}
		gl.glEnd();
		gl.glPopAttrib();
	}

	/* The lines between the nearest surface points of the objects */
	private void renderDistanceLines(GL2 gl, double[] b, boolean flip) {
		if (!highlightObjects || distanceLines == null || distanceLines.isEmpty() || !iso.isDisplayed()) {
			return;
		}
		IsoSurface.MeasureData m = iso.getMeasureData();
		if (m == null || m.triangles != distanceMesh) {
			return;
		}
		gl.glPushAttrib(GL2.GL_ENABLE_BIT | GL2.GL_CURRENT_BIT | GL2.GL_LINE_BIT | GL2.GL_POINT_BIT);
		gl.glDisable(GL2.GL_LIGHTING);
		gl.glDisable(GL2.GL_TEXTURE_2D);
		gl.glDisable(GL2.GL_TEXTURE_3D);
		gl.glDisable(GL2.GL_DEPTH_TEST);
		gl.glColor3f(1f, 1f, 0.2f);
		gl.glLineWidth(2);
		gl.glBegin(GL2.GL_LINES);
		for (double[] l : distanceLines) {
			vertex(gl, b, flip, l);
			vertex(gl, b, flip, new double[] { l[3], l[4], l[5] });
		}
		gl.glEnd();
		gl.glPointSize(6);
		gl.glBegin(GL2.GL_POINTS);
		for (double[] l : distanceLines) {
			vertex(gl, b, flip, l);
			vertex(gl, b, flip, new double[] { l[3], l[4], l[5] });
		}
		gl.glEnd();
		gl.glPopAttrib();
	}

	/*
	 * The numbers of the measured objects at the centroids (in front of the scene):
	 * textured quads of the digits of an atlas (one batch), overlapping labels are
	 * not drawn (the selected label always)!
	 */
	private void renderObjectLabels(GL2 gl, double[] b, boolean flip, float pixelScale) {
		labelBounds.clear();
		if (!highlightObjects || objectLabels == null || objectLabels.length == 0) {
			return;
		}
		IsoSurface.MeasureData m = iso.getMeasureData();
		if (m == null || m.triangles != objectMesh || !iso.isDisplayed()) {
			return;
		}
		int size = Math.round(13 * Math.max(1, pixelScale));
		GLContext context = GLContext.getCurrent();
		if (atlas == null || atlasContext != context || atlas.size != size) {
			if (atlas != null && atlasContext == context) {
				atlas.dispose(gl);
			}
			atlas = new DigitAtlas(gl, size);
			atlasContext = context;
		}
		double[] mv = new double[16], pr = new double[16];
		int[] vp = new int[4];
		gl.glGetDoublev(GL2.GL_MODELVIEW_MATRIX, mv, 0);
		gl.glGetDoublev(GL2.GL_PROJECTION_MATRIX, pr, 0);
		gl.glGetIntegerv(GL2.GL_VIEWPORT, vp, 0);
		double[] matrix = Clipping.multiply(pr, mv);
		labelViewportHeight = vp[3];
		/* The positions of the labels (window pixels), the selected label first */
		List<int[]> placed = new ArrayList<>();
		int pad = Math.max(1, Math.round(pixelScale));
		int count = objectLabels.length;
		for (int pass = 0; pass < 2; pass++) {
			for (int i = 0; i < count; i++) {
				double[] l = objectLabels[i];
				int number = (int) l[3];
				boolean selected = number == selectedObject;
				if (selected != (pass == 0)) {
					continue;
				}
				double y = flip ? b[2] + l[1] * (b[3] - b[2]) : b[3] - l[1] * (b[3] - b[2]);
				double[] c = Clipping.transform(matrix,
						new double[] { b[0] + l[0] * (b[1] - b[0]), y, b[4] + l[2] * (b[5] - b[4]), 1 });
				if (c[3] <= 0) {
					continue;
				}
				double nx = c[0] / c[3], ny = c[1] / c[3];
				if (nx < -1.1 || nx > 1.1 || ny < -1.1 || ny > 1.1) {
					continue; // outside of the view
				}
				int wx = (int) Math.round((nx + 1) / 2 * vp[2]);
				int wy = (int) Math.round((ny + 1) / 2 * vp[3]);
				int scale = selected ? 3 : 2; // the selected label 1.5 times larger
				int width = atlas.width(number) * scale / 2, height = atlas.cellHeight * scale / 2;
				int[] r = { wx - width / 2, wy - height / 2, wx - width / 2 + width, wy - height / 2 + height, number,
						selected ? 1 : 0 };
				/* Not over another label */
				boolean free = true;
				for (int[] q : placed) {
					if (r[0] < q[2] - pad && r[2] > q[0] + pad && r[1] < q[3] - pad && r[3] > q[1] + pad) {
						free = false;
						break;
					}
				}
				if (free || selected) {
					placed.add(r);
					labelBounds.add(new int[] { vp[0] + r[0], vp[1] + r[1], vp[0] + r[2], vp[1] + r[3], number });
				}
			}
		}
		/* All labels in one batch (window coordinates) */
		gl.glPushAttrib(GL2.GL_ENABLE_BIT | GL2.GL_CURRENT_BIT | GL2.GL_COLOR_BUFFER_BIT | GL2.GL_TEXTURE_BIT
				| GL2.GL_DEPTH_BUFFER_BIT);
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glPushMatrix();
		gl.glLoadIdentity();
		gl.glOrtho(0, vp[2], 0, vp[3], -1, 1);
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glPushMatrix();
		gl.glLoadIdentity();
		gl.glDisable(GL2.GL_LIGHTING);
		gl.glDisable(GL2.GL_DEPTH_TEST);
		gl.glDisable(GL2.GL_CULL_FACE);
		gl.glDisable(GL2.GL_TEXTURE_3D);
		gl.glDisable(GL2.GL_CLIP_PLANE0);
		gl.glEnable(GL2.GL_TEXTURE_2D);
		gl.glEnable(GL2.GL_BLEND);
		gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
		gl.glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, GL2.GL_MODULATE);
		gl.glBindTexture(GL2.GL_TEXTURE_2D, atlas.texture);
		gl.glBegin(GL2.GL_QUADS);
		/* The other labels first, the selected label on top */
		for (int pass = 0; pass < 2; pass++) {
			for (int[] r : placed) {
				if ((r[5] == 1) != (pass == 1)) {
					continue;
				}
				if (r[5] == 1) {
					gl.glColor4f(1f, 0.9f, 0.1f, 1f);
				} else {
					gl.glColor4f(1f, 1f, 1f, 1f);
				}
				String text = String.valueOf(r[4]);
				float scale = r[5] == 1 ? 1.5f : 1f;
				float x = r[0];
				float h = atlas.cellHeight * scale;
				for (int k = 0; k < text.length(); k++) {
					int digit = text.charAt(k) - '0';
					float w = atlas.cellWidth * scale;
					float u0 = digit * atlas.u, u1 = (digit + 1) * atlas.u, v1 = atlas.v;
					gl.glTexCoord2f(u0, v1);
					gl.glVertex2f(x, r[1]);
					gl.glTexCoord2f(u1, v1);
					gl.glVertex2f(x + w, r[1]);
					gl.glTexCoord2f(u1, 0);
					gl.glVertex2f(x + w, r[1] + h);
					gl.glTexCoord2f(u0, 0);
					gl.glVertex2f(x, r[1] + h);
					x += atlas.advance * scale;
				}
			}
		}
		gl.glEnd();
		gl.glBindTexture(GL2.GL_TEXTURE_2D, 0);
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glPopMatrix();
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glPopMatrix();
		gl.glPopAttrib();
	}

	/*
	 * A texture with the digits 0 - 9 (white with a dark outline, colored with
	 * the vertex color)!
	 */
	private static final class DigitAtlas {
		final int size;
		final int cellWidth, cellHeight, advance;
		/* The size of a cell in texture coordinates */
		final float u, v;
		final int texture;

		DigitAtlas(GL2 gl, int size) {
			this.size = size;
			java.awt.Font font = new java.awt.Font("SansSerif", java.awt.Font.BOLD, size * 3 / 2);
			/* Drawn with the size of the selected label (scaled down for the others) */
			java.awt.image.BufferedImage probe = new java.awt.image.BufferedImage(1, 1,
					java.awt.image.BufferedImage.TYPE_INT_ARGB);
			java.awt.Graphics2D pg = probe.createGraphics();
			java.awt.FontMetrics fm = pg.getFontMetrics(font);
			int digitWidth = 0;
			for (char ch = '0'; ch <= '9'; ch++) {
				digitWidth = Math.max(digitWidth, fm.charWidth(ch));
			}
			pg.dispose();
			int outline = Math.max(2, size / 6);
			int cw = digitWidth + 2 * outline, ch = fm.getAscent() + fm.getDescent() + 2 * outline;
			/* The cells in the size of the normal labels (the selected label is scaled up) */
			cellWidth = cw * 2 / 3;
			cellHeight = ch * 2 / 3;
			advance = (digitWidth - outline / 2) * 2 / 3;
			int tw = Integer.highestOneBit(cw * 10 - 1) << 1, th = Integer.highestOneBit(ch - 1) << 1;
			java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(tw, th,
					java.awt.image.BufferedImage.TYPE_INT_ARGB);
			java.awt.Graphics2D g = image.createGraphics();
			g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
			g.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING,
					java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g.setRenderingHint(java.awt.RenderingHints.KEY_STROKE_CONTROL, java.awt.RenderingHints.VALUE_STROKE_PURE);
			for (int d = 0; d < 10; d++) {
				java.awt.font.TextLayout layout = new java.awt.font.TextLayout(String.valueOf(d), font,
						g.getFontRenderContext());
				java.awt.Shape shape = layout.getOutline(java.awt.geom.AffineTransform.getTranslateInstance(
						d * cw + outline + (digitWidth - fm.charWidth('0' + d)) / 2.0, outline + fm.getAscent()));
				g.setColor(new java.awt.Color(0, 0, 0, 230));
				g.setStroke(new java.awt.BasicStroke(outline * 2, java.awt.BasicStroke.CAP_ROUND,
						java.awt.BasicStroke.JOIN_ROUND));
				g.draw(shape);
				g.setColor(java.awt.Color.WHITE);
				g.fill(shape);
			}
			g.dispose();
			u = cw / (float) tw;
			v = ch / (float) th;
			java.nio.ByteBuffer buffer = com.jogamp.common.nio.Buffers.newDirectByteBuffer(tw * th * 4);
			int[] argb = image.getRGB(0, 0, tw, th, null, 0, tw);
			for (int pixel : argb) {
				buffer.put((byte) ((pixel >> 16) & 0xff)).put((byte) ((pixel >> 8) & 0xff)).put((byte) (pixel & 0xff))
						.put((byte) ((pixel >> 24) & 0xff));
			}
			buffer.rewind();
			int[] id = new int[1];
			gl.glGenTextures(1, id, 0);
			texture = id[0];
			gl.glBindTexture(GL2.GL_TEXTURE_2D, texture);
			gl.glPixelStorei(GL2.GL_UNPACK_ALIGNMENT, 4);
			gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR);
			gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR);
			gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP_TO_EDGE);
			gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP_TO_EDGE);
			gl.glTexImage2D(GL2.GL_TEXTURE_2D, 0, GL2.GL_RGBA, tw, th, 0, GL2.GL_RGBA, GL2.GL_UNSIGNED_BYTE, buffer);
			gl.glBindTexture(GL2.GL_TEXTURE_2D, 0);
		}

		/* The width of a number (window pixels of the normal size) */
		int width(int number) {
			int digits = String.valueOf(number).length();
			return advance * (digits - 1) + cellWidth;
		}

		void dispose(GL2 gl) {
			gl.glDeleteTextures(1, new int[] { texture }, 0);
		}
	}

	private static void vertex(GL2 gl, double[] b, boolean flip, double[] r) {
		double y = flip ? b[2] + r[1] * (b[3] - b[2]) : b[3] - r[1] * (b[3] - b[2]);
		gl.glVertex3d(b[0] + r[0] * (b[1] - b[0]), y, b[4] + r[2] * (b[5] - b[4]));
	}

	/* ------------------------------ Table ------------------------------ */

	private static String format(double v) {
		if (Double.isNaN(v)) {
			return "NaN";
		}
		if (v == 0) {
			return "0";
		}
		return IJ.d2s(v, 4, 9);
	}

	/*
	 * Opens the table below the 3D view (in a new stack) if it is in the stack of
	 * the 3D view (else the 3D view is hidden and not rendered)!
	 */
	private static void keep3dViewVisible(IWorkbenchWindow window, IWorkbenchPage page, IViewPart table) {
		try {
			MUIElement tableElement = sharedRef(table);
			MUIElement spaceElement = sharedRef(page.findViewReference("com.eco.bio7.spatial"));
			if (tableElement == null || spaceElement == null || tableElement.getParent() != spaceElement.getParent()
					|| !((Object) spaceElement.getParent() instanceof MPartStack)) {
				return;
			}
			MPartStack spaceStack = (MPartStack) (Object) spaceElement.getParent();
			EModelService modelService = window.getService(EModelService.class);
			MPartStack stack = modelService.createModelElement(MPartStack.class);
			stack.setElementId("com.eco.bio7.spatial.tableStack");
			/* The new stack below the 3D view (the 3D view keeps 65 percent) */
			modelService.insert((MPartSashContainerElement) stack, (MPartSashContainerElement) spaceStack,
					EModelService.BELOW, 0.35f);
			@SuppressWarnings("unchecked")
			MElementContainer<MUIElement> target = (MElementContainer<MUIElement>) (MElementContainer<?>) stack;
			modelService.move(tableElement, target);
			stack.setSelectedElement((org.eclipse.e4.ui.model.application.ui.basic.MStackElement) tableElement);
			if (spaceStack.getSelectedElement() == null || spaceStack.getSelectedElement() == tableElement) {
				spaceStack.setSelectedElement((org.eclipse.e4.ui.model.application.ui.basic.MStackElement) spaceElement);
			}
		} catch (RuntimeException e) {
			e.printStackTrace();
		}
	}

	/* The element of a view in the perspective (the placeholder of the shared part) */
	private static MUIElement sharedRef(Object view) {
		IViewPart part = view instanceof IViewReference ? ((IViewReference) view).getView(false)
				: view instanceof IViewPart ? (IViewPart) view : null;
		if (part == null) {
			return null;
		}
		MPart mpart = part.getSite().getService(MPart.class);
		if (mpart == null) {
			return null;
		}
		MPlaceholder placeholder = mpart.getCurSharedRef();
		return placeholder != null ? placeholder : mpart;
	}

	/*
	 * Opens the Bio7 table and creates a new sheet with the column names and the
	 * rows (in the user interface thread)!
	 */
	private static Grid showSheet(String name, String[] header, List<String[]> rows, int[] rowColors) {
		Grid[] result = new Grid[1];
		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(() -> {
			try {
				IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
				if (window == null && PlatformUI.getWorkbench().getWorkbenchWindowCount() > 0) {
					window = PlatformUI.getWorkbench().getWorkbenchWindows()[0];
				}
				if (window == null) {
					return;
				}
				IWorkbenchPage page = window.getActivePage();
				IViewPart table = page.showView(RTable.ID, null, IWorkbenchPage.VIEW_CREATE);
				keep3dViewVisible(window, page, table);
				page.bringToTop(table);
			} catch (PartInitException e) {
				e.printStackTrace();
				return;
			}
			if (RTable.getGrid() == null || RTable.getTabFolder() == null) {
				return;
			}
			Bio7Grid.createSheet(header.length, Math.max(1, rows.size()), name);
			Grid grid = RTable.getGrid();
			for (int j = 0; j < header.length; j++) {
				GridColumn column = grid.getColumn(j);
				column.setText(header[j]);
				int chars = header[j].length();
				for (String[] row : rows) {
					chars = Math.max(chars, row[j] != null ? row[j].length() : 0);
				}
				column.setWidth(Math.max(80, chars * 9 + 24));
			}
			for (int i = 0; i < rows.size(); i++) {
				for (int j = 0; j < header.length; j++) {
					grid.getItem(i).setText(j, rows.get(i)[j]);
				}
				/* The color of the object (like in the 3D view) */
				if (rowColors != null && i < rowColors.length && rowColors[i] >= 0) {
					int rgb = rowColors[i];
					grid.getItem(i).setBackground(0, new Color(grid.getDisplay(), (rgb >> 16) & 0xff, (rgb >> 8) & 0xff,
							rgb & 0xff));
				}
			}
			CTabFolder folder = RTable.getTabFolder();
			folder.setSelection(folder.getItemCount() - 1);
			result[0] = grid;
		});
		return result[0];
	}
}
