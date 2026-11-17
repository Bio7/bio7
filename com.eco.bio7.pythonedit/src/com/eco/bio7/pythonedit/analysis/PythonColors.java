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
package com.eco.bio7.pythonedit.analysis;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;

/**
 * The colors of the hover: hex colors ('#2B5C8F', '#abc', '#abc8', '#RRGGBBAA') and the
 * named colors of matplotlib (CSS4 and tab:...) in a string of the editor.
 */
final class PythonColors {

	/** A color of the hover: the text in the editor and the hex value (#RRGGBB) with its alpha (0-255). */
	record Color(String text, String hex, int alpha) {
	}

	private static final Pattern HEX = Pattern.compile("#([0-9a-fA-F]{3,4}|[0-9a-fA-F]{6}|[0-9a-fA-F]{8})");

	private static final String[] NAMES = {
			"aliceblue", "#F0F8FF", "antiquewhite", "#FAEBD7", "aqua", "#00FFFF", "aquamarine", "#7FFFD4", "azure", "#F0FFFF", "beige", "#F5F5DC",
			"bisque", "#FFE4C4", "black", "#000000", "blanchedalmond", "#FFEBCD", "blue", "#0000FF", "blueviolet", "#8A2BE2", "brown", "#A52A2A",
			"burlywood", "#DEB887", "cadetblue", "#5F9EA0", "chartreuse", "#7FFF00", "chocolate", "#D2691E", "coral", "#FF7F50", "cornflowerblue", "#6495ED",
			"cornsilk", "#FFF8DC", "crimson", "#DC143C", "cyan", "#00FFFF", "darkblue", "#00008B", "darkcyan", "#008B8B", "darkgoldenrod", "#B8860B",
			"darkgray", "#A9A9A9", "darkgreen", "#006400", "darkgrey", "#A9A9A9", "darkkhaki", "#BDB76B", "darkmagenta", "#8B008B",
			"darkolivegreen", "#556B2F", "darkorange", "#FF8C00", "darkorchid", "#9932CC", "darkred", "#8B0000", "darksalmon", "#E9967A",
			"darkseagreen", "#8FBC8F", "darkslateblue", "#483D8B", "darkslategray", "#2F4F4F", "darkslategrey", "#2F4F4F", "darkturquoise", "#00CED1",
			"darkviolet", "#9400D3", "deeppink", "#FF1493", "deepskyblue", "#00BFFF", "dimgray", "#696969", "dimgrey", "#696969", "dodgerblue", "#1E90FF",
			"firebrick", "#B22222", "floralwhite", "#FFFAF0", "forestgreen", "#228B22", "fuchsia", "#FF00FF", "gainsboro", "#DCDCDC", "ghostwhite", "#F8F8FF",
			"gold", "#FFD700", "goldenrod", "#DAA520", "gray", "#808080", "green", "#008000", "greenyellow", "#ADFF2F", "grey", "#808080",
			"honeydew", "#F0FFF0", "hotpink", "#FF69B4", "indianred", "#CD5C5C", "indigo", "#4B0082", "ivory", "#FFFFF0", "khaki", "#F0E68C",
			"lavender", "#E6E6FA", "lavenderblush", "#FFF0F5", "lawngreen", "#7CFC00", "lemonchiffon", "#FFFACD", "lightblue", "#ADD8E6",
			"lightcoral", "#F08080", "lightcyan", "#E0FFFF", "lightgoldenrodyellow", "#FAFAD2", "lightgray", "#D3D3D3", "lightgreen", "#90EE90",
			"lightgrey", "#D3D3D3", "lightpink", "#FFB6C1", "lightsalmon", "#FFA07A", "lightseagreen", "#20B2AA", "lightskyblue", "#87CEFA",
			"lightslategray", "#778899", "lightslategrey", "#778899", "lightsteelblue", "#B0C4DE", "lightyellow", "#FFFFE0", "lime", "#00FF00",
			"limegreen", "#32CD32", "linen", "#FAF0E6", "magenta", "#FF00FF", "maroon", "#800000", "mediumaquamarine", "#66CDAA", "mediumblue", "#0000CD",
			"mediumorchid", "#BA55D3", "mediumpurple", "#9370DB", "mediumseagreen", "#3CB371", "mediumslateblue", "#7B68EE", "mediumspringgreen", "#00FA9A",
			"mediumturquoise", "#48D1CC", "mediumvioletred", "#C71585", "midnightblue", "#191970", "mintcream", "#F5FFFA", "mistyrose", "#FFE4E1",
			"moccasin", "#FFE4B5", "navajowhite", "#FFDEAD", "navy", "#000080", "oldlace", "#FDF5E6", "olive", "#808000", "olivedrab", "#6B8E23",
			"orange", "#FFA500", "orangered", "#FF4500", "orchid", "#DA70D6", "palegoldenrod", "#EEE8AA", "palegreen", "#98FB98", "paleturquoise", "#AFEEEE",
			"palevioletred", "#DB7093", "papayawhip", "#FFEFD5", "peachpuff", "#FFDAB9", "peru", "#CD853F", "pink", "#FFC0CB", "plum", "#DDA0DD",
			"powderblue", "#B0E0E6", "purple", "#800080", "rebeccapurple", "#663399", "red", "#FF0000", "rosybrown", "#BC8F8F", "royalblue", "#4169E1",
			"saddlebrown", "#8B4513", "salmon", "#FA8072", "sandybrown", "#F4A460", "seagreen", "#2E8B57", "seashell", "#FFF5EE", "sienna", "#A0522D",
			"silver", "#C0C0C0", "skyblue", "#87CEEB", "slateblue", "#6A5ACD", "slategray", "#708090", "slategrey", "#708090", "snow", "#FFFAFA",
			"springgreen", "#00FF7F", "steelblue", "#4682B4", "tab:blue", "#1F77B4", "tab:brown", "#8C564B", "tab:cyan", "#17BECF", "tab:gray", "#7F7F7F",
			"tab:green", "#2CA02C", "tab:olive", "#BCBD22", "tab:orange", "#FF7F0E", "tab:pink", "#E377C2", "tab:purple", "#9467BD", "tab:red", "#D62728",
			"tan", "#D2B48C", "teal", "#008080", "thistle", "#D8BFD8", "tomato", "#FF6347", "turquoise", "#40E0D0", "violet", "#EE82EE", "wheat", "#F5DEB3",
			"white", "#FFFFFF", "whitesmoke", "#F5F5F5", "yellow", "#FFFF00", "yellowgreen", "#9ACD32" };

	private static final Map<String, String> MAP = new HashMap<>();
	static {
		for (int i = 0; i < NAMES.length; i += 2) {
			MAP.put(NAMES[i], NAMES[i + 1]);
		}
	}

	private PythonColors() {
	}

	/** The color of the string at the offset (the content of the quotes) or null. */
	static Color at(IDocument doc, int offset) {
		try {
			IRegion line = doc.getLineInformationOfOffset(offset);
			String text = doc.get(line.getOffset(), line.getLength());
			int pos = offset - line.getOffset();
			int start = pos, end = pos;
			while (start > 0 && text.charAt(start - 1) != '\'' && text.charAt(start - 1) != '"') {
				start--;
			}
			while (end < text.length() && text.charAt(end) != '\'' && text.charAt(end) != '"') {
				end++;
			}
			if (start == 0 || end >= text.length() || text.charAt(start - 1) != text.charAt(end)) {
				return null;
			}
			return of(text.substring(start, end).trim());
		} catch (BadLocationException e) {
			return null;
		}
	}

	/** The color of a text (a hex color or a name of matplotlib) or null. */
	static Color of(String s) {
		if (HEX.matcher(s).matches()) {
			String h = s.substring(1);
			if (h.length() <= 4) {
				/* #rgb, #rgba: each digit twice */
				StringBuilder b = new StringBuilder();
				for (char ch : h.toCharArray()) {
					b.append(ch).append(ch);
				}
				h = b.toString();
			}
			int alpha = h.length() == 8 ? Integer.parseInt(h.substring(6), 16) : 255;
			return new Color(s, "#" + h.substring(0, 6).toUpperCase(Locale.ROOT), alpha);
		}
		String hex = MAP.get(s.toLowerCase(Locale.ROOT).replace(" ", ""));
		return hex == null ? null : new Color(s, hex, 255);
	}
}
