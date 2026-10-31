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
package com.eco.bio7.worldwind;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;

import org.eclipse.swt.graphics.RGB;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;
import com.jogamp.opengl.util.texture.awt.AWTTextureIO;

import gov.nasa.worldwind.layers.CompassLayer;
import gov.nasa.worldwind.render.DrawContext;

/**
 * The compass of the globe with the colors of the theme of Bio7 (set by the
 * theme engine: the colors of a custom theme or the light or dark colors of the
 * default themes). The compass is drawn (not loaded from an image file), the
 * key of the texture contains the colors, so a new texture is created when the
 * colors change.
 */
public class ThemedCompassLayer extends CompassLayer {

	/* The size of the drawn compass (sharp on high DPI displays) */
	private static final int SIZE = 256;
	/* The displayed size (the letters are outside of the ring, larger than the original icon with 64) */
	private static final double ORIGINAL_SIZE = 110;

	private static final RGB[] LIGHT = { new RGB(0xf4, 0xf4, 0xf4), new RGB(0xa0, 0xa6, 0xb2), new RGB(0x33, 0x33, 0x33), new RGB(0x33, 0x33, 0x33) };
	private static final RGB[] DARK = { new RGB(0x3c, 0x3c, 0x3c), new RGB(0x62, 0x66, 0x6b), new RGB(0xdd, 0xdd, 0xdd), new RGB(0xdd, 0xdd, 0xdd) };

	/* The colors (face, ring, text, north) and the key of their texture (changed together) */
	private record State(RGB[] colors, String key) {
	}

	private static volatile State state = new State(LIGHT, key(LIGHT));

	public ThemedCompassLayer() {
		this.iconScale = ORIGINAL_SIZE / SIZE;
	}

	/**
	 * Sets the colors of the compass of a custom theme, called by the theme engine
	 * of Bio7.
	 *
	 * @param face  the background of the compass
	 * @param ring  the ring and the south needle
	 * @param text  the letters of the directions
	 * @param north the north needle
	 */
	public static void setThemeColors(RGB face, RGB ring, RGB text, RGB north) {
		if (face == null || ring == null || text == null || north == null) {
			return;
		}
		set(new RGB[] { face, ring, text, north });
	}

	/** Sets the colors of the light or dark default theme. */
	public static void setDefaultColors(boolean dark) {
		set(dark ? DARK : LIGHT);
	}

	private static void set(RGB[] newColors) {
		state = new State(newColors, key(newColors));
		if (WorldWindView.getWwd() != null) {
			WorldWindView.getWwd().redraw();
		}
	}

	private static String key(RGB[] c) {
		StringBuilder k = new StringBuilder("bio7-themed-compass");
		for (RGB rgb : c) {
			k.append('-').append(Integer.toHexString((rgb.red << 16) | (rgb.green << 8) | rgb.blue));
		}
		return k.toString();
	}

	/* The key of the texture with the colors (a new texture for other colors) */
	@Override
	public String getIconFilePath() {
		return state.key();
	}

	@Override
	protected void initializeTexture(DrawContext dc) {
		State current = state;
		String key = current.key();
		RGB[] c = current.colors();
		if (dc.getTextureCache().getTexture(key) != null) {
			return;
		}
		GL gl = dc.getGL();
		TextureData data = AWTTextureIO.newTextureData(gl.getGLProfile(), draw(c), false);
		Texture texture = TextureIO.newTexture(data);
		texture.bind(gl);
		this.iconWidth = texture.getWidth();
		this.iconHeight = texture.getHeight();
		dc.getTextureCache().put(key, texture);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_EDGE);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP_TO_EDGE);
	}

	/**
	 * Draws the compass rose with the colors (face, ring, text, north): a double
	 * ring, four long main points (beyond the ring) and four short diagonal points,
	 * each point with a light and a dark half, and the letters of the directions
	 * outside of the main points.
	 */
	static BufferedImage draw(RGB[] colors) {
		/* The ring band is translucent, the area between the ring and the star nearly transparent */
		Color band = awt(colors[0], 170);
		Color disc = awt(colors[0], 45);
		Color halo = awt(colors[0], 235);
		Color light = awt(colors[0], 255);
		Color dark = awt(colors[1], 255);
		Color line = awt(colors[2], 255);
		Color north = awt(colors[3], 255);
		Color northDark = awt(mix(colors[3], colors[0], 0.45), 255);
		BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		double c = SIZE / 2.0;
		double outer = 78;
		double inner = 65;
		/* The double ring (a translucent band) and the inner area */
		java.awt.geom.Area ring = new java.awt.geom.Area(new Ellipse2D.Double(c - outer, c - outer, 2 * outer, 2 * outer));
		ring.subtract(new java.awt.geom.Area(new Ellipse2D.Double(c - inner, c - inner, 2 * inner, 2 * inner)));
		g.setColor(band);
		g.fill(ring);
		g.setColor(disc);
		g.fill(new Ellipse2D.Double(c - inner, c - inner, 2 * inner, 2 * inner));
		g.setColor(line);
		g.setStroke(new BasicStroke(2.5f));
		g.draw(new Ellipse2D.Double(c - outer, c - outer, 2 * outer, 2 * outer));
		g.setStroke(new BasicStroke(1.8f));
		g.draw(new Ellipse2D.Double(c - inner, c - inner, 2 * inner, 2 * inner));
		/* The short diagonal points (to the inner ring) */
		for (int i = 0; i < 4; i++) {
			point(g, c, Math.toRadians(45 + i * 90), inner - 3, 17, light, dark, line);
		}
		/* The long main points (beyond the ring), the north point in the north color */
		for (int i = 0; i < 4; i++) {
			boolean isNorth = i == 0;
			point(g, c, Math.toRadians(i * 90), 90, 21, isNorth ? north : light, isNorth ? northDark : dark, line);
		}
		/*
		 * The letters of the directions (outside of the main points) with an outline in
		 * the face color, readable on every background (e.g., dark letters in space)
		 */
		Font font = new Font(Font.SANS_SERIF, Font.BOLD, 34);
		FontMetrics fm = g.getFontMetrics(font);
		double lr = 108;
		String[] letters = { "N", "E", "S", "W" };
		for (int i = 0; i < 4; i++) {
			double a = Math.toRadians(i * 90);
			double x = c + lr * Math.sin(a);
			double y = c - lr * Math.cos(a);
			String s = letters[i];
			java.awt.font.TextLayout layout = new java.awt.font.TextLayout(s, font, g.getFontRenderContext());
			java.awt.Shape glyph = layout.getOutline(java.awt.geom.AffineTransform.getTranslateInstance(x - fm.stringWidth(s) / 2.0,
					y + (fm.getAscent() - fm.getDescent()) / 2.0));
			g.setColor(halo);
			g.setStroke(new BasicStroke(7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.draw(glyph);
			g.setColor(line);
			g.fill(glyph);
		}
		g.dispose();
		return image;
	}

	/*
	 * A point of the compass rose in the direction a (0: north, clockwise): the
	 * tip at the length, the sides at the given distance from the center in the
	 * diagonal directions. The left half is light, the right half dark (with a line
	 * from the tip to the center).
	 */
	private static void point(Graphics2D g, double c, double a, double length, double side, Color light, Color dark, Color line) {
		double tx = c + length * Math.sin(a);
		double ty = c - length * Math.cos(a);
		double lx = c + side * Math.sin(a - Math.PI / 4);
		double ly = c - side * Math.cos(a - Math.PI / 4);
		double rx = c + side * Math.sin(a + Math.PI / 4);
		double ry = c - side * Math.cos(a + Math.PI / 4);
		Path2D left = new Path2D.Double();
		left.moveTo(tx, ty);
		left.lineTo(lx, ly);
		left.lineTo(c, c);
		left.closePath();
		Path2D right = new Path2D.Double();
		right.moveTo(tx, ty);
		right.lineTo(rx, ry);
		right.lineTo(c, c);
		right.closePath();
		g.setColor(light);
		g.fill(left);
		g.setColor(dark);
		g.fill(right);
		g.setColor(line);
		g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		Path2D outline = new Path2D.Double();
		outline.moveTo(lx, ly);
		outline.lineTo(tx, ty);
		outline.lineTo(rx, ry);
		g.draw(outline);
		g.draw(new java.awt.geom.Line2D.Double(tx, ty, c, c));
	}

	private static RGB mix(RGB a, RGB b, double f) {
		return new RGB((int) Math.round(a.red * (1 - f) + b.red * f), (int) Math.round(a.green * (1 - f) + b.green * f),
				(int) Math.round(a.blue * (1 - f) + b.blue * f));
	}

	private static Color awt(RGB rgb, int alpha) {
		return new Color(rgb.red, rgb.green, rgb.blue, alpha);
	}
}
