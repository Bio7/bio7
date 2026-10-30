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
package com.eco.bio7.themes;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Tree;

/**
 * The selection of the trees and tables without the focus with the color of a
 * custom theme: the native color (e.g., the light grey of macOS) can't be set
 * with CSS and doesn't fit the colors of a theme. The selection with the focus
 * keeps the native color (the accent color of the system).
 */
final class SelectionColors {

	/* A tree or table with the listener of the theme */
	private static final String PAINTED = "bio7.themes.selectionPainted";

	private static Color color;
	private static boolean installed;

	private SelectionColors() {
	}

	/** Sets the color of the selection without the focus (null for the native color). */
	static void apply(RGB rgb) {
		Display display = Display.getDefault();
		install(display);
		color = rgb == null ? null : new Color(display, rgb);
		for (Shell shell : display.getShells()) {
			styleAll(shell);
		}
	}

	/* The trees and tables created later get the listener, too */
	private static void install(Display display) {
		if (installed) {
			return;
		}
		installed = true;
		display.addFilter(SWT.Skin, event -> {
			if (color != null && (event.widget instanceof Tree || event.widget instanceof Table)) {
				style((Control) event.widget);
			}
		});
	}

	private static void styleAll(Control control) {
		if (control.isDisposed()) {
			return;
		}
		if (control instanceof Tree || control instanceof Table) {
			style(control);
		}
		if (control instanceof Composite composite) {
			for (Control child : composite.getChildren()) {
				styleAll(child);
			}
		}
	}

	private static void style(Control control) {
		if (color != null && control.getData(PAINTED) == null) {
			control.setData(PAINTED, Boolean.TRUE);
			control.addListener(SWT.EraseItem, SelectionColors::erase);
			control.addListener(SWT.PaintItem, SelectionColors::paint);
		}
		control.redraw();
	}

	/* The selected item without the focus: the color of the theme instead of the native selection */
	private static void erase(Event event) {
		if (color == null || (event.detail & SWT.SELECTED) == 0) {
			return;
		}
		Control control = (Control) event.widget;
		if (control.isFocusControl()) {
			return;
		}
		Rectangle area = ((org.eclipse.swt.widgets.Scrollable) control).getClientArea();
		/* The whole row (the first column also left of the item, e.g., the expand arrow of a tree) */
		int x = event.index == 0 ? area.x : event.x;
		int width = event.index == 0 ? event.x + event.width - area.x : event.width;
		Color old = event.gc.getBackground();
		event.gc.setBackground(color);
		event.gc.fillRectangle(x, event.y, width, event.height);
		event.gc.setBackground(old);
		/*
		 * The text and the image are drawn in PaintItem (the native text of a selection is
		 * white). A tree or table which paints its items itself (e.g., the styled labels of
		 * the Project Explorer) paints the text (else the text is painted twice)!
		 */
		event.detail &= ownerDrawn(control) ? ~SWT.SELECTED : ~(SWT.SELECTED | SWT.FOREGROUND);
	}

	/* The items are painted by another listener (e.g., the JFace owner draw label providers) */
	private static boolean ownerDrawn(Control control) {
		return control.getListeners(SWT.PaintItem).length > 1;
	}

	/* The image and the text of a selected item without the focus in the text color of the item */
	private static void paint(Event event) {
		if (color == null || !(event.item instanceof org.eclipse.swt.widgets.Item item) || !unfocusedSelection(event)
				|| ownerDrawn((Control) event.widget)) {
			return;
		}
		org.eclipse.swt.graphics.Image image;
		String text;
		Color foreground;
		org.eclipse.swt.graphics.Font font;
		if (item instanceof org.eclipse.swt.widgets.TableItem tableItem) {
			image = tableItem.getImage(event.index);
			text = tableItem.getText(event.index);
			foreground = tableItem.getForeground(event.index);
			font = tableItem.getFont(event.index);
		} else {
			org.eclipse.swt.widgets.TreeItem treeItem = (org.eclipse.swt.widgets.TreeItem) item;
			image = treeItem.getImage(event.index);
			text = treeItem.getText(event.index);
			foreground = treeItem.getForeground(event.index);
			font = treeItem.getFont(event.index);
		}
		org.eclipse.swt.graphics.GC gc = event.gc;
		int x = event.x;
		if (image != null && !image.isDisposed()) {
			Rectangle b = image.getBounds();
			gc.drawImage(image, x, event.y + Math.max(0, (event.height - b.height) / 2));
			x += b.width + 3;
		}
		if (text != null && !text.isEmpty()) {
			gc.setFont(font);
			gc.setForeground(foreground);
			org.eclipse.swt.graphics.Point extent = gc.textExtent(text);
			gc.drawText(text, x, event.y + Math.max(0, (event.height - extent.y) / 2), true);
		}
	}

	/* The item of the event is selected and the tree or table has not the focus */
	private static boolean unfocusedSelection(Event event) {
		Control control = (Control) event.widget;
		if (control.isFocusControl()) {
			return false;
		}
		if (control instanceof Table table && event.item instanceof org.eclipse.swt.widgets.TableItem item) {
			return table.isSelected(table.indexOf(item));
		}
		if (control instanceof Tree tree) {
			for (org.eclipse.swt.widgets.TreeItem selected : tree.getSelection()) {
				if (selected == event.item) {
					return true;
				}
			}
		}
		return false;
	}
}
