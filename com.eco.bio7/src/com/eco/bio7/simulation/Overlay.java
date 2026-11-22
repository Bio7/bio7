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

import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;

/** Draws the shapes of a grid model (in cells) with Java2D on the image of ImageJ. */
final class Overlay {

	private Overlay() {
	}

	/**
	 * Draws the shapes.
	 *
	 * @param g      the graphics of the image
	 * @param shapes the shapes (coordinates in cells)
	 * @param scale  the pixels of a cell
	 * @param images the files of the images (by name)
	 */
	static void paint(Graphics2D g, List<Shape> shapes, double scale, Map<String, String> images) {
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		for (Shape s : shapes) {
			double[] p = s.p;
			java.awt.Color stroke = Assets.awt(s.stroke), fill = Assets.awt(s.fill);
			g.setStroke(new BasicStroke((float) Math.max(0.5, s.width), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			switch (s.kind) {
			case "line", "line3" -> {
				if (stroke != null) {
					int k = s.kind.equals("line3") ? 3 : 2;
					g.setColor(stroke);
					g.draw(new java.awt.geom.Line2D.Double(p[0] * scale, p[1] * scale, p[k] * scale, p[k + 1] * scale));
				}
			}
			case "path", "polygon" -> {
				if (p.length < 4) {
					continue;
				}
				Path2D path = new Path2D.Double();
				path.moveTo(p[0] * scale, p[1] * scale);
				for (int i = 2; i + 1 < p.length; i += 2) {
					path.lineTo(p[i] * scale, p[i + 1] * scale);
				}
				if (s.kind.equals("polygon")) {
					path.closePath();
					if (fill != null) {
						g.setColor(fill);
						g.fill(path);
					}
				}
				if (stroke != null) {
					g.setColor(stroke);
					g.draw(path);
				}
			}
			case "circle", "rect" -> {
				java.awt.Shape sh = s.kind.equals("circle") ? new Ellipse2D.Double((p[0] - p[2]) * scale, (p[1] - p[2]) * scale, 2 * p[2] * scale, 2 * p[2] * scale)
						: new Rectangle2D.Double(p[0] * scale, p[1] * scale, p[2] * scale, p[3] * scale);
				if (fill != null) {
					g.setColor(fill);
					g.fill(sh);
				}
				if (stroke != null) {
					g.setColor(stroke);
					g.draw(sh);
				}
			}
			case "text" -> {
				if (s.text != null) {
					g.setColor(stroke != null ? stroke : java.awt.Color.BLACK);
					g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, (int) Math.max(6, Math.round(s.width * scale))));
					g.drawString(s.text, (float) (p[0] * scale), (float) (p[1] * scale));
				}
			}
			case "image" -> {
				BufferedImage img = Assets.image(images.get(s.text));
				if (img == null) {
					continue;
				}
				double size = s.width * scale, k = size / Math.max(img.getWidth(), img.getHeight());
				AffineTransform t = new AffineTransform();
				t.translate(p[0] * scale, p[1] * scale);
				t.rotate(Math.toRadians(s.angle));
				t.scale(k, k);
				t.translate(-img.getWidth() / 2.0, -img.getHeight() / 2.0);
				g.drawImage(img, t, null);
			}
			default -> {
			}
			}
		}
	}
}
