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

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.handlers.HandlerUtil;

import com.eco.bio7.gdal.Gdal;
import com.eco.bio7.gdal.ui.GdalActivation;

import ij.ImagePlus;
import ij.WindowManager;
import ij.plugin.frame.RoiManager;

/**
 * The commands of GDAL (menu File > GDAL and the context menu of files): the
 * file of the selection (Project Explorer) or of a file dialog. GDAL is loaded
 * when it is used (activated in the preferences, a new library is tested
 * first).
 */
public class GdalHandler extends AbstractHandler {

	public static final String OPEN_IMAGEJ = "com.eco.bio7.gdal.openImageJ", EXPORT_GEOTIFF = "com.eco.bio7.gdal.exportGeoTiff",
			VECTOR_ROIS = "com.eco.bio7.gdal.vectorRois", RASTER_WORLDWIND = "com.eco.bio7.gdal.rasterWorldWind",
			VECTOR_WORLDWIND = "com.eco.bio7.gdal.vectorWorldWind", INFO = "com.eco.bio7.gdal.info", PREFERENCES = "com.eco.bio7.gdal.openPreferences";

	@Override
	public Object execute(ExecutionEvent event) {
		Shell shell = HandlerUtil.getActiveShell(event);
		String id = event.getCommand().getId();
		if (PREFERENCES.equals(id)) {
			GdalActivation.openPreferences(shell);
			return null;
		}
		if (!GdalActivation.ensureLoaded(shell)) {
			return null;
		}
		switch (id) {
		case OPEN_IMAGEJ -> {
			Path file = file(event, shell, "Open a raster with GDAL", SWT.OPEN);
			if (file != null) {
				run("Open " + file.getFileName(), shell, () -> {
					ImagePlus imp = GdalImageJ.open(file);
					Display.getDefault().syncExec(imp::show);
				});
			}
		}
		case EXPORT_GEOTIFF -> {
			ImagePlus imp = WindowManager.getCurrentImage();
			if (imp == null) {
				MessageDialog.openInformation(shell, "GDAL", "No ImageJ image is open.");
				return null;
			}
			FileDialog fd = new FileDialog(shell, SWT.SAVE);
			fd.setText("Export as GeoTIFF");
			fd.setFilterExtensions(new String[] { "*.tif;*.tiff" });
			fd.setFileName(imp.getTitle().replaceAll("\\.[^.]*$", "") + ".tif");
			fd.setOverwrite(true);
			String f = fd.open();
			if (f != null) {
				run("Export " + imp.getTitle(), shell, () -> GdalImageJ.exportGeoTiff(imp, Path.of(f)));
			}
		}
		case VECTOR_ROIS -> {
			ImagePlus imp = WindowManager.getCurrentImage();
			if (imp == null || imp.getProperty(GdalImageJ.GEOTRANSFORM) == null) {
				MessageDialog.openInformation(shell, "GDAL", "Please open a georeferenced raster with GDAL first (File > GDAL > Open Raster in ImageJ).");
				return null;
			}
			Path file = file(event, shell, "Vectors as ROIs of " + imp.getTitle(), SWT.OPEN);
			if (file != null) {
				run("ROIs of " + file.getFileName(), shell, () -> {
					int[] n = new int[1];
					Display.getDefault().syncExec(() -> {
						RoiManager rm = RoiManager.getInstance2() != null ? RoiManager.getInstance2() : new RoiManager();
						n[0] = GdalImageJ.addVectorRois(file, imp, rm);
						rm.runCommand(imp, "Show All");
					});
					System.out.println("GDAL: " + n[0] + " ROIs of " + file.getFileName());
				});
			}
		}
		case RASTER_WORLDWIND -> {
			Path file = file(event, shell, "Add a raster to WorldWind", SWT.OPEN);
			if (file != null) {
				run("WorldWind: " + file.getFileName(), shell, () -> GdalWorldWind.addRaster(file));
			}
		}
		case VECTOR_WORLDWIND -> {
			Path file = file(event, shell, "Add vectors to WorldWind", SWT.OPEN);
			if (file != null) {
				run("WorldWind: " + file.getFileName(), shell, () -> {
					int n = GdalWorldWind.addVector(file);
					System.out.println("GDAL: " + n + " features of " + file.getFileName() + " in WorldWind");
				});
			}
		}
		case INFO -> {
			Path file = file(event, shell, "GDAL Info", SWT.OPEN);
			if (file != null) {
				run("GDAL Info", shell, () -> {
					String text;
					try (var ds = Gdal.open(file.toString())) {
						text = ds.bandCount() > 0 ? ds.info() : vectorInfo(ds);
					}
					String t = text;
					Display.getDefault().asyncExec(() -> new TextDialog(shell, "GDAL Info: " + file.getFileName(), t).open());
				});
			}
		}
		default -> {
		}
		}
		return null;
	}

	private static String vectorInfo(com.eco.bio7.gdal.GdalDataset ds) {
		StringBuilder s = new StringBuilder("Driver: " + ds.driverName() + "\n");
		for (var l : ds.layers()) {
			s.append("\nLayer: ").append(l.name()).append("\nFeatures: ").append(l.featureCount()).append("\nFields: ").append(l.fieldNames());
			try (var srs = l.spatialReference()) {
				s.append("\nCoordinate system: ").append(srs == null ? "unknown" : srs.toString());
			}
			double[] e = l.extent();
			if (e != null) {
				s.append("\nExtent: ").append(java.util.Arrays.toString(e));
			}
			s.append('\n');
		}
		return s.toString();
	}

	@FunctionalInterface
	private interface Work {
		void run() throws Exception;
	}

	/* In a job: an error is shown in a dialog */
	private static void run(String name, Shell shell, Work work) {
		Job job = new Job("GDAL: " + name) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				monitor.beginTask(name, IProgressMonitor.UNKNOWN);
				try {
					work.run();
				} catch (Exception | LinkageError e) {
					String msg = e.getMessage() == null ? e.toString() : e.getMessage();
					Display.getDefault().asyncExec(() -> MessageDialog.openError(shell, "GDAL", msg));
				} finally {
					monitor.done();
				}
				return Status.OK_STATUS;
			}
		};
		job.setUser(true);
		job.schedule();
	}

	/* The file of the selection (a file of the workspace) or of a file dialog */
	private static Path file(ExecutionEvent event, Shell shell, String title, int style) {
		ISelection sel = HandlerUtil.getCurrentSelection(event);
		if (sel instanceof IStructuredSelection ss && ss.getFirstElement() instanceof IAdaptable a) {
			IFile f = a.getAdapter(IFile.class);
			if (f != null && f.getLocation() != null && "popup".equals(event.getParameter("source"))) {
				return f.getLocation().toFile().toPath();
			}
		}
		FileDialog fd = new FileDialog(shell, style);
		fd.setText(title);
		String f = fd.open();
		return f == null ? null : Path.of(f);
	}

	/* A dialog with a long text (the output of gdalinfo) */
	private static final class TextDialog extends Dialog {
		private final String title, text;

		TextDialog(Shell shell, String title, String text) {
			super(shell);
			this.title = title;
			this.text = text;
			setShellStyle(getShellStyle() | SWT.RESIZE);
		}

		@Override
		protected void configureShell(Shell s) {
			super.configureShell(s);
			s.setText(title);
		}

		@Override
		protected Control createDialogArea(Composite parent) {
			Composite c = (Composite) super.createDialogArea(parent);
			Text t = new Text(c, SWT.MULTI | SWT.READ_ONLY | SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
			t.setText(text);
			t.setFont(org.eclipse.jface.resource.JFaceResources.getTextFont());
			com.eco.bio7.util.Util.setReadableTextColors(t);
			GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
			gd.widthHint = 700;
			gd.heightHint = 500;
			t.setLayoutData(gd);
			return c;
		}

		@Override
		protected void createButtonsForButtonBar(Composite parent) {
			createButton(parent, IDialogConstants.OK_ID, IDialogConstants.CLOSE_LABEL, true);
		}
	}
}
