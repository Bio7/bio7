/*******************************************************************************
 * Copyright (c) 2026 M. Austenfeld
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.nebula.widgets.grid.internal;

import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;

/**
 * Bio7: the measured texts of a font (exact, with the GC) in a cache: the
 * texts of the cells are measured for each painting (e.g., scrolling) and
 * measuring a text is slow (e.g., macOS: about 17 microseconds). Each text is
 * measured once (the latest 50 000 texts of each font), also the widths of the
 * characters, of "..." and the height of a line.
 */
public final class TextWidths {

	private static final int MAX_TEXTS = 50_000;

	private static final class Measures {
		final int[] chars = new int[256];
		int dots = -1;
		int height = -1;
		final Map<String, Integer> widths = new LinkedHashMap<String, Integer>(1024, 0.75f, true) {
			private static final long serialVersionUID = 1L;

			@Override
			protected boolean removeEldestEntry(Map.Entry<String, Integer> eldest) {
				return size() > MAX_TEXTS;
			}
		};
		final Map<String, String> shortTexts = new LinkedHashMap<String, String>(1024, 0.75f, true) {
			private static final long serialVersionUID = 1L;

			@Override
			protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
				return size() > MAX_TEXTS;
			}
		};

		Measures() {
			java.util.Arrays.fill(chars, -1);
		}
	}

	/* The measures of the fonts (a disposed font is removed) */
	private static final Map<Font, Measures> FONTS = new IdentityHashMap<>();
	private static Font lastFont;
	private static Measures lastMeasures;

	private TextWidths() {
	}

	private static Measures measures(GC gc) {
		Font font = gc.getFont();
		if (font == lastFont && lastMeasures != null && !font.isDisposed()) {
			return lastMeasures;
		}
		FONTS.keySet().removeIf(Font::isDisposed);
		Measures m = FONTS.get(font);
		if (m == null) {
			if (FONTS.size() > 32) {
				FONTS.clear();
			}
			m = new Measures();
			FONTS.put(font, m);
		}
		lastFont = font;
		lastMeasures = m;
		return m;
	}

	/** The width of a text in the font of the GC (gc.stringExtent(text).x). */
	public static int width(GC gc, String text) {
		Measures m = measures(gc);
		Integer w = m.widths.get(text);
		if (w == null) {
			w = gc.stringExtent(text).x;
			m.widths.put(text, w);
		}
		return w;
	}

	/** The width of a character in the font of the GC (gc.getCharWidth(c)). */
	public static int charWidth(GC gc, char c) {
		if (c >= 256) {
			return gc.getCharWidth(c);
		}
		Measures m = measures(gc);
		if (m.chars[c] < 0) {
			m.chars[c] = gc.getCharWidth(c);
		}
		return m.chars[c];
	}

	/** The width of "..." in the font of the GC. */
	public static int dots(GC gc) {
		Measures m = measures(gc);
		if (m.dots < 0) {
			m.dots = gc.stringExtent("...").x;
		}
		return m.dots;
	}

	/** The height of a line of text in the font of the GC (gc.stringExtent(text).y). */
	public static int height(GC gc) {
		Measures m = measures(gc);
		if (m.height < 0) {
			m.height = gc.stringExtent("Xg").y;
		}
		return m.height;
	}

	/** A shortened text of the cache (null: not yet shortened, see put). */
	public static String shortText(GC gc, String key) {
		return measures(gc).shortTexts.get(key);
	}

	/** Stores a shortened text. */
	public static void putShortText(GC gc, String key, String text) {
		measures(gc).shortTexts.put(key, text);
	}
}
