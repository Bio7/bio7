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

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.graphics.RGB;

/**
 * The color schemes of the code editors (R, ImageJ macro) for the light and the
 * dark theme: a color (and bold) for each role of the code. An editor maps its
 * color preferences to the roles.
 */
public final class ColorSchemes {

	/** The roles of the code. */
	public enum Role {
		KEYWORD, CONSTANT, BUILTIN, STRING, COMMENT, DEFAULT, OPERATOR, BRACE, NUMBER, ASSIGNMENT, FUNCTION_CALL, FUNCTION_DEFINITION, NAMESPACE,
		/* The comment color unless a scheme has its own (the ImageJ macro editor) */
		MULTILINE_COMMENT
	}

	/** A color and the bold style. */
	public record Style(RGB color, boolean bold) {
	}

	/** A scheme: its name, light or dark, and the styles of the roles. */
	public record Scheme(String name, boolean dark, Map<Role, Style> styles) {
		public Style style(Role role) {
			return styles.get(role);
		}
	}

	/** The preference of the scheme of the light theme (in the store of an editor). */
	public static final String LIGHT_PREFERENCE = "COLOR_SCHEME_LIGHT";
	/** The preference of the scheme of the dark theme. */
	public static final String DARK_PREFERENCE = "COLOR_SCHEME_DARK";

	private static final Map<String, Scheme> SCHEMES = new LinkedHashMap<>();

	/* The former names of renamed schemes (saved preferences keep their scheme) */
	private static final Map<String, String> RENAMED = Map.of("GitHub Light", "Clear Light", "GitHub Dark", "Clear Dark");

	static {
		/* Light: the classic colors of Bio7 (with functions), Clear, Atom One, Solarized */
		add("Bio7 Classic", false, "7F0055", "7F0055", "7F0055", "2A00FF", "808080", "000000", "000000", "000000", "000000", "000000", "00627A",
				"00627A b", "7F0055");
		add("Clear Light", false, "CF222E", "0550AE", "8250DF", "0A3069", "6E7781", "1F2328", "CF222E", "1F2328", "0550AE", "CF222E", "8250DF",
				"6639BA b", "953800");
		add("One Light", false, "A626A4", "986801", "0184BC", "50A14F", "A0A1A7", "383A42", "0184BC", "383A42", "986801", "A626A4", "4078F2",
				"4078F2 b", "C18401");
		add("Solarized Light", false, "859900", "2AA198", "6C71C4", "2AA198", "93A1A1", "586E75", "657B83", "586E75", "D33682", "CB4B16", "268BD2",
				"268BD2 b", "B58900");
		/* Dark: the classic dark colors of Bio7 (with functions), Atom One, Clear, Dracula, Nord, Monokai */
		add("Bio7 Dark", true, "A7EC21", "B166DA", "B166DA", "17C6A3", "808080", "DBB04E", "E6E6FA", "FAF3F3", "6897BB", "FAF3F3", "61AFEF",
				"61AFEF b", "E5C07B");
		add("One Dark", true, "C678DD", "D19A66", "56B6C2", "98C379", "7F848E", "ABB2BF", "56B6C2", "ABB2BF", "D19A66", "C678DD", "61AFEF",
				"61AFEF b", "E5C07B");
		add("Clear Dark", true, "FF7B72", "79C0FF", "D2A8FF", "A5D6FF", "8B949E", "E6EDF3", "FF7B72", "E6EDF3", "79C0FF", "FF7B72", "D2A8FF",
				"D2A8FF b", "FFA657");
		add("Dracula", true, "FF79C6", "BD93F9", "8BE9FD", "F1FA8C", "6272A4", "F8F8F2", "FF79C6", "F8F8F2", "BD93F9", "FF79C6", "50FA7B",
				"50FA7B b", "8BE9FD");
		add("Nord", true, "81A1C1", "B48EAD", "8FBCBB", "A3BE8C", "616E88", "D8DEE9", "81A1C1", "ECEFF4", "B48EAD", "81A1C1", "88C0D0",
				"88C0D0 b", "8FBCBB");
		add("Monokai", true, "F92672", "AE81FF", "66D9EF", "E6DB74", "75715E", "F8F8F2", "F92672", "F8F8F2", "AE81FF", "F92672", "A6E22E",
				"A6E22E b", "66D9EF");
	}

	/* The colors in the order of the roles ("RRGGBB" or "RRGGBB b" for bold) */
	private static void add(String name, boolean dark, String... colors) {
		Map<Role, Style> styles = new EnumMap<>(Role.class);
		Role[] roles = Role.values();
		for (int i = 0; i < roles.length && i < colors.length; i++) {
			String c = colors[i].trim();
			boolean bold = c.endsWith(" b");
			int v = Integer.parseInt(c.substring(0, 6), 16);
			styles.put(roles[i], new Style(new RGB((v >> 16) & 0xFF, (v >> 8) & 0xFF, v & 0xFF), bold));
		}
		/* The classic schemes keep their colors of multiline comments */
		String multiline = name.equals("Bio7 Classic") ? "3F7F5F" : name.equals("Bio7 Dark") ? "FAF3F3" : null;
		if (multiline != null) {
			int v = Integer.parseInt(multiline, 16);
			styles.put(Role.MULTILINE_COMMENT, new Style(new RGB((v >> 16) & 0xFF, (v >> 8) & 0xFF, v & 0xFF), false));
		} else {
			styles.put(Role.MULTILINE_COMMENT, styles.get(Role.COMMENT));
		}
		SCHEMES.put(name, new Scheme(name, dark, styles));
	}

	private ColorSchemes() {
	}

	/** The names of the schemes of the light or the dark theme. */
	public static List<String> names(boolean dark) {
		List<String> names = new ArrayList<>();
		for (Scheme s : SCHEMES.values()) {
			if (s.dark() == dark) {
				names.add(s.name());
			}
		}
		return names;
	}

	/** The default scheme of the light or the dark theme (the classic colors of Bio7). */
	public static String defaultName(boolean dark) {
		return dark ? "Bio7 Dark" : "Bio7 Classic";
	}

	/** The scheme of the name (the default scheme of the theme if unknown). */
	public static Scheme get(String name, boolean dark) {
		Scheme s = name == null ? null : SCHEMES.get(RENAMED.getOrDefault(name, name));
		return s != null && s.dark() == dark ? s : SCHEMES.get(defaultName(dark));
	}

	/** The name of the selected scheme of a theme in the store of an editor. */
	public static Scheme selected(org.eclipse.jface.preference.IPreferenceStore own, boolean dark) {
		return get(own.getString(dark ? DARK_PREFERENCE : LIGHT_PREFERENCE), dark);
	}

	/** Returns true for the classic schemes of Bio7 (the former colors of the editors). */
	public static boolean isClassic(Scheme scheme) {
		return scheme.name().equals(defaultName(false)) || scheme.name().equals(defaultName(true));
	}

	/**
	 * Sets the default colors (colourkey, colourkey1, ...) and bold styles
	 * (BOLD_COLOURKEY, BOLD_COLOURKEY1, ...) of an editor to a scheme.
	 *
	 * @param roles the role of each index (null: not set)
	 */
	public static void setDefaults(org.eclipse.jface.preference.IPreferenceStore store, Scheme scheme, Role[] roles) {
		for (int i = 0; i < roles.length; i++) {
			if (roles[i] == null) {
				continue;
			}
			Style style = scheme.style(roles[i]);
			org.eclipse.jface.preference.PreferenceConverter.setDefault(store, i == 0 ? "colourkey" : "colourkey" + i, style.color());
			store.setDefault(i == 0 ? "BOLD_COLOURKEY" : "BOLD_COLOURKEY" + i, style.bold());
		}
	}

	/** Sets the colors and bold styles of the roles to their defaults (after a scheme was selected). */
	public static void resetToDefaults(org.eclipse.jface.preference.IPreferenceStore store, Role[] roles) {
		for (int i = 0; i < roles.length; i++) {
			store.setToDefault(i == 0 ? "colourkey" : "colourkey" + i);
			store.setToDefault(i == 0 ? "BOLD_COLOURKEY" : "BOLD_COLOURKEY" + i);
		}
	}

	/** The entries of a combo field editor (label, value) of the schemes of a theme. */
	public static String[][] comboEntries(boolean dark) {
		List<String> names = names(dark);
		String[][] entries = new String[names.size()][2];
		for (int i = 0; i < names.size(); i++) {
			entries[i][0] = names.get(i);
			entries[i][1] = names.get(i);
		}
		return entries;
	}
}
