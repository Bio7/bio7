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
package com.eco.bio7.simulation;

/**
 * A shape drawn by a model in a time step (drawLine, drawPath, drawPolygon,
 * drawCircle, drawRect, drawText, drawImage, drawLine3 of {@link Simulation}).
 * The coordinates are those of the model: cells of a grid (x to the right, y
 * down), the box of an agent model (x, y, z) or longitude and latitude of a
 * geospatial agent model.
 */
public final class Shape {

	/**
	 * The kind: line, line3, path, polygon, circle, rect, text, image and the 3D
	 * shapes of an agent model pillar, point3, path3, box, sphere, polygon3.
	 */
	public final String kind;

	/**
	 * The coordinates (line: x1, y1, x2, y2; line3: x1, y1, z1, x2, y2, z2; path
	 * and polygon: x0, y0, x1, y1, ...; circle: x, y, radius; rect: x, y, w, h;
	 * text and image: x, y; pillar: x, y, radius, height; point3: x, y, z, size;
	 * path3 and polygon3: x0, y0, z0, x1, y1, z1, ...; box: x, y, z (the corner),
	 * width, height, depth; sphere: x, y, z, radius).
	 */
	public final double[] p;

	/** The color of the line (or of the text), null: none. */
	public final String stroke;

	/** The color of the fill, null: none. */
	public final String fill;

	/** The text or the name of the image. */
	public final String text;

	/** The width of the line in pixels, the size of a text or an image. */
	public final double width;

	/** The angle of an image in degrees (clockwise). */
	public final double angle;

	/**
	 * The height of a 2D shape above the floor of an agent model (rect, circle,
	 * polygon, path, line, text, image; 0: on the floor).
	 */
	public final double z;

	public Shape(String kind, double[] p, String stroke, String fill, String text, double width, double angle) {
		this(kind, p, stroke, fill, text, width, angle, 0);
	}

	public Shape(String kind, double[] p, String stroke, String fill, String text, double width, double angle, double z) {
		this.kind = kind;
		this.z = Double.isFinite(z) ? z : 0;
		this.p = p;
		this.stroke = empty(stroke);
		this.fill = empty(fill);
		this.text = text;
		this.width = width;
		this.angle = angle;
	}

	private static String empty(String c) {
		return c == null || c.isEmpty() || c.equalsIgnoreCase("none") ? null : c;
	}

	/** The shape as JSON (for the HTML export and the scripts). */
	String json() {
		StringBuilder b = new StringBuilder("{\"k\":").append(JavaToJs.quote(kind)).append(",\"p\":[");
		for (int i = 0; i < p.length; i++) {
			b.append(i > 0 ? "," : "").append(Double.isFinite(p[i]) ? Double.toString(p[i]) : "0");
		}
		b.append(']');
		if (stroke != null) {
			b.append(",\"s\":").append(JavaToJs.quote(stroke));
		}
		if (fill != null) {
			b.append(",\"f\":").append(JavaToJs.quote(fill));
		}
		if (text != null) {
			b.append(",\"t\":").append(JavaToJs.quote(text));
		}
		if (z != 0) {
			b.append(",\"z\":").append(z);
		}
		return b.append(",\"w\":").append(width).append(",\"a\":").append(angle).append('}').toString();
	}
}
