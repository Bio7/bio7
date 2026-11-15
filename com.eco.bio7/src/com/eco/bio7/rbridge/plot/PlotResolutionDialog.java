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
package com.eco.bio7.rbridge.plot;

import java.io.File;
import java.util.Locale;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;

import ij.IJ;
import ij.ImagePlus;
import ij.measure.Calibration;

/**
 * The resolution of a plot transferred to ImageJ (R and Python plot devices):
 * the plot keeps its size in inches (the size on the screen at 96 dpi) and is
 * drawn again by R or matplotlib with more pixels per inch, e.g., 300 dpi for a
 * publication. The ImageJ image is calibrated in inches (a TIFF saved by ImageJ
 * keeps the resolution).
 */
public class PlotResolutionDialog extends Dialog {

	/** The resolution of the plots on the screen (pixels per inch). */
	public static final double SCREEN_DPI = 96;

	private static final int[] PRESETS = { 96, 192, 300, 600 };
	private static final String[] NAMES = { "Screen", "2x (high-resolution screen)", "Publication (print)", "Publication (line art)" };
	/* The last choice (for the next transfer) */
	private static int lastDpi = 300;

	private final int width;
	private final int height;
	private Button[] radios;
	private Button customRadio;
	private Spinner custom;
	private Label sizeLabel;
	private int dpi;

	/**
	 * @param width  the width of the plot on the screen (pixels at 96 dpi)
	 * @param height the height of the plot on the screen
	 */
	public PlotResolutionDialog(Shell parent, int width, int height) {
		super(parent);
		this.width = width;
		this.height = height;
	}

	/**
	 * Asks for the resolution.
	 *
	 * @return the resolution (dpi) or 0 if canceled
	 */
	public static int ask(Shell parent, int width, int height) {
		PlotResolutionDialog d = new PlotResolutionDialog(parent, width, height);
		return d.open() == OK ? d.dpi : 0;
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText("Plot to ImageJ");
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite area = (Composite) super.createDialogArea(parent);
		area.setLayout(new GridLayout(2, false));
		Label info = new Label(area, SWT.WRAP);
		info.setText(String.format(Locale.ROOT, "The plot is drawn again with the resolution (size %.2f x %.2f in, %.1f x %.1f cm):", width / SCREEN_DPI, height / SCREEN_DPI,
				width / SCREEN_DPI * 2.54, height / SCREEN_DPI * 2.54));
		GridData id = new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1);
		id.widthHint = 380;
		info.setLayoutData(id);
		radios = new Button[PRESETS.length];
		for (int i = 0; i < PRESETS.length; i++) {
			radios[i] = new Button(area, SWT.RADIO);
			radios[i].setText(PRESETS[i] + " dpi  -  " + NAMES[i]);
			radios[i].setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
			radios[i].setSelection(PRESETS[i] == lastDpi);
			radios[i].addListener(SWT.Selection, _ -> updateSize());
		}
		customRadio = new Button(area, SWT.RADIO);
		customRadio.setText("Custom (dpi):");
		customRadio.addListener(SWT.Selection, _ -> updateSize());
		custom = new Spinner(area, SWT.BORDER);
		custom.setValues(lastDpi, 48, 2400, 0, 50, 100);
		custom.addListener(SWT.Modify, _ -> {
			customRadio.setSelection(true);
			for (Button r : radios) {
				r.setSelection(false);
			}
			updateSize();
		});
		boolean preset = false;
		for (int p : PRESETS) {
			preset |= p == lastDpi;
		}
		customRadio.setSelection(!preset);
		sizeLabel = new Label(area, SWT.NONE);
		sizeLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		updateSize();
		return area;
	}

	private int selectedDpi() {
		for (int i = 0; i < radios.length; i++) {
			if (radios[i].getSelection()) {
				return PRESETS[i];
			}
		}
		return custom.getSelection();
	}

	private void updateSize() {
		int d = selectedDpi();
		double s = d / SCREEN_DPI;
		sizeLabel.setText(String.format(Locale.ROOT, "Image: %d x %d pixels", Math.round(width * s), Math.round(height * s)));
		sizeLabel.getParent().layout(true);
	}

	@Override
	protected void okPressed() {
		dpi = selectedDpi();
		lastDpi = dpi;
		super.okPressed();
	}

	/**
	 * Opens an image file written by R or matplotlib in ImageJ, calibrated in
	 * inches with the resolution, and deletes the file. Any thread.
	 */
	public static void openInImageJ(File png, String title, int dpi) {
		Display.getDefault().asyncExec(() -> {
			ImagePlus imp = IJ.openImage(png.getAbsolutePath());
			png.delete();
			if (imp == null) {
				IJ.error("The plot image could not be opened");
				return;
			}
			imp.setTitle(title + " (" + dpi + " dpi)");
			Calibration cal = imp.getCalibration();
			cal.setUnit("inch");
			cal.pixelWidth = 1.0 / dpi;
			cal.pixelHeight = 1.0 / dpi;
			imp.show();
		});
	}
}
