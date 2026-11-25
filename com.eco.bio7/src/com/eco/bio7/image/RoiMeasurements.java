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
package com.eco.bio7.image;

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.rbridge.RState;
import com.eco.bio7.rbridge.RUtil;

import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Roi;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.plugin.filter.Analyzer;
import ij.plugin.frame.RoiManager;

/**
 * "Measure RM" of the Image-Methods view: the measurements of ImageJ for each
 * selection of the ROI Manager (the selected ones, else all) in the current
 * image (of a stack the current slice) as a table, one row per selection: the
 * name of the selection (ROI), its group (Group, 0: none), the area, the mean,
 * the standard deviation, the median, the minimum and the maximum, the
 * integrated density, the centroid, the perimeter, the shape descriptors and the
 * Feret diameters (the columns and units of ImageJ). The data frame (R) or
 * DataFrame (Python) is RoiMeasurements, with imageSizeX and imageSizeY.
 */
public final class RoiMeasurements {

	/** The name of the table in R and Python. */
	public static final String NAME = "RoiMeasurements";

	/** The measurements of ImageJ. */
	private static final int MEASUREMENTS = Measurements.AREA | Measurements.MEAN | Measurements.STD_DEV | Measurements.MEDIAN | Measurements.MIN_MAX
			| Measurements.INTEGRATED_DENSITY | Measurements.CENTROID | Measurements.PERIMETER | Measurements.SHAPE_DESCRIPTORS | Measurements.FERET;

	/** The text of the tooltip of the action (R and Python). */
	public static final String TOOLTIP = "Measure RM: measures each selection of the ROI Manager\n" + "(the selected ones, else all) in the current image\n"
			+ "(of a stack the current slice) and transfers the table\n" + "\"RoiMeasurements\" to R (data frame) or Python (pandas DataFrame):\n"
			+ "one row per selection with the name (ROI), the group,\n" + "area, mean, standard deviation, median, min, max,\n"
			+ "integrated density, centroid (X, Y), perimeter,\n" + "shape descriptors (circularity, aspect ratio, roundness,\n"
			+ "solidity) and Feret diameters in the units of the image.\n" + "Also imageSizeX and imageSizeY are transferred.";

	private RoiMeasurements() {
	}

	/** The selections of the ROI Manager: the selected ones, else all (null with a message). */
	public static Roi[] selections() {
		RoiManager rm = RoiManager.getInstance();
		if (rm == null || rm.getCount() == 0) {
			Bio7Dialog.message("Please add selections to the ROI Manager!");
			return null;
		}
		Roi[] rois = rm.getSelectedRoisAsArray();
		if (rois.length < 1) {
			Bio7Dialog.message("NO ROI's available in ROI Manager!");
			return null;
		}
		return rois;
	}

	/**
	 * The measurements of the selections in the image (call in the thread of the
	 * user interface): the columns (String[] ROI, int[] Group, double[] for the
	 * measurements) in the order of ImageJ. The selection of the image and the
	 * results table of ImageJ are not changed.
	 */
	public static Map<String, Object> measure(ImagePlus imp, Roi[] rois) {
		ResultsTable rt = new ResultsTable();
		Analyzer analyzer = new Analyzer(imp, MEASUREMENTS, rt);
		Roi original = imp.getRoi();
		String[] names = new String[rois.length];
		int[] groups = new int[rois.length];
		try {
			for (int i = 0; i < rois.length; i++) {
				imp.setRoi(rois[i], false);
				analyzer.measure();
				names[i] = rois[i].getName() == null ? "roi" + (i + 1) : rois[i].getName();
				groups[i] = rois[i].getGroup();
			}
		} finally {
			if (original != null) {
				imp.setRoi(original, false);
			} else {
				imp.deleteRoi();
			}
		}
		Map<String, Object> columns = new LinkedHashMap<>();
		columns.put("ROI", names);
		columns.put("Group", groups);
		for (String heading : rt.getHeadings()) {
			/* Not the label and not a column which is already there (e.g., Group of newer ImageJ versions) */
			if (heading == null || heading.isEmpty() || " ".equals(heading) || "Label".equals(heading) || columns.containsKey(heading) || !rt.columnExists(heading)) {
				continue;
			}
			double[] values = rt.getColumnAsDoubles(rt.getColumnIndex(heading));
			if (values != null && values.length == rois.length) {
				columns.put(heading, values);
			}
		}
		return columns;
	}

	/** Measures the selections of the ROI Manager in the current image and transfers the table to R (in a job). */
	public static void toR() {
		ImagePlus imp = WindowManager.getCurrentImage();
		if (imp == null) {
			Bio7Dialog.message("No image opened!");
			return;
		}
		Roi[] rois = selections();
		if (rois == null) {
			return;
		}
		/* R is started if necessary (as Python for its transfers) */
		if (!RImageMethodsView.ensureR()) {
			return;
		}
		if (RState.isBusy()) {
			Bio7Dialog.message("R is busy!");
			return;
		}
		Map<String, Object> columns = measure(imp, rois);
		int w = imp.getWidth(), h = imp.getHeight();
		Job job = new Job("Measure RM (R)") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				monitor.beginTask("Transfer the measurements ...", IProgressMonitor.UNKNOWN);
				try {
					RUtil.toR("imageSizeX", w);
					RUtil.toR("imageSizeY", h);
					RUtil.dataFrameToR(NAME, columns);
					System.out.println(rois.length + " selections measured: data frame " + NAME + " in R.");
				} catch (Exception e) {
					System.err.println("Measure RM: " + e.getMessage());
				}
				monitor.done();
				return Status.OK_STATUS;
			}
		};
		job.schedule();
	}
}
