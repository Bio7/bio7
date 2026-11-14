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
package com.eco.bio7.rbridge.debug;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.rbridge.Bio7Grid;
import com.eco.bio7.rbridge.RTable;

import ij.ImagePlus;
import ij.gui.Plot;
import ij.process.FloatProcessor;

/**
 * Opens a variable of a debug session (written by .bio7dbg$export, see
 * DebugSession): a data frame or a character matrix in a new sheet of the Bio7
 * table, a numeric matrix as an image in ImageJ, a numeric vector as a plot,
 * anything else as text in the debug text view.
 */
final class DebugVariableExport {

	private DebugVariableExport() {
	}

	/* A table larger than this is not opened (the sheet would be slow) */
	private static final long MAX_CELLS = 2_000_000;

	static void open(String name, String kind, Path file) {
		try {
			switch (kind) {
			case "table" -> table(name, file);
			case "image" -> image(name, file);
			case "plot" -> plot(name, file);
			default -> text(name, file);
			}
		} catch (IOException | RuntimeException e) {
			Display.getDefault().asyncExec(() -> Bio7Dialog.message("The variable " + name + " could not be opened:\n" + e.getMessage()));
		} finally {
			try {
				Files.deleteIfExists(file);
			} catch (IOException e) {
				/* deleted on exit */
			}
		}
	}

	private static void table(String name, Path file) throws IOException {
		List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
		if (lines.isEmpty()) {
			return;
		}
		int columns = lines.get(0).split("\t", -1).length;
		if ((long) columns * lines.size() > MAX_CELLS) {
			throw new IOException("too large for the table (" + lines.size() + " x " + columns + ")");
		}
		String[][] values = new String[lines.size()][columns];
		for (int i = 0; i < lines.size(); i++) {
			String[] p = lines.get(i).split("\t", -1);
			for (int j = 0; j < columns; j++) {
				values[i][j] = j < p.length ? p[j] : "";
			}
		}
		Display.getDefault().syncExec(() -> {
			try {
				PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(RTable.ID);
			} catch (PartInitException | RuntimeException e) {
				/* The table is not available */
			}
		});
		if (RTable.getGrid() == null) {
			throw new IOException("the Bio7 table is not available");
		}
		Bio7Grid.createSheet(columns, values.length, name);
		Bio7Grid.setValues(values);
	}

	private static void image(String name, Path file) throws IOException {
		try (DataInputStream in = new DataInputStream(new java.io.BufferedInputStream(Files.newInputStream(file)))) {
			int rows = in.readInt(), cols = in.readInt();
			float[] pixels = readFloats(in, rows * cols);
			/* R: rows x columns, the image: width = columns, height = rows (the matrix as printed) */
			ImagePlus imp = new ImagePlus(name, new FloatProcessor(cols, rows, pixels));
			Display.getDefault().asyncExec(imp::show);
		}
	}

	private static void plot(String name, Path file) throws IOException {
		try (DataInputStream in = new DataInputStream(new java.io.BufferedInputStream(Files.newInputStream(file)))) {
			int n = in.readInt();
			float[] y = readFloats(in, n);
			float[] x = new float[n];
			for (int i = 0; i < n; i++) {
				x[i] = i + 1;
			}
			Plot plot = new Plot(name, "Index", name);
			plot.add(n > 200 ? "line" : "connected circle", toDoubles(x), toDoubles(y));
			plot.setLimitsToFit(false);
			Display.getDefault().asyncExec(plot::show);
		}
	}

	private static void text(String name, Path file) throws IOException {
		String s = Files.readString(file, StandardCharsets.UTF_8);
		DebugProgress.printToTextView("▶ " + name + ":\n" + s);
	}

	private static float[] readFloats(InputStream in, int n) throws IOException {
		float[] f = new float[n];
		DataInputStream d = (DataInputStream) in;
		for (int i = 0; i < n; i++) {
			f[i] = d.readFloat();
		}
		return f;
	}

	private static double[] toDoubles(float[] f) {
		double[] d = new double[f.length];
		for (int i = 0; i < f.length; i++) {
			d[i] = f[i];
		}
		return d;
	}
}
