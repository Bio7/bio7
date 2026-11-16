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
package com.eco.bio7.pybridge;

import java.awt.Polygon;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;

import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.pyffm.PyException;
import com.eco.bio7.pyffm.PySession;
import com.eco.bio7.pyffm.PyValue;

import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Roi;
import ij.plugin.frame.RoiManager;

/**
 * The transfer of the selections (ROIs) of the ROI Manager to the embedded
 * Python (the view Geometry Selections, Python as target): the coordinates as
 * lists of numpy arrays or the geometries as GeoDataFrame (shapely, geopandas)
 * with the columns of a DataFrame and the projection of a georeferenced file.
 */
public final class PythonGeometryTransfer {

	private PythonGeometryTransfer() {
	}

	/**
	 * Transfers the selected ROIs of the ROI Manager (all if none is selected).
	 * UI thread.
	 *
	 * @param kind 0 polygons, 1 lines, 2 points
	 */
	public static void transfer(boolean asList, int kind, boolean setCrs, String crsFile, String dataFrame, boolean centroid) {
		ImagePlus imp = WindowManager.getCurrentImage();
		if (imp == null) {
			Bio7Dialog.message("No image available!");
			return;
		}
		RoiManager rm = RoiManager.getInstance();
		if (rm == null) {
			Bio7Dialog.message("No ROI Manager available!");
			return;
		}
		Roi[] rois = rm.getSelectedRoisAsArray();
		if (rois.length == 0) {
			Bio7Dialog.message("The ROI Manager has no selections!");
			return;
		}
		if (setCrs && (crsFile == null || crsFile.isBlank() || !new java.io.File(crsFile).isFile())) {
			Bio7Dialog.message("Please load a georeferenced file (e.g., a GeoTIFF) with \"Load File\" to add its projection.");
			return;
		}
		if (!PythonImageTransfer.ensurePython()) {
			return;
		}
		/* The coordinates of the selections (as for R: the polygons of the ROIs) */
		List<PyValue> xs = new ArrayList<>(), ys = new ArrayList<>();
		for (Roi roi : rois) {
			Polygon p = roi.getPolygon();
			double[] x = new double[p.npoints], y = new double[p.npoints];
			for (int i = 0; i < p.npoints; i++) {
				x[i] = p.xpoints[i];
				y[i] = p.ypoints[i];
			}
			xs.add(PyValue.of(x));
			ys.add(PyValue.of(y));
		}
		int w = imp.getWidth(), h = imp.getHeight();
		Job job = new Job("Transfer Selections to Python") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				monitor.beginTask("Transfer selections...", IProgressMonitor.UNKNOWN);
				Map<String, PyValue> a = new LinkedHashMap<>();
				a.put("xs", new PyValue.PyList(xs));
				a.put("ys", new PyValue.PyList(ys));
				a.put("size", PyValue.of(w, h));
				a.put("as_list", flag(asList));
				a.put("kind", new PyValue.Array(new long[] { kind }, "i8", new int[0]));
				a.put("set_crs", flag(setCrs && !asList));
				a.put("centroid", flag(centroid));
				a.put("df", dataFrame == null ? PyValue.None.INSTANCE : PyValue.of(dataFrame));
				a.put("crs_file", crsFile == null ? PyValue.None.INSTANCE : PyValue.of(crsFile));
				if (setCrs && !asList) {
					/* The georeference with GDAL of Bio7 (else rasterio in Python) */
					try {
						Object[] geo = com.eco.bio7.image.TransferSelectionCoordsJob.gdalGeoreference(crsFile);
						if (geo != null) {
							a.put("gt", PyValue.of((double[]) geo[0]));
							a.put("crs", PyValue.of((String) geo[1]));
						}
					} catch (LinkageError | RuntimeException e) {
						/* GDAL is not available (e.g., macOS) */
					}
				}
				try {
					PySession py = EmbeddedPython.session();
					load(py);
					py.assign("_bio7_geo", new PyValue.PyDict(a));
					PyValue r = py.eval("__import__('_bio7geo').transfer(_bio7_geo)");
					py.exec("del _bio7_geo");
					String text = r instanceof PyValue.Str s ? s.asString() : String.valueOf(r);
					EmbeddedPython.print("Selections transferred to Python: " + text + "\n", false);
					PythonShellView.refreshOpenViews();
				} catch (Exception e) {
					String msg = e.getMessage() == null ? e.toString() : e.getMessage();
					/* The last line of a Python traceback is the error */
					String[] lines = msg.strip().split("\n");
					Display.getDefault().asyncExec(() -> Bio7Dialog.message("The selections could not be transferred to Python:\n\n" + lines[lines.length - 1]));
				}
				monitor.done();
				return Status.OK_STATUS;
			}
		};
		job.schedule();
	}

	private static PyValue flag(boolean b) {
		return new PyValue.Array(new boolean[] { b }, "b1", new int[0]);
	}

	/* The module of the transfer (once) */
	private static void load(PySession py) throws PyException, java.io.IOException {
		PyValue loaded = py.eval("'_bio7geo' in __import__('sys').modules");
		if (loaded instanceof PyValue.Array l && l.getDouble(0) != 0) {
			return;
		}
		String source;
		try (InputStream in = PythonGeometryTransfer.class.getResourceAsStream("python_geometry.py")) {
			source = new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
		py.assign("_bio7_src", PyValue.of(source));
		py.exec("import types as _t, sys as _s\n_m = _t.ModuleType('_bio7geo')\nexec(compile(_bio7_src, '<_bio7geo>', 'exec'), _m.__dict__)\n"
				+ "_s.modules['_bio7geo'] = _m\ndel _t, _s, _m, _bio7_src");
	}

	/** The pandas DataFrames of the Python workspace (for the selection of the view). */
	public static List<String> dataFrames() {
		List<String> names = new ArrayList<>();
		PySession py = EmbeddedPython.session();
		if (py == null) {
			return names;
		}
		try {
			PyValue v = py.eval("sorted(n for n, v in vars(__import__('__main__')).items() if not n.startswith('_') and type(v).__name__ in ('DataFrame', 'GeoDataFrame'))");
			if (v instanceof PyValue.Str s) {
				names.addAll(List.of(s.values()));
			} else if (v instanceof PyValue.PyList l) {
				for (PyValue e : l.values()) {
					if (e instanceof PyValue.Str s) {
						names.add(s.asString());
					}
				}
			}
		} catch (PyException e) {
			/* Python busy */
		}
		return names;
	}
}
