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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;

/**
 * Changes the color of the Bio7 *.svg icons. The green of the Bio7 icons
 * (#638d43) and its shades are replaced by the icon color of the preferences
 * (one color for the light and one for the dark theme). The recolored icons are
 * written once to the state location of this plugin and used instead of the
 * original icons. The Eclipse icons and the Bio7 logo keep their colors. A
 * change of the colors is applied after a restart.
 */
public final class IconColors {

	/** Preference key: true if the icons are recolored. */
	public static final String ENABLED = "ICON_COLOR_ENABLED";
	/** Preference key: the icon color of the light theme. */
	public static final String COLOR_LIGHT = "ICON_COLOR_LIGHT";
	/** Preference key: the icon color of the dark theme. */
	public static final String COLOR_DARK = "ICON_COLOR_DARK";

	/** The green of the Bio7 icons. */
	public static final RGB BIO7_GREEN = new RGB(0x63, 0x8d, 0x43);
	/** The default icon color of the dark theme (a lighter green). */
	public static final RGB DARK_GREEN = new RGB(0x86, 0xb5, 0x5a);

	/* Shades of the Bio7 green which are recolored (hue distance in degrees, minimal saturation)! */
	private static final float HUE_RANGE = 40f;
	private static final float MIN_SATURATION = 0.12f;

	private static final Pattern HEX = Pattern.compile("#([0-9a-fA-F]{6}|[0-9a-fA-F]{3})(?![0-9a-fA-F])");

	private static final Map<String, URL> CACHE = new ConcurrentHashMap<String, URL>();

	/* The color of this session (a change is applied after a restart)! */
	private static RGB sessionColor;

	private IconColors() {
	}

	/** The preference store with the icon colors (defaults are set). */
	public static IPreferenceStore getPreferenceStore() {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		store.setDefault(ENABLED, true);
		PreferenceConverter.setDefault(store, COLOR_LIGHT, BIO7_GREEN);
		PreferenceConverter.setDefault(store, COLOR_DARK, DARK_GREEN);
		return store;
	}

	/** The icon color of the preferences for the light or dark theme. */
	public static RGB getPreferenceColor(boolean dark) {
		IPreferenceStore store = getPreferenceStore();
		if (!store.getBoolean(ENABLED)) {
			return BIO7_GREEN;
		}
		return PreferenceConverter.getColor(store, dark ? COLOR_DARK : COLOR_LIGHT);
	}

	/** The icon color which is used in this session. */
	public static synchronized RGB getColor() {
		if (sessionColor == null) {
			try {
				sessionColor = getPreferenceColor(Util.isDarkTheme());
			} catch (RuntimeException e) {
				sessionColor = BIO7_GREEN;
			}
		}
		return sessionColor;
	}

	/** True if the icons are displayed with another color than the Bio7 green. */
	public static boolean isActive() {
		return !BIO7_GREEN.equals(getColor());
	}

	/**
	 * Like {@link AbstractUIPlugin#imageDescriptorFromPlugin(String, String)} but
	 * a *.svg icon is returned with the icon color.
	 */
	public static ImageDescriptor imageDescriptorFromPlugin(String bundleId, String path) {
		if (isActive() && isSvg(path)) {
			URL url = recolored(bundleId, path);
			if (url != null) {
				return ImageDescriptor.createFromURL(url);
			}
		}
		return AbstractUIPlugin.imageDescriptorFromPlugin(bundleId, path);
	}

	/**
	 * Returns the recolored icon for an icon URI of the application model
	 * (platform:/plugin/bundle/path.svg) or the URI itself.
	 */
	public static String recolorURI(String uri) {
		if (uri == null || !isActive() || !isSvg(uri) || !uri.startsWith("platform:/plugin/")) {
			return uri;
		}
		String rest = uri.substring("platform:/plugin/".length());
		int slash = rest.indexOf('/');
		if (slash < 0) {
			return uri;
		}
		URL url = recolored(rest.substring(0, slash), rest.substring(slash + 1));
		return url == null ? uri : url.toString();
	}

	/** Returns the recolored icon for the URL of a *.svg icon or the URL itself. */
	public static URL recolor(URL url, String bundleId, String path) {
		if (url == null || !isActive() || !isSvg(path)) {
			return url;
		}
		URL result = recolored(bundleId, path);
		return result == null ? url : result;
	}

	/**
	 * The recolored icon (a file which is created once) of a *.svg icon in a
	 * bundle or null if the icon keeps its colors.
	 */
	public static URL recolored(String bundleId, String path) {
		if (bundleId == null || !isActive() || !isSvg(path)) {
			return null;
		}
		if (!bundleId.startsWith("com.eco.bio7")) {
			return null;
		}
		String clean = path.replace("$nl$/", "").replaceAll("^/+", "");
		String key = bundleId + "/" + clean;
		URL cached = CACHE.get(key);
		if (cached != null) {
			return cached;
		}
		Bundle bundle = Platform.getBundle(bundleId);
		if (bundle == null) {
			return null;
		}
		URL source = FileLocator.find(bundle, IPath.fromPortableString(clean));
		if (source == null) {
			return null;
		}
		try {
			String svg = read(source);
			if (keepsColors(clean, svg)) {
				return null;
			}
			RGB color = getColor();
			File file = Activator.getDefault().getStateLocation().append("icons").append(hex(color)).append(bundleId).append(clean).toFile();
			if (!file.exists()) {
				file.getParentFile().mkdirs();
				Files.writeString(file.toPath(), recolor(svg, color), StandardCharsets.UTF_8);
			}
			URL url = file.toURI().toURL();
			CACHE.put(key, url);
			return url;
		} catch (IOException | RuntimeException e) {
			return null;
		}
	}

	/* The Eclipse icons and the Bio7 logo keep their colors! */
	private static boolean keepsColors(String path, String svg) {
		String head = svg.length() > 600 ? svg.substring(0, 600) : svg;
		if (head.contains("eclipse.platform.images")) {
			return true;
		}
		String name = path.substring(path.lastIndexOf('/') + 1).toLowerCase(Locale.ROOT);
		return path.contains("bio7app/") || name.startsWith("bio7") || name.startsWith("logo");
	}

	/**
	 * Replaces the Bio7 green and its shades in the svg text by the given color.
	 * The difference to the Bio7 green (hue, saturation, brightness) is kept, so
	 * that lighter and darker shades stay lighter and darker.
	 */
	public static String recolor(String svg, RGB target) {
		float[] base = BIO7_GREEN.getHSB();
		float[] to = target.getHSB();
		Matcher m = HEX.matcher(svg);
		StringBuilder out = new StringBuilder(svg.length());
		while (m.find()) {
			RGB rgb = parse(m.group(1));
			float[] hsb = rgb.getHSB();
			String replacement = m.group();
			float dh = hueDistance(hsb[0], base[0]);
			if (hsb[1] >= MIN_SATURATION && Math.abs(dh) <= HUE_RANGE) {
				float h = (to[0] + dh + 360f) % 360f;
				float s = clamp(hsb[1] * ratio(to[1], base[1]));
				float b = clamp(hsb[2] * ratio(to[2], base[2]));
				replacement = "#" + hex(new RGB(h, s, b));
			}
			m.appendReplacement(out, Matcher.quoteReplacement(replacement));
		}
		m.appendTail(out);
		return out.toString();
	}

	private static boolean isSvg(String path) {
		return path != null && path.toLowerCase(Locale.ROOT).endsWith(".svg");
	}

	private static float hueDistance(float h, float base) {
		float d = (h - base) % 360f;
		if (d > 180f) {
			d -= 360f;
		} else if (d < -180f) {
			d += 360f;
		}
		return d;
	}

	private static float ratio(float to, float base) {
		return base <= 0f ? 1f : to / base;
	}

	private static float clamp(float v) {
		return Math.max(0f, Math.min(1f, v));
	}

	private static RGB parse(String hex) {
		if (hex.length() == 3) {
			hex = "" + hex.charAt(0) + hex.charAt(0) + hex.charAt(1) + hex.charAt(1) + hex.charAt(2) + hex.charAt(2);
		}
		int v = Integer.parseInt(hex, 16);
		return new RGB((v >> 16) & 0xFF, (v >> 8) & 0xFF, v & 0xFF);
	}

	/** The color as hex string without '#', e.g., 638d43. */
	public static String hex(RGB rgb) {
		return String.format(Locale.ROOT, "%02x%02x%02x", rgb.red, rgb.green, rgb.blue);
	}

	private static String read(URL url) throws IOException {
		try (InputStream in = url.openStream()) {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			in.transferTo(out);
			return out.toString(StandardCharsets.UTF_8);
		}
	}
}
