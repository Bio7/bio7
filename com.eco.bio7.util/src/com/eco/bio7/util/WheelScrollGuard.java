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
package com.eco.bio7.util;

import java.util.Set;
import java.util.WeakHashMap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Slider;

/**
 * The mouse wheel scrolls a scrolled page (e.g., the tabs of the options of the
 * 3D view) and not the sliders under the mouse: else the value of a slider
 * changes unintended when the page is scrolled over it (e.g., on macOS the
 * slider gets the wheel events). The wheel event of a slider of a registered
 * composite is canceled and scrolls the next scrolled composite instead.
 */
public final class WheelScrollGuard {

	/* The pixels scrolled by one step of the mouse wheel */
	private static final int STEP = 20;

	private static final Set<Composite> ROOTS = java.util.Collections.newSetFromMap(new WeakHashMap<>());
	private static boolean installed;

	private WheelScrollGuard() {
	}

	/** The sliders in the composite (also created later) don't react to the mouse wheel. */
	public static void install(Composite root) {
		if (root == null || root.isDisposed()) {
			return;
		}
		ROOTS.add(root);
		root.addListener(SWT.Dispose, _ -> ROOTS.remove(root));
		if (installed) {
			return;
		}
		installed = true;
		root.getDisplay().addFilter(SWT.MouseVerticalWheel, FILTER);
	}

	private static final Listener FILTER = new Listener() {
		public void handleEvent(Event e) {
			if (!(e.widget instanceof Scale || e.widget instanceof Slider) || !inRoot((Control) e.widget)) {
				return;
			}
			/* The slider keeps its value, the page is scrolled */
			e.doit = false;
			ScrolledComposite scroll = scrolledParent((Control) e.widget);
			if (scroll != null && scroll.getContent() != null) {
				Point origin = scroll.getOrigin();
				int max = Math.max(0, scroll.getContent().getSize().y - scroll.getClientArea().height);
				int y = Math.max(0, Math.min(max, origin.y - e.count * STEP));
				scroll.setOrigin(origin.x, y);
			}
		}
	};

	private static boolean inRoot(Control control) {
		for (Control c = control; c != null; c = c.getParent()) {
			if (c instanceof Composite && ROOTS.contains(c)) {
				return true;
			}
		}
		return false;
	}

	private static ScrolledComposite scrolledParent(Control control) {
		for (Control c = control.getParent(); c != null; c = c.getParent()) {
			if (c instanceof ScrolledComposite scroll) {
				return scroll;
			}
		}
		return null;
	}
}
