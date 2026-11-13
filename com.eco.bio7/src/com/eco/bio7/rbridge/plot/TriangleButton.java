/*******************************************************************************
 * Copyright (c) 2007-2026 M. Austenfeld
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/
package com.eco.bio7.rbridge.plot;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageDataProvider;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

/**
 * A push button with a triangle (previous or next plot of the history) as an
 * icon in the text color of the button.
 * <p>
 * The characters "◀" and "▶" are drawn by macOS with different fonts (also
 * emoji variants): the buttons had different sizes and the triangles sat at
 * different heights. The icons have the same size, are antialiased for each
 * zoom and are created again if a theme (CSS) changes the text color.
 */
final class TriangleButton {

	private TriangleButton() {
	}

	/**
	 * Creates the button.
	 *
	 * @param left true: a triangle to the left (previous), else to the right (next)
	 */
	static Button create(Composite parent, boolean left, String tip, Runnable action) {
		Button b = new Button(parent, SWT.PUSH);
		b.setToolTipText(tip);
		/*
		 * macOS draws a push button with an image (no text) with another bezel. A font
		 * gives it the bezel (and the height) of the buttons with a text!
		 */
		b.setFont(b.getFont());
		Image[] image = new Image[1];
		RGB[] color = new RGB[1];
		Runnable update = () -> {
			if (b.isDisposed()) {
				return;
			}
			RGB now = b.getForeground().getRGB();
			if (image[0] != null && now.equals(color[0])) {
				return;
			}
			Image old = image[0];
			color[0] = now;
			image[0] = new Image(b.getDisplay(), icon(left, now));
			b.setImage(image[0]);
			if (old != null) {
				old.dispose();
			}
		};
		update.run();
		/* The colors of a theme are set after the creation of the button */
		b.addListener(SWT.Paint, _ -> {
			if (!b.isDisposed() && !b.getForeground().getRGB().equals(color[0])) {
				b.getDisplay().asyncExec(update);
			}
		});
		b.addListener(SWT.Dispose, _ -> {
			if (image[0] != null) {
				image[0].dispose();
			}
		});
		b.addListener(SWT.Selection, _ -> action.run());
		return b;
	}

	/* A triangle pointing to the left or to the right, drawn for each zoom (antialiased) */
	private static ImageDataProvider icon(boolean left, RGB color) {
		return zoom -> {
			int size = Math.max(8, 12 * zoom / 100);
			ImageData data = new ImageData(size, size, 24, new PaletteData(0xFF0000, 0xFF00, 0xFF));
			int pixel = data.palette.getPixel(color);
			int samples = 4;
			for (int y = 0; y < size; y++) {
				for (int x = 0; x < size; x++) {
					data.setPixel(x, y, pixel);
					int inside = 0;
					for (int sy = 0; sy < samples; sy++) {
						for (int sx = 0; sx < samples; sx++) {
							double px = (x + (sx + 0.5) / samples) / size;
							double py = (y + (sy + 0.5) / samples) / size;
							/* The triangle (0.25, 0.12), (0.25, 0.88), (0.85, 0.5), mirrored for the left one */
							double tx = left ? 1 - px : px;
							if (tx >= 0.25 && Math.abs(py - 0.5) <= 0.38 * (0.85 - tx) / 0.6) {
								inside++;
							}
						}
					}
					data.setAlpha(x, y, 255 * inside / (samples * samples));
				}
			}
			return data;
		};
	}
}
