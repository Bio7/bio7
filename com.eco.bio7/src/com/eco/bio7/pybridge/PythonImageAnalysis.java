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

import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.image.ClusterDialog;
import com.eco.bio7.image.ClusterJob;
import com.eco.bio7.image.PcaDialog;
import com.eco.bio7.image.PcaJob;
import com.eco.bio7.pyffm.PyException;
import com.eco.bio7.pyffm.PySession;
import com.eco.bio7.pyffm.PyValue;
import com.eco.bio7.util.Util;

import ij.ImagePlus;
import ij.WindowManager;

/**
 * "Cluster Pic" and "PCA" of the Image-Methods view in the embedded Python
 * (the dialogs of R with the options of scikit-learn): the pixels of the
 * selected images clustered (a new image of the clusters) or the principal
 * components as new images, with plots in the plot view and the summary in
 * the console.
 */
public final class PythonImageAnalysis {

	private PythonImageAnalysis() {
	}

	/** "Cluster Pic" (UI thread). */
	public static void cluster() {
		ClusterDialog dialog = new ClusterDialog(Util.getShell(), true);
		dialog.open();
		if (!ClusterJob.doAnalysis()) {
			return;
		}
		start("Cluster Images (Python)", ClusterJob.images(), ClusterJob.onlyTransfer() ? "transfer_only" : "cluster", ClusterJob.number(), ClusterJob.algorithm(),
				ClusterJob.options());
		ClusterJob.doClusterImages(false);
	}

	/** "PCA" (UI thread). */
	public static void pca() {
		PcaDialog dialog = new PcaDialog(Util.getShell(), true);
		dialog.open();
		if (!PcaJob.doAnalysis()) {
			return;
		}
		start("PCA of Images (Python)", PcaJob.images(), PcaJob.onlyTransfer() ? "transfer_only" : "pca", PcaJob.number(), null, PcaJob.options());
		PcaJob.doClusterImages(false);
	}

	private static void start(String title, String[] imageTitles, String function, int k, String algorithm, String options) {
		if (imageTitles.length == 0) {
			Bio7Dialog.message("Please add images to the list on the right (>>).");
			return;
		}
		List<ImagePlus> images = new ArrayList<>();
		for (String t : imageTitles) {
			ImagePlus imp = WindowManager.getImage(t);
			if (imp == null) {
				Bio7Dialog.message("The image is not open: " + t);
				return;
			}
			images.add(imp);
		}
		if (!PythonImageTransfer.ensurePython()) {
			return;
		}
		Job job = new Job(title) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				monitor.beginTask(title, IProgressMonitor.UNKNOWN);
				long time = System.currentTimeMillis();
				try {
					List<PyValue> arrays = new ArrayList<>();
					for (ImagePlus imp : images) {
						arrays.add(PythonConvert.image(imp));
					}
					Map<String, PyValue> a = new LinkedHashMap<>();
					a.put("images", new PyValue.PyList(arrays));
					List<PyValue> names = new ArrayList<>();
					for (String t : imageTitles) {
						names.add(PyValue.of(t));
					}
					a.put("names", new PyValue.PyList(names));
					a.put("k", new PyValue.Array(new long[] { k }, "i8", new int[0]));
					a.put("algorithm", algorithm == null ? PyValue.None.INSTANCE : PyValue.of(algorithm));
					a.put("options", PyValue.of(options == null ? "" : options));
					PySession py = EmbeddedPython.session();
					load(py);
					py.assign("_bio7_ia", new PyValue.PyDict(a));
					PyValue r = py.eval("__import__('_bio7ia')." + function + "(_bio7_ia)");
					py.exec("del _bio7_ia");
					String text = r instanceof PyValue.Str s ? s.asString() : String.valueOf(r);
					EmbeddedPython.print("\n" + text + String.format(java.util.Locale.ROOT, "\n(%.1f s)\n", (System.currentTimeMillis() - time) / 1000.0), false);
					PythonShellView.refreshOpenViews();
				} catch (Exception e) {
					String msg = e.getMessage() == null ? e.toString() : e.getMessage();
					EmbeddedPython.print("\n" + title + ": " + msg + "\n", true);
				}
				EmbeddedPython.prompt();
				monitor.done();
				return Status.OK_STATUS;
			}
		};
		job.schedule();
	}

	/* The module of the analysis (once) */
	private static void load(PySession py) throws PyException, java.io.IOException {
		PyValue loaded = py.eval("'_bio7ia' in __import__('sys').modules");
		if (loaded instanceof PyValue.Array l && l.getDouble(0) != 0) {
			return;
		}
		String source;
		try (InputStream in = PythonImageAnalysis.class.getResourceAsStream("python_imageanalysis.py")) {
			source = new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
		py.assign("_bio7_src", PyValue.of(source));
		py.exec("import types as _t, sys as _s\n_m = _t.ModuleType('_bio7ia')\nexec(compile(_bio7_src, '<_bio7ia>', 'exec'), _m.__dict__)\n"
				+ "_s.modules['_bio7ia'] = _m\ndel _t, _s, _m, _bio7_src");
	}
}
