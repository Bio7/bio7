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

import java.lang.reflect.Field;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Shell;

import com.eco.bio7.Bio7Plugin;

/**
 * The gaps of the workbench window (all themes): the gap below the main toolbar
 * (the top gutter of the layout of the window) and the gap between the views
 * (the width of the sashes, also the area to drag the sashes). The layouts of
 * the workbench have no API for the gaps, so they are set by reflection; the
 * view containers created later (e.g., a new perspective) get the gap when they
 * are resized.
 */
public final class WindowGaps {

	/** Preference keys: the gap below the main toolbar and between the views (points). */
	public static final String GAP_TOOLBAR = "THEME_GAP_TOOLBAR";
	public static final String GAP_VIEWS = "THEME_GAP_VIEWS";
	/** The default gaps (the gaps of the workbench). */
	public static final int DEFAULT_GAP = 4;
	/** The smallest gap between the views (the sashes can be dragged). */
	public static final int MIN_GAP_VIEWS = 1;

	private static final String SASH_LAYOUT = "org.eclipse.e4.ui.workbench.renderers.swt.SashLayout";
	private static boolean installed;

	private WindowGaps() {
	}

	private static IPreferenceStore store() {
		IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
		store.setDefault(GAP_TOOLBAR, DEFAULT_GAP);
		store.setDefault(GAP_VIEWS, DEFAULT_GAP);
		return store;
	}

	/** The gap below the main toolbar: of the active custom theme (if it has own gaps) or of the preferences. */
	public static int getToolbarGap() {
		CustomTheme theme = activeTheme();
		return Math.max(0, theme != null && theme.hasGaps() ? theme.getGapToolbar() : store().getInt(GAP_TOOLBAR));
	}

	/** The gap between the views: of the active custom theme (if it has own gaps) or of the preferences. */
	public static int getViewsGap() {
		CustomTheme theme = activeTheme();
		return Math.max(MIN_GAP_VIEWS, theme != null && theme.hasGaps() ? theme.getGapViews() : store().getInt(GAP_VIEWS));
	}

	/** The gaps of the preferences (all themes without own gaps). */
	public static int getPreferenceToolbarGap() {
		return Math.max(0, store().getInt(GAP_TOOLBAR));
	}

	public static int getPreferenceViewsGap() {
		return Math.max(MIN_GAP_VIEWS, store().getInt(GAP_VIEWS));
	}

	/* The active custom theme (in memory, the gaps are read for every resize of a view container) */
	private static CustomTheme activeTheme() {
		return ToolbarArtwork.getTheme();
	}

	/** Stores the gaps of the preferences (all themes without own gaps) and applies the gaps to all windows. */
	public static void setGaps(int toolbar, int views) {
		IPreferenceStore store = store();
		store.setValue(GAP_TOOLBAR, Math.max(0, toolbar));
		store.setValue(GAP_VIEWS, Math.max(MIN_GAP_VIEWS, views));
		apply();
	}

	/** Applies the gaps of the preferences to all windows (and to the view containers created later). */
	public static void apply() {
		Display display = Display.getDefault();
		install(display);
		for (Shell shell : display.getShells()) {
			if (applyTo(shell)) {
				shell.layout(true, true);
			}
		}
	}

	/* A view container created later (e.g., a new perspective) or a window: the gaps when it is resized */
	private static void install(Display display) {
		if (installed) {
			return;
		}
		installed = true;
		display.addFilter(SWT.Resize, event -> {
			if (event.widget instanceof Composite composite && isGapLayout(composite.getLayout())) {
				if (setGaps(composite)) {
					display.asyncExec(() -> {
						if (!composite.isDisposed()) {
							composite.layout(true);
						}
					});
				}
			}
		});
	}

	/* The gaps of the control and its children, true if a gap was changed */
	private static boolean applyTo(Control control) {
		boolean changed = false;
		if (control instanceof Composite composite) {
			changed |= setGaps(composite);
			for (Control child : composite.getChildren()) {
				changed |= applyTo(child);
			}
		}
		return changed;
	}

	private static boolean isGapLayout(Layout layout) {
		return layout != null && (SASH_LAYOUT.equals(layout.getClass().getName()) || hasField(layout, "gutterTop"));
	}

	/* The gap below the toolbar (the layout of the window) or between the views (a sash layout) */
	private static boolean setGaps(Composite composite) {
		Layout layout = composite.getLayout();
		if (layout == null) {
			return false;
		}
		try {
			if (SASH_LAYOUT.equals(layout.getClass().getName())) {
				return set(layout, "sashWidth", getViewsGap());
			}
			if (composite instanceof Shell && hasField(layout, "gutterTop")) {
				return set(layout, "gutterTop", getToolbarGap());
			}
		} catch (ReflectiveOperationException | RuntimeException e) {
			/* Another version of the workbench: the gaps of the workbench */
		}
		return false;
	}

	private static boolean set(Layout layout, String name, int value) throws ReflectiveOperationException {
		Field field = field(layout.getClass(), name);
		field.setAccessible(true);
		if (field.getInt(layout) == value) {
			return false;
		}
		field.setInt(layout, value);
		return true;
	}

	private static boolean hasField(Layout layout, String name) {
		return field(layout.getClass(), name) != null;
	}

	private static Field field(Class<?> type, String name) {
		for (Class<?> c = type; c != null; c = c.getSuperclass()) {
			try {
				return c.getDeclaredField(name);
			} catch (NoSuchFieldException e) {
				/* The superclass */
			}
		}
		return null;
	}
}
