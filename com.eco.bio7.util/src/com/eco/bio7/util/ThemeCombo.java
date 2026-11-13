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
package com.eco.bio7.util;

import java.lang.reflect.Field;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

/**
 * A CCombo (the colors of the themes, a read-only Combo is a native popup
 * button on macOS which is not colored) with the height of a text field.
 * <p>
 * On macOS the preferred height of CCombo is the height of its text without a
 * padding (the text is cut). The preferred height is the height of the font
 * with a padding like a text field (the arrow is laid out in this height).
 */
public class ThemeCombo extends CCombo {

	public ThemeCombo(Composite parent, int style) {
		super(parent, style);
		/* After the layout of CCombo (its resize listener comes first): the text in the vertical center */
		addListener(SWT.Resize, _ -> centerText());
		/* The arrow in the colors of the combo (the native arrow button is dark, not visible in a dark theme) */
		if (field("arrow") instanceof Button arrow) {
			arrow.addListener(SWT.Paint, e -> paintArrow(e.gc, arrow));
		}
	}

	/*
	 * Over the native arrow button (the paint event comes after it): the background
	 * of the combo, a line to the text and a small triangle in the text color
	 */
	private void paintArrow(GC gc, Button arrow) {
		Point size = arrow.getSize();
		if (size.x <= 0 || size.y <= 0) {
			return;
		}
		Color background = getBackground();
		Color foreground = getForeground();
		gc.setBackground(background);
		gc.fillRectangle(0, 0, size.x, size.y);
		gc.setAntialias(SWT.ON);
		/* A faint line between the text and the arrow */
		Color line = new Color(mix(background.getRGB(), foreground.getRGB(), 0.25));
		gc.setForeground(line);
		gc.drawLine(0, 2, 0, size.y - 3);
		line.dispose();
		int w = Math.max(5, Math.min(9, size.x - 6));
		int h = (w + 1) / 2;
		int x = (size.x - w) / 2 + 1;
		int y = (size.y - h) / 2;
		gc.setBackground(isEnabled() ? foreground : new Color(mix(background.getRGB(), foreground.getRGB(), 0.45)));
		gc.fillPolygon(new int[] { x, y, x + w, y, x + w / 2, y + h });
	}

	private static RGB mix(RGB a, RGB b, double f) {
		return new RGB((int) Math.round(a.red * (1 - f) + b.red * f), (int) Math.round(a.green * (1 - f) + b.green * f),
				(int) Math.round(a.blue * (1 - f) + b.blue * f));
	}

	/* CCombo does not allow subclasses (only the height is changed here, see computeSize) */
	@Override
	protected void checkSubclass() {
	}

	/*
	 * The text field of CCombo (CCombo hides its controls, getChildren() is empty):
	 * the private field "text", null if not available
	 */
	private Text textField() {
		return field("text") instanceof Text text && !text.isDisposed() ? text : null;
	}

	/* A private field of CCombo (its controls "text", "arrow", "list"), null if not available */
	private Object field(String name) {
		try {
			Field field = CCombo.class.getDeclaredField(name);
			field.setAccessible(true);
			return field.get(this);
		} catch (ReflectiveOperationException | RuntimeException e) {
			return null;
		}
	}

	/* The height of the font of the combo with a padding like a text field */
	private int fieldHeight() {
		GC gc = new GC(this);
		try {
			gc.setFont(getFont());
			return gc.getFontMetrics().getHeight() + 6 + 2 * getBorderWidth();
		} finally {
			gc.dispose();
		}
	}

	@Override
	public Point computeSize(int wHint, int hHint, boolean changed) {
		Point size = super.computeSize(wHint, hHint, changed);
		if (hHint == SWT.DEFAULT) {
			/* Also higher than CCombo (macOS: the height of its text without a padding, the text is cut) */
			size.y = Math.max(size.y, fieldHeight());
		}
		return size;
	}

	/*
	 * CCombo gives the text field the whole height, macOS draws the text at the top
	 * of the field: the text field gets the height of its text in the center
	 */
	private void centerText() {
		Text text = textField();
		if (text == null) {
			return;
		}
		Rectangle bounds = text.getBounds();
		int height = Math.min(bounds.height, text.computeSize(SWT.DEFAULT, SWT.DEFAULT).y);
		int y = Math.max(0, (getClientArea().height - height) / 2);
		if (height > 0 && (bounds.y != y || bounds.height != height)) {
			text.setBounds(bounds.x, y, bounds.width, height);
		}
	}
}
