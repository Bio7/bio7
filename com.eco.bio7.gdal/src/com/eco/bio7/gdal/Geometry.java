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
package com.eco.bio7.gdal;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A geometry with its coordinates (parsed from WKT): points, line strings and
 * polygons (also multi geometries and collections). Each part is a list of
 * paths of {x, y} coordinates: a point is one path with one coordinate, a line
 * string one path, a polygon its outer ring and its holes.
 *
 * @param type  POINT, LINESTRING, POLYGON, or COLLECTION (see members), EMPTY
 * @param parts the parts (e.g., the polygons of a multi polygon)
 * @param members the geometries of a collection (else empty)
 */
public record Geometry(String type, List<List<double[][]>> parts, List<Geometry> members) {

	/** Parses WKT (2D, Z and M coordinates; only x and y are kept). */
	public static Geometry parse(String wkt) {
		Parser p = new Parser(wkt);
		Geometry g = p.geometry();
		return g;
	}

	/** All points of all parts and members (e.g., for the extent). */
	public List<double[]> allPoints() {
		List<double[]> out = new ArrayList<>();
		for (List<double[][]> part : parts) {
			for (double[][] path : part) {
				for (double[] c : path) {
					out.add(c);
				}
			}
		}
		for (Geometry m : members) {
			out.addAll(m.allPoints());
		}
		return out;
	}

	/**
	 * The geometry as GeoJSON geometry (2D; a geometry with several parts is a
	 * Multi geometry). Written in Java: OGR_G_ExportToJson of GDAL 3.13 doesn't
	 * free its memory (about 1.2 KB per call).
	 */
	public String toGeoJson() {
		StringBuilder b = new StringBuilder();
		json(b);
		return b.toString();
	}

	private void json(StringBuilder b) {
		switch (type) {
		case "COLLECTION" -> {
			b.append("{\"type\": \"GeometryCollection\", \"geometries\": [");
			for (int i = 0; i < members.size(); i++) {
				if (i > 0) {
					b.append(", ");
				}
				members.get(i).json(b);
			}
			b.append("]}");
		}
		case "EMPTY" -> b.append("{\"type\": \"GeometryCollection\", \"geometries\": []}");
		default -> {
			boolean multi = parts.size() > 1;
			String name = switch (type) {
			case "POINT" -> "Point";
			case "LINESTRING" -> "LineString";
			default -> "Polygon";
			};
			b.append("{\"type\": \"").append(multi ? "Multi" + name : name).append("\", \"coordinates\": ");
			if (multi) {
				b.append('[');
			}
			for (int p = 0; p < parts.size(); p++) {
				if (p > 0) {
					b.append(", ");
				}
				List<double[][]> part = parts.get(p);
				switch (type) {
				case "POINT" -> coordinate(b, part.get(0)[0]);
				case "LINESTRING" -> path(b, part.get(0));
				default -> {
					b.append('[');
					for (int r = 0; r < part.size(); r++) {
						if (r > 0) {
							b.append(", ");
						}
						path(b, part.get(r));
					}
					b.append(']');
				}
				}
			}
			if (multi) {
				b.append(']');
			}
			b.append('}');
		}
		}
	}

	private static void path(StringBuilder b, double[][] path) {
		b.append('[');
		for (int i = 0; i < path.length; i++) {
			if (i > 0) {
				b.append(", ");
			}
			coordinate(b, path[i]);
		}
		b.append(']');
	}

	private static void coordinate(StringBuilder b, double[] c) {
		b.append('[').append(number(c[0])).append(", ").append(number(c[1])).append(']');
	}

	/* A JSON number: whole numbers without ".0", else the shortest form of Java */
	private static String number(double v) {
		if (v == Math.rint(v) && Math.abs(v) < 1e15) {
			return String.valueOf((long) v);
		}
		return Double.toString(v);
	}

	private static final class Parser {
		private final String s;
		private int i;

		Parser(String s) {
			this.s = s;
		}

		Geometry geometry() {
			String word = word().toUpperCase(Locale.ROOT);
			/* Z, M, ZM */
			skipSpaces();
			int save = i;
			String dim = word();
			if (!(dim.equalsIgnoreCase("Z") || dim.equalsIgnoreCase("M") || dim.equalsIgnoreCase("ZM"))) {
				i = save;
			}
			skipSpaces();
			if (s.regionMatches(true, i, "EMPTY", 0, 5)) {
				i += 5;
				return new Geometry("EMPTY", List.of(), List.of());
			}
			switch (word) {
			case "POINT":
				return new Geometry("POINT", List.of(List.<double[][]>of(new double[][] { pointInParens() })), List.of());
			case "LINESTRING":
				return new Geometry("LINESTRING", List.of(List.<double[][]>of(path())), List.of());
			case "POLYGON":
				return new Geometry("POLYGON", List.of(rings()), List.of());
			case "MULTIPOINT": {
				List<List<double[][]>> parts = new ArrayList<>();
				expect('(');
				do {
					skipSpaces();
					double[] c = s.charAt(i) == '(' ? pointInParens() : coordinate();
					parts.add(List.<double[][]>of(new double[][] { c }));
				} while (comma());
				expect(')');
				return new Geometry("POINT", parts, List.of());
			}
			case "MULTILINESTRING": {
				List<List<double[][]>> parts = new ArrayList<>();
				expect('(');
				do {
					parts.add(List.<double[][]>of(path()));
				} while (comma());
				expect(')');
				return new Geometry("LINESTRING", parts, List.of());
			}
			case "MULTIPOLYGON": {
				List<List<double[][]>> parts = new ArrayList<>();
				expect('(');
				do {
					parts.add(rings());
				} while (comma());
				expect(')');
				return new Geometry("POLYGON", parts, List.of());
			}
			case "GEOMETRYCOLLECTION": {
				List<Geometry> members = new ArrayList<>();
				expect('(');
				do {
					skipSpaces();
					members.add(geometry());
				} while (comma());
				expect(')');
				return new Geometry("COLLECTION", List.of(), members);
			}
			default:
				throw new GdalException("Unsupported geometry: " + word);
			}
		}

		private List<double[][]> rings() {
			List<double[][]> rings = new ArrayList<>();
			expect('(');
			do {
				rings.add(path());
			} while (comma());
			expect(')');
			return rings;
		}

		private double[][] path() {
			List<double[]> c = new ArrayList<>();
			expect('(');
			do {
				c.add(coordinate());
			} while (comma());
			expect(')');
			return c.toArray(new double[0][]);
		}

		private double[] pointInParens() {
			expect('(');
			double[] c = coordinate();
			expect(')');
			return c;
		}

		private double[] coordinate() {
			double x = number(), y = number();
			/* Z and M values are skipped */
			while (true) {
				skipSpaces();
				char ch = s.charAt(i);
				if (ch == ',' || ch == ')') {
					break;
				}
				number();
			}
			return new double[] { x, y };
		}

		private double number() {
			skipSpaces();
			int start = i;
			while (i < s.length() && "+-.0123456789eEnaNAif".indexOf(s.charAt(i)) >= 0) {
				i++;
			}
			if (start == i) {
				throw new GdalException("Invalid WKT at " + start + ": " + s.substring(start, Math.min(s.length(), start + 20)));
			}
			return Double.parseDouble(s.substring(start, i));
		}

		private String word() {
			skipSpaces();
			int start = i;
			while (i < s.length() && Character.isLetter(s.charAt(i))) {
				i++;
			}
			return s.substring(start, i);
		}

		private boolean comma() {
			skipSpaces();
			if (i < s.length() && s.charAt(i) == ',') {
				i++;
				return true;
			}
			return false;
		}

		private void expect(char c) {
			skipSpaces();
			if (i >= s.length() || s.charAt(i) != c) {
				throw new GdalException("Invalid WKT: '" + c + "' expected at " + i);
			}
			i++;
		}

		private void skipSpaces() {
			while (i < s.length() && Character.isWhitespace(s.charAt(i))) {
				i++;
			}
		}
	}
}
