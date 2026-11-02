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

import org.eclipse.e4.ui.workbench.renderers.swt.CTabRendering;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

/**
 * The tab renderer of the Bio7 themes (the default themes on Linux and the
 * custom themes). The folder places its tool bars in the tab row (e.g., the
 * minimize and maximize buttons) with the height of the tab row, but moved down
 * by half of the space (see CTabFolder.getControlY), so they reach a little
 * below the tab row (e.g., over the empty editor area). On GTK the tool bars of
 * the folder are kept in the tab row and are transparent (the folder is visible
 * behind them).
 */
public class Bio7TabRendering extends CTabRendering {

	private static final boolean GTK = "gtk".equals(SWT.getPlatform());
	private static final Color TRANSPARENT = new Color(0, 0, 0, 0);
	/* A tool bar of the folder with the listener of a renderer (see keepInTabRow) */
	private static final String KEPT = "bio7.themes.keptInTabRow";

	private final CTabFolder folder;
	private boolean pending;

	public Bio7TabRendering(CTabFolder parent) {
		super(parent);
		folder = parent;
	}

	@Override
	protected void draw(int part, int state, Rectangle bounds, GC gc) {
		super.draw(part, state, bounds, gc);
		if (GTK && part == PART_BACKGROUND) {
			toolBarsLater();
		}
	}

	/* After the paint (the CSS engine can set the colors again, nothing is changed if they are right) */
	private void toolBarsLater() {
		if (pending || folder.isDisposed()) {
			return;
		}
		pending = true;
		folder.getDisplay().asyncExec(() -> {
			pending = false;
			if (folder.isDisposed()) {
				return;
			}
			for (Control child : folder.getChildren()) {
				if (child instanceof ToolBar toolBar) {
					if (toolBar.getData(KEPT) != this) {
						toolBar.setData(KEPT, this);
						/*
						 * The folder sets the bounds of the tool bar again (e.g., after a resize): the
						 * events are sent then (the folder ignores the resize of the tool bar meanwhile)
						 */
						Listener keep = _ -> keepInTabRow(toolBar);
						toolBar.addListener(SWT.Resize, keep);
						toolBar.addListener(SWT.Move, keep);
					}
					keepInTabRow(toolBar);
					transparent(toolBar);
				}
			}
		});
	}

	/* The tool bar ends at the bottom of the tab row (it keeps its preferred height) */
	private void keepInTabRow(ToolBar toolBar) {
		if (toolBar.isDisposed() || toolBar.getData(KEPT) != this || folder.isDisposed() || folder.getTabPosition() != SWT.TOP
				|| folder.getItemCount() > 0) {
			return;
		}
		int tabRowBottom = folder.getTabHeight() + 1;
		Rectangle bounds = toolBar.getBounds();
		if (bounds.y + bounds.height <= tabRowBottom || bounds.y >= tabRowBottom) {
			return;
		}
		int preferred = toolBar.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
		int height = Math.max(preferred, tabRowBottom - bounds.y);
		int y = Math.max(0, tabRowBottom - height);
		if (y != bounds.y || height != bounds.height) {
			toolBar.setBounds(bounds.x, y, bounds.width, height);
		}
	}

	private static void transparent(ToolBar toolBar) {
		Color color = toolBar.getBackground();
		if (color == null || color.getAlpha() != 0) {
			toolBar.setBackground(TRANSPARENT);
		}
		for (ToolItem item : toolBar.getItems()) {
			Color itemColor = item.getBackground();
			if (itemColor == null || itemColor.getAlpha() != 0) {
				item.setBackground(TRANSPARENT);
			}
		}
	}
}
