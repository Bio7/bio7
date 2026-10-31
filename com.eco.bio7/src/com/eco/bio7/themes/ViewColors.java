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
		/* The gaps of the theme (or of the preferences) */
		WindowGaps.apply();
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
			applyGridSelection(null);
			/* The compass of the globe with the colors of the default theme */
			com.eco.bio7.worldwind.ThemedCompassLayer.setDefaultColors(com.eco.bio7.util.Util.isThemeBlack());
			/* The default themes: the boxes with the color of the parent (no native tint) */
			GroupColors.applyDefault();
			/* The local HTML pages (e.g., the R help): the CSS of the default themes */
			com.eco.bio7.util.HtmlTheme.setThemeCss(null);
			applyAssistColors(null);
			ThemeToolTips.apply(null, null, null);
			applyHeapStatus(null);
			SelectionColors.apply(null);
		} else {
			ThemeToolTips.apply(theme.getColor("assistBackground"), theme.getColor("assistText"), theme.getColor("groupBorder"));
			applyHeapStatus(theme);
			/* The local HTML pages (e.g., the R help) with the colors of the theme */
			com.eco.bio7.util.HtmlTheme.setThemeCss(theme.htmlCss());
			applyAssistColors(theme);
			SelectionColors.apply(theme.getColor("inactiveSelection"));
			GroupColors.apply(theme.getColor("groupBackground"), theme.getColor("groupBorder"), theme.getColor("text"));
			GridHeaderColors.setColors(theme.getColor("tableHeader"), theme.getColor("tableHeaderText"), theme.getColor("tableHeaderLine"),
					theme.getColor("viewBackground"));
			applyGridSelection(theme);
			/* The compass of the globe with the colors of the theme */
			com.eco.bio7.worldwind.ThemedCompassLayer.setThemeColors(theme.getColor("widgetBackground"), theme.getColor("tableHeaderLine"),
					theme.getColor("widgetText"), theme.getColor("iconColor"));
		}
	}

	/*
	 * The selected cells and the headers of the selected cells of the tables: the
	 * light blue of the system has a light text in the dark default theme (not
	 * readable). A custom theme: the colors of the unfocused selection and the text
	 * of the theme, the dark default theme: grey colors, the light default theme:
	 * the system colors.
	 */
	private static void applyGridSelection(CustomTheme theme) {
		if (theme != null) {
			RGB inactive = theme.getColor("inactiveSelection");
			RGB text = theme.getColor("text");
			RGB header = theme.getColor("tableHeader");
			if (inactive == null || text == null || header == null) {
				GridHeaderColors.setSelectionColors(null, null, null, null, null, null);
				return;
			}
			/* The selection with the focus: stronger (towards the text color) */
			GridHeaderColors.setSelectionColors(mix(inactive, text, 0.2), text, inactive, text, mix(header, inactive, 0.7),
					mix(inactive, text, 0.35));
		} else if (com.eco.bio7.util.Util.isThemeBlack()) {
			GridHeaderColors.setSelectionColors(new RGB(0x50, 0x55, 0x5c), new RGB(0xf0, 0xf0, 0xf0), new RGB(0x3c, 0x3f, 0x44),
					new RGB(0xcc, 0xcc, 0xcc), new RGB(0x46, 0x4a, 0x50), new RGB(0x62, 0x67, 0x6e));
			/* The lines between the cells: not the light background of the system */
			GridHeaderColors.setCellLineColor(new RGB(0x45, 0x45, 0x45));
		} else {
			GridHeaderColors.setSelectionColors(null, null, null, null, null, null);
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

	/*
	 * The popups of the code completion (and the information and the hovers) read
	 * their colors from the JFace color registry. The preferences of the stylesheets
	 * (the base theme and the custom theme set the same keys) can be applied in
	 * another order (e.g., at the start): the colors of a custom theme are set
	 * directly, the original colors again for the default themes.
	 */
	private static final String[][] ASSIST_KEYS = { { "CONTENT_ASSIST_BACKGROUND_COLOR", "assistBackground" },
			{ "CONTENT_ASSIST_FOREGROUND_COLOR", "assistText" }, { "org.eclipse.ui.workbench.INFORMATION_BACKGROUND", "assistBackground" },
			{ "org.eclipse.ui.workbench.INFORMATION_FOREGROUND", "assistText" }, { "org.eclipse.ui.workbench.HOVER_BACKGROUND", "assistBackground" },
			{ "org.eclipse.ui.workbench.HOVER_FOREGROUND", "assistText" } };
	private static java.util.Map<String, RGB> originalAssistColors;
	/* The custom theme of the colors of the code completion (null: the default themes) */
	private static CustomTheme assistTheme;
	private static boolean assistListener;

	private static void applyAssistColors(CustomTheme theme) {
		org.eclipse.jface.resource.ColorRegistry registry = org.eclipse.jface.resource.JFaceResources.getColorRegistry();
		assistTheme = theme;
		if (!assistListener) {
			assistListener = true;
			/* A color set later by the preferences of a stylesheet: the color of the custom theme again */
			registry.addListener(event -> {
				CustomTheme active = assistTheme;
				if (active == null) {
					return;
				}
				for (String[] key : ASSIST_KEYS) {
					RGB rgb = active.getColor(key[1]);
					if (key[0].equals(event.getProperty()) && rgb != null && !rgb.equals(registry.getRGB(key[0]))) {
						Display.getDefault().asyncExec(() -> {
							if (assistTheme == active) {
								registry.put(key[0], rgb);
							}
						});
					}
				}
			});
		}
		if (theme != null) {
			if (originalAssistColors == null) {
				originalAssistColors = new java.util.HashMap<>();
				for (String[] key : ASSIST_KEYS) {
					RGB rgb = registry.getRGB(key[0]);
					if (rgb != null) {
						originalAssistColors.put(key[0], rgb);
					}
				}
			}
			for (String[] key : ASSIST_KEYS) {
				registry.put(key[0], theme.getColor(key[1]));
			}
		} else if (originalAssistColors != null) {
			for (java.util.Map.Entry<String, RGB> e : originalAssistColors.entrySet()) {
				registry.put(e.getKey(), e.getValue());
			}
			originalAssistColors = null;
		}
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
