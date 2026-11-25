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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;

import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.pyffm.PySession;
import com.eco.bio7.pyffm.PyValue;
import com.eco.bio7.util.Util;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.Roi;
import ij.measure.ResultsTable;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;

/**
 * The transfers of the Image-Methods view to the embedded Python (when Python
 * is selected in the view): numpy arrays and pandas DataFrames with the names
 * of the transfers to R. Called in the UI thread; the transfers run in jobs.
 */
public final class PythonImageTransfer {

	/* The data types of the combo of the view: Double, Integer, Byte, RGB Byte */
	private static final String[] DTYPES = { "f8", "i4", "u1", "u1" };

	private PythonImageTransfer() {
	}

	/** Starts the embedded Python if necessary (UI thread); false (with a message) if not available. */
	public static boolean ensurePython() {
		if (EmbeddedPython.isRunning()) {
			return true;
		}
		String[] error = new String[1];
		/* In a job: the user interface stays usable during the start (e.g., a slow computer) */
		error[0] = com.eco.bio7.rcp.BackgroundStart.run("Starting Python", EmbeddedPython::start);
		if (error[0] != null) {
			Bio7Dialog.message("Python could not be started:\n" + error[0]);
			return false;
		}
		return true;
	}

	/* Runs a transfer in a job: the value is assigned to the name */
	private interface Transfer {
		Map<String, PyValue> values() throws Exception;
	}

	private static void run(String title, Transfer transfer) {
		if (!ensurePython()) {
			return;
		}
		Job job = new Job(title) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				monitor.beginTask("Transfer Data ...", IProgressMonitor.UNKNOWN);
				try {
					Map<String, PyValue> values = transfer.values();
					if (values == null) {
						return Status.OK_STATUS;
					}
					PySession py = EmbeddedPython.session();
					StringBuilder names = new StringBuilder();
					for (Map.Entry<String, PyValue> e : values.entrySet()) {
						py.assign(e.getKey(), e.getValue());
						names.append(names.isEmpty() ? "" : ", ").append(e.getKey()).append(describe(e.getValue()));
					}
					EmbeddedPython.print("\nTransferred to Python: " + names + "\n", false);
					PythonShellView.refreshOpenViews();
					EmbeddedPython.prompt();
				} catch (Exception e) {
					EmbeddedPython.print("\n" + title + ": " + e.getMessage() + "\n", true);
					EmbeddedPython.prompt();
				}
				monitor.done();
				return Status.OK_STATUS;
			}
		};
		job.schedule();
	}

	private static String describe(PyValue v) {
		return switch (v) {
		case PyValue.Array a -> " (" + a.dtype() + " " + java.util.Arrays.toString(a.shape()) + ")";
		case PyValue.Frame f -> " (DataFrame " + f.length() + " rows, " + f.columns().size() + " columns)";
		case PyValue.PyList l -> " (list of " + l.length() + ")";
		default -> "";
		};
	}

	private static ImagePlus currentImage() throws Exception {
		ImagePlus imp = WindowManager.getCurrentImage();
		if (imp == null) {
			throw new IllegalStateException("No image opened!");
		}
		return imp;
	}

	/* imageSizeX and imageSizeY of the image (as the transfers to R): e.g., to create an image from a vector (Pic->) */
	private static void putImageSize(Map<String, PyValue> m, ImagePlus imp) {
		m.put("imageSizeX", new PyValue.Array(new int[] { imp.getWidth() }, "i4", new int[0]));
		m.put("imageSizeY", new PyValue.Array(new int[] { imp.getHeight() }, "i4", new int[0]));
	}

	/* The polygon of a selection as selectionx and selectiony (integer coordinates, as the transfers to R) */
	private static void putSelection(Map<String, PyValue> m, Roi roi) {
		java.awt.Polygon p = roi.getPolygon();
		m.put("selectionx", PyValue.of(java.util.Arrays.copyOf(p.xpoints, p.npoints)));
		m.put("selectiony", PyValue.of(java.util.Arrays.copyOf(p.ypoints, p.npoints)));
	}

	private static Map<String, PyValue> one(String name, PyValue value) {
		Map<String, PyValue> m = new LinkedHashMap<>();
		m.put(name, value);
		return m;
	}

	/* ---------------- The transfers ---------------- */

	/** "IJ RT": the results table as DataFrame IJResultsTable. */
	public static void resultsTable() {
		run("Transfer to Python", () -> {
			ResultsTable rt = ResultsTable.getResultsTable();
			if (rt == null || rt.size() == 0) {
				throw new IllegalStateException("The ImageJ results table is empty");
			}
			return one("IJResultsTable", PythonConvert.resultsTable(rt));
		});
	}

	/**
	 * "IJ Stack" (as for R: the data type of the view): all slices of the current
	 * image as numpy array imageStack (slices, height, width), one image (height,
	 * width). Double float64 (the exact values), Integer int32, Byte uint8 (an RGB
	 * image: its luminance); RGB Byte uint8 (slices, height, width, 3). Also
	 * imageSizeX and imageSizeY.
	 */
	public static void stack(int transferType) {
		run("Transfer ImageJ Stack", () -> {
			ImagePlus imp = currentImage();
			ij.ImageStack stack = imp.getStack();
			int w = imp.getWidth(), h = imp.getHeight(), plane = w * h, n = stack.getSize();
			Map<String, PyValue> m = new LinkedHashMap<>();
			putImageSize(m, imp);
			if (transferType == 3) {
				byte[] rgb = new byte[n * plane * 3];
				for (int k = 0; k < n; k++) {
					ImageProcessor ip = stack.getProcessor(k + 1);
					int[] px = (int[]) (ip instanceof ij.process.ColorProcessor ? ip : ip.convertToRGB()).getPixels();
					int o = k * plane * 3;
					for (int i = 0; i < plane; i++) {
						rgb[o + 3 * i] = (byte) (px[i] >> 16);
						rgb[o + 3 * i + 1] = (byte) (px[i] >> 8);
						rgb[o + 3 * i + 2] = (byte) px[i];
					}
				}
				m.put("imageStack", new PyValue.Array(rgb, "u1", n == 1 ? new int[] { h, w, 3 } : new int[] { n, h, w, 3 }));
				return m;
			}
			double[] v = new double[n * plane];
			for (int k = 0; k < n; k++) {
				values(com.eco.bio7.image.ImageMethods.luminance(stack.getProcessor(k + 1), transferType), v, k * plane);
			}
			PyValue.Array a = new PyValue.Array(v, "f8", n == 1 ? new int[] { h, w } : new int[] { n, h, w });
			m.put("imageStack", convert(a, DTYPES[Math.max(0, Math.min(transferType, 2))]));
			return m;
		});
	}

	/*
	 * The values of an image in the order of the rows into v from offset (as for R:
	 * the values of float and double images, else the pixels; an RGB image: its
	 * luminance)
	 */
	private static void values(ImageProcessor ip, double[] v, int offset) {
		ImageProcessor p = ip instanceof ij.process.ColorProcessor ? ip.convertToFloat() : ip;
		int w = p.getWidth(), h = p.getHeight();
		for (int y = 0, i = offset; y < h; y++) {
			for (int x = 0; x < w; x++, i++) {
				v[i] = p instanceof ij.process.DoubleProcessor dp ? dp.getPixelValueDouble(x, y)
						: p instanceof ij.process.FloatProcessor fp ? fp.getf(x, y) : p.getPixel(x, y);
			}
		}
	}

	/** "ROI" (as for R): imageSizeX, imageSizeY and the polygon of the current selection (selectionx, selectiony). */
	public static void roi() {
		run("Transfer to Python", () -> {
			ImagePlus imp = currentImage();
			Map<String, PyValue> m = new LinkedHashMap<>();
			putImageSize(m, imp);
			Roi roi = imp.getRoi();
			if (roi == null) {
				throw new IllegalStateException("The image has no selection");
			}
			putSelection(m, roi);
			return m;
		});
	}

	/**
	 * "Particles" (as for R): imageSizeX, imageSizeY, the polygon of the current
	 * selection (if any), the particle analysis of ImageJ and its results as
	 * DataFrame Particles.
	 */
	public static void particles() {
		if (!ensurePython()) {
			return;
		}
		if (WindowManager.getCurrentImage() == null) {
			Bio7Dialog.message("No image opened!");
			return;
		}
		ImagePlus imp = WindowManager.getCurrentImage();
		Roi selection = imp.getRoi();
		/* The dialog of the analysis (macro) runs in its own thread */
		Thread t = new Thread(() -> {
			IJ.runMacro("run(\"Analyze Particles...\")");
			run("Transfer to Python", () -> {
				Map<String, PyValue> m = new LinkedHashMap<>();
				putImageSize(m, imp);
				if (selection != null) {
					putSelection(m, selection);
				}
				ResultsTable rt = ResultsTable.getResultsTable();
				if (rt == null || rt.size() == 0) {
					throw new IllegalStateException("No particles in the results table (select 'Display results')");
				}
				m.put("Particles", PythonConvert.resultsTable(rt));
				return m;
			});
		}, "Particle analysis");
		t.start();
	}

	/**
	 * "Pixel" (as for R): imageSizeX, imageSizeY and the pixels of the selection of
	 * the current image in all opened images (the tabs of the ImageJ view, of a
	 * stack the current slice): a DataFrame (named in the dialog) with a column per
	 * image (layer1, layer2, ...) and the class of the dialog (column Class).
	 */
	public static void pixels(int transferType) {
		if (transferType == 3) {
			Bio7Dialog.message("RGB special transfer not supported!\nPlease split the RGB channels\nand transfer the(layer) selection in e.g. byte mode!");
			return;
		}
		ImagePlus current = WindowManager.getCurrentImage();
		if (current == null) {
			Bio7Dialog.message("No image available!");
			return;
		}
		Roi roi = current.getRoi();
		if (roi == null) {
			Bio7Dialog.message("No selection available!");
			return;
		}
		if (!ensurePython()) {
			return;
		}
		/* The dialog of the transfer to R: the name and the class */
		com.eco.bio7.image.ImageSelectionInputDialog dialog = new com.eco.bio7.image.ImageSelectionInputDialog(Util.getShell());
		if (dialog.open() != Window.OK) {
			return;
		}
		String name = pythonName(com.eco.bio7.image.ImageSelectionTransferJob.input(), java.util.Set.of());
		boolean signature = com.eco.bio7.image.ImageSelectionTransferJob.isSignature();
		int cls = com.eco.bio7.image.ImageSelectionTransferJob.signature();
		/* The images of the tabs of the ImageJ view (the layers) */
		List<ImageProcessor> layers = new ArrayList<>();
		for (org.eclipse.swt.custom.CTabItem item : com.eco.bio7.image.ui.CanvasView.getCanvas_view().tabFolder.getItems()) {
			ImagePlus imp = com.eco.bio7.image.ui.IJTabs.getImagePlus(item);
			if (imp != null) {
				layers.add(imp.getProcessor());
			}
		}
		run("Transfer Selection to Python", () -> {
			Map<String, PyValue> m = new LinkedHashMap<>();
			putImageSize(m, current);
			m.put(name, layerFrame(roi, layers, null, transferType, signature ? Integer.valueOf(cls) : null));
			return m;
		});
	}

	/**
	 * "Pixel RM" (as for R): the pixels of the selections of the ROI Manager (the
	 * selected ones, else all) in all opened images (the tabs of the ImageJ view,
	 * of a stack the current slice): imageSizeX, imageSizeY and a DataFrame per
	 * selection (named as the selection) with a column per image (layer1, layer2,
	 * ...) and the class of the dialog.
	 */
	public static void pixelsRoiManager(int transferType) {
		if (transferType == 3) {
			Bio7Dialog.message("RGB special transfer not supported!\nPlease split the RGB channels\nand transfer the (layer) selection in e.g. byte mode!");
			return;
		}
		Roi[] rois = roiManagerSelections();
		ImagePlus current = WindowManager.getCurrentImage();
		if (rois == null || current == null) {
			if (current == null) {
				Bio7Dialog.message("No image available!");
			}
			return;
		}
		if (current.getStackSize() > 1) {
			Bio7Dialog.message("Info: For a stack only the first slice (layer)\n" + "will be measured for all ROI's in the ROI Manager!");
		}
		if (!ensurePython()) {
			return;
		}
		com.eco.bio7.image.ImageRoiSelectionInputDialog dialog = new com.eco.bio7.image.ImageRoiSelectionInputDialog(Util.getShell());
		dialog.open();
		if (com.eco.bio7.image.ImageRoiSelectionTransferJob.cancelJob) {
			return;
		}
		String mode = com.eco.bio7.image.ImageRoiSelectionTransferJob.signatureMode();
		int number = com.eco.bio7.image.ImageRoiSelectionTransferJob.signatureNumber();
		/* The images of the tabs of the ImageJ view (the layers) */
		List<ImageProcessor> layers = new ArrayList<>();
		for (org.eclipse.swt.custom.CTabItem item : com.eco.bio7.image.ui.CanvasView.getCanvas_view().tabFolder.getItems()) {
			ImagePlus imp = com.eco.bio7.image.ui.IJTabs.getImagePlus(item);
			if (imp != null) {
				layers.add(imp.getProcessor());
			}
		}
		run("Transfer Selections to Python", () -> roiFrames(current, rois, layers, null, transferType, mode, number));
	}

	/**
	 * "Pixel RM Stack" (as for R): the pixels of the selections of the ROI Manager
	 * in the slices of the current stack (the range of the dialog): imageSizeX,
	 * imageSizeY and a DataFrame per selection with a column per slice (layer and
	 * the number of the slice) and the class of the dialog.
	 */
	public static void pixelsRoiManagerStack(int transferType) {
		if (transferType == 3) {
			Bio7Dialog.message("RGB special transfer not supported!\nPlease split the RGB channels\nand transfer the(slice) selection in e.g. byte mode!");
			return;
		}
		Roi[] rois = roiManagerSelections();
		ImagePlus imp = WindowManager.getCurrentImage();
		if (rois == null || imp == null) {
			if (imp == null) {
				Bio7Dialog.message("No image available!");
			}
			return;
		}
		if (imp.getStackSize() <= 1) {
			Bio7Dialog.message("Please select a stack with more than one image!");
			return;
		}
		if (!ensurePython()) {
			return;
		}
		com.eco.bio7.image.ImageRoiStackSelectionInputDialog dialog = new com.eco.bio7.image.ImageRoiStackSelectionInputDialog(Util.getShell());
		dialog.open();
		if (com.eco.bio7.image.ImageStackRoiSelectionTransferJob.cancelJob) {
			return;
		}
		String mode = com.eco.bio7.image.ImageStackRoiSelectionTransferJob.signatureMode();
		int number = com.eco.bio7.image.ImageStackRoiSelectionTransferJob.signatureNumber();
		int from = Math.max(1, com.eco.bio7.image.ImageStackRoiSelectionTransferJob.minStackSlice());
		int to = Math.min(imp.getStackSize(), com.eco.bio7.image.ImageStackRoiSelectionTransferJob.maxStackSlice());
		ImageStack stack = imp.getStack();
		List<ImageProcessor> layers = new ArrayList<>();
		List<String> names = new ArrayList<>();
		for (int i = from; i <= to; i++) {
			layers.add(stack.getProcessor(i));
			names.add("layer" + i);
		}
		if (layers.isEmpty()) {
			Bio7Dialog.message("No slices in the selected range!");
			return;
		}
		run("Transfer Selections to Python", () -> roiFrames(imp, rois, layers, names, transferType, mode, number));
	}

	/* The selections of the ROI Manager: the selected ones, else all (null with a message) */
	private static Roi[] roiManagerSelections() {
		RoiManager rm = RoiManager.getInstance();
		if (rm == null) {
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

	/*
	 * As for R: imageSizeX, imageSizeY and a DataFrame per selection (named as the
	 * selection) with the class (column Class) and a column per layer. layerNames
	 * null: layer1, layer2, ...
	 */
	private static Map<String, PyValue> roiFrames(ImagePlus imp, Roi[] rois, List<ImageProcessor> layers, List<String> layerNames, int transferType, String mode,
			int number) {
		Map<String, PyValue> result = new LinkedHashMap<>();
		putImageSize(result, imp);
		for (int r = 0; r < rois.length; r++) {
			Roi roi = rois[r];
			Integer cls = switch (mode) {
			case "fixed" -> number;
			case "increasing" -> r + 1;
			case "group" -> roi.getGroup() > 0 ? roi.getGroup() : null;
			case "name" -> classFromName(roi.getName());
			default -> null;
			};
			String roiName = roi.getName() == null ? "roi" + (r + 1) : roi.getName();
			String name = pythonName(roiName, result.keySet());
			result.put(name, layerFrame(roi, layers, layerNames, transferType, cls));
		}
		return result;
	}

	/*
	 * The pixels of a selection in the layers (as for R: the contained points, the
	 * value of the data type): the column Class (if cls is not null) and a column
	 * per layer (layerNames null: layer1, layer2, ...)
	 */
	private static PyValue.Frame layerFrame(Roi roi, List<ImageProcessor> layers, List<String> layerNames, int transferType, Integer cls) {
		String dtype = DTYPES[Math.max(0, Math.min(transferType, 2))];
		java.awt.Point[] points = roi.getContainedPoints();
		Map<String, PyValue> cols = new LinkedHashMap<>();
		if (cls != null) {
			int[] c = new int[points.length];
			java.util.Arrays.fill(c, cls);
			cols.put("Class", convert(PyValue.of(c), dtype));
		}
		for (int k = 0; k < layers.size(); k++) {
			ImageProcessor ip = layers.get(k);
			double[] v = new double[points.length];
			for (int i = 0; i < points.length; i++) {
				int x = points[i].x, y = points[i].y;
				if (x < 0 || y < 0 || x >= ip.getWidth() || y >= ip.getHeight()) {
					v[i] = Double.NaN;
					continue;
				}
				v[i] = transferType == 0 ? (ip instanceof ij.process.DoubleProcessor dp ? dp.getPixelValueDouble(x, y) : ip.getPixelValue(x, y))
						: transferType == 1 ? com.eco.bio7.image.ImageMethods.luminancePixel(ip, x, y) : com.eco.bio7.image.ImageMethods.luminancePixel(ip, x, y) & 0xff;
			}
			cols.put(layerNames == null ? "layer" + (k + 1) : layerNames.get(k), convert(PyValue.of(v), dtype));
		}
		return new PyValue.Frame(cols, null);
	}

	/* The class from the name of a selection: the number after the last '_' (as for R) */
	private static Integer classFromName(String name) {
		if (name == null) {
			return null;
		}
		int i = name.lastIndexOf('_');
		if (i < 0) {
			return null;
		}
		try {
			return Integer.parseInt(name.substring(i + 1).trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	/* A valid Python name from the name of a selection (unique in the transfer) */
	private static String pythonName(String name, java.util.Set<String> used) {
		String n = name.replaceAll("[^A-Za-z0-9_]", "_");
		if (n.isEmpty() || Character.isDigit(n.charAt(0))) {
			n = "X" + n;
		}
		if (java.util.Set.of("imageSizeX", "imageSizeY").contains(n) || isKeyword(n)) {
			n = n + "_";
		}
		String unique = n;
		for (int i = 2; used.contains(unique); i++) {
			unique = n + "_" + i;
		}
		return unique;
	}

	private static boolean isKeyword(String n) {
		return java.util.Set.of("False", "None", "True", "and", "as", "assert", "async", "await", "break", "class", "continue", "def", "del", "elif", "else", "except",
				"finally", "for", "from", "global", "if", "import", "in", "is", "lambda", "nonlocal", "not", "or", "pass", "raise", "return", "try", "while", "with", "yield")
				.contains(n);
	}

	/**
	 * "Measure RM" (as for R, see RoiMeasurements): the measurements of ImageJ for
	 * each selection of the ROI Manager in the current image as the pandas
	 * DataFrame RoiMeasurements (one row per selection), imageSizeX and imageSizeY.
	 */
	public static void measurements() {
		ImagePlus imp = WindowManager.getCurrentImage();
		if (imp == null) {
			Bio7Dialog.message("No image opened!");
			return;
		}
		Roi[] rois = com.eco.bio7.image.RoiMeasurements.selections();
		if (rois == null) {
			return;
		}
		/* Measured now (the selection of the image is set for each measurement) */
		Map<String, Object> columns = com.eco.bio7.image.RoiMeasurements.measure(imp, rois);
		run("Measure RM (Python)", () -> {
			Map<String, PyValue> m = new LinkedHashMap<>();
			putImageSize(m, imp);
			Map<String, PyValue> cols = new LinkedHashMap<>();
			for (Map.Entry<String, Object> e : columns.entrySet()) {
				Object v = e.getValue();
				cols.put(e.getKey(), v instanceof String[] s ? PyValue.of(s) : v instanceof int[] i ? PyValue.of(i) : PyValue.of((double[]) v));
			}
			m.put(com.eco.bio7.image.RoiMeasurements.NAME, new PyValue.Frame(cols, null));
			return m;
		});
	}

	/**
	 * "Pic&lt;-" (as for R): the current image (of a stack the current slice),
	 * imageSizeX, imageSizeY and imageDataName. Double: optionally a matrix
	 * (height, width), else a vector (in the order of the rows); Integer and Byte: a
	 * vector; RGB: optionally an integer matrix (pixels, 3: R, G, B), else the byte
	 * vectors name_R, name_G and name_B.
	 */
	public static void imageToPython(String name, int transferType) {
		ImagePlus imp = WindowManager.getCurrentImage();
		if (imp == null) {
			Bio7Dialog.message("No image opened!");
			return;
		}
		if (!ensurePython()) {
			return;
		}
		/* The questions of the transfer to R */
		boolean matrix = false;
		if (transferType == 0) {
			matrix = question("Should a matrix be created from the image?", "Matrix?");
		} else if (transferType == 3) {
			matrix = question("Do you want to create a integer matrix?", "Bio7");
		}
		boolean asMatrix = matrix;
		/* The current image (of a stack the current slice) */
		ImageProcessor ip = imp.getProcessor();
		run("Transfer Image to Python", () -> {
			int w = ip.getWidth(), h = ip.getHeight(), n = w * h;
			Map<String, PyValue> m = new LinkedHashMap<>();
			putImageSize(m, imp);
			m.put("imageDataName", PyValue.of(name));
			if (transferType == 3) {
				byte[] r = new byte[n], g = new byte[n], b = new byte[n];
				for (int y = 0, i = 0; y < h; y++) {
					for (int x = 0; x < w; x++, i++) {
						int c = ip.getPixel(x, y);
						r[i] = (byte) ((c >> 16) & 0xff);
						g[i] = (byte) ((c >> 8) & 0xff);
						b[i] = (byte) (c & 0xff);
					}
				}
				if (asMatrix) {
					int[] d = new int[n * 3];
					for (int i = 0; i < n; i++) {
						d[3 * i] = r[i] & 0xff;
						d[3 * i + 1] = g[i] & 0xff;
						d[3 * i + 2] = b[i] & 0xff;
					}
					m.put(name, new PyValue.Array(d, "i4", new int[] { n, 3 }));
				} else {
					m.put(name + "_R", new PyValue.Array(r, "u1", new int[] { n }));
					m.put(name + "_G", new PyValue.Array(g, "u1", new int[] { n }));
					m.put(name + "_B", new PyValue.Array(b, "u1", new int[] { n }));
				}
				return m;
			}
			/* An RGB image in the other types: its luminance */
			double[] v = new double[n];
			values(com.eco.bio7.image.ImageMethods.luminance(ip, transferType), v, 0);
			PyValue.Array a = new PyValue.Array(v, "f8", asMatrix ? new int[] { h, w } : new int[] { n });
			m.put(name, convert(a, DTYPES[Math.max(0, Math.min(transferType, 2))]));
			return m;
		});
	}

	/* A question with Yes and No (as the transfers to R) */
	private static boolean question(String text, String title) {
		com.eco.bio7.util.ThemedMessageBox message = new com.eco.bio7.util.ThemedMessageBox(Util.getShell(),
				org.eclipse.swt.SWT.ICON_QUESTION | org.eclipse.swt.SWT.YES | org.eclipse.swt.SWT.NO);
		message.setMessage(text);
		message.setText(title);
		return message.open() == org.eclipse.swt.SWT.YES;
	}

	/**
	 * "Pic-&gt;": an ImageJ image from a numpy array (a vector, as for R, with
	 * imageSizeX and imageSizeY).
	 *
	 * @param imageType the combo of the view: 0 Colour, 1 Greyscale, 2 Float, 3
	 *                  Short, 4 Double (64-bit)
	 */
	public static void imageFromPython(String name, int imageType) {
		if (!ensurePython()) {
			return;
		}
		Job job = new Job("Image from Python") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					PyValue v = EmbeddedPython.session().eval(name);
					PyValue.Array a;
					try {
						a = PythonConvert.toArray(v);
					} catch (IllegalArgumentException e) {
						throw new IllegalArgumentException(name + ": " + e.getMessage());
					}
					/* A vector (e.g., changed pixels): the image with imageSizeX and imageSizeY (as for R) */
					if (a.shape().length == 1) {
						int w = (int) PythonConvert.toArray(EmbeddedPython.session().eval("imageSizeX")).getDouble(0);
						int h = (int) PythonConvert.toArray(EmbeddedPython.session().eval("imageSizeY")).getDouble(0);
						if (w * h != a.length()) {
							throw new IllegalArgumentException(name + ": a vector of " + a.length() + " values does not fit imageSizeX * imageSizeY = " + (w * h));
						}
						a = new PyValue.Array(a.data(), a.dtype(), new int[] { h, w });
					} else if (imageType == 0 && a.shape().length == 2 && a.shape()[1] == 3) {
						/* The RGB matrix of Pic<- (pixels, 3): an RGB image with imageSizeX and imageSizeY */
						int w = (int) PythonConvert.toArray(EmbeddedPython.session().eval("imageSizeX")).getDouble(0);
						int h = (int) PythonConvert.toArray(EmbeddedPython.session().eval("imageSizeY")).getDouble(0);
						if (w * h != a.shape()[0]) {
							throw new IllegalArgumentException(name + ": " + a.shape()[0] + " pixels do not fit imageSizeX * imageSizeY = " + (w * h));
						}
						a = new PyValue.Array(a.data(), a.dtype(), new int[] { h, w, 3 });
					}
					/* As for R: Double creates a 64-bit image (DoubleProcessor) */
					String dtype = switch (imageType) {
					case 0, 1 -> "u1";
					case 3 -> "u2";
					case 4 -> "f8";
					default -> "f4";
					};
					if (imageType == 0 && !(a.shape().length >= 3 && a.shape()[a.shape().length - 1] == 3)) {
						throw new IllegalArgumentException("A colour image needs an array (height, width, 3)");
					}
					ImagePlus imp = PythonConvert.toImage(convert(a, dtype), name, imageType == 4);
					Display.getDefault().asyncExec(imp::show);
				} catch (Exception e) {
					EmbeddedPython.print("\nImage from Python: " + e.getMessage() + "\n", true);
					EmbeddedPython.prompt();
				}
				return Status.OK_STATUS;
			}
		};
		job.schedule();
	}

	/* ---------------- Helpers ---------------- */

	/* An array in another data type (u1 and u2 are clamped) */
	static PyValue.Array convert(PyValue.Array a, String dtype) {
		if (a.dtype().equals(dtype)) {
			return a;
		}
		int len = a.length();
		Object data = switch (dtype) {
		case "f8" -> a.asDoubles();
		case "f4" -> {
			float[] f = new float[len];
			for (int i = 0; i < len; i++) {
				f[i] = (float) a.getDouble(i);
			}
			yield f;
		}
		case "i4" -> a.asInts();
		case "u1" -> {
			byte[] b = new byte[len];
			for (int i = 0; i < len; i++) {
				b[i] = (byte) Math.max(0, Math.min(255, Math.round(a.getDouble(i))));
			}
			yield b;
		}
		case "u2" -> {
			short[] s = new short[len];
			for (int i = 0; i < len; i++) {
				s[i] = (short) Math.max(0, Math.min(65535, Math.round(a.getDouble(i))));
			}
			yield s;
		}
		default -> throw new IllegalArgumentException("Unknown dtype: " + dtype);
		};
		return new PyValue.Array(data, dtype, a.shape());
	}
}
