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

	/* macOS: the groups are painted by GroupColors */
	private static final boolean COCOA = "cocoa".equals(org.eclipse.swt.SWT.getPlatform());

	/** The color slots (key, label). */
	public static final String[][] SLOTS = { { "background", "Window background" }, { "viewBackground", "View background" },
			{ "panelBackground", "View panels" }, { "text", "Text" }, { "tabRow", "Tab row" }, { "tabSelected", "Selected tab" },
			{ "tabText", "Tab text" }, { "tabSelectedText", "Selected tab text" }, { "viewToolbarText", "View toolbar text" }, { "tabBorder", "Tab border" }, { "toolbarTop", "Toolbar (top)" },
			{ "toolbarBottom", "Toolbar (bottom)" }, { "toolbarText", "Toolbar text" }, { "statusBar", "Status bar" },
			{ "iconColor", "Icon color" }, { "iconDetail", "Icon detail color" }, { "editorBackground", "Editor background" },
			{ "editorText", "Editor text" }, { "consoleBackground", "Console background" }, { "consoleText", "Console text" },
			{ "widgetBackground", "Buttons" }, { "widgetText", "Buttons text" }, { "fieldBackground", "Text fields" },
			{ "fieldText", "Text fields text" }, { "tabRowEnd", "Tab row (gradient end)" },
			{ "tabSelectedEnd", "Selected tab (gradient end)" }, { "tableHeader", "Table headers" },
			{ "tableHeaderText", "Table headers text" }, { "tableHeaderLine", "Table header lines" }, { "assistBackground", "Code completion" },
			{ "assistText", "Code completion text" }, { "groupBackground", "Groups" }, { "groupBorder", "Group border" },
			{ "inactiveSelection", "Selection (unfocused)" } };

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
	/* The font of the views and the text font of the editors and the console ("" = the default, see FontData) */
	private String font = "";
	private String textFont = "";
	/* The view tab rows: a gradient (to the gradient end colors) and the artwork of the toolbar */
	/* The style of the tab rows (see STYLES): flat, gradient or raised */
	private String tabStyle = "flat";
	private boolean tabArtwork;
	/* The fade of the artwork in the tab rows (an own setting, see FADES) */
	private String tabFade = "center";
	private int tabFadeWidth = 70;
	/* The opacity of the artwork in the tab rows (an own setting) */
	private int tabOpacity = 60;
	/* An own image of the tab rows (a file in the theme folder) or "" for the image of the toolbar */
	private String tabImage = "";

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
			put("iconColor", "dddddd");
			put("iconDetail", "2b2b2b");
			put("editorBackground", "1e1e1e");
			put("editorText", "cccccc");
			put("consoleBackground", "1e1e1e");
			put("consoleText", "cccccc");
			put("widgetBackground", "3c3c3c");
			put("widgetText", "dddddd");
			put("fieldBackground", "2b2b2b");
			put("fieldText", "dddddd");
			put("tabRowEnd", "252526");
			put("viewToolbarText", "cccccc");
			put("tableHeader", "2d2d30");
			put("tableHeaderText", "cccccc");
			put("tableHeaderLine", "4b4c4f");
			put("assistBackground", "2d2d30");
			put("assistText", "cccccc");
			put("groupBackground", "252526");
			put("groupBorder", "3c3c3c");
			put("inactiveSelection", "3a3d41");
			put("tabSelectedEnd", "1e1e1e");
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
			put("editorBackground", "ffffff");
			put("editorText", "000000");
			put("consoleBackground", "ffffff");
			put("consoleText", "000000");
			put("widgetBackground", "ffffff");
			put("widgetText", "000000");
			put("fieldBackground", "ffffff");
			put("fieldText", "000000");
			put("tabRowEnd", "eeeeee");
			put("viewToolbarText", "000000");
			put("tableHeader", "eeeeee");
			put("tableHeaderText", "000000");
			put("tableHeaderLine", "b6bccc");
			put("assistBackground", "f7f7f7");
			put("assistText", "000000");
			put("groupBackground", "ebebeb");
			put("groupBorder", "d0d0d0");
			put("inactiveSelection", "d8e0ea");
			put("tabSelectedEnd", "f4f4f4");
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

	/**
	 * Returns the font of the views (all widgets and tabs) as a FontData string
	 * (see FontData.toString) or "" for the default font.
	 */
	public String getFont() {
		return font;
	}

	public void setFont(String font) {
		this.font = font == null ? "" : font;
	}

	/**
	 * Returns the text font of the editors and the console as a FontData string
	 * or "" for the default text font.
	 */
	public String getTextFont() {
		return textFont;
	}

	public void setTextFont(String textFont) {
		this.textFont = textFont == null ? "" : textFont;
	}

	/** True if the tab rows and the selected tabs have a gradient (to the gradient end colors). */
	public boolean isTabGradient() {
		return !"flat".equals(tabStyle);
	}

	public void setTabGradient(boolean tabGradient) {
		setTabStyle(tabGradient ? "gradient" : "flat");
	}

	/** The style of the tab rows: flat, gradient or raised (see STYLES). */
	public String getTabStyle() {
		return tabStyle;
	}

	public void setTabStyle(String tabStyle) {
		this.tabStyle = java.util.List.of(STYLES).contains(tabStyle) ? tabStyle : "flat";
	}

	/** True if the tab rows show the artwork of the toolbar (image, mode, opacity, fade). */
	public boolean isTabArtwork() {
		return tabArtwork;
	}

	public void setTabArtwork(boolean tabArtwork) {
		this.tabArtwork = tabArtwork;
	}

	/** The fade of the artwork in the tab rows (see FADES). */
	public String getTabFade() {
		return tabFade;
	}

	public void setTabFade(String tabFade) {
		this.tabFade = java.util.List.of(FADES).contains(tabFade) ? tabFade : "center";
	}

	/** The width of the fade of the artwork in the tab rows (5 - 100 %). */
	public int getTabFadeWidth() {
		return tabFadeWidth;
	}

	/** The opacity of the artwork in the tab rows (0 - 100 %). */
	public int getTabOpacity() {
		return tabOpacity;
	}

	public void setTabOpacity(int tabOpacity) {
		this.tabOpacity = Math.max(0, Math.min(100, tabOpacity));
	}

	public void setTabFadeWidth(int tabFadeWidth) {
		this.tabFadeWidth = Math.max(5, Math.min(100, tabFadeWidth));
	}

	/** An own image of the tab rows (a file in the theme folder) or "" for the image of the toolbar artwork. */
	public String getTabImage() {
		return tabImage;
	}

	public void setTabImage(String tabImage) {
		this.tabImage = tabImage == null ? "" : tabImage;
	}

	/** The image of the artwork in the tab rows: the own image or the image of the toolbar. */
	public String getTabArtworkImage() {
		return tabImage.isEmpty() ? artwork : tabImage;
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
		copy.tabImage = tabImage;
		copy.mode = mode;
		copy.tileSize = tileSize;
		copy.opacity = opacity;
		copy.fade = fade;
		copy.fadeWidth = fadeWidth;
		copy.style = style;
		copy.font = font;
		copy.textFont = textFont;
		copy.tabStyle = tabStyle;
		copy.tabArtwork = tabArtwork;
		copy.tabFade = tabFade;
		copy.tabFadeWidth = tabFadeWidth;
		copy.tabOpacity = tabOpacity;
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

	/**
	 * True if the theme has the values of the signature (see signature): the
	 * properties of a newer version (e.g., new colors with a fallback, fonts) which
	 * are not in the signature of an older version are not compared!
	 */
	public boolean matchesSignature(String signature) {
		if (signature.equals(signature())) {
			return true;
		}
		if (!signature.startsWith("{") || !signature.endsWith("}")) {
			return false;
		}
		for (Map.Entry<Object, Object> e : new java.util.TreeMap<Object, Object>(toProperties()).entrySet()) {
			String key = e.getKey().toString();
			boolean inSignature = signature.startsWith("{" + key + "=") || signature.contains(", " + key + "=");
			if (!inSignature) {
				continue;
			}
			String entry = key + "=" + e.getValue();
			boolean same = (signature.startsWith("{" + entry) || signature.contains(", " + entry))
					&& (signature.contains(entry + ", ") || signature.endsWith(entry + "}"));
			if (!same) {
				return false;
			}
		}
		return true;
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
		p.setProperty("font", font);
		p.setProperty("font.text", textFont);
		p.setProperty("tabs.style", tabStyle);
		p.setProperty("tabs.artwork", String.valueOf(tabArtwork));
		p.setProperty("tabs.image", tabImage);
		p.setProperty("tabs.fade", tabFade);
		p.setProperty("tabs.fadeWidth", String.valueOf(tabFadeWidth));
		p.setProperty("tabs.opacity", String.valueOf(tabOpacity));
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
		/* A theme of an older version: the editors and the console like the views */
		for (String[] fallback : new String[][] { { "editorBackground", "viewBackground" }, { "editorText", "text" },
				{ "consoleBackground", "viewBackground" }, { "consoleText", "text" }, { "viewToolbarText", "text" },
				{ "tableHeader", "tabRow" }, { "tableHeaderText", "text" }, { "tableHeaderLine", "tabBorder" },
				{ "assistBackground", "editorBackground" }, { "assistText", "editorText" }, { "groupBackground", "panelBackground" },
				{ "groupBorder", "tabBorder" } }) {
			if (p.getProperty("color." + fallback[0]) == null) {
				t.colors.put(fallback[0], t.colors.get(fallback[1]));
			}
		}
		/* A theme of an older version: the unfocused selection a light tint of the icon color over the view background */
		if (p.getProperty("color.inactiveSelection") == null) {
			RGB view = t.colors.get("viewBackground"), icon = t.colors.get("iconColor");
			double f = t.dark ? 0.22 : 0.16;
			t.colors.put("inactiveSelection", new RGB((int) Math.round(view.red * (1 - f) + icon.red * f),
					(int) Math.round(view.green * (1 - f) + icon.green * f), (int) Math.round(view.blue * (1 - f) + icon.blue * f)));
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
		t.setFont(p.getProperty("font", ""));
		t.setTextFont(p.getProperty("font.text", ""));
		/* The style of the tab rows (an older theme: the gradient switch) */
		t.setTabStyle(p.getProperty("tabs.style", Boolean.parseBoolean(p.getProperty("tabs.gradient", "false")) ? "gradient" : "flat"));
		t.setTabArtwork(Boolean.parseBoolean(p.getProperty("tabs.artwork", "false")));
		String tabArt = p.getProperty("tabs.image", "");
		/* Only a file in the theme folder (no paths)! */
		t.tabImage = tabArt.contains("/") || tabArt.contains("\\") || tabArt.contains("..") ? "" : tabArt;
		/* A theme without an own fade of the tab rows: the fade of the toolbar */
		t.setTabFade(p.getProperty("tabs.fade", t.fade));
		t.setTabFadeWidth(parseInt(p.getProperty("tabs.fadeWidth"), t.fadeWidth));
		/* A theme without an own opacity of the tab rows: the opacity of the toolbar */
		t.setTabOpacity(parseInt(p.getProperty("tabs.opacity"), t.opacity));
		/* A theme without the gradient end colors: no gradient (the colors of the tabs) */
		if (p.getProperty("color.tabRowEnd") == null) {
			t.colors.put("tabRowEnd", t.colors.get("tabRow"));
			t.colors.put("tabSelectedEnd", t.colors.get("tabSelected"));
		}
		/* A theme without the colors of the text fields: the colors of the buttons */
		if (p.getProperty("color.fieldBackground") == null && p.getProperty("color.widgetBackground") != null) {
			t.colors.put("fieldBackground", t.colors.get("widgetBackground"));
			t.colors.put("fieldText", t.colors.get("widgetText"));
		}
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
		/*
		 * The popups of the code completion (the editors without own colors, e.g., R,
		 * Python, ImageJ macro, the JDT editors have own preferences), the additional
		 * information and the hovers: the colors of the JFace color registry are set
		 * with the preferences of the workbench (like the base dark theme)!
		 */
		css.append("IEclipsePreferences#org-eclipse-ui-workbench:bio7-custom-theme {\n\tpreferences:\n");
		for (String[] pref : new String[][] { { "CONTENT_ASSIST_BACKGROUND_COLOR", "assistBackground" }, { "CONTENT_ASSIST_FOREGROUND_COLOR", "assistText" },
				{ "org.eclipse.ui.workbench.INFORMATION_BACKGROUND", "assistBackground" }, { "org.eclipse.ui.workbench.INFORMATION_FOREGROUND", "assistText" },
				{ "org.eclipse.ui.workbench.HOVER_BACKGROUND", "assistBackground" }, { "org.eclipse.ui.workbench.HOVER_FOREGROUND", "assistText" } }) {
			RGB rgb = colors.get(pref[1]);
			css.append("\t\t'").append(pref[0]).append('=').append(rgb.red).append(',').append(rgb.green).append(',').append(rgb.blue).append("'\n");
		}
		css.append("}\n\n");
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
		/*
		 * The tab rows with the tab row color, a gradient or the artwork (painted by
		 * ThemeTabRendering, also flat: the default renderer fills the empty part of
		 * the tab row with the color of the selected tab), the selected tabs with a gradient
		 */
		rule(css, ".MPartStack,\n.MPartStack.active,\n.MPartStack.active.noFocus",
				"swt-tab-renderer: url('bundleclass://com.eco.bio7/com.eco.bio7.themes.ThemeTabRendering');"
						+ (isTabGradient() ? "\n\tswt-selected-tab-fill: " + c("tabSelected") + " " + c("tabSelectedEnd") + " 100% 100%;" : ""));
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
		/*
		 * The background of a tab item is the background of its folder (the empty part
		 * of the tab row and the wrapped tool bars): the base dark theme sets the color
		 * of the selected tab (ACTIVE_TAB_BG_END) with more specific selectors!
		 */
		rule(css, ".MPartStack > CTabItem,\n.MPartStack > CTabItem CLabel,\n.MPartStack.active > CTabItem,\n"
				+ ".MPartStack.active > CTabItem CLabel,\nCTabFolder.MArea CTabItem,\nCTabFolder.MArea CTabItem CLabel,\n"
				+ "CTabFolder > Composite#ToolbarComposite", "background-color: " + c("tabRow") + ";");
		/* The tab folders in the views (e.g., the options of the views)! */
		rule(css, ".MPart CTabFolder", "background-color: " + c("tabRow") + ";\n\tcolor: " + c("tabText") + ";");
		/* The view tool bars in the tab rows! */
		rule(css, "CTabFolder ToolBar,\nCTabFolder .MToolBar,\n.MPartStack .MToolBar,\nCTabFolder ToolItem",
				"background-color: " + c("tabRow") + ";\n\tcolor: " + c("viewToolbarText") + ";");
		/* The tool bars of views with an own color in the base themes (more specific selectors)! */
		rule(css, "#org-eclipse-ui-console-ConsoleView.MToolBar,\n#com-eco-bio7-browser-Browser.MToolBar,\n"
				+ "#com-eco-bio7-browser-Browser.MToolBar Label,\n#com-eco-bio7-imagej.MToolBar,\n#com-eco-bio7-thumbnails.MToolBar,\n"
				+ "#com-eco-bio7-thumbnails.MToolBar Label,\n#com-eco-bio7-rbridge-RTable.MToolBar,\nComposite #com-eco-bio7-rbridge-RTable.MToolBar,\n"
				+ "#com-eco-bio7-rbridge-RTable.MToolBar Label,\n#org-eclipse-ui-views-ContentOutline.MToolBar,\n"
				+ "#com-eco-bio7-ijmacro-editor-toolbar-debug.MToolBar", "background-color: " + c("tabRow") + ";\n\tcolor: " + c("viewToolbarText") + ";");
		/*
		 * Not ".MPart": the tab items have the classes of their parts and the
		 * background of a tab item is the background of the tab row (white tab rows)!
		 */
		/* macOS: not the groups (their content would cover the box painted by GroupColors) */
		rule(css, "Composite.MPart,\n.MPart Composite,\n.MPart Label,\n.MPart CLabel,\n" + (COCOA ? "" : ".MPart Group,\n") + ".MPart Link,\n"
				+ ".MPart Sash,\n.MPart SashForm,\n.MPart ScrolledComposite,\n.MPart Canvas,\n.MPart Form,\n.MPart ScrolledForm,\n"
				+ ".MPart Section,\n.MPart ViewForm", "background-color: " + c("panelBackground") + ";\n\tcolor: " + c("text") + ";");
		/*
		 * Windows: check boxes, radio buttons and sliders paint the system background
		 * (macOS: transparent). Not the push buttons (they keep the native look)!
		 */
		/* The labels of the check boxes and radio buttons have the text color (like the labels, not the push buttons) */
		rule(css, ".MPart Button[style~='SWT.CHECK'],\n.MPart Button[style~='SWT.RADIO'],\n.MPart Scale,\n.MPart Slider",
				"background-color: " + c("panelBackground") + ";\n\tcolor: " + c("text") + ";");
		/* The buttons in the views */
		rule(css, ".MPart Button", "background-color: " + c("widgetBackground") + ";\n\tcolor: " + c("widgetText") + ";");
		/* The text fields and the text areas of the views (e.g., the output of the R shell, not the editors) */
		rule(css, ".MPart Text,\n.MPart Spinner,\n.MPart Combo,\n.MPart CCombo,\n.MPart DateTime,\n.View StyledText",
				"background-color: " + c("fieldBackground") + ";\n\tcolor: " + c("fieldText") + ";");
		/* The console (the text area and its page)! */
		rule(css, "#org-eclipse-ui-console-ConsoleView .MPart > Composite,\n#org-eclipse-ui-console-ConsoleView .MPart StyledText,\n"
				+ "#org-eclipse-ui-console-ConsoleView .MPart PageBook Label,\n"
				+ ".MPartStack.active #org-eclipse-ui-console-ConsoleView .MPart > Composite,\n"
				+ ".MPartStack.active #org-eclipse-ui-console-ConsoleView .MPart StyledText,\n"
				+ ".MPartStack.active #org-eclipse-ui-console-ConsoleView .MPart PageBook Label",
				"background-color: " + c("consoleBackground") + ";\n\tcolor: " + c("consoleText") + ";");
		/* The content of the views (lists, trees, tables)! */
		rule(css, ".MPart Tree,\n.MPart Table,\n.MPart List,\n.MPart Grid", "background-color: " + c("viewBackground") + ";\n\tcolor: " + c("text") + ";");
		/*
		 * The widgets in the groups have the color of the groups (macOS: the groups are
		 * custom boxes with the color, see GroupColors), not the group itself: it has
		 * the color of its parent (around the rounded box)!
		 */
		rule(css, ".MPart Group Composite,\n.MPart Group Label,\n.MPart Group CLabel,\n.MPart Group Link,\n.MPart Group Canvas,\n"
				+ ".MPart Group ScrolledComposite,\n.MPart Group Button[style~='SWT.CHECK'],\n.MPart Group Button[style~='SWT.RADIO'],\n"
				+ ".MPart Group Scale,\n.MPart Group Slider" + (COCOA ? "" : ",\n.MPart Group"),
				"background-color: " + c("groupBackground") + ";");
		/* The links of the forms (e.g., "Create a project..." in the empty Project Explorer have a white background) */
		rule(css, ".MPart Hyperlink,\n.MPart ImageHyperlink,\n.MPart FormText", "background-color: " + c("panelBackground") + ";");
		/*
		 * The dialogs (e.g., the preferences, the wizards): the modal shells and their
		 * widgets like the widgets of the views!
		 */
		rule(css, dialog("", "Composite", "Label", "CLabel", COCOA ? "Link" : "Group", "Link", "Sash", "SashForm", "ScrolledComposite", "Canvas",
				"PageBook", "ViewForm", "Form", "ScrolledForm", "Section", "Hyperlink", "ImageHyperlink", "FormText", "TabFolder", "CTabFolder",
				"Button[style~='SWT.CHECK']", "Button[style~='SWT.RADIO']", "Scale", "Slider", "ProgressBar"),
				"background-color: " + c("panelBackground") + ";\n\tcolor: " + c("text") + ";");
		rule(css, dialog(COCOA ? "Group Composite" : "Group", "Group Composite", "Group Label", "Group CLabel", "Group Link", "Group Canvas", "Group Button[style~='SWT.CHECK']",
				"Group Button[style~='SWT.RADIO']", "Group Scale", "Group Slider"), "background-color: " + c("groupBackground") + ";");
		rule(css, dialog("Button"), "background-color: " + c("widgetBackground") + ";\n\tcolor: " + c("widgetText") + ";");
		rule(css, dialog("Text", "Spinner", "Combo", "CCombo", "DateTime", "StyledText"),
				"background-color: " + c("fieldBackground") + ";\n\tcolor: " + c("fieldText") + ";");
		rule(css, dialog("Tree", "Table", "List", "Grid"), "background-color: " + c("viewBackground") + ";\n\tcolor: " + c("text") + ";");
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
		/*
		 * The status line and the progress area (the base dark theme: the dark background,
		 * the view background of the theme)!
		 */
		rule(css, "StatusLine,\n#org-eclipse-ui-StatusLine,\n#org-eclipse-ui-StatusLine CLabel,\n#org-eclipse-ui-ProgressBar,\n"
				+ "#org-eclipse-ui-ProgressBar Composite,\n#org-eclipse-ui-ProgressBar Canvas",
				"background-color: " + c("statusBar") + ";\n\tcolor: " + c("text") + ";");
		/* The heap status paints with its own colors (set with setBackground and setForeground, the rest see ViewColors)! */
		rule(css, "#org-eclipse-ui-trim-status,\n#org-eclipse-ui-trim-status Composite,\n#org-eclipse-ui-trim-status Label,\nHeapStatus",
				"background-color: " + c("statusBar") + ";\n\tcolor: " + c("text") + ";");
		/* The font of the views (all widgets and the tabs) */
		String fontCss = fontCss(font);
		if (fontCss != null) {
			rule(css, ".MPart,\n.MPart Composite,\n.MPart Label,\n.MPart CLabel,\n.MPart Button,\n.MPart Text,\n.MPart Combo,\n"
					+ ".MPart CCombo,\n.MPart Spinner,\n.MPart List,\n.MPart Tree,\n.MPart Table,\n.MPart Grid,\n.MPart Group,\n"
					+ ".MPart Link,\n.MPart ToolBar,\n.MPart CTabFolder,\nCTabItem,\n.MPartStack,\n.MPartStack CTabItem", fontCss);
			/* The tool bars with text (the views, e.g., the console, the main toolbar, the perspectives, the status bar); the menus are native */
			rule(css, "CTabFolder ToolBar,\nCTabFolder .MToolBar,\n.MPartStack .MToolBar,\n.MToolBar,\n#org-eclipse-ui-main-toolbar ToolBar,\n"
					+ "#PerspectiveSwitcher ToolBar,\n#org-eclipse-ui-trim-status Label,\n#org-eclipse-ui-trim-status Composite,\n"
					/* The tool bars with an own font size in the base themes (more specific selectors) */
					+ "#org-eclipse-ui-console-ConsoleView.MToolBar,\n#com-eco-bio7-browser-Browser.MToolBar,\n#com-eco-bio7-imagej.MToolBar,\n"
					+ "#com-eco-bio7-thumbnails.MToolBar,\n#com-eco-bio7-rbridge-RTable.MToolBar,\n#org-eclipse-ui-views-ContentOutline.MToolBar,\n"
					+ "#com-eco-bio7-ijmacro-editor-toolbar-debug.MToolBar,\n#PerspectiveSwitcher ToolBar", fontCss);
		}
		/* The text font of the editors and the console (and the text areas of the views) */
		String textFontCss = fontCss(textFont);
		if (textFontCss != null) {
			rule(css, "FontDefinition#org-eclipse-jface-textfont,\nFontDefinition#org-eclipse-debug-ui-consoleFont", textFontCss);
			rule(css, ".View StyledText,\n#org-eclipse-ui-console-ConsoleView .MPart StyledText", textFontCss);
		}
		return css.toString();
	}

	/* The CSS of a font (FontData string) or null for the default font */
	private static String fontCss(String fontData) {
		if (fontData == null || fontData.isEmpty()) {
			return null;
		}
		try {
			org.eclipse.swt.graphics.FontData fd = new org.eclipse.swt.graphics.FontData(fontData);
			return "font-family: '" + availableFont(fd.getName()).replace("'", "") + "';\n\tfont-size: " + fd.getHeight() + "px;\n\tfont-weight: "
					+ ((fd.getStyle() & org.eclipse.swt.SWT.BOLD) != 0 ? "bold" : "normal") + ";\n\tfont-style: "
					+ ((fd.getStyle() & org.eclipse.swt.SWT.ITALIC) != 0 ? "italic" : "normal") + ";";
		} catch (RuntimeException e) {
			return null;
		}
	}

	/*
	 * A font name can be a list of alternatives (e.g., "Inter, Segoe UI, Helvetica
	 * Neue"): the first installed font (the stylesheet is written on the computer of
	 * the user) or else the first name!
	 */
	static String availableFont(String names) {
		String[] list = names.split(",");
		if (list.length == 1) {
			return names.trim();
		}
		org.eclipse.swt.widgets.Display display = org.eclipse.swt.widgets.Display.getCurrent();
		if (display != null) {
			for (String name : list) {
				String n = name.trim();
				if (!n.isEmpty() && (display.getFontList(n, true).length > 0 || display.getFontList(n, false).length > 0)) {
					return n;
				}
			}
		}
		return list[0].trim();
	}

	/** A short description of a font (FontData string), e.g., "Verdana 12 bold", or "Default". */
	public static String describeFont(String fontData) {
		if (fontData == null || fontData.isEmpty()) {
			return "Default";
		}
		try {
			org.eclipse.swt.graphics.FontData fd = new org.eclipse.swt.graphics.FontData(fontData);
			return availableFont(fd.getName()) + " " + fd.getHeight() + ((fd.getStyle() & org.eclipse.swt.SWT.BOLD) != 0 ? " bold" : "")
					+ ((fd.getStyle() & org.eclipse.swt.SWT.ITALIC) != 0 ? " italic" : "");
		} catch (RuntimeException e) {
			return "Default";
		}
	}

	private String c(String key) {
		return "#" + hex(colors.get(key));
	}

	private static void colorDefinition(StringBuilder css, String color, String... names) {
		for (String n : names) {
			css.append("ColorDefinition#org-eclipse-ui-workbench-").append(n).append(" {\n\tcolor: ").append(color).append(";\n}\n\n");
		}
	}

	/* The selectors of the widgets in the dialogs (the modal shells), "" for the shells */
	private static String dialog(String... widgets) {
		StringBuilder selector = new StringBuilder();
		for (String shell : new String[] { "Shell[style~='SWT.APPLICATION_MODAL']", "Shell[style~='SWT.PRIMARY_MODAL']" }) {
			for (String widget : widgets) {
				if (selector.length() > 0) {
					selector.append(",\n");
				}
				selector.append(shell);
				if (!widget.isEmpty()) {
					selector.append(' ').append(widget);
				}
			}
		}
		return selector.toString();
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
