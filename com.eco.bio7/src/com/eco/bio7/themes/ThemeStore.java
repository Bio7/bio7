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
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.css.swt.theme.ITheme;
import org.eclipse.e4.ui.css.swt.theme.IThemeEngine;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.Bundle;

import com.eco.bio7.util.IconColors;

import com.eco.bio7.Bio7Plugin;

/**
 * The custom themes: stored in folders of the state location (themes/name),
 * registered at the theme engine (next to the Bio7 default themes which are not
 * changed), activated, exported and imported (*.bio7theme files) and the
 * example themes of this plugin.
 */
public final class ThemeStore {

	/** The extension of an exported theme. */
	public static final String EXTENSION = "bio7theme";
	/* The active custom theme (folder name, empty for a default theme)! */
	private static final String ACTIVE = "THEME_ACTIVE_CUSTOM";
	/* The backup of the icon colors (older versions, removed)! */
	private static final String BACKUP = "THEME_ICON_COLORS_BACKUP.";
	private static final String[] ICON_KEYS = { IconColors.COLOR_LIGHT, IconColors.COLOR_DARK, IconColors.DETAIL_LIGHT, IconColors.DETAIL_DARK };
	/* Allowed files in a theme (no code)! */
	private static final Set<String> ALLOWED = Set.of("properties", "svg", "png", "jpg", "jpeg", "gif");
	private static final long MAX_SIZE = 20L * 1024 * 1024;
	/* Images which are converted to PNG when they are chosen as artwork (e.g., TIFF is not supported by SWT)! */
	private static final Set<String> CONVERTED = Set.of("tif", "tiff", "bmp");
	/* The example themes in this plugin! */
	private static final String EXAMPLES = "themes/examples/";
	/*
	 * The version of the example themes: an example of an older version is
	 * installed again if it has not been changed by the user!
	 */
	private static final int EXAMPLES_VERSION = 7;

	private static final Set<String> REGISTERED = new HashSet<String>();
	private static boolean listening;
	private static boolean iconListener;
	private static boolean applyingIconColors;

	private ThemeStore() {
	}

	/**
	 * Copies the example themes (once), registers the custom themes and activates
	 * the custom theme of the last session (called before the workbench window is
	 * opened, the Bio7 default themes are not changed).
	 */
	public static void startup() {
		installExamples();
		registerAll();
		String active = activeFolder();
		/* The theme of the theme engine (e.g., selected in Preferences > Appearance)! */
		String engineTheme = org.eclipse.core.runtime.preferences.InstanceScope.INSTANCE.getNode("org.eclipse.e4.ui.css.swt.theme").get("themeid", "");
		if (CustomTheme.isCustomThemeId(engineTheme)) {
			active = CustomTheme.folderOf(engineTheme);
		}
		if (!active.isEmpty()) {
			CustomTheme theme = find(active);
			if (theme != null) {
				activate(theme);
			}
		}
	}

	/** The folder with the custom themes. */
	public static File folder() {
		File dir = Bio7Plugin.getDefault().getStateLocation().append("themes").toFile();
		dir.mkdirs();
		return dir;
	}

	public static File dir(CustomTheme theme) {
		return new File(folder(), theme.getFolderName());
	}

	/** All custom themes (sorted by name). */
	public static List<CustomTheme> themes() {
		List<CustomTheme> list = new ArrayList<CustomTheme>();
		File[] dirs = folder().listFiles(File::isDirectory);
		if (dirs != null) {
			for (File dir : dirs) {
				try {
					list.add(CustomTheme.load(dir));
				} catch (IOException | RuntimeException e) {
					/* Not a theme! */
				}
			}
		}
		list.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
		return list;
	}

	public static CustomTheme find(String folderName) {
		File dir = new File(folder(), folderName);
		try {
			return dir.isDirectory() ? CustomTheme.load(dir) : null;
		} catch (IOException e) {
			return null;
		}
	}

	/** A new folder name for a theme name. */
	public static String newFolderName(String name) {
		String base = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
		if (base.isEmpty()) {
			base = "theme";
		}
		String folder = base;
		for (int i = 2; new File(folder(), folder).exists(); i++) {
			folder = base + "-" + i;
		}
		return folder;
	}

	public static void save(CustomTheme theme) throws IOException {
		theme.save(dir(theme));
	}

	public static void delete(CustomTheme theme) {
		if (theme.getFolderName().equals(activeFolder())) {
			activateDefault(theme.isDark());
		}
		deleteTree(dir(theme));
	}

	/* ######################## Theme engine ######################## */

	public static IThemeEngine engine() {
		MApplication app = PlatformUI.getWorkbench().getService(MApplication.class);
		return app == null ? null : app.getContext().get(IThemeEngine.class);
	}

	/*
	 * The stylesheet of a theme saved by an older version of Bio7 is generated
	 * again (e.g., with new rules), before the theme engine reads it!
	 */
	private static void updateCss(CustomTheme theme) {
		File css = new File(dir(theme), CustomTheme.CSS);
		try {
			String generated = theme.toCss();
			if (!css.exists() || !generated.equals(Files.readString(css.toPath()))) {
				Files.writeString(css.toPath(), generated);
			}
		} catch (IOException e) {
			/* The old stylesheet is used! */
		}
	}

	/** Registers the theme (once, the stylesheet is read again when the theme is set). */
	public static void register(CustomTheme theme) {
		IThemeEngine engine = engine();
		if (engine == null || REGISTERED.contains(theme.getThemeId())) {
			return;
		}
		for (ITheme t : engine.getThemes()) {
			if (t.getId().equals(theme.getThemeId())) {
				REGISTERED.add(theme.getThemeId());
				return;
			}
		}
		try {
			String css = new File(dir(theme), CustomTheme.CSS).toURI().toString();
			engine.registerTheme(theme.getThemeId(), theme.getName() + " (Custom)", css);
			REGISTERED.add(theme.getThemeId());
		} catch (IllegalArgumentException e) {
			REGISTERED.add(theme.getThemeId());
		}
	}

	public static void registerAll() {
		for (CustomTheme theme : themes()) {
			updateCss(theme);
			register(theme);
		}
		listenToThemeChanges();
		listenToIconColors();
	}

	/*
	 * The icon colors changed in the icon preferences are the icon colors of the
	 * active custom theme (stored in the theme)!
	 */
	private static void listenToIconColors() {
		if (iconListener) {
			return;
		}
		iconListener = true;
		IconColors.getPreferenceStore().addPropertyChangeListener(event -> {
			String active = activeFolder();
			if (active.isEmpty() || applyingIconColors) {
				return;
			}
			CustomTheme theme = find(active);
			if (theme == null) {
				return;
			}
			String property = event.getProperty();
			IPreferenceStore icons = IconColors.getPreferenceStore();
			String color = theme.isDark() ? IconColors.COLOR_DARK : IconColors.COLOR_LIGHT;
			String detail = theme.isDark() ? IconColors.DETAIL_DARK : IconColors.DETAIL_LIGHT;
			if (property.equals(color) || property.equals(detail)) {
				theme.setColor("iconColor", PreferenceConverter.getColor(icons, color));
				theme.setColor("iconDetail", PreferenceConverter.getColor(icons, detail));
				try {
					save(theme);
				} catch (IOException e) {
					/* The theme keeps its colors! */
				}
			}
		});
	}

	/** The folder of the active custom theme or an empty string. */
	public static String activeFolder() {
		return Bio7Plugin.getDefault().getPreferenceStore().getString(ACTIVE);
	}

	/** Activates a custom theme (colors, artwork and icon colors). */
	public static void activate(CustomTheme theme) {
		try {
			/* The stylesheet is written again (e.g., with the stylesheets of new plugins)! */
			save(theme);
		} catch (IOException e) {
			/* The stylesheet of the last save! */
		}
		register(theme);
		IThemeEngine engine = engine();
		if (engine == null) {
			return;
		}
		Bio7Plugin.getDefault().getPreferenceStore().setValue(ACTIVE, theme.getFolderName());
		setIconColors(theme);
		setEngineTheme(engine, theme.getThemeId());
		ToolbarArtwork.setTheme(theme, dir(theme));
		/* After the preferences of the theme (stylesheets)! */
		org.eclipse.swt.widgets.Display.getDefault().asyncExec(() -> setEditorBackground(theme));
		flush();
	}

	/*
	 * The theme engine does nothing if the theme is already active, so the changed
	 * stylesheet of the active theme (e.g., new colors) is loaded with "force"
	 * (ThemeEngine, not in the interface)!
	 */
	private static void setEngineTheme(IThemeEngine engine, String id) {
		ITheme active = engine.getActiveTheme();
		if (active != null && id.equals(active.getId())) {
			try {
				engine.getClass().getMethod("setTheme", ITheme.class, boolean.class, boolean.class).invoke(engine, active, true, true);
				return;
			} catch (ReflectiveOperationException | RuntimeException e) {
				/* The normal method! */
			}
		}
		engine.setTheme(id, true);
	}

	/* ######################## Editor background ######################## */

	private static final String EDITORS = "org.eclipse.ui.editors";
	private static final String EDITOR_BACKGROUND = "AbstractTextEditor.Color.Background";
	private static final String EDITOR_BACKGROUND_DEFAULT = "AbstractTextEditor.Color.Background.SystemDefault";
	private static final String EDITOR_SET = "THEME_EDITOR_BACKGROUND_SET";

	/* The text editors have the view background of the custom theme! */
	private static void setEditorBackground(CustomTheme theme) {
		RGB view = theme.getColor("viewBackground");
		org.eclipse.core.runtime.preferences.IEclipsePreferences node = org.eclipse.core.runtime.preferences.InstanceScope.INSTANCE.getNode(EDITORS);
		node.putBoolean(EDITOR_BACKGROUND_DEFAULT, false);
		node.put(EDITOR_BACKGROUND, view.red + "," + view.green + "," + view.blue);
		Bio7Plugin.getDefault().getPreferenceStore().setValue(EDITOR_SET, true);
		try {
			node.flush();
		} catch (org.osgi.service.prefs.BackingStoreException e) {
			/* Written at shutdown! */
		}
		flush();
	}

	/* The editor background of a custom theme is removed (the default theme sets its own)! */
	private static void removeEditorBackground() {
		IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
		if (!store.getBoolean(EDITOR_SET)) {
			return;
		}
		org.eclipse.core.runtime.preferences.IEclipsePreferences node = org.eclipse.core.runtime.preferences.InstanceScope.INSTANCE.getNode(EDITORS);
		node.remove(EDITOR_BACKGROUND);
		node.remove(EDITOR_BACKGROUND_DEFAULT);
		store.setToDefault(EDITOR_SET);
		try {
			node.flush();
		} catch (org.osgi.service.prefs.BackingStoreException e) {
			/* Written at shutdown! */
		}
	}

	/* The preferences are written immediately (e.g., before a restart)! */
	private static void flush() {
		for (IPreferenceStore store : new IPreferenceStore[] { Bio7Plugin.getDefault().getPreferenceStore(), IconColors.getPreferenceStore() }) {
			if (store instanceof IPersistentPreferenceStore && store.needsSaving()) {
				try {
					((IPersistentPreferenceStore) store).save();
				} catch (IOException e) {
					/* Written at shutdown! */
				}
			}
		}
	}

	/** Activates the Bio7 light or dark default theme (not changed by the custom themes). */
	public static void activateDefault(boolean dark) {
		Bio7Plugin.getDefault().getPreferenceStore().setValue(ACTIVE, "");
		restoreIconColors();
		removeEditorBackground();
		ToolbarArtwork.setTheme(null, null);
		IThemeEngine engine = engine();
		if (engine != null) {
			engine.setTheme(dark ? CustomTheme.DARK_ID : CustomTheme.LIGHT_ID, true);
		}
		flush();
	}

	/* The artwork follows the theme of the Eclipse theme selection (e.g., Preferences > Appearance)! */
	private static void listenToThemeChanges() {
		if (listening) {
			return;
		}
		IEventBroker broker = PlatformUI.getWorkbench().getService(IEventBroker.class);
		if (broker == null) {
			return;
		}
		listening = true;
		broker.subscribe(IThemeEngine.Events.THEME_CHANGED, event -> {
			Object t = event.getProperty(IThemeEngine.Events.THEME);
			String id = t instanceof ITheme ? ((ITheme) t).getId() : null;
			if (CustomTheme.isCustomThemeId(id)) {
				CustomTheme theme = find(CustomTheme.folderOf(id));
				CustomTheme current = ToolbarArtwork.getTheme();
				if (theme != null && (current == null || !current.getFolderName().equals(theme.getFolderName()))) {
					Bio7Plugin.getDefault().getPreferenceStore().setValue(ACTIVE, theme.getFolderName());
					ToolbarArtwork.setTheme(theme, dir(theme));
					org.eclipse.swt.widgets.Display.getDefault().asyncExec(() -> setEditorBackground(theme));
					flush();
				}
			} else if (ToolbarArtwork.getTheme() != null) {
				Bio7Plugin.getDefault().getPreferenceStore().setValue(ACTIVE, "");
				ToolbarArtwork.setTheme(null, null);
				removeEditorBackground();
				flush();
			}
		});
	}

	/* ######################## Icon colors ######################## */

	private static void setIconColors(CustomTheme theme) {
		IPreferenceStore icons = IconColors.getPreferenceStore();
		applyingIconColors = true;
		try {
			icons.setValue(IconColors.ENABLED, true);
			PreferenceConverter.setValue(icons, theme.isDark() ? IconColors.COLOR_DARK : IconColors.COLOR_LIGHT, theme.getColor("iconColor"));
			PreferenceConverter.setValue(icons, theme.isDark() ? IconColors.DETAIL_DARK : IconColors.DETAIL_LIGHT, theme.getColor("iconDetail"));
		} finally {
			applyingIconColors = false;
		}
	}

	/* The Bio7 default themes have the default icon colors (the Bio7 green and white)! */
	private static void restoreIconColors() {
		IPreferenceStore icons = IconColors.getPreferenceStore();
		IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
		applyingIconColors = true;
		try {
			icons.setToDefault(IconColors.ENABLED);
			for (String key : ICON_KEYS) {
				icons.setToDefault(key);
				/* The backup of older versions! */
				store.setToDefault(BACKUP + key);
			}
		} finally {
			applyingIconColors = false;
		}
	}

	/** True if the icon colors of the session differ from the icon colors of the theme (a restart is needed). */
	public static boolean iconColorsChanged(boolean dark) {
		RGB color = IconColors.getPreferenceColor(dark);
		RGB detail = IconColors.getPreferenceDetail(dark);
		return dark == com.eco.bio7.util.Util.isDarkTheme()
				&& (!color.equals(IconColors.getColor()) || !detail.equals(IconColors.getDetailColor()));
	}

	/* ######################## Export and import ######################## */

	/** Exports the theme folder as one file (zip). */
	public static void export(CustomTheme theme, File file) throws IOException {
		save(theme);
		File dir = dir(theme);
		try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(file.toPath()))) {
			File[] files = dir.listFiles(File::isFile);
			if (files != null) {
				for (File f : files) {
					if (allowed(f.getName()) || f.getName().equals(CustomTheme.CSS)) {
						zip.putNextEntry(new ZipEntry(f.getName()));
						Files.copy(f.toPath(), zip);
						zip.closeEntry();
					}
				}
			}
		}
	}

	/**
	 * The bundled example theme with the folder name of the theme or null (e.g., for
	 * the original colors of an example installed by an older version).
	 */
	public static CustomTheme bundledExample(CustomTheme theme) {
		URL url = Bio7Plugin.getDefault().getBundle().getEntry(EXAMPLES + theme.getFolderName() + "/" + CustomTheme.PROPERTIES);
		if (url == null) {
			return null;
		}
		try (InputStream in = url.openStream()) {
			return CustomTheme.load(in, theme.getFolderName());
		} catch (IOException e) {
			return null;
		}
	}

	/** Imports a theme file (only the theme description and images, no subfolders). */
	public static CustomTheme importTheme(File file) throws IOException {
		File tmp = Files.createTempDirectory("bio7theme").toFile();
		try {
			long total = 0;
			try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(file.toPath()))) {
				ZipEntry entry;
				while ((entry = zip.getNextEntry()) != null) {
					String name = entry.getName();
					if (entry.isDirectory() || name.contains("/") || name.contains("\\") || name.contains("..") || !allowed(name)) {
						continue;
					}
					File out = new File(tmp, name);
					total += Files.copy(zip, out.toPath(), StandardCopyOption.REPLACE_EXISTING);
					if (total > MAX_SIZE) {
						throw new IOException("The theme is too large (max. 20 MB).");
					}
				}
			}
			if (!new File(tmp, CustomTheme.PROPERTIES).isFile()) {
				throw new IOException("The file contains no theme (theme.properties is missing).");
			}
			CustomTheme loaded = CustomTheme.load(tmp);
			CustomTheme theme = loaded.copy(newFolderName(loaded.getName()));
			if (!theme.hasOriginalColors()) {
				theme.markOriginalColors();
			}
			File dir = dir(theme);
			dir.mkdirs();
			File[] files = tmp.listFiles(File::isFile);
			if (files != null) {
				for (File f : files) {
					Files.copy(f.toPath(), new File(dir, f.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
				}
			}
			save(theme);
			register(theme);
			return theme;
		} finally {
			deleteTree(tmp);
		}
	}

	/** Copies an image into the theme folder (the artwork of the theme). */
	public static String copyArtwork(CustomTheme theme, File image) throws IOException {
		String extension = image.getName().substring(image.getName().lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
		boolean convert = CONVERTED.contains(extension);
		if ((!allowed(image.getName()) && !convert) || image.getName().endsWith(".properties")) {
			throw new IOException("Only SVG, PNG, JPG, GIF, TIFF or BMP images are supported.");
		}
		if (image.length() > MAX_SIZE) {
			throw new IOException("The image is too large (max. 20 MB).");
		}
		java.awt.image.BufferedImage converted = null;
		if (convert) {
			converted = javax.imageio.ImageIO.read(image);
			if (converted == null) {
				throw new IOException("The image cannot be read (unsupported " + extension.toUpperCase(Locale.ROOT) + " format).");
			}
		}
		File dir = dir(theme);
		dir.mkdirs();
		String name = "artwork." + (convert ? "png" : extension);
		File[] old = dir.listFiles((d, n) -> n.startsWith("artwork."));
		if (old != null) {
			for (File f : old) {
				f.delete();
			}
		}
		if (converted != null) {
			/* Stored as PNG (supported by SWT and in shared themes)! */
			javax.imageio.ImageIO.write(converted, "png", new File(dir, name));
		} else {
			Files.copy(image.toPath(), new File(dir, name).toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
		return name;
	}

	private static boolean allowed(String name) {
		int dot = name.lastIndexOf('.');
		return dot > 0 && ALLOWED.contains(name.substring(dot + 1).toLowerCase(Locale.ROOT));
	}

	/* ######################## Examples ######################## */

	/** Copies the example themes of this plugin (once, a deleted example is not copied again). */
	public static void installExamples() {
		IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
		Bundle bundle = Bio7Plugin.getDefault().getBundle();
		Enumeration<String> paths = bundle.getEntryPaths(EXAMPLES);
		if (paths == null) {
			return;
		}
		while (paths.hasMoreElements()) {
			String path = paths.nextElement();
			String folder = path.substring(EXAMPLES.length()).replace("/", "");
			if (folder.isEmpty() || store.getInt("THEME_EXAMPLE_VERSION." + folder) >= EXAMPLES_VERSION) {
				continue;
			}
			File dir = new File(folder(), folder);
			if (store.getBoolean("THEME_EXAMPLE_INSTALLED." + folder) && !unchangedExample(dir, store.getString("THEME_EXAMPLE_SIGNATURE." + folder))) {
				/* Changed by the user (or deleted): not installed again! */
				store.setValue("THEME_EXAMPLE_VERSION." + folder, EXAMPLES_VERSION);
				continue;
			}
			Enumeration<String> files = bundle.getEntryPaths(path);
			if (files == null) {
				continue;
			}
			dir.mkdirs();
			while (files.hasMoreElements()) {
				String file = files.nextElement();
				URL url = bundle.getEntry(file);
				try (InputStream in = url.openStream()) {
					Files.copy(in, new File(dir, file.substring(file.lastIndexOf('/') + 1)).toPath(), StandardCopyOption.REPLACE_EXISTING);
				} catch (IOException e) {
					/* Next file! */
				}
			}
			try {
				/* Writes the stylesheet (for the operating system)! */
				CustomTheme theme = CustomTheme.load(dir);
				theme.markOriginalColors();
				save(theme);
				store.setValue("THEME_EXAMPLE_SIGNATURE." + folder, theme.signature());
			} catch (IOException e) {
				deleteTree(dir);
				continue;
			}
			store.setValue("THEME_EXAMPLE_INSTALLED." + folder, true);
			store.setValue("THEME_EXAMPLE_VERSION." + folder, EXAMPLES_VERSION);
		}
		flush();
	}

	/*
	 * True if the installed example is unchanged. The examples of the first version
	 * have no signature (installed again, the themes were new in this version)!
	 */
	private static boolean unchangedExample(File dir, String signature) {
		if (!dir.isDirectory()) {
			return false;
		}
		if (signature.isEmpty()) {
			return true;
		}
		try {
			return CustomTheme.load(dir).signature().equals(signature);
		} catch (IOException e) {
			return false;
		}
	}

	private static void deleteTree(File file) {
		File[] children = file.listFiles();
		if (children != null) {
			for (File c : children) {
				deleteTree(c);
			}
		}
		file.delete();
	}

}
