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

import java.awt.image.ColorModel;
import java.util.function.Consumer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/**
 * An editor for a transfer function (like the color map editor of ParaView):
 * <ul>
 * <li>Opacity: the opacity curve above the histogram of the data. Click to add
 * a point, drag a point to move it, right click a point to remove it.</li>
 * <li>Colors: the color bar with the color points (triangles). Click the bar to
 * add a point, drag a point to move it, double click a point to select its
 * color, right click a point to remove it.</li>
 * <li>Presets, reverse colors, linear or constant opacity.</li>
 * </ul>
 * Changes are reported (coalesced) to the change listener.
 */
public class TransferFunctionEditor extends Composite {

	private static final int MARGIN = 8;
	private static final String CUSTOM = "Custom";
	private static final String IMAGE_LUT = "Image LUT";
	private static final int HANDLE = 5;

	private final TransferFunction tf;
	private final Canvas opacityCanvas;
	private final Canvas colorCanvas;
	private final Label infoLabel;
	private Combo presets;
	private int[] histogram;
	private double dataMin = Double.NaN, dataMax = Double.NaN;
	private ColorModel imageLut;
	private double marker = Double.NaN;
	private String markerLabel;
	private Consumer<TransferFunction> changeListener;
	private boolean changePending;

	private TransferFunction.OpacityPoint selectedOpacity;
	private TransferFunction.ColorPoint selectedColor;
	private boolean dragging;

	/**
	 * Creates the editor for a transfer function (edited directly).
	 *
	 * @param parent the parent composite
	 * @param tf     the transfer function
	 */
	public TransferFunctionEditor(Composite parent, TransferFunction tf) {
		super(parent, SWT.NONE);
		this.tf = tf;
		setLayout(new GridLayout(5, false));

		opacityCanvas = new Canvas(this, SWT.DOUBLE_BUFFERED | SWT.BORDER);
		GridData gdOpacity = new GridData(SWT.FILL, SWT.FILL, true, true, 5, 1);
		gdOpacity.heightHint = 160;
		gdOpacity.widthHint = 420;
		opacityCanvas.setLayoutData(gdOpacity);
		opacityCanvas.setToolTipText(
				"Opacity: click to add a point, drag a point to move it, right click a point to remove it");
		opacityCanvas.addListener(SWT.Paint, e -> paintOpacity(e.gc));
		opacityCanvas.addListener(SWT.MouseDown, this::opacityMouseDown);
		opacityCanvas.addListener(SWT.MouseMove, this::opacityMouseMove);
		opacityCanvas.addListener(SWT.MouseUp, e -> dragging = false);

		colorCanvas = new Canvas(this, SWT.DOUBLE_BUFFERED | SWT.BORDER);
		GridData gdColor = new GridData(SWT.FILL, SWT.FILL, true, false, 5, 1);
		gdColor.heightHint = 52;
		colorCanvas.setLayoutData(gdColor);
		colorCanvas.setToolTipText(
				"Colors: click to add a point, drag a point to move it, double click a point to select the color, right click a point to remove it");
		colorCanvas.addListener(SWT.Paint, e -> paintColors(e.gc));
		colorCanvas.addListener(SWT.MouseDown, this::colorMouseDown);
		colorCanvas.addListener(SWT.MouseMove, this::colorMouseMove);
		colorCanvas.addListener(SWT.MouseUp, e -> dragging = false);
		colorCanvas.addListener(SWT.MouseDoubleClick, this::colorDoubleClick);

		presets = new Combo(this, SWT.READ_ONLY);
		presets.setToolTipText("Sets the colors of a preset (the opacities are kept)");
		updatePresets();
		presets.addListener(SWT.Selection, e -> {
			String name = presets.getItem(presets.getSelectionIndex());
			if (name.equals(IMAGE_LUT)) {
				tf.setColors(imageLut);
			} else if (!name.equals(CUSTOM)) {
				tf.setPreset(name);
			}
			selectedColor = null;
			changed();
		});

		Button reverse = new Button(this, SWT.PUSH);
		reverse.setText("Reverse");
		reverse.setToolTipText("Reverses the colors");
		reverse.addListener(SWT.Selection, e -> {
			tf.reverseColors();
			changed();
		});

		Button linear = new Button(this, SWT.PUSH);
		linear.setText("Linear Opacity");
		linear.setToolTipText("A linear opacity ramp from 0 (minimum) to 1 (maximum)");
		linear.addListener(SWT.Selection, e -> {
			tf.setLinearOpacity();
			selectedOpacity = null;
			changed();
		});

		Button constant = new Button(this, SWT.PUSH);
		constant.setText("Constant Opacity");
		constant.setToolTipText("A constant opacity of 0.5");
		constant.addListener(SWT.Selection, e -> {
			tf.setConstantOpacity(0.5);
			selectedOpacity = null;
			changed();
		});

		infoLabel = new Label(this, SWT.NONE);
		infoLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		updateInfo();
	}

	/** The histogram of the data (256 bins, displayed logarithmically) or null. */
	public void setHistogram(int[] histogram) {
		this.histogram = histogram;
		opacityCanvas.redraw();
	}

	/** The data values of the positions 0 and 1 (for the info of a point). */
	public void setDataRange(double min, double max) {
		dataMin = min;
		dataMax = max;
		updateInfo();
	}

	/**
	 * A marker (a dashed line) at the position x (0 - 1), e.g., the iso value. NaN
	 * removes the marker.
	 */
	public void setMarker(double x, String label) {
		marker = x;
		markerLabel = label;
		opacityCanvas.redraw();
	}

	/** The LUT of the image (for the preset "Image LUT", e.g., no LUT for RGB images). */
	public void setImageLut(ColorModel lut) {
		imageLut = lut;
		updatePresets();
	}

	/*
	 * The presets ("Image LUT" only with a LUT) with the name of the current
	 * colors selected ("Custom" for edited colors)!
	 */
	private void updatePresets() {
		if (presets == null || presets.isDisposed()) {
			return;
		}
		presets.removeAll();
		presets.add(CUSTOM);
		if (imageLut != null) {
			presets.add(IMAGE_LUT);
		}
		for (String name : TransferFunction.PRESETS) {
			presets.add(name);
		}
		String name = tf.getColorName();
		int index = name != null ? presets.indexOf(name) : -1;
		presets.select(Math.max(0, index));
	}

	/** A listener which is called (coalesced, in the UI thread) after changes. */
	public void setChangeListener(Consumer<TransferFunction> listener) {
		changeListener = listener;
	}

	public TransferFunction getTransferFunction() {
		return tf;
	}

	/* Redraws and reports the change (at most every 80 ms)! */
	private void changed() {
		opacityCanvas.redraw();
		colorCanvas.redraw();
		updateInfo();
		// The combo shows the preset (or "Custom" after a change of the colors)
		String name = tf.getColorName();
		int index = name != null ? presets.indexOf(name) : 0;
		if (index >= 0 && presets.getSelectionIndex() != index) {
			presets.select(index);
		}
		if (changePending) {
			return;
		}
		changePending = true;
		getDisplay().timerExec(80, () -> {
			changePending = false;
			if (!isDisposed() && changeListener != null) {
				changeListener.accept(tf);
			}
		});
	}

	private void updateInfo() {
		if (infoLabel == null || infoLabel.isDisposed()) {
			return;
		}
		String text = "";
		if (selectedOpacity != null) {
			text = "Position " + format(selectedOpacity.x) + dataValue(selectedOpacity.x) + ", opacity "
					+ String.format("%.2f", selectedOpacity.a);
		} else if (selectedColor != null) {
			text = "Position " + format(selectedColor.x) + dataValue(selectedColor.x);
		} else if (!Double.isNaN(dataMin)) {
			text = "Range " + String.format("%.4g", dataMin) + " - " + String.format("%.4g", dataMax);
		}
		infoLabel.setText(text);
		layout();
	}

	private static String format(double v) {
		return String.format("%.3f", v);
	}

	private String dataValue(double x) {
		return Double.isNaN(dataMin) ? "" : " (value " + String.format("%.4g", dataMin + x * (dataMax - dataMin)) + ")";
	}

	/* Opacity canvas */

	private Rectangle plotArea(Canvas canvas) {
		Rectangle r = canvas.getClientArea();
		return new Rectangle(MARGIN, MARGIN, Math.max(1, r.width - 2 * MARGIN), Math.max(1, r.height - 2 * MARGIN));
	}

	private void paintOpacity(GC gc) {
		Rectangle area = plotArea(opacityCanvas);
		gc.setAntialias(SWT.ON);
		// The histogram (logarithmic)
		if (histogram != null) {
			double max = 0;
			for (int v : histogram) {
				max = Math.max(max, Math.log1p(v));
			}
			gc.setBackground(new Color(170, 170, 170));
			for (int i = 0; i < 256; i++) {
				if (histogram[i] == 0 || max == 0) {
					continue;
				}
				int x0 = area.x + (int) (i / 256.0 * area.width);
				int x1 = area.x + (int) ((i + 1) / 256.0 * area.width);
				int h = (int) (Math.log1p(histogram[i]) / max * area.height);
				gc.fillRectangle(x0, area.y + area.height - h, Math.max(1, x1 - x0), h);
			}
		}
		// The colors of the function as a filled area below the opacity curve
		for (int px = 0; px < area.width; px++) {
			double x = px / (double) (area.width - 1);
			double[] c = tf.colorAt(x);
			int h = (int) (tf.opacityAt(x) * area.height);
			gc.setForeground(new Color((int) (c[0] * 255), (int) (c[1] * 255), (int) (c[2] * 255)));
			gc.setAlpha(110);
			gc.drawLine(area.x + px, area.y + area.height, area.x + px, area.y + area.height - h);
		}
		gc.setAlpha(255);
		// The marker (e.g., the iso value)
		if (!Double.isNaN(marker)) {
			int mx = area.x + (int) Math.round(marker * area.width);
			gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_DARK_RED));
			gc.setLineStyle(SWT.LINE_DASH);
			gc.drawLine(mx, area.y, mx, area.y + area.height);
			gc.setLineStyle(SWT.LINE_SOLID);
			if (markerLabel != null) {
				gc.drawText(markerLabel, mx + 3, area.y, true);
			}
		}
		// The opacity curve and the points
		gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_FOREGROUND));
		gc.setLineWidth(2);
		int[] points = new int[tf.getOpacityPoints().size() * 2];
		int k = 0;
		for (TransferFunction.OpacityPoint p : tf.getOpacityPoints()) {
			points[k++] = area.x + (int) Math.round(p.x * area.width);
			points[k++] = area.y + (int) Math.round((1 - p.a) * area.height);
		}
		gc.drawPolyline(points);
		gc.setLineWidth(1);
		for (TransferFunction.OpacityPoint p : tf.getOpacityPoints()) {
			int x = area.x + (int) Math.round(p.x * area.width);
			int y = area.y + (int) Math.round((1 - p.a) * area.height);
			gc.setBackground(getDisplay().getSystemColor(p == selectedOpacity ? SWT.COLOR_RED : SWT.COLOR_WHITE));
			gc.fillRectangle(x - HANDLE, y - HANDLE, 2 * HANDLE, 2 * HANDLE);
			gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_BLACK));
			gc.drawRectangle(x - HANDLE, y - HANDLE, 2 * HANDLE, 2 * HANDLE);
		}
	}

	private TransferFunction.OpacityPoint opacityHit(int mx, int my) {
		Rectangle area = plotArea(opacityCanvas);
		for (TransferFunction.OpacityPoint p : tf.getOpacityPoints()) {
			int x = area.x + (int) Math.round(p.x * area.width);
			int y = area.y + (int) Math.round((1 - p.a) * area.height);
			if (Math.abs(mx - x) <= HANDLE + 2 && Math.abs(my - y) <= HANDLE + 2) {
				return p;
			}
		}
		return null;
	}

	private void opacityMouseDown(Event e) {
		Rectangle area = plotArea(opacityCanvas);
		TransferFunction.OpacityPoint hit = opacityHit(e.x, e.y);
		selectedColor = null;
		if (e.button == 3) {
			if (hit != null && !isEnd(hit)) {
				tf.removeOpacityPoint(hit);
				selectedOpacity = null;
				changed();
			}
			return;
		}
		if (e.button != 1) {
			return;
		}
		if (hit == null) {
			double x = clamp((e.x - area.x) / (double) area.width);
			double a = clamp(1 - (e.y - area.y) / (double) area.height);
			hit = tf.addOpacityPoint(x, a);
		}
		selectedOpacity = hit;
		dragging = true;
		changed();
	}

	private void opacityMouseMove(Event e) {
		if (!dragging || selectedOpacity == null) {
			return;
		}
		Rectangle area = plotArea(opacityCanvas);
		// The end points keep their positions (only the opacity changes)
		if (!isEnd(selectedOpacity)) {
			selectedOpacity.x = clamp((e.x - area.x) / (double) area.width);
		}
		selectedOpacity.a = clamp(1 - (e.y - area.y) / (double) area.height);
		tf.sortOpacities();
		changed();
	}

	private boolean isEnd(TransferFunction.OpacityPoint p) {
		java.util.List<TransferFunction.OpacityPoint> list = tf.getOpacityPoints();
		return p == list.get(0) || p == list.get(list.size() - 1);
	}

	/* Color canvas */

	private static final int BAR = 26;

	private void paintColors(GC gc) {
		Rectangle area = plotArea(colorCanvas);
		for (int px = 0; px < area.width; px++) {
			double[] c = tf.colorAt(px / (double) (area.width - 1));
			gc.setForeground(new Color((int) (c[0] * 255), (int) (c[1] * 255), (int) (c[2] * 255)));
			gc.drawLine(area.x + px, area.y, area.x + px, area.y + BAR);
		}
		gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_BLACK));
		gc.drawRectangle(area.x, area.y, area.width - 1, BAR);
		gc.setAntialias(SWT.ON);
		for (TransferFunction.ColorPoint p : tf.getColorPoints()) {
			int x = area.x + (int) Math.round(p.x * area.width);
			int y = area.y + BAR + 2;
			int[] triangle = { x, y, x - 6, y + 11, x + 6, y + 11 };
			gc.setBackground(new Color((int) (p.r * 255), (int) (p.g * 255), (int) (p.b * 255)));
			gc.fillPolygon(triangle);
			gc.setForeground(getDisplay().getSystemColor(p == selectedColor ? SWT.COLOR_RED : SWT.COLOR_BLACK));
			gc.setLineWidth(p == selectedColor ? 2 : 1);
			gc.drawPolygon(triangle);
		}
		gc.setLineWidth(1);
	}

	private TransferFunction.ColorPoint colorHit(int mx, int my) {
		Rectangle area = plotArea(colorCanvas);
		if (my < area.y + BAR) {
			return null;
		}
		for (TransferFunction.ColorPoint p : tf.getColorPoints()) {
			int x = area.x + (int) Math.round(p.x * area.width);
			if (Math.abs(mx - x) <= 7) {
				return p;
			}
		}
		return null;
	}

	private void colorMouseDown(Event e) {
		Rectangle area = plotArea(colorCanvas);
		TransferFunction.ColorPoint hit = colorHit(e.x, e.y);
		selectedOpacity = null;
		if (e.button == 3) {
			if (hit != null) {
				tf.removeColorPoint(hit);
				selectedColor = null;
				changed();
			}
			return;
		}
		if (e.button != 1) {
			return;
		}
		if (hit == null && e.y <= area.y + BAR) {
			// A click on the bar adds a point with the interpolated color
			hit = tf.addColorPoint(clamp((e.x - area.x) / (double) area.width));
		}
		selectedColor = hit;
		dragging = hit != null;
		changed();
	}

	private void colorMouseMove(Event e) {
		if (!dragging || selectedColor == null) {
			return;
		}
		Rectangle area = plotArea(colorCanvas);
		selectedColor.x = clamp((e.x - area.x) / (double) area.width);
		tf.sortColors();
		tf.setColorsEdited();
		changed();
	}

	private void colorDoubleClick(Event e) {
		TransferFunction.ColorPoint hit = colorHit(e.x, e.y);
		if (hit == null) {
			return;
		}
		dragging = false;
		ColorDialog dialog = new ColorDialog(getShell());
		dialog.setRGB(new RGB((int) (hit.r * 255), (int) (hit.g * 255), (int) (hit.b * 255)));
		RGB rgb = dialog.open();
		if (rgb != null) {
			hit.r = rgb.red / 255.0;
			hit.g = rgb.green / 255.0;
			hit.b = rgb.blue / 255.0;
			tf.setColorsEdited();
			changed();
		}
	}

	private static double clamp(double v) {
		return Math.max(0, Math.min(1, v));
	}

	/**
	 * Opens a non-modal dialog with an editor for the transfer function. The
	 * changes are applied directly with the listener.
	 *
	 * @param parent    the parent shell
	 * @param title     the title of the dialog
	 * @param tf        the transfer function (edited directly)
	 * @param histogram the histogram of the data (256 bins) or null
	 * @param min       the data value of the position 0 (or NaN)
	 * @param max       the data value of the position 1 (or NaN)
	 * @param imageLut  the LUT of the image or null
	 * @param apply     the listener which applies the changes
	 * @return the shell of the dialog
	 */
	public static Shell openDialog(Shell parent, String title, TransferFunction tf, int[] histogram, double min,
			double max, ColorModel imageLut, Consumer<TransferFunction> apply) {
		return openDialog(parent, title, tf, histogram, min, max, imageLut, Double.NaN, null, apply);
	}

	/**
	 * Opens a non-modal dialog with an editor for the transfer function and a
	 * marker (see setMarker).
	 */
	public static Shell openDialog(Shell parent, String title, TransferFunction tf, int[] histogram, double min,
			double max, ColorModel imageLut, double marker, String markerLabel, Consumer<TransferFunction> apply) {
		Shell shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MODELESS);
		shell.setText(title);
		shell.setLayout(new GridLayout(1, false));
		TransferFunctionEditor editor = new TransferFunctionEditor(shell, tf);
		editor.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		editor.setHistogram(histogram);
		editor.setDataRange(min, max);
		editor.setImageLut(imageLut);
		editor.setMarker(marker, markerLabel);
		editor.setChangeListener(apply);
		Button close = new Button(shell, SWT.PUSH);
		close.setText("Close");
		close.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
		close.addListener(SWT.Selection, e -> shell.close());
		shell.pack();
		Rectangle p = parent.getBounds();
		Rectangle s = shell.getBounds();
		shell.setLocation(p.x + (p.width - s.width) / 2, p.y + (p.height - s.height) / 3);
		shell.open();
		return shell;
	}
}
