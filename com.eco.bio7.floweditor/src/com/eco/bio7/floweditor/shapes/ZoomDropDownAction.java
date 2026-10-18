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

package com.eco.bio7.floweditor.shapes;

import org.eclipse.draw2d.zoom.ZoomListener;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

/**
 * The zoom of the Flow editor in the tool bar: a drop down button with the
 * current zoom and a menu with the zoom levels (e.g., 50%, 100%, Page, Width).
 * A click on the button sets the zoom to 100%. It replaces the zoom combo of
 * GEF, because a native combo in the main tool bar shifts the gradient of the
 * tool bar (dark theme) on macOS.
 */
public class ZoomDropDownAction extends Action implements IMenuCreator, ZoomListener {

	/** The id of the action. */
	public static final String ID = "com.eco.bio7.floweditor.zoomlevels";

	private ZoomManager manager;
	private Menu menu;

	public ZoomDropDownAction() {
		super("100%", IAction.AS_DROP_DOWN_MENU);
		setId(ID);
		setToolTipText("Zoom (click: 100%)");
		setMenuCreator(this);
		update();
	}

	/** The zoom manager of the active editor (or null). */
	public void setZoomManager(ZoomManager zoomManager) {
		if (manager == zoomManager) {
			return;
		}
		if (manager != null) {
			manager.removeZoomListener(this);
		}
		manager = zoomManager;
		if (manager != null) {
			manager.addZoomListener(this);
		}
		update();
	}

	private void update() {
		setEnabled(manager != null);
		setText(manager == null ? "100%" : manager.getZoomAsText());
	}

	@Override
	public void zoomChanged(double zoom) {
		update();
	}

	@Override
	public void run() {
		if (manager != null) {
			manager.setZoom(1.0);
		}
	}

	@Override
	public Menu getMenu(Control parent) {
		disposeMenu();
		menu = new Menu(parent);
		if (manager == null) {
			return menu;
		}
		String current = manager.getZoomAsText();
		for (String level : manager.getZoomLevelsAsText()) {
			MenuItem item = new MenuItem(menu, SWT.RADIO);
			item.setText(level);
			item.setSelection(level.equals(current));
			item.addListener(SWT.Selection, e -> {
				if (manager != null && item.getSelection()) {
					manager.setZoomAsText(level);
				}
			});
		}
		return menu;
	}

	@Override
	public Menu getMenu(Menu parent) {
		return null;
	}

	@Override
	public void dispose() {
		disposeMenu();
		setZoomManager(null);
	}

	private void disposeMenu() {
		if (menu != null && !menu.isDisposed()) {
			menu.dispose();
		}
		menu = null;
	}
}
