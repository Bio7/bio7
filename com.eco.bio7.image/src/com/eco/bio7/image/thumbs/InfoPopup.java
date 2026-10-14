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
package com.eco.bio7.image.thumbs;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * The information popup of the thumbnails (replaces the tooltip). It is shown
 * when the mouse rests on a thumbnail. A click on the popup opens a window
 * with the information which stays open until it is closed (several windows
 * can be opened, e.g., to compare images).
 */
final class InfoPopup {

	private static final int SHOW_DELAY = 600;
	private static final int HIDE_DELAY = 250;
	/* The popup shows the beginning of long information! */
	private static final int MAX_LINES = 20;

	private final ThumbnailGrid grid;
	private Shell popup;
	private ThumbnailGrid.Item popupItem;
	/* The thumbnail under the mouse (null if none)! */
	private ThumbnailGrid.Item hoverItem;
	/* Invalidates scheduled shows! */
	private int showToken;
	private final List<Shell> pinned = new ArrayList<Shell>();

	InfoPopup(ThumbnailGrid grid) {
		this.grid = grid;
		grid.addListener(SWT.Dispose, e -> {
			hide();
			for (Shell s : new ArrayList<Shell>(pinned)) {
				if (!s.isDisposed()) {
					s.dispose();
				}
			}
		});
	}

	/** Called when the mouse moves to another thumbnail (null if none). */
	void hoverChanged(ThumbnailGrid.Item item) {
		hoverItem = item;
		final int token = ++showToken;
		Display display = grid.getDisplay();
		if (isVisible() && item != popupItem) {
			/* The mouse can move to the popup (it is not hidden immediately)! */
			display.timerExec(HIDE_DELAY, this::hideIfLeft);
		}
		if (item != null && item != popupItem) {
			display.timerExec(SHOW_DELAY, () -> {
				if (token == showToken && hoverItem == item && !grid.isDisposed() && !isMouseInPopup()) {
					show(item, display.getCursorLocation());
				}
			});
		}
	}

	/** Hides the popup (e.g., after a click or a key in the grid). */
	void hide() {
		showToken++;
		if (popup != null && !popup.isDisposed()) {
			popup.dispose();
		}
		popup = null;
		popupItem = null;
	}

	private boolean isVisible() {
		return popup != null && !popup.isDisposed();
	}

	private boolean isMouseInPopup() {
		return isVisible() && popup.getBounds().contains(popup.getDisplay().getCursorLocation());
	}

	private void hideIfLeft() {
		if (isVisible() && !isMouseInPopup() && hoverItem != popupItem) {
			hide();
		}
	}

	private void show(ThumbnailGrid.Item item, Point cursor) {
		hide();
		Display display = grid.getDisplay();
		String info = ThumbnailGrid.tooltip(item);
		String[] lines = info.split("\n", -1);
		if (lines.length > MAX_LINES) {
			info = String.join("\n", java.util.Arrays.copyOf(lines, MAX_LINES)) + "\n …";
		}
		popup = new Shell(grid.getShell(), SWT.TOOL | SWT.ON_TOP | SWT.NO_FOCUS | SWT.NO_TRIM);
		popupItem = item;
		Color bg = display.getSystemColor(SWT.COLOR_INFO_BACKGROUND);
		Color fg = display.getSystemColor(SWT.COLOR_INFO_FOREGROUND);
		popup.setBackground(bg);
		popup.setBackgroundMode(SWT.INHERIT_FORCE);
		GridLayout layout = new GridLayout(1, false);
		layout.marginWidth = 8;
		layout.marginHeight = 6;
		popup.setLayout(layout);
		Label text = new Label(popup, SWT.NONE);
		text.setText(info);
		text.setForeground(fg);
		Label hint = new Label(popup, SWT.NONE);
		hint.setText("Click to keep the information open");
		hint.setForeground(display.getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
		hint.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		Listener pin = e -> {
			Point at = popup.getLocation();
			hide();
			pin(item, at);
		};
		Listener exit = e -> display.timerExec(HIDE_DELAY, this::hideIfLeft);
		for (Control c : new Control[] { popup, text, hint }) {
			c.addListener(SWT.MouseDown, pin);
			c.addListener(SWT.MouseExit, exit);
		}
		Rectangle area = grid.getMonitor().getClientArea();
		Point size = popup.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		size.x = Math.min(size.x, Math.min(700, area.width));
		size.y = Math.min(size.y, area.height);
		/* Below and right of the mouse, inside the monitor! */
		int x = Math.max(area.x, Math.min(cursor.x + 12, area.x + area.width - size.x));
		int y = cursor.y + 18;
		if (y + size.y > area.y + area.height) {
			y = Math.max(area.y, cursor.y - size.y - 6);
		}
		popup.setBounds(x, y, size.x, size.y);
		popup.setVisible(true);
	}

	/* A window with the complete information which stays open until it is closed! */
	private void pin(ThumbnailGrid.Item item, Point at) {
		Shell shell = new Shell(grid.getShell(), SWT.DIALOG_TRIM | SWT.RESIZE | SWT.MODELESS);
		shell.setText(item.file.getName());
		shell.setLayout(new GridLayout(1, false));
		Text text = new Text(shell, SWT.MULTI | SWT.READ_ONLY | SWT.V_SCROLL | SWT.H_SCROLL);
		text.setText(ThumbnailGrid.tooltip(item));
		text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		Rectangle area = grid.getMonitor().getClientArea();
		Point size = shell.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		size.x = Math.max(300, Math.min(size.x + 20, Math.min(700, area.width)));
		size.y = Math.max(150, Math.min(size.y + 20, Math.min(600, area.height)));
		int x = Math.max(area.x, Math.min(at.x, area.x + area.width - size.x));
		int y = Math.max(area.y, Math.min(at.y, area.y + area.height - size.y));
		shell.setBounds(x, y, size.x, size.y);
		/* Escape closes the window! */
		shell.addListener(SWT.Traverse, e -> {
			if (e.detail == SWT.TRAVERSE_ESCAPE) {
				shell.close();
			}
		});
		text.addListener(SWT.Traverse, e -> {
			if (e.detail == SWT.TRAVERSE_ESCAPE) {
				shell.close();
			}
		});
		pinned.add(shell);
		shell.addListener(SWT.Dispose, e -> pinned.remove(shell));
		shell.open();
		text.setSelection(0);
	}
}
