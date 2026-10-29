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

import java.lang.reflect.Method;

import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.nebula.widgets.grid.GridHeaderColors;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 * The colors of a custom theme for views which paint with their own colors and
 * can't be styled with CSS: the Snippets view (a GEF palette with white
 * figures). The figures are colored with the view background and the text color
 * of the active custom theme when the view is visible (by reflection, the view
 * is optional). The headers of the tables (Nebula grids) paint with the table
 * header colors.
 */
final class ViewColors {

	private static final String[] FIGURE_VIEWS = { "org.eclipse.wst.common.snippets.internal.ui.SnippetsView" };

	private static boolean installed;

	private ViewColors() {
	}

	/* Colors the views when they are visible (in all workbench windows) */
	static void install() {
		if (installed || !PlatformUI.isWorkbenchRunning()) {
			return;
		}
		installed = true;
		IPartListener2 listener = new IPartListener2() {
			@Override
			public void partVisible(IWorkbenchPartReference ref) {
				if (isFigureView(ref.getId())) {
					Display.getDefault().asyncExec(() -> color(ref.getPart(false)));
				}
			}
		};
		for (IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
			window.getPartService().addPartListener(listener);
		}
		PlatformUI.getWorkbench().addWindowListener(new IWindowListener() {
			@Override
			public void windowOpened(IWorkbenchWindow window) {
				window.getPartService().addPartListener(listener);
			}

			@Override
			public void windowActivated(IWorkbenchWindow window) {
			}

			@Override
			public void windowDeactivated(IWorkbenchWindow window) {
			}

			@Override
			public void windowClosed(IWorkbenchWindow window) {
			}
		});
	}

	/* Colors the open views (e.g., after a custom theme was activated) */
	static void apply() {
		applyGridHeaders();
		if (!PlatformUI.isWorkbenchRunning()) {
			return;
		}
		for (IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
			IWorkbenchPage page = window.getActivePage();
			if (page == null) {
				continue;
			}
			for (String id : FIGURE_VIEWS) {
				IViewPart view = page.findView(id);
				if (view != null) {
					color(view);
				}
			}
		}
	}

	/* The headers of the tables (Nebula grids, e.g., the Table view) or their default colors without a custom theme */
	private static void applyGridHeaders() {
		CustomTheme theme = ThemeStore.find(ThemeStore.activeFolder());
		if (theme == null) {
			GridHeaderColors.setColors(null, null, null, null);
			GroupColors.apply(null, null, null);
			ThemeToolTips.apply(null, null, null);
			applyHeapStatus(null);
			SelectionColors.apply(null);
		} else {
			ThemeToolTips.apply(theme.getColor("assistBackground"), theme.getColor("assistText"), theme.getColor("groupBorder"));
			applyHeapStatus(theme);
			SelectionColors.apply(theme.getColor("inactiveSelection"));
			GroupColors.apply(theme.getColor("groupBackground"), theme.getColor("groupBorder"), theme.getColor("text"));
			GridHeaderColors.setColors(theme.getColor("tableHeader"), theme.getColor("tableHeaderText"), theme.getColor("tableHeaderLine"),
					theme.getColor("viewBackground"));
		}
	}

	/*
	 * The memory display of the status line (HeapStatus) has fixed colors for the
	 * bar and the border (set in its constructor, only the background and the text
	 * are set by CSS): set by reflection, the original colors again without a
	 * custom theme (the text color is set by the CSS of every theme).
	 */
	private static final String[] HEAP_COLORS = { "usedMemCol", "armCol", "freeMemCol", "topLeftCol", "bottomRightCol", "sepCol" };
	private static final String HEAP_ORIGINAL = "bio7.themes.heapColors";

	private static void applyHeapStatus(CustomTheme theme) {
		for (org.eclipse.swt.widgets.Shell shell : Display.getDefault().getShells()) {
			heapStatus(shell, theme);
		}
	}

	private static void heapStatus(Control control, CustomTheme theme) {
		if (control.isDisposed()) {
			return;
		}
		if ("org.eclipse.ui.internal.HeapStatus".equals(control.getClass().getName())) {
			colorHeapStatus(control, theme);
			return;
		}
		if (control instanceof Composite composite) {
			for (Control child : composite.getChildren()) {
				heapStatus(child, theme);
			}
		}
	}

	private static void colorHeapStatus(Control heap, CustomTheme theme) {
		try {
			Color[] original = (Color[]) heap.getData(HEAP_ORIGINAL);
			if (original == null) {
				original = new Color[HEAP_COLORS.length];
				for (int i = 0; i < HEAP_COLORS.length; i++) {
					original[i] = (Color) field(heap, HEAP_COLORS[i]).get(heap);
				}
				heap.setData(HEAP_ORIGINAL, original);
			}
			Color[] colors = original;
			if (theme != null) {
				Display display = heap.getDisplay();
				RGB bar = theme.getColor("statusBar"), text = theme.getColor("text");
				/* The used memory: the icon color of the theme (softened), the free memory: a little lighter or darker than the bar */
				Color used = new Color(display, mix(theme.getColor("iconColor"), bar, 0.45));
				Color free = new Color(display, mix(bar, text, 0.08));
				Color line = new Color(display, theme.getColor("tabBorder"));
				colors = new Color[] { used, used, free, line, line, line };
				heap.setBackground(new Color(display, bar));
			}
			for (int i = 0; i < HEAP_COLORS.length; i++) {
				field(heap, HEAP_COLORS[i]).set(heap, colors[i]);
			}
			if (theme == null) {
				/*
				 * The default light theme has no text color for the memory display (the light
				 * text of a dark theme stays): the text fits the background!
				 */
				Color bg = heap.getBackground();
				boolean dark = (bg.getRed() * 299 + bg.getGreen() * 587 + bg.getBlue() * 114) / 1000 < 128;
				heap.setForeground(dark ? new Color(heap.getDisplay(), 238, 238, 238) : new Color(heap.getDisplay(), 30, 30, 30));
			}
			heap.redraw();
		} catch (ReflectiveOperationException | RuntimeException e) {
			/* Another version of the workbench: the colors of the memory display */
		}
	}

	private static java.lang.reflect.Field field(Object object, String name) throws NoSuchFieldException {
		java.lang.reflect.Field field = object.getClass().getDeclaredField(name);
		field.setAccessible(true);
		return field;
	}

	private static RGB mix(RGB a, RGB b, double f) {
		return new RGB((int) Math.round(a.red * (1 - f) + b.red * f), (int) Math.round(a.green * (1 - f) + b.green * f),
				(int) Math.round(a.blue * (1 - f) + b.blue * f));
	}

	private static boolean isFigureView(String id) {
		for (String v : FIGURE_VIEWS) {
			if (v.equals(id)) {
				return true;
			}
		}
		return false;
	}

	/* The figure canvas of a view and its figures with the colors of the custom theme */
	private static void color(Object part) {
		if (!(part instanceof IViewPart)) {
			return;
		}
		CustomTheme theme = ThemeStore.find(ThemeStore.activeFolder());
		if (theme == null) {
			return;
		}
		Object widget = ((IViewPart) part).getSite().getService(MPart.class) instanceof MPart mpart ? mpart.getWidget() : null;
		if (!(widget instanceof Control control) || control.isDisposed()) {
			return;
		}
		RGB bg = theme.getColor("viewBackground"), fg = theme.getColor("text");
		Color background = new Color(control.getDisplay(), bg), foreground = new Color(control.getDisplay(), fg);
		colorCanvases(control, background, foreground);
	}

	private static void colorCanvases(Control control, Color background, Color foreground) {
		if (control.getClass().getName().endsWith("FigureCanvas")) {
			control.setBackground(background);
			try {
				Object contents = control.getClass().getMethod("getContents").invoke(control);
				if (contents != null) {
					colorFigure(contents, background, foreground, true);
				}
			} catch (ReflectiveOperationException | RuntimeException e) {
				/* Not a draw2d canvas */
			}
			control.redraw();
		}
		if (control instanceof Composite composite) {
			for (Control child : composite.getChildren()) {
				colorCanvases(child, background, foreground);
			}
		}
	}

	/* The figures with an own background (e.g., the white palette) and the text color */
	private static void colorFigure(Object figure, Color background, Color foreground, boolean root) throws ReflectiveOperationException {
		Class<?> c = figure.getClass();
		Object localBackground = c.getMethod("getLocalBackgroundColor").invoke(figure);
		if (root || localBackground != null) {
			Method setBackground = c.getMethod("setBackgroundColor", Color.class);
			setBackground.invoke(figure, background);
		}
		Object localForeground = c.getMethod("getLocalForegroundColor").invoke(figure);
		if (root || localForeground != null) {
			c.getMethod("setForegroundColor", Color.class).invoke(figure, foreground);
		}
		for (Object child : (java.util.List<?>) c.getMethod("getChildren").invoke(figure)) {
			colorFigure(child, background, foreground, false);
		}
	}
}
