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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.eclipse.swt.graphics.RGB;

/**
 * A custom Bio7 theme: a light or dark Bio7 theme as base and the colors of the
 * workbench, the toolbar artwork (an SVG or PNG image which fades over the main
 * toolbar) and the colors of the icons. A theme is stored in a folder
 * (theme.properties, the generated theme.css and the artwork image) and can be
 * exported as one file (*.bio7theme, a zip of the folder).
 */
public class CustomTheme {

	/** The theme ids start with the id of the Eclipse theme, so that Bio7 detects a dark theme. */
	public static final String DARK_ID = "org.eclipse.e4.ui.css.theme.e4_dark";
	public static final String LIGHT_ID = "org.eclipse.e4.ui.css.theme.e4_default";
	private static final String CUSTOM = ".custom.";

	public static final String PROPERTIES = "theme.properties";
	public static final String CSS = "theme.css";

	/** The color slots (key, label). */
	public static final String[][] SLOTS = { { "background", "Window background" }, { "viewBackground", "View background" },
			{ "panelBackground", "View panels" }, { "text", "Text" }, { "tabRow", "Tab row" }, { "tabSelected", "Selected tab" },
			{ "tabText", "Tab text" }, { "tabSelectedText", "Selected tab text" }, { "tabBorder", "Tab border" }, { "toolbarTop", "Toolbar (top)" },
			{ "toolbarBottom", "Toolbar (bottom)" }, { "toolbarText", "Toolbar text" }, { "statusBar", "Status bar" },
			{ "iconColor", "Icon color" }, { "iconDetail", "Icon detail color" } };

	/** The artwork modes and the fade directions. */
	public static final String[] MODES = { "tile", "stretch" };
	/** The styles of the whole toolbar (flat color, gradient, raised with highlight and shadow). */
	public static final String[] STYLES = { "flat", "gradient", "raised" };
	public static final String[] STYLE_LABELS = { "Flat", "Gradient", "Raised" };
	public static final String[] FADES = { "none", "left", "right", "center", "edges" };
	public static final String[] FADE_LABELS = { "No fade", "Visible left, fades to the right", "Visible right, fades to the left",
			"Visible in the center", "Visible at both edges" };

	private final String folderName;
	private String name = "New Theme";
	private String author = "";
	private boolean dark = true;
	private final Map<String, RGB> colors = new LinkedHashMap<String, RGB>();
	/* The colors of the theme when it was installed, created or imported (for a reset)! */
	private final Map<String, RGB> originalColors = new LinkedHashMap<String, RGB>();
	private String artwork = "";
	private String mode = "tile";
	private int tileSize = 100;
	private int opacity = 60;
	private String fade = "left";
	private int fadeWidth = 70;
	private String style = "raised";

	public CustomTheme(String folderName, boolean dark) {
		this.folderName = folderName;
		this.dark = dark;
		setDefaults();
	}

	/** The default colors of the Bio7 light or dark theme. */
	public void setDefaults() {
		colors.clear();
		if (dark) {
			put("background", "1e1e1e");
			put("viewBackground", "1e1e1e");
			put("panelBackground", "1e1e1e");
			put("text", "dddddd");
			put("tabRow", "252526");
			put("tabSelected", "1e1e1e");
			put("tabText", "dddddd");
			put("tabSelectedText", "f7f8f8");
			put("tabBorder", "4b4c4f");
			put("toolbarTop", "41464a");
			put("toolbarBottom", "1e1e1e");
			put("toolbarText", "ebe8e4");
			put("statusBar", "1e1e1e");
			put("iconColor", "86b55a");
			put("iconDetail", "ffffff");
		} else {
			put("background", "ffffff");
			put("viewBackground", "ffffff");
			put("panelBackground", "f4f4f4");
			put("text", "000000");
			put("tabRow", "eeeeee");
			/* The selected tab has the color of the view panel (like the dark theme)! */
			put("tabSelected", "f4f4f4");
			put("tabText", "333333");
			put("tabSelectedText", "000000");
			put("tabBorder", "b6bccc");
			put("toolbarTop", "f6f6f6");
			put("toolbarBottom", "e4e4e4");
			put("toolbarText", "000000");
			put("statusBar", "ececec");
			put("iconColor", "638d43");
			put("iconDetail", "ffffff");
		}
	}

	private void put(String key, String hex) {
		colors.put(key, parse(hex));
	}

	public String getFolderName() {
		return folderName;
	}

	/** The id of the theme in the theme engine. */
	public String getThemeId() {
		return (dark ? DARK_ID : LIGHT_ID) + CUSTOM + folderName;
	}

	/** True if the id is the id of a custom theme. */
	public static boolean isCustomThemeId(String id) {
		return id != null && id.contains(CUSTOM);
	}

	/** The folder name of a custom theme id. */
	public static String folderOf(String id) {
		return id.substring(id.indexOf(CUSTOM) + CUSTOM.length());
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public boolean isDark() {
		return dark;
	}

	public void setDark(boolean dark) {
		this.dark = dark;
	}

	/*
	 * The view panels (composites and labels of the views) of a dark theme have the
	 * view background, of a light theme a tint between the window background and
	 * the tab row (white panels look empty, like in the Bio7 light theme)!
	 */
	private RGB defaultPanelBackground() {
		RGB view = colors.get("viewBackground");
		if (dark) {
			return view;
		}
		RGB a = colors.get("background"), b = colors.get("tabRow");
		return new RGB((a.red + b.red) / 2, (a.green + b.green) / 2, (a.blue + b.blue) / 2);
	}

	public RGB getColor(String key) {
		return colors.get(key);
	}

	public void setColor(String key, RGB rgb) {
		colors.put(key, rgb);
	}

	/** The current colors are the original colors of the theme (e.g., after an installation). */
	public void markOriginalColors() {
		originalColors.clear();
		originalColors.putAll(colors);
	}

	public boolean hasOriginalColors() {
		return !originalColors.isEmpty();
	}

	/** Sets the colors to the original colors of the theme or of the other theme (e.g., a bundled example). */
	public void resetColors(CustomTheme source) {
		Map<String, RGB> from = source != null ? source.colors : originalColors;
		if (from.isEmpty()) {
			setDefaults();
			return;
		}
		colors.putAll(from);
	}

	public String getArtwork() {
		return artwork;
	}

	public void setArtwork(String artwork) {
		this.artwork = artwork == null ? "" : artwork;
	}

	public String getMode() {
		return mode;
	}

	public void setMode(String mode) {
		this.mode = mode;
	}

	public int getTileSize() {
		return tileSize;
	}

	public void setTileSize(int tileSize) {
		this.tileSize = Math.max(10, Math.min(400, tileSize));
	}

	public int getOpacity() {
		return opacity;
	}

	public void setOpacity(int opacity) {
		this.opacity = Math.max(0, Math.min(100, opacity));
	}

	public String getFade() {
		return fade;
	}

	public void setFade(String fade) {
		this.fade = fade;
	}

	public int getFadeWidth() {
		return fadeWidth;
	}

	public void setFadeWidth(int fadeWidth) {
		this.fadeWidth = Math.max(5, Math.min(100, fadeWidth));
	}

	public String getStyle() {
		return style;
	}

	public void setStyle(String style) {
		this.style = java.util.List.of(STYLES).contains(style) ? style : "raised";
	}

	/** A copy with another folder name (e.g., for a duplicate). */
	public CustomTheme copy(String newFolder) {
		CustomTheme copy = new CustomTheme(newFolder, dark);
		copy.name = name;
		copy.author = author;
		copy.colors.clear();
		copy.colors.putAll(colors);
		copy.originalColors.putAll(originalColors);
		copy.artwork = artwork;
		copy.mode = mode;
		copy.tileSize = tileSize;
		copy.opacity = opacity;
		copy.fade = fade;
		copy.fadeWidth = fadeWidth;
		copy.style = style;
		return copy;
	}

	/* ######################## Storage ######################## */

	public void save(File dir) throws IOException {
		dir.mkdirs();
		Properties p = toProperties();
		try (OutputStream out = Files.newOutputStream(new File(dir, PROPERTIES).toPath())) {
			p.store(out, "Bio7 theme");
		}
		Files.writeString(new File(dir, CSS).toPath(), toCss());
	}

	/** The settings of the theme (sorted, without the date of the file), e.g., to detect changes. */
	public String signature() {
		return new java.util.TreeMap<Object, Object>(toProperties()).toString();
	}

	private Properties toProperties() {
		Properties p = new Properties();
		p.setProperty("name", name);
		p.setProperty("author", author);
		p.setProperty("base", dark ? "dark" : "light");
		for (Map.Entry<String, RGB> e : colors.entrySet()) {
			p.setProperty("color." + e.getKey(), hex(e.getValue()));
		}
		p.setProperty("artwork", artwork);
		p.setProperty("artwork.mode", mode);
		p.setProperty("artwork.size", Integer.toString(tileSize));
		p.setProperty("artwork.opacity", Integer.toString(opacity));
		p.setProperty("artwork.fade", fade);
		p.setProperty("artwork.fadeWidth", Integer.toString(fadeWidth));
		p.setProperty("toolbar.style", style);
		for (Map.Entry<String, RGB> e : originalColors.entrySet()) {
			p.setProperty("original.color." + e.getKey(), hex(e.getValue()));
		}
		return p;
	}

	public static CustomTheme load(File dir) throws IOException {
		try (InputStream in = Files.newInputStream(new File(dir, PROPERTIES).toPath())) {
			return load(in, dir.getName());
		}
	}

	/** Loads the theme description (e.g., of a bundled example). */
	public static CustomTheme load(InputStream in, String folderName) throws IOException {
		Properties p = new Properties();
		p.load(in);
		boolean dark = !"light".equalsIgnoreCase(p.getProperty("base", "dark"));
		CustomTheme t = new CustomTheme(folderName, dark);
		t.name = p.getProperty("name", folderName);
		t.author = p.getProperty("author", "");
		for (String[] slot : SLOTS) {
			String v = p.getProperty("color." + slot[0]);
			if (v != null && v.matches("#?[0-9a-fA-F]{6}")) {
				t.colors.put(slot[0], parse(v));
			}
		}
		if (p.getProperty("color.panelBackground") == null) {
			/* A theme of an older version (the view panels were the view background)! */
			t.colors.put("panelBackground", t.defaultPanelBackground());
		}
		for (String[] slot : SLOTS) {
			String v = p.getProperty("original.color." + slot[0]);
			if (v != null && v.matches("#?[0-9a-fA-F]{6}")) {
				t.originalColors.put(slot[0], parse(v));
			}
		}
		if (!t.originalColors.isEmpty()) {
			/* All slots (e.g., a slot of a newer version)! */
			for (Map.Entry<String, RGB> e : t.colors.entrySet()) {
				t.originalColors.putIfAbsent(e.getKey(), e.getValue());
			}
		}
		String art = p.getProperty("artwork", "");
		/* Only a file in the theme folder (no paths)! */
		t.artwork = art.contains("/") || art.contains("\\") || art.contains("..") ? "" : art;
		t.mode = p.getProperty("artwork.mode", "tile");
		t.tileSize = parseInt(p.getProperty("artwork.size"), 100);
		t.opacity = parseInt(p.getProperty("artwork.opacity"), 60);
		t.fade = p.getProperty("artwork.fade", "left");
		t.fadeWidth = parseInt(p.getProperty("artwork.fadeWidth"), 70);
		t.setStyle(p.getProperty("toolbar.style", "raised"));
		t.setTileSize(t.tileSize);
		t.setOpacity(t.opacity);
		t.setFadeWidth(t.fadeWidth);
		return t;
	}

	/* ######################## CSS ######################## */

	/** The base stylesheet of the Bio7 theme for the operating system. */
	private String baseStylesheet() {
		String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
		String base = "platform:/plugin/com.eco.bio7.themes/css/";
		if (os.startsWith("mac")) {
			return base + (dark ? "e4-dark_mac.css" : "e4_default_bio7_mac.css");
		} else if (os.startsWith("windows")) {
			return base + (dark ? "e4-dark_win.css" : "e4_default_bio7_windows.css");
		}
		return base + (dark ? "e4-dark.css" : "e4_default_bio7_linux.css");
	}

	/*
	 * The stylesheets which other plugins contribute to the base theme (extension
	 * point org.eclipse.e4.ui.css.swt.theme, e.g., the preferences of the editors in
	 * the dark theme), because they are only applied for the id of the base theme!
	 */
	private java.util.List<String> contributedStylesheets() {
		java.util.List<String> sheets = new java.util.ArrayList<String>();
		String baseId = dark ? DARK_ID : LIGHT_ID;
		String os = org.eclipse.core.runtime.Platform.getOS();
		for (org.eclipse.core.runtime.IConfigurationElement e : org.eclipse.core.runtime.Platform.getExtensionRegistry()
				.getConfigurationElementsFor("org.eclipse.e4.ui.css.swt.theme")) {
			if (!"stylesheet".equals(e.getName()) || e.getAttribute("uri") == null) {
				continue;
			}
			String sheetOs = e.getAttribute("os");
			if (sheetOs != null && !sheetOs.isEmpty() && !sheetOs.equals(os)) {
				continue;
			}
			for (org.eclipse.core.runtime.IConfigurationElement ref : e.getChildren("themeid")) {
				if (baseId.equals(ref.getAttribute("refid"))) {
					String uri = e.getAttribute("uri");
					sheets.add(uri.contains(":") ? uri : "platform:/plugin/" + e.getContributor().getName() + "/" + uri.replaceAll("^/+", ""));
					break;
				}
			}
		}
		return sheets;
	}

	/**
	 * The stylesheet of the theme: the Bio7 theme and the colors of the theme. The
	 * main toolbar is painted by {@link ToolbarArtwork} (gradient and artwork).
	 */
	public String toCss() {
		StringBuilder css = new StringBuilder();
		css.append("/* Bio7 custom theme \"").append(name.replace("*/", "")).append("\" (generated, based on the Bio7 ")
				.append(dark ? "dark" : "light").append(" theme) */\n\n");
		css.append("@import url(\"").append(baseStylesheet()).append("\");\n");
		/* The stylesheets of other plugins for the base theme (e.g., the colors of the editors)! */
		for (String sheet : contributedStylesheets()) {
			css.append("@import url(\"").append(sheet).append("\");\n");
		}
		css.append("\n");
		colorDefinition(css, c("viewBackground"), "DARK_BACKGROUND");
		colorDefinition(css, c("text"), "DARK_FOREGROUND");
		colorDefinition(css, c("tabRow"), "ACTIVE_UNSELECTED_TABS_COLOR_START", "ACTIVE_UNSELECTED_TABS_COLOR_END",
				"INACTIVE_UNSELECTED_TABS_COLOR_START", "INACTIVE_UNSELECTED_TABS_COLOR_END");
		colorDefinition(css, c("tabSelected"), "ACTIVE_TAB_BG_START", "ACTIVE_TAB_BG_END", "INACTIVE_TAB_BG_START", "INACTIVE_TAB_BG_END",
				"ACTIVE_NOFOCUS_TAB_BG_START", "ACTIVE_NOFOCUS_TAB_BG_END");
		colorDefinition(css, c("tabText"), "ACTIVE_TAB_UNSELECTED_TEXT_COLOR", "INACTIVE_TAB_TEXT_COLOR");
		colorDefinition(css, c("tabSelectedText"), "ACTIVE_TAB_SELECTED_TEXT_COLOR", "ACTIVE_NOFOCUS_TAB_SELECTED_TEXT_COLOR",
				"ACTIVE_TAB_TEXT_COLOR", "ACTIVE_NOFOCUS_TAB_TEXT_COLOR");
		colorDefinition(css, c("tabBorder"), "ACTIVE_TAB_OUTER_KEYLINE_COLOR", "ACTIVE_TAB_INNER_KEYLINE_COLOR", "ACTIVE_TAB_OUTLINE_COLOR",
				"INACTIVE_TAB_OUTER_KEYLINE_COLOR", "INACTIVE_TAB_INNER_KEYLINE_COLOR", "INACTIVE_TAB_OUTLINE_COLOR");

		rule(css, ".MTrimmedWindow.topLevel,\n.MPartSashContainer,\n#org-eclipse-ui-trim-vertical1,\n#org-eclipse-ui-trim-vertical2",
				"background-color: " + c("background") + ";");
		rule(css, ".MPartStack,\n.MPartStack.active,\n.MPartStack.active.noFocus",
				"swt-unselected-tabs-color: " + c("tabRow") + ";\n\tswt-selected-tab-fill: " + c("tabSelected") + " " + c("tabSelected")
						+ " 100% 100%;\n\tswt-outer-keyline-color: " + c("tabBorder") + ";\n\tswt-tab-outline: " + c("tabBorder") + ";");
		/* The selected editor tab has the color of the editors (the view background)! */
		rule(css, "#org-eclipse-ui-editorss CTabFolder,\n#org-eclipse-ui-editorss CTabFolder.active",
				"swt-selected-tab-fill: " + c("viewBackground") + ";\n\tswt-selected-tab-highlight: " + c("viewBackground")
						+ ";\n\tswt-unselected-tabs-color: " + c("tabRow") + ";");
		rule(css, "CTabItem", "color: " + c("tabText") + ";");
		/* The selected tab of the tab folders in the views (the base themes: a grey gradient)! */
		rule(css, "CTabItem:selected", "background-color: " + c("tabSelected") + ";\n\tcolor: " + c("tabSelectedText") + ";");
		/*
		 * The background of a tab item of a view is the background of the tab row (the
		 * base themes: ".View"). After "CTabItem:selected" (the same specificity)!
		 */
		rule(css, "CTabItem.MPart", "background-color: " + c("tabRow") + ";");
		/* The tab folders in the views (e.g., the options of the views)! */
		rule(css, ".MPart CTabFolder", "background-color: " + c("tabRow") + ";\n\tcolor: " + c("tabText") + ";");
		/* The view tool bars in the tab rows! */
		rule(css, "CTabFolder ToolBar,\nCTabFolder .MToolBar,\n.MPartStack .MToolBar", "background-color: " + c("tabRow") + ";");
		/* The tool bars of views with an own color in the base themes (more specific selectors)! */
		rule(css, "#org-eclipse-ui-console-ConsoleView.MToolBar,\n#com-eco-bio7-browser-Browser.MToolBar,\n"
				+ "#com-eco-bio7-browser-Browser.MToolBar Label,\n#com-eco-bio7-imagej.MToolBar,\n#com-eco-bio7-thumbnails.MToolBar,\n"
				+ "#com-eco-bio7-thumbnails.MToolBar Label,\n#com-eco-bio7-rbridge-RTable.MToolBar,\nComposite #com-eco-bio7-rbridge-RTable.MToolBar,\n"
				+ "#com-eco-bio7-rbridge-RTable.MToolBar Label,\n#org-eclipse-ui-views-ContentOutline.MToolBar,\n"
				+ "#com-eco-bio7-ijmacro-editor-toolbar-debug.MToolBar", "background-color: " + c("tabRow") + ";");
		/*
		 * Not ".MPart": the tab items have the classes of their parts and the
		 * background of a tab item is the background of the tab row (white tab rows)!
		 */
		rule(css, "Composite.MPart,\n.MPart Composite,\n.MPart Label,\n.MPart CLabel,\n.MPart Group,\n.MPart Link,\n"
				+ ".MPart Sash,\n.MPart SashForm,\n.MPart ScrolledComposite,\n.MPart Canvas,\n.MPart Form,\n.MPart ScrolledForm,\n"
				+ ".MPart Section,\n.MPart ViewForm", "background-color: " + c("panelBackground") + ";\n\tcolor: " + c("text") + ";");
		/* The content of the views (lists, trees, tables)! */
		rule(css, ".MPart Tree,\n.MPart Table,\n.MPart List,\n.MPart Grid", "background-color: " + c("viewBackground") + ";\n\tcolor: " + c("text") + ";");
		/*
		 * The main toolbar (the gradient and the artwork are painted over it)! All
		 * selectors of the base stylesheets with a gradient get a plain color: a CSS
		 * gradient paints over the artwork when a control is resized (e.g., when an
		 * editor is opened) and its images are not disposed.
		 */
		rule(css, ".MTrimBar,\n#org-eclipse-ui-main-toolbar,\n#PerspectiveSwitcher,\n#org-eclipse-ui-main-toolbar .MToolBar,\n"
				+ "#org-eclipse-ui-main-toolbar .MToolControl,\n#org-eclipse-ui-main-toolbar ImageBasedFrame,\n"
				+ "#org-eclipse-ui-main-toolbar ToolBar,\n#org-eclipse-ui-main-toolbar #PerspectiveSwitcher,\n#PerspectiveSwitcher ToolBar",
				"background-color: " + c("toolbarBottom") + ";\n\tcolor: " + c("toolbarText") + ";");
		/* The items have no own background (the toolbar background is painted by ToolbarArtwork)! */
		rule(css, "#org-eclipse-ui-main-toolbar ToolItem,\n#PerspectiveSwitcher ToolItem", "color: " + c("toolbarText") + ";");
		rule(css, "#PerspectiveSwitcher ToolBar", "color: " + c("toolbarText") + ";");
		rule(css, "#org-eclipse-ui-trim-status,\n#org-eclipse-ui-trim-status Composite,\n#org-eclipse-ui-trim-status Label",
				"background-color: " + c("statusBar") + ";\n\tcolor: " + c("text") + ";");
		return css.toString();
	}

	private String c(String key) {
		return "#" + hex(colors.get(key));
	}

	private static void colorDefinition(StringBuilder css, String color, String... names) {
		for (String n : names) {
			css.append("ColorDefinition#org-eclipse-ui-workbench-").append(n).append(" {\n\tcolor: ").append(color).append(";\n}\n\n");
		}
	}

	private static void rule(StringBuilder css, String selector, String body) {
		css.append(selector).append(" {\n\t").append(body).append("\n}\n\n");
	}

	/* ######################## Helper ######################## */

	public static String hex(RGB rgb) {
		return String.format(Locale.ROOT, "%02x%02x%02x", rgb.red, rgb.green, rgb.blue);
	}

	public static RGB parse(String hex) {
		hex = hex.startsWith("#") ? hex.substring(1) : hex;
		int v = Integer.parseInt(hex, 16);
		return new RGB((v >> 16) & 0xff, (v >> 8) & 0xff, v & 0xff);
	}

	private static int parseInt(String s, int def) {
		try {
			return s == null ? def : Integer.parseInt(s.trim());
		} catch (NumberFormatException e) {
			return def;
		}
	}
}
