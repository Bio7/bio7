/*******************************************************************************
 * Copyright (c) 2007-2026 M. Austenfeld
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/
package com.eco.bio7.floweditor.figures;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Control;

/**
 * The colors and fonts of the flow editor in the light and the dark theme of
 * Bio7: neutral surfaces in the colors of the theme, the type of a shape as an
 * accent (start/end green, scripts blue, ImageJ macros violet, loops and
 * decisions orange, notes amber) and the highlight of the running shape. The
 * figures take the colors when they paint, so a change of the theme only
 * repaints the editors (see {@link #addRepaintTarget(Control)}).
 */
public final class FlowColors {

	/** The colors of one theme. */
	public static final class Palette {
		public final Color canvas, surface, border, text, secondary, line;
		public final Color green, greenTint, greenText;
		public final Color blue, blueTint;
		public final Color violet, violetTint;
		public final Color orange, orangeTint, orangeText;
		public final Color noteFill, noteBorder, noteText, noteIcon;
		public final Color runFill, runBorder;

		Palette(int[]... c) {
			int i = 0;
			canvas = color(c[i++]);
			surface = color(c[i++]);
			border = color(c[i++]);
			text = color(c[i++]);
			secondary = color(c[i++]);
			line = color(c[i++]);
			green = color(c[i++]);
			greenTint = color(c[i++]);
			greenText = color(c[i++]);
			blue = color(c[i++]);
			blueTint = color(c[i++]);
			violet = color(c[i++]);
			violetTint = color(c[i++]);
			orange = color(c[i++]);
			orangeTint = color(c[i++]);
			orangeText = color(c[i++]);
			noteFill = color(c[i++]);
			noteBorder = color(c[i++]);
			noteText = color(c[i++]);
			noteIcon = color(c[i++]);
			runFill = color(c[i++]);
			runBorder = color(c[i++]);
		}

		/* Created once for the lifetime of Bio7 (no OS resources: nothing to dispose) */
		private static Color color(int[] rgb) {
			return new Color(rgb[0], rgb[1], rgb[2]);
		}
	}

	private static final Palette LIGHT = new Palette(//
			new int[] { 255, 255, 255 }, new int[] { 246, 247, 249 }, new int[] { 201, 204, 211 }, new int[] { 31, 35, 40 }, new int[] { 92, 99, 112 }, new int[] { 139, 144, 153 }, //
			new int[] { 46, 158, 71 }, new int[] { 233, 246, 236 }, new int[] { 29, 94, 44 }, //
			new int[] { 31, 111, 235 }, new int[] { 227, 237, 253 }, //
			new int[] { 138, 63, 196 }, new int[] { 241, 230, 251 }, //
			new int[] { 200, 101, 27 }, new int[] { 253, 240, 228 }, new int[] { 122, 58, 12 }, //
			new int[] { 255, 246, 217 }, new int[] { 217, 184, 96 }, new int[] { 74, 59, 16 }, new int[] { 154, 117, 20 }, //
			new int[] { 232, 240, 254 }, new int[] { 31, 111, 235 });

	private static final Palette DARK = new Palette(//
			new int[] { 30, 31, 34 }, new int[] { 43, 45, 48 }, new int[] { 69, 72, 78 }, new int[] { 223, 225, 229 }, new int[] { 157, 160, 168 }, new int[] { 125, 129, 138 }, //
			new int[] { 95, 184, 101 }, new int[] { 38, 49, 39 }, new int[] { 207, 233, 209 }, //
			new int[] { 110, 162, 255 }, new int[] { 36, 50, 74 }, //
			new int[] { 201, 155, 240 }, new int[] { 58, 44, 67 }, //
			new int[] { 224, 138, 60 }, new int[] { 53, 41, 31 }, new int[] { 246, 210, 176 }, //
			new int[] { 52, 48, 31 }, new int[] { 125, 106, 55 }, new int[] { 236, 226, 195 }, new int[] { 227, 196, 111 }, //
			new int[] { 38, 50, 71 }, new int[] { 76, 141, 246 });

	private static final String SMALL_FONT = "com.eco.bio7.floweditor.small";

	private static volatile boolean dark;
	private static boolean initialized;
	private static final Set<Control> TARGETS = Collections.newSetFromMap(new WeakHashMap<Control, Boolean>());
	/* The canvases (the flow, the overview of the outline) with the background of the theme (true: an editor) */
	private static final Map<Control, IFigure> CANVASES = new WeakHashMap<>();
	private static final Map<Control, Boolean> EDITOR_CANVAS = new WeakHashMap<>();
	/* The backgrounds of a custom theme of Bio7 (null: the colors of the palette) */
	private static volatile RGB editorBackground, viewBackground, themeText;
	/* The canvases colored completely (the palette, the rulers) */
	private static final Set<Control> THEMED = Collections.newSetFromMap(new WeakHashMap<Control, Boolean>());
	private static final Map<RGB, Color> THEME_COLORS = new java.util.HashMap<>();

	private FlowColors() {
	}

	/** The colors of the current theme. */
	public static Palette get() {
		init();
		return dark ? DARK : LIGHT;
	}

	/** Returns true for a dark theme. */
	public static boolean isDark() {
		init();
		return dark;
	}

	/** The bold font (of the JFace registry: not disposed here). */
	public static Font bold() {
		return JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT);
	}

	/** A smaller font for the type of a file (of the JFace registry). */
	public static Font small() {
		FontRegistry registry = JFaceResources.getFontRegistry();
		if (!registry.hasValueFor(SMALL_FONT)) {
			FontData[] data = JFaceResources.getDefaultFont().getFontData();
			FontData[] smaller = new FontData[data.length];
			for (int i = 0; i < data.length; i++) {
				smaller[i] = new FontData(data[i].getName(), Math.max(7, data[i].getHeight() - 2), data[i].getStyle());
			}
			registry.put(SMALL_FONT, smaller);
		}
		return registry.get(SMALL_FONT);
	}

	/**
	 * Gives a canvas of draw2d (the flow, the overview of the outline) the
	 * background of the theme: its root figure paints the whole area; also after a
	 * change of the theme.
	 */
	public static void addCanvas(Control control, IFigure root, boolean editor) {
		init();
		synchronized (TARGETS) {
			CANVASES.put(control, root);
			EDITOR_CANVAS.put(control, editor);
		}
		applyCanvas(control, root);
	}

	/**
	 * The backgrounds of the editors and the views of a custom theme of Bio7 (set by
	 * the themes of Bio7; null: the colors of the flow editor for a light or a dark
	 * theme). The canvases are colored again.
	 */
	public static void setThemeBackgrounds(RGB editor, RGB view, RGB text) {
		editorBackground = editor;
		viewBackground = view;
		themeText = text;
		Runnable apply = () -> {
			Map<Control, IFigure> canvases;
			synchronized (TARGETS) {
				canvases = new java.util.HashMap<>(CANVASES);
			}
			canvases.forEach(FlowColors::applyCanvas);
			Control[] themed;
			synchronized (TARGETS) {
				themed = THEMED.toArray(new Control[0]);
			}
			for (Control c : themed) {
				if (c != null && !c.isDisposed()) {
					c.redraw();
				}
			}
		};
		if (org.eclipse.swt.widgets.Display.getCurrent() != null) {
			apply.run();
		} else {
			org.eclipse.swt.widgets.Display.getDefault().asyncExec(apply);
		}
	}

	/** The background of a canvas: of the custom theme, else of the palette. */
	public static Color canvasColor(boolean editor) {
		RGB rgb = editor ? editorBackground : viewBackground;
		if (rgb == null) {
			return get().canvas;
		}
		synchronized (THEME_COLORS) {
			return THEME_COLORS.computeIfAbsent(rgb, c -> new Color(c));
		}
	}

	/** The text color of a custom theme, else of the palette. */
	public static Color textColor() {
		RGB rgb = themeText;
		if (rgb == null) {
			return get().text;
		}
		synchronized (THEME_COLORS) {
			return THEME_COLORS.computeIfAbsent(rgb, c -> new Color(c));
		}
	}

	/**
	 * Colors a canvas of draw2d and all its figures (e.g., the palette, the
	 * rulers) with the background (of the editors or the views) and the text color
	 * of the theme: when it paints (the figures may be created later), and so also
	 * after a change of the theme.
	 */
	public static void themeFigureCanvas(Control canvas, boolean editor) {
		synchronized (TARGETS) {
			if (!THEMED.add(canvas)) {
				return;
			}
		}
		init();
		canvas.addListener(org.eclipse.swt.SWT.Paint, e -> recolor(canvas, editor));
		recolor(canvas, editor);
	}

	/**
	 * Themes the canvases of draw2d in the composite and its composites (e.g., the
	 * rulers, the title and the button of the palette), except one (the flow).
	 */
	public static void themeChildCanvases(org.eclipse.swt.widgets.Composite parent, Control except, boolean editor) {
		if (parent == null || parent.isDisposed()) {
			return;
		}
		for (Control child : parent.getChildren()) {
			if (child == except) {
				continue;
			}
			if (rootOf(child) != null) {
				themeFigureCanvas(child, editor);
			} else if (child instanceof org.eclipse.swt.widgets.Composite composite) {
				themeChildCanvases(composite, except, editor);
			}
		}
	}

	/*
	 * The root figure of a canvas of draw2d: a FigureCanvas or a canvas with its own
	 * LightweightSystem (e.g., the title and the button of the GEF palette, found
	 * by reflection)
	 */
	private static IFigure rootOf(Control control) {
		if (control instanceof org.eclipse.draw2d.FigureCanvas fc) {
			return fc.getLightweightSystem().getRootFigure();
		}
		if (!(control instanceof org.eclipse.swt.widgets.Canvas)) {
			return null;
		}
		for (Class<?> c = control.getClass(); c != null && c != org.eclipse.swt.widgets.Canvas.class; c = c.getSuperclass()) {
			for (java.lang.reflect.Field f : c.getDeclaredFields()) {
				if (org.eclipse.draw2d.LightweightSystem.class.isAssignableFrom(f.getType())) {
					try {
						f.setAccessible(true);
						Object lws = f.get(control);
						return lws instanceof org.eclipse.draw2d.LightweightSystem l ? l.getRootFigure() : null;
					} catch (ReflectiveOperationException | RuntimeException e) {
						return null;
					}
				}
			}
		}
		return null;
	}

	private static void recolor(Control canvas, boolean editor) {
		if (canvas.isDisposed()) {
			return;
		}
		Color bg = canvasColor(editor), fg = textColor();
		if (!bg.equals(canvas.getBackground())) {
			canvas.setBackground(bg);
		}
		IFigure root = rootOf(canvas);
		if (root != null) {
			colorTree(root, bg, fg);
		}
	}

	/*
	 * Only plain colors (none, white, the list and button defaults or colors set
	 * here before) are replaced: the highlights of a selected or hovered entry keep
	 * their colors. Only changed colors are set (a figure repaints when its color
	 * changes).
	 */
	private static void colorTree(IFigure figure, Color bg, Color fg) {
		/* The arrow button of the palette (collapse, expand): light grey with the triangle in the text color */
		if (figure instanceof org.eclipse.draw2d.Button && figure.getClass().getName().endsWith("ArrowButton")) {
			Color button = mix(bg, fg, 0.22);
			if (!button.equals(figure.getLocalBackgroundColor())) {
				figure.setBackgroundColor(button);
			}
			for (Object child : figure.getChildren()) {
				IFigure arrow = (IFigure) child;
				if (!fg.equals(arrow.getLocalBackgroundColor())) {
					arrow.setBackgroundColor(fg);
				}
				if (!fg.equals(arrow.getLocalForegroundColor())) {
					arrow.setForegroundColor(fg);
				}
			}
			return;
		}
		Color b = figure.getLocalBackgroundColor();
		if (!bg.equals(b) && (b == null || isPlain(b, true))) {
			figure.setBackgroundColor(bg);
		}
		Color f = figure.getLocalForegroundColor();
		if (!fg.equals(f) && (f == null || isPlain(f, false))) {
			figure.setForegroundColor(fg);
		}
		for (Object child : figure.getChildren()) {
			colorTree((IFigure) child, bg, fg);
		}
	}

	/* A mixture of two colors (share of b: 0 to 1), cached */
	private static Color mix(Color a, Color b, double share) {
		RGB x = a.getRGB(), y = b.getRGB();
		RGB rgb = new RGB((int) Math.round(x.red + (y.red - x.red) * share), (int) Math.round(x.green + (y.green - x.green) * share),
				(int) Math.round(x.blue + (y.blue - x.blue) * share));
		synchronized (THEME_COLORS) {
			return THEME_COLORS.computeIfAbsent(rgb, c -> new Color(c));
		}
	}

	/**
	 * The colors of the palette of the flow editor (GEF paints its drawers, the
	 * headers of the categories, with these colors): the view background and the
	 * text color of the theme, the buttons and the hover a little towards the text,
	 * the selection towards the accent.
	 */
	public static org.eclipse.gef.ui.palette.PaletteColorProvider paletteColorProvider() {
		return new org.eclipse.gef.ui.palette.PaletteColorProvider() {
			private Color bg() {
				return canvasColor(false);
			}

			public Color getListBackground() {
				return bg();
			}

			public Color getListForeground() {
				return textColor();
			}

			public Color getButton() {
				return mix(bg(), textColor(), 0.06);
			}

			public Color getButtonDarker() {
				return mix(bg(), textColor(), 0.16);
			}

			public Color getButtonDarkest() {
				return mix(bg(), textColor(), 0.32);
			}

			public Color getListHoverBackgroundColor() {
				return mix(bg(), textColor(), 0.08);
			}

			public Color getListSelectedBackgroundColor() {
				return mix(bg(), get().runBorder, 0.22);
			}

			/*
			 * The title of the palette (a gradient from the "foreground" to the background):
			 * the color of the expanded categories, without a gradient
			 */
			public Color getTitleBackground() {
				return getListBackground(0.85);
			}

			public Color getTitleForeground() {
				return getListBackground(0.85);
			}

			public Color getDrawerEditPartBackground() {
				return bg();
			}

			public Color getGroupEditPartBackground() {
				return bg();
			}

			public Color getToolbarEditPartBackground() {
				return bg();
			}

			public Color getSliderPaletteEditPartBackground() {
				return bg();
			}
		};
	}

	private static boolean isPlain(Color c, boolean background) {
		RGB rgb = c.getRGB();
		synchronized (THEME_COLORS) {
			if (THEME_COLORS.containsKey(rgb)) {
				return true;
			}
		}
		if (rgb.equals(LIGHT.canvas.getRGB()) || rgb.equals(DARK.canvas.getRGB()) || rgb.equals(LIGHT.text.getRGB()) || rgb.equals(DARK.text.getRGB())) {
			return true;
		}
		if (background) {
			return rgb.equals(new RGB(255, 255, 255)) || rgb.equals(org.eclipse.draw2d.ColorConstants.listBackground.getRGB())
					|| rgb.equals(org.eclipse.draw2d.ColorConstants.button.getRGB());
		}
		return rgb.equals(new RGB(0, 0, 0)) || rgb.equals(org.eclipse.draw2d.ColorConstants.listForeground.getRGB());
	}

	private static void applyCanvas(Control control, IFigure root) {
		if (control == null || control.isDisposed()) {
			return;
		}
		Boolean editor;
		synchronized (TARGETS) {
			editor = EDITOR_CANVAS.get(control);
		}
		Color c = canvasColor(editor == null || editor);
		control.setBackground(c);
		if (root != null) {
			root.setOpaque(true);
			root.setBackgroundColor(c);
		}
		control.redraw();
	}

	/** Repaints the control (e.g., the canvas of a flow) when the theme changes. */
	public static void addRepaintTarget(Control control) {
		init();
		synchronized (TARGETS) {
			TARGETS.add(control);
		}
	}

	private static synchronized void init() {
		if (initialized) {
			return;
		}
		initialized = true;
		try {
			dark = com.eco.bio7.util.Util.isDarkTheme();
			com.eco.bio7.util.Util.addThemeListener(d -> {
				dark = d;
				Control[] controls;
				Map<Control, IFigure> canvases;
				synchronized (TARGETS) {
					controls = TARGETS.toArray(new Control[0]);
					canvases = new java.util.HashMap<>(CANVASES);
				}
				canvases.forEach(FlowColors::applyCanvas);
				for (Control c : controls) {
					if (c != null && !c.isDisposed()) {
						c.redraw();
					}
				}
			});
		} catch (RuntimeException | LinkageError e) {
			/* No theme information: the light colors */
		}
	}

	/* ---------------- The types of the files of a flow ---------------- */

	/** The name of the type of a file (by its extension, as the flow executes it). */
	public static String typeName(String path) {
		switch (extension(path)) {
		case "bsh":
			return "BeanShell";
		case "groovy":
			return "Groovy";
		case "js":
			return "JavaScript";
		case "py":
			return "Python";
		case "r":
			return "R";
		case "java":
			return "Java";
		case "ecoflow":
			return "Flow";
		case "txt":
		case "ijm":
			return "ImageJ macro";
		case "exe":
		case "bat":
		case "sh":
			return "Program";
		case "rdata":
			return "R workspace";
		case "rbatch":
			return "R batch";
		case "pdf":
			return "PDF";
		case "rhtml":
		case "rmw":
		case "rmd":
		case "rst":
			return "R document";
		default:
			return "File";
		}
	}

	/** Returns true for an ImageJ macro (violet instead of blue). */
	public static boolean isMacro(String path) {
		String e = extension(path);
		return e.equals("txt") || e.equals("ijm");
	}

	/** The name of the file (the last part of the path). */
	public static String fileName(String path) {
		if (path == null) {
			return "";
		}
		String p = path.replace('\\', '/');
		while (p.endsWith("/")) {
			p = p.substring(0, p.length() - 1);
		}
		int i = p.lastIndexOf('/');
		return i >= 0 ? p.substring(i + 1) : p;
	}

	private static String extension(String path) {
		String name = fileName(path);
		int i = name.lastIndexOf('.');
		return i >= 0 ? name.substring(i + 1).toLowerCase(java.util.Locale.ROOT) : "";
	}

	/* ---------------- Painting ---------------- */

	/**
	 * Paints the highlight of a running shape over a rounded rectangle: a thick
	 * border in the accent and an inner ring.
	 */
	public static void paintRunning(Graphics g, Rectangle r, int radius) {
		Palette p = get();
		g.setForegroundColor(p.runBorder);
		g.setLineWidthFloat(2.5f);
		g.drawRoundRectangle(r.getShrinked(1, 1), radius, radius);
		int alpha = g.getAlpha();
		g.setAlpha(80);
		g.setLineWidthFloat(3f);
		g.drawRoundRectangle(r.getShrinked(4, 4), Math.max(0, radius - 4), Math.max(0, radius - 4));
		g.setAlpha(alpha);
	}

	/** Shared size of the corners of the rectangular shapes. */
	public static final int RADIUS = 10;
}
