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

import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

/**
 * A menu of a view tool bar (an action with a menu creator, e.g., the ImageJ
 * menus "File", "Edit", ... of the ImageJ-Canvas) as a push button with the text
 * and an arrow ("File \u25BE") which opens the menu below it.
 * <p>
 * Windows (and Linux) paint the arrow of a drop-down tool item natively in the
 * colors of the system, which are not visible in a dark (or colored) theme. The
 * arrow as a part of the text has the text color of the theme (CSS). The whole
 * button opens the menu (the drop-down item only with its small arrow). The same
 * look on all platforms (also macOS).
 */
public class MenuButtonItem extends ContributionItem {

	/** The arrow after the text (a small black down-pointing triangle). */
	public static final String ARROW = " \u25BE";

	private final IAction action;
	private ToolItem item;
	/* The text and the state of the action (e.g., setEnabled) as with ActionContributionItem */
	private final IPropertyChangeListener actionListener = _ -> {
		if (item != null && !item.isDisposed()) {
			item.getDisplay().asyncExec(this::update);
		}
	};

	private MenuButtonItem(IAction action) {
		super(action.getId());
		this.action = action;
	}

	/**
	 * The contribution item for a tool bar: the push button with the arrow (the
	 * item of JFace if the action has no menu).
	 */
	public static IContributionItem of(IAction action) {
		if (action.getMenuCreator() == null) {
			return new ActionContributionItem(action);
		}
		return new MenuButtonItem(action);
	}

	@Override
	public void fill(ToolBar parent, int index) {
		item = index >= 0 ? new ToolItem(parent, SWT.PUSH, index) : new ToolItem(parent, SWT.PUSH);
		item.setText(text());
		item.setToolTipText(action.getToolTipText());
		item.setEnabled(action.isEnabled());
		item.setData(this);
		item.addListener(SWT.Selection, _ -> showMenu());
		item.addListener(SWT.Dispose, _ -> {
			action.removePropertyChangeListener(actionListener);
			item = null;
		});
		action.addPropertyChangeListener(actionListener);
	}

	private String text() {
		String text = action.getText() == null ? "" : action.getText();
		/* The mnemonic (&) is not shown in tool bars */
		return text.replace("&", "") + ARROW;
	}

	/* The menu of the action below the button (as the drop-down item) */
	private void showMenu() {
		IMenuCreator creator = action.getMenuCreator();
		if (item == null || item.isDisposed() || creator == null) {
			return;
		}
		ToolBar bar = item.getParent();
		Menu menu = creator.getMenu(bar);
		if (menu == null || menu.isDisposed()) {
			return;
		}
		Rectangle bounds = item.getBounds();
		Point location = bar.toDisplay(bounds.x, bounds.y + bounds.height);
		menu.setLocation(location);
		menu.setVisible(true);
	}

	@Override
	public void update() {
		if (item != null && !item.isDisposed()) {
			item.setText(text());
			item.setEnabled(action.isEnabled());
		}
	}

	@Override
	public boolean isDynamic() {
		return false;
	}

	@Override
	public void dispose() {
		if (item != null && !item.isDisposed()) {
			item.dispose();
		}
		item = null;
	}
}
