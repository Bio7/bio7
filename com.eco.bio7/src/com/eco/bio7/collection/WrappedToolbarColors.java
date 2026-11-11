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
package com.eco.bio7.collection;

import java.lang.reflect.Field;

import org.eclipse.e4.ui.workbench.renderers.swt.CTabRendering;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Widget;

/**
 * The tool bar of a view (and the view menu) is in the tab row of the view
 * stack and has the color of the tab row (CSS). If the view is too narrow, the
 * tool bars wrap into a row below the tabs, which has the color of the selected
 * tab (CTabFolder sets the folder background, and the CSS rules of the tool bar
 * composite, the tool bars and their items override it): then the composite,
 * the tool bars and the items get this background (otherwise they are a
 * lighter strip in the dark theme). The CSS engine can reset the colors at any
 * time, so they are checked when the composite or a tool bar is painted.
 */
public final class WrappedToolbarColors {

	/* The color of the tab row (restored if the tool bars are in the tab row again) */
	private static final String TAB_ROW_COLOR = "com.eco.bio7.tabRowColor";
	private static boolean installed;

	private WrappedToolbarColors() {
	}

	public static void install(Display display) {
		if (installed) {
			return;
		}
		installed = true;
		display.addFilter(SWT.Paint, WrappedToolbarColors::paint);
	}

	private static void paint(Event event) {
		if (!(event.widget instanceof Composite control) || control.isDisposed()) {
			return;
		}
		/* The composite in the tab row (top right) or one of its tool bars */
		Composite topRight = control instanceof ToolBar ? control.getParent() : control;
		if (topRight == null || !(topRight.getParent() instanceof CTabFolder folder) || folder.getTopRight() != topRight) {
			return;
		}
		boolean wrapped = folder.getTabPosition() == SWT.TOP && topRight.getBounds().y >= folder.getTabHeight();
		Color folderBg = rowColor(folder);
		update(topRight, wrapped, folderBg);
		for (Control child : topRight.getChildren()) {
			if (child instanceof ToolBar bar) {
				update(bar, wrapped, folderBg);
				for (ToolItem item : bar.getItems()) {
					update(item, wrapped, folderBg);
				}
			}
		}
	}

	/*
	 * The row below the tabs continues the selected tab: the renderer of the CSS
	 * paints it with the fill color of the selected tab (e.g., #1E1E1E in the dark
	 * theme), which can differ from the background of the folder (e.g., #292929 of
	 * the ImageJ stack). Else the selection background or the background.
	 */
	private static Color rowColor(CTabFolder folder) {
		if (folder.getRenderer() instanceof CTabRendering renderer) {
			try {
				Field field = CTabRendering.class.getDeclaredField("selectedTabFillColors");
				field.setAccessible(true);
				if (field.get(renderer) instanceof Color[] colors && colors.length > 0) {
					Color last = colors[colors.length - 1];
					if (last != null && !last.isDisposed()) {
						return last;
					}
				}
			} catch (ReflectiveOperationException | RuntimeException e) {
				/* The colors of the folder */
			}
		}
		Color selection = folder.getSelectionBackground();
		return selection != null && !selection.isDisposed() ? selection : folder.getBackground();
	}

	private static void update(Widget widget, boolean wrapped, Color folderBg) {
		Color current = background(widget);
		if (wrapped) {
			if (current != null && !current.equals(folderBg)) {
				if (widget.getData(TAB_ROW_COLOR) == null) {
					widget.setData(TAB_ROW_COLOR, current);
				}
				setBackground(widget, folderBg);
			}
			if (widget instanceof Control c && c.getBackgroundImage() != null) {
				c.setBackgroundImage(null);
			}
		} else if (widget.getData(TAB_ROW_COLOR) instanceof Color tabRow) {
			widget.setData(TAB_ROW_COLOR, null);
			if (!tabRow.isDisposed() && !tabRow.equals(current)) {
				setBackground(widget, tabRow);
			}
		}
	}

	private static Color background(Widget widget) {
		if (widget instanceof ToolItem item) {
			return item.getBackground();
		}
		return ((Control) widget).getBackground();
	}

	private static void setBackground(Widget widget, Color color) {
		if (widget instanceof ToolItem item) {
			item.setBackground(color);
		} else {
			((Control) widget).setBackground(color);
		}
	}
}
