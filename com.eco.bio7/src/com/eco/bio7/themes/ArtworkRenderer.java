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
import java.net.MalformedURLException;
import java.util.Locale;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;

/**
 * Renders the background of the main toolbar of a custom theme: a vertical
 * gradient (toolbar top and bottom color) and the artwork (an SVG or PNG image,
 * tiled or stretched) which is blended with the opacity and the fade of the
 * theme (e.g., visible at the left side and faded out to the right).
 */
public final class ArtworkRenderer {

	private ArtworkRenderer() {
	}

	/**
	 * The image data (in pixels of the zoom) for the toolbar size in points.
	 *
	 * @param themeDir the folder of the theme (with the artwork file)
	 */
	public static ImageData render(CustomTheme theme, File themeDir, int width, int height, int zoom) {
		RGB bottom = theme.getColor("toolbarBottom");
		/* A flat toolbar has one color! */
		RGB top = "flat".equals(theme.getStyle()) ? bottom : theme.getColor("toolbarTop");
		return render(theme, themeDir, width, height, zoom, top, bottom, true, theme.getArtwork(), "raised".equals(theme.getStyle()), theme.getFade(),
				theme.getFadeWidth(), theme.getOpacity());
	}

	/**
	 * The image data of a view tab row (in pixels of the zoom): the tab row color
	 * or the gradient to the gradient end color and the artwork of the theme (if
	 * enabled for the tab rows).
	 */
	public static ImageData renderTabs(CustomTheme theme, File themeDir, int width, int height, int zoom) {
		RGB top = theme.getColor("tabRow");
		RGB bottom = theme.isTabGradient() ? theme.getColor("tabRowEnd") : top;
		/* The tab rows have an own fade (e.g., the artwork between the tabs and the tool bars) */
		/* The own image of the tab rows or the image of the toolbar */
		return render(theme, themeDir, width, height, zoom, top, bottom, theme.isTabArtwork(), theme.getTabArtworkImage(), "raised".equals(theme.getTabStyle()),
				theme.getTabFade(),
				theme.getTabFadeWidth(), theme.getTabOpacity());
	}

	private static ImageData render(CustomTheme theme, File themeDir, int width, int height, int zoom, RGB top, RGB bottom,
			boolean withArtwork, String artworkImage, boolean raised, String fadeMode, int fadeWidth, int artworkOpacity) {
		int w = Math.max(1, width * zoom / 100);
		int h = Math.max(1, height * zoom / 100);
		ImageData out = new ImageData(w, h, 24, new PaletteData(0xFF0000, 0xFF00, 0xFF));
		int[] row = new int[w];
		int[][] base = new int[h][];
		for (int y = 0; y < h; y++) {
			double t = h == 1 ? 0 : (double) y / (h - 1);
			int r = (int) Math.round(top.red + (bottom.red - top.red) * t);
			int g = (int) Math.round(top.green + (bottom.green - top.green) * t);
			int b = (int) Math.round(top.blue + (bottom.blue - top.blue) * t);
			base[y] = new int[] { r, g, b };
		}
		ImageData art = withArtwork ? loadArtwork(theme, artworkImage, themeDir, w, h) : null;
		double opacity = artworkOpacity / 100.0;
		double[] fade = new double[w];
		for (int x = 0; x < w; x++) {
			fade[x] = fade(fadeMode, w == 1 ? 0 : (double) x / (w - 1), fadeWidth / 100.0);
		}
		for (int y = 0; y < h; y++) {
			int[] bc = base[y];
			for (int x = 0; x < w; x++) {
				int r = bc[0], g = bc[1], b = bc[2];
				if (art != null && opacity > 0 && fade[x] > 0) {
					int ax = x % art.width;
					int ay = y % art.height;
					RGB p = art.palette.getRGB(art.getPixel(ax, ay));
					double a = alpha(art, ax, ay) * opacity * fade[x];
					r = (int) Math.round(r * (1 - a) + p.red * a);
					g = (int) Math.round(g * (1 - a) + p.green * a);
					b = (int) Math.round(b * (1 - a) + p.blue * a);
				}
				row[x] = (r << 16) | (g << 8) | b;
			}
			out.setPixels(0, y, w, row, 0);
		}
		if (raised) {
			raise(out, zoom);
		}
		return out;
	}

	/*
	 * The raised look of the whole toolbar (over the gradient and the artwork): a
	 * light edge at the top and a dark edge at the bottom!
	 */
	private static void raise(ImageData data, int zoom) {
		int line = Math.max(1, zoom / 100);
		int[] row = new int[data.width];
		for (int y = 0; y < data.height; y++) {
			double light = 0, dark = 0;
			if (y < line) {
				light = 0.45;
			} else if (y < 2 * line) {
				light = 0.18;
			} else if (y >= data.height - line) {
				dark = 0.35;
			} else if (y >= data.height - 2 * line) {
				dark = 0.12;
			}
			if (light == 0 && dark == 0) {
				continue;
			}
			data.getPixels(0, y, data.width, row, 0);
			for (int x = 0; x < row.length; x++) {
				int r = (row[x] >> 16) & 0xff, g = (row[x] >> 8) & 0xff, b = row[x] & 0xff;
				if (light > 0) {
					r = (int) (r + (255 - r) * light);
					g = (int) (g + (255 - g) * light);
					b = (int) (b + (255 - b) * light);
				} else {
					r = (int) (r * (1 - dark));
					g = (int) (g * (1 - dark));
					b = (int) (b * (1 - dark));
				}
				row[x] = (r << 16) | (g << 8) | b;
			}
			data.setPixels(0, y, data.width, row, 0);
		}
	}

	/* The artwork scaled to the toolbar (a tile with the height of the tile size or the full size)! */
	private static ImageData loadArtwork(CustomTheme theme, String name, File themeDir, int w, int h) {
		if (name == null || name.isEmpty() || themeDir == null) {
			return null;
		}
		File file = new File(themeDir, name);
		if (!file.isFile()) {
			return null;
		}
		try {
			ImageDescriptor descriptor = ImageDescriptor.createFromURL(file.toURI().toURL());
			ImageData natural = descriptor.getImageData(100);
			if (natural == null) {
				return null;
			}
			boolean stretch = "stretch".equals(theme.getMode());
			int targetH = stretch ? h : Math.max(4, h * theme.getTileSize() / 100);
			int targetW = stretch ? w : Math.max(4, natural.width * targetH / Math.max(1, natural.height));
			ImageData data;
			if (name.toLowerCase(Locale.ROOT).endsWith(".svg") && !stretch) {
				/* SVG: rendered at the size of the tile (sharp)! */
				int z = Math.max(1, Math.round(100f * targetH / natural.height));
				data = descriptor.getImageData(z);
				if (data == null || data.height != targetH) {
					data = (data == null ? natural : data).scaledTo(targetW, targetH);
				}
			} else {
				data = natural.scaledTo(targetW, targetH);
			}
			return data;
		} catch (MalformedURLException | RuntimeException e) {
			return null;
		}
	}

	private static double alpha(ImageData data, int x, int y) {
		if (data.alphaData != null) {
			return data.getAlpha(x, y) / 255.0;
		}
		if (data.transparentPixel != -1 && data.getPixel(x, y) == data.transparentPixel) {
			return 0;
		}
		return 1;
	}

	/* The visibility of the artwork at the position t (0..1) of the width! */
	static double fade(String fade, double t, double width) {
		switch (fade) {
		case "left":
			return smooth(1 - t / width);
		case "right":
			return smooth(1 - (1 - t) / width);
		case "center":
			return smooth(1 - Math.abs(t - 0.5) / (width / 2));
		case "edges":
			return Math.max(smooth(1 - t / (width / 2)), smooth(1 - (1 - t) / (width / 2)));
		default:
			return 1;
		}
	}

	private static double smooth(double v) {
		v = Math.max(0, Math.min(1, v));
		return v * v * (3 - 2 * v);
	}
}
