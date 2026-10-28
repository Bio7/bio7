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
package org.eclipse.nebula.widgets.grid;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * Added for Bio7: the colors of the headers (columns, rows and the corner) of
 * all grids, e.g., set by a custom theme. Without colors the headers are
 * painted like before (the colors of the shell and the 3D lines of the system).
 * With colors the headers are flat with one line at the right and the bottom.
 */
public final class GridHeaderColors {

	private static Color background;
	private static Color foreground;
	private static Color line;
	private static Color cellLine;

	private GridHeaderColors() {
	}

	/**
	 * Sets the header colors of all grids and redraws the open grids.
	 *
	 * @param back the background or null for the default colors
	 * @param text the text color
	 * @param lines the color of the lines between the headers
	 * @param cells the background of the cells (for the lines between the cells)
	 */
	public static void setColors(RGB back, RGB text, RGB lines, RGB cells) {
		Display display = Display.getDefault();
		if (back == null || text == null || lines == null) {
			background = foreground = line = cellLine = null;
		} else {
			background = new Color(display, back);
			foreground = new Color(display, text);
			line = new Color(display, lines);
			/* Between the lines of the headers and the background of the cells (the lines are not too strong) */
			cellLine = new Color(display, mix(lines, cells, 0.5));
		}
		for (Shell shell : display.getShells()) {
			redrawGrids(shell);
		}
	}

	private static RGB mix(RGB a, RGB b, double f) {
		if (b == null) {
			return a;
		}
		return new RGB((int) Math.round(a.red * (1 - f) + b.red * f), (int) Math.round(a.green * (1 - f) + b.green * f),
				(int) Math.round(a.blue * (1 - f) + b.blue * f));
	}

	private static void redrawGrids(Control control) {
		if (control.isDisposed()) {
			return;
		}
		if (control instanceof Grid) {
			control.redraw();
		}
		if (control instanceof org.eclipse.swt.widgets.Composite) {
			for (Control child : ((org.eclipse.swt.widgets.Composite) control).getChildren()) {
				redrawGrids(child);
			}
		}
	}

	/** True if the headers are painted with the colors (flat). */
	public static boolean isSet() {
		return background != null;
	}

	/** The background of the headers or the default color. */
	public static Color background(Color fallback) {
		return background != null ? background : fallback;
	}

	/** The text color of the headers or the default color. */
	public static Color foreground(Color fallback) {
		return foreground != null ? foreground : fallback;
	}

	/** The color of the lines between the cells (if no own color was set) or the default color. */
	public static Color cellLine(Color fallback) {
		return cellLine != null ? cellLine : fallback;
	}

	/**
	 * The color of a shadow line of a header (a system color, e.g.,
	 * SWT.COLOR_WIDGET_DARK_SHADOW): with colors the dark shadow is the line
	 * color, the other lines (the 3D effect) have the background color.
	 */
	public static Color shadow(Display display, int systemColor) {
		if (line == null) {
			return display.getSystemColor(systemColor);
		}
		return systemColor == SWT.COLOR_WIDGET_DARK_SHADOW ? line : background;
	}
}
