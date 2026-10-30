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

import org.eclipse.e4.ui.workbench.renderers.swt.CTabRendering;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageDataProvider;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.ImageGcDrawer;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

/**
 * The tab renderer of a custom theme with a gradient or the artwork in the tab
 * rows of the views (set by the CSS of every custom theme, also with a flat tab row).
 * The tab row is painted with a picture (the tab row color or the gradient to
 * the gradient end color and the artwork of the toolbar, see
 * ArtworkRenderer.renderTabs) under the tabs. The rest is painted like the
 * default renderer.
 */
public class ThemeTabRendering extends CTabRendering {

	private static final Color TRANSPARENT = new Color(0, 0, 0, 0);
	private static final boolean WINDOWS = "win32".equals(SWT.getPlatform());
	private static final boolean GTK = "gtk".equals(SWT.getPlatform());
	/* The part of the picture under a tool bar (Windows, see setPart) and its bounds */
	private static final String PART = "bio7.themes.tabPart";
	private static final String PART_KEY = "bio7.themes.tabPartKey";
	private static final String PENDING = "bio7.themes.tabPending";

	private final CTabFolder folder;
	private Image picture;
	/* The tab row is the first row of the continuous artwork directly below the toolbar (see paintTopEdge) */
	private boolean continuousTop;
	/* The tab row of the last paint */
	private Rectangle header;
	/* A control of the tab row with a paint listener (see paintBehind) */
	private static final String PAINTED = "bio7.themes.tabPainted";
	private Object pictureKey;

	public ThemeTabRendering(CTabFolder parent) {
		super(parent);
		folder = parent;
		parent.addDisposeListener(_ -> disposePicture());
	}

	@Override
	protected void draw(int part, int state, Rectangle bounds, GC gc) {
		CustomTheme theme = ToolbarArtwork.getTheme();
		boolean translucent = theme != null && theme.getTabSelectedOpacity() < 100;
		if (part >= 0 && (state & SWT.SELECTED) != 0 && translucent) {
			drawTranslucentTab(part, state, bounds, gc, theme.getTabSelectedOpacity());
		} else {
			super.draw(part, state, bounds, gc);
		}
		if (part == PART_BACKGROUND) {
			paintPicture(gc, bounds);
		} else {
			/* The border line at the top is drawn by several parts (e.g., the border, the selected tab) */
			paintTopEdge(gc);
		}
	}

	/*
	 * The selected tab with a translucent fill: the picture of the tab row (painted
	 * before the tabs) shines through. The GC of SWT ignores the alpha of a color,
	 * so the tab is painted twice offscreen (a black and a white fill): the
	 * difference is the fill, the rest (text, image, close button, with their
	 * antialiasing) is the same. The content is painted opaque over the fill with
	 * the opacity of the theme.
	 */
	private void drawTranslucentTab(int part, int state, Rectangle bounds, GC gc, int opacity) {
		Color[] original = fillColors();
		if (original == null || original.length == 0 || original[0] == null || original[0].isDisposed()) {
			super.draw(part, state, bounds, gc);
			return;
		}
		RGB fill = original[0].getRGB();
		/* A little larger (the outline of the tab) */
		Rectangle area = new Rectangle(bounds.x - 2, bounds.y - 2, bounds.width + 4, bounds.height + 4);
		Display display = folder.getDisplay();
		Image result = null;
		try {
			result = new Image(display, (ImageDataProvider) zoom -> {
				ImageData black = renderTab(part, state, bounds, area, original, new Color(0, 0, 0), zoom);
				ImageData white = renderTab(part, state, bounds, area, original, new Color(255, 255, 255), zoom);
				return black == null || white == null ? null : composite(black, white, fill, opacity / 100.0);
			});
			gc.drawImage(result, area.x, area.y);
		} catch (RuntimeException e) {
			super.draw(part, state, bounds, gc);
		} finally {
			if (result != null && !result.isDisposed()) {
				result.dispose();
			}
		}
	}

	private Color[] fillColors() {
		try {
			java.lang.reflect.Field field = CTabRendering.class.getDeclaredField("selectedTabFillColors");
			field.setAccessible(true);
			return (Color[]) field.get(this);
		} catch (ReflectiveOperationException | RuntimeException e) {
			return null;
		}
	}

	private void setFillColors(Color[] colors) {
		try {
			java.lang.reflect.Field field = CTabRendering.class.getDeclaredField("selectedTabFillColors");
			field.setAccessible(true);
			field.set(this, colors);
		} catch (ReflectiveOperationException | RuntimeException e) {
			/* The colors of the CSS */
		}
	}

	/* The selected tab with the fill color offscreen (transparent where the renderer paints nothing) */
	private ImageData renderTab(int part, int state, Rectangle bounds, Rectangle area, Color[] original, Color fill, int zoom) {
		Color[] colors = new Color[original.length];
		java.util.Arrays.fill(colors, fill);
		Image image = new Image(folder.getDisplay(), new ImageGcDrawer() {
			@Override
			public void drawOn(GC offscreen, int width, int height) {
				org.eclipse.swt.graphics.Transform transform = new org.eclipse.swt.graphics.Transform(offscreen.getDevice());
				transform.translate(-area.x, -area.y);
				offscreen.setTransform(transform);
				setFillColors(colors);
				/*
				 * The highlight and the outlines of the tab with the fill color: translucent like the
				 * fill (else lines over the picture, e.g., the highlight of the editors). Not null: the
				 * renderer paints system colors then!
				 */
				Object highlight = field("selectedTabHighlightColor");
				Object outline = field("tabOutlineColor");
				Object keyline = field("outerKeylineColor");
				setField("selectedTabHighlightColor", fill);
				setField("tabOutlineColor", fill);
				setField("outerKeylineColor", fill);
				try {
					superDraw(part, state, bounds, offscreen);
				} finally {
					setFillColors(original);
					setField("selectedTabHighlightColor", highlight);
					setField("tabOutlineColor", outline);
					setField("outerKeylineColor", keyline);
					offscreen.setTransform(null);
					transform.dispose();
				}
			}

			@Override
			public int getGcStyle() {
				return SWT.TRANSPARENT;
			}
		}, area.width, area.height);
		try {
			return image.getImageData(zoom);
		} finally {
			image.dispose();
		}
	}

	private Object field(String name) {
		try {
			java.lang.reflect.Field f = CTabRendering.class.getDeclaredField(name);
			f.setAccessible(true);
			return f.get(this);
		} catch (ReflectiveOperationException | RuntimeException e) {
			return null;
		}
	}

	private void setField(String name, Object value) {
		try {
			java.lang.reflect.Field f = CTabRendering.class.getDeclaredField(name);
			f.setAccessible(true);
			f.set(this, value);
		} catch (ReflectiveOperationException | RuntimeException e) {
			/* The color of the CSS */
		}
	}

	private void superDraw(int part, int state, Rectangle bounds, GC gc) {
		super.draw(part, state, bounds, gc);
	}

	/*
	 * The tab over the picture: the fill (the difference of the black and the white
	 * fill) with the opacity, the content (the same in both) opaque over it.
	 */
	private static ImageData composite(ImageData black, ImageData white, RGB fill, double opacity) {
		int w = Math.min(black.width, white.width), h = Math.min(black.height, white.height);
		ImageData out = new ImageData(w, h, 24, new PaletteData(0xFF0000, 0xFF00, 0xFF));
		byte[] alpha = new byte[w * h];
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				int coverage = black.getAlpha(x, y);
				if (coverage == 0) {
					continue;
				}
				RGB b = unpremultiply(black.palette.getRGB(black.getPixel(x, y)), coverage);
				RGB t = unpremultiply(white.palette.getRGB(white.getPixel(x, y)), white.getAlpha(x, y));
				/* The share of the fill: the difference of the two fills (0: content, 1: only the fill) */
				double share = Math.max(Math.max(t.red - b.red, t.green - b.green), t.blue - b.blue) / 255.0;
				share = Math.max(0, Math.min(1, share));
				double content = 1 - share;
				/* The content without the fill (black fill: the content times its share) */
				double cr = content > 0 ? b.red / content : 0, cg = content > 0 ? b.green / content : 0, cb = content > 0 ? b.blue / content : 0;
				double a = content + share * opacity;
				int r = 0, g = 0, bl = 0;
				if (a > 0) {
					r = clamp((content * cr + share * opacity * fill.red) / a);
					g = clamp((content * cg + share * opacity * fill.green) / a);
					bl = clamp((content * cb + share * opacity * fill.blue) / a);
				}
				out.setPixel(x, y, (r << 16) | (g << 8) | bl);
				alpha[y * w + x] = (byte) clamp(a * coverage);
			}
		}
		out.alphaData = alpha;
		return out;
	}

	/* The colors of the partly covered pixels (e.g., the antialiased edges) are multiplied with their alpha */
	private static RGB unpremultiply(RGB rgb, int alpha) {
		if (alpha <= 0 || alpha >= 255) {
			return rgb;
		}
		return new RGB(clamp(rgb.red * 255.0 / alpha), clamp(rgb.green * 255.0 / alpha), clamp(rgb.blue * 255.0 / alpha));
	}

	private static int clamp(double v) {
		return (int) Math.max(0, Math.min(255, Math.round(v)));
	}

	/*
	 * The continuous artwork: the outer border line at the top of a tab row directly
	 * below the toolbar is painted over with the picture (no line between the
	 * toolbar and the tab row, e.g., without a gap below the toolbar).
	 */
	private void paintTopEdge(GC gc) {
		Image image = picture;
		Rectangle row = header;
		if (!continuousTop || image == null || image.isDisposed() || row == null || folder.getTabPosition() != SWT.TOP) {
			return;
		}
		Rectangle clipping = gc.getClipping();
		/* The top line and the lines at the left and the right of the tab row (the picture continues to the next views) */
		for (Rectangle edge : new Rectangle[] { new Rectangle(row.x, row.y, row.width, 1), new Rectangle(row.x, row.y, 1, row.height),
				new Rectangle(row.x + row.width - 1, row.y, 1, row.height) }) {
			gc.setClipping(clipping.intersection(edge));
			gc.drawImage(image, row.x, row.y);
		}
		gc.setClipping(clipping);
	}

	/* The picture of the tab row over the background of the tab row (the tabs are painted later) */
	private void paintPicture(GC gc, Rectangle bounds) {
		CustomTheme theme = ToolbarArtwork.getTheme();
		File dir = ToolbarArtwork.getThemeDir();
		if (folder.isDisposed()) {
			return;
		}
		/* A default theme (e.g., the dark theme): the color of the tab row of this renderer (from the CSS) */
		org.eclipse.swt.graphics.RGB rowColor = theme == null ? tabRowColor() : null;
		if (theme == null && rowColor == null) {
			return;
		}
		Point size = folder.getSize();
		int height = folder.getTabHeight() + 2;
		if (size.x <= 0 || height <= 2) {
			return;
		}
		boolean onBottom = folder.getTabPosition() == SWT.BOTTOM;
		Rectangle header = new Rectangle(0, onBottom ? size.y - height : 0, size.x, height);
		Rectangle area = header.intersection(bounds);
		if (area.isEmpty()) {
			return;
		}
		/* The continuous artwork: the part of the band of the toolbar at the position of the tab row */
		ArtworkRenderer.Band band = theme != null ? ToolbarArtwork.bandOf(folder, header.x, header.y) : null;
		/* The tab rows below the first tab rows: the picture, no artwork or the artwork of the view tabs */
		String lower = band != null && ToolbarArtwork.isBelowBand(folder) ? theme.getTabsLower() : "band";
		if (!"band".equals(lower)) {
			band = null;
		}
		boolean plain = "none".equals(lower);
		Object key = size.x + "x" + height + "/" + ToolbarArtwork.getVersion() + "/" + (theme != null ? System.identityHashCode(theme) : rowColor) + "/" + band + "/" + lower;
		if (picture == null || picture.isDisposed() || !key.equals(pictureKey)) {
			Image old = picture;
			final int width = size.x;
			final int h = height;
			ArtworkRenderer.Band part = band;
			picture = new Image(folder.getDisplay(), (ImageDataProvider) zoom -> theme != null
					? plain ? ArtworkRenderer.renderTabColors(theme, dir, width, h, zoom) : ArtworkRenderer.renderTabs(theme, dir, width, h, zoom, part)
					: solid(rowColor, width * zoom / 100, h * zoom / 100));
			pictureKey = key;
			if (old != null && !old.isDisposed()) {
				old.dispose();
			}
			/* The controls of the tab row paint the new picture */
			folder.redraw(header.x, header.y, header.width, header.height, true);
		}
		Rectangle clipping = gc.getClipping();
		gc.setClipping(clipping.intersection(area));
		gc.drawImage(picture, header.x, header.y);
		gc.setClipping(clipping);
		this.header = header;
		/* A tab row of the band directly below the toolbar (see paintTopEdge) */
		continuousTop = band != null && !ToolbarArtwork.isBelowBand(folder) && ToolbarArtwork.isBelowToolbar(folder);
		for (Control child : folder.getChildren()) {
			/* Not the content of the views (below the tab row) */
			if (inTabRow(child, header)) {
				paintBehind(child);
			}
		}
	}

	/* The color of the unselected tabs of the renderer (set by the CSS of the default theme) or null */
	private org.eclipse.swt.graphics.RGB tabRowColor() {
		try {
			java.lang.reflect.Field field = CTabRendering.class.getDeclaredField("unselectedTabsColors");
			field.setAccessible(true);
			Color[] colors = (Color[]) field.get(this);
			return colors != null && colors.length > 0 && colors[0] != null && !colors[0].isDisposed() ? colors[0].getRGB() : null;
		} catch (ReflectiveOperationException | RuntimeException e) {
			return null;
		}
	}

	private static org.eclipse.swt.graphics.ImageData solid(org.eclipse.swt.graphics.RGB rgb, int width, int height) {
		org.eclipse.swt.graphics.ImageData data = new org.eclipse.swt.graphics.ImageData(Math.max(1, width), Math.max(1, height), 24,
				new org.eclipse.swt.graphics.PaletteData(0xFF0000, 0xFF00, 0xFF));
		int pixel = data.palette.getPixel(rgb);
		for (int y = 0; y < data.height; y++) {
			for (int x = 0; x < data.width; x++) {
				data.setPixel(x, y, pixel);
			}
		}
		return data;
	}

	private static boolean inTabRow(Control child, Rectangle header) {
		Rectangle bounds = child.getBounds();
		if (bounds.y >= header.height) {
			return false;
		}
		/*
		 * GTK: also a tool bar higher than the tab row (e.g., minimize and maximize of
		 * the empty editor area with a low tab row). It is transparent, so the folder
		 * is visible below the picture (not the color of the CSS)!
		 */
		return bounds.height <= header.height + 4 || (GTK && child instanceof ToolBar);
	}

	/*
	 * The controls in the tab row (e.g., the view menu, minimize and maximize and
	 * the tool bars of a view in the tab row, also in a composite) paint the part
	 * of the picture under them as their background: the colors set by the CSS
	 * engine (also again after a view was moved) are painted over and a background
	 * image of the folder would be drawn shifted by macOS (as a pattern). Windows
	 * erases a tool bar with its background (the paint comes too late) and fills
	 * the tool items with their color, so a tool bar gets the part of the picture
	 * as its own background image there (see setPart)!
	 */
	private void paintBehind(Control control) {
		if (WINDOWS && control instanceof ToolBar toolBar) {
			if (control.getData(PAINTED) != this) {
				control.setData(PAINTED, this);
				/* The part is set again when the tool bar is moved or styled again (e.g., by the CSS engine) */
				Listener update = _ -> later(toolBar);
				control.addListener(SWT.Paint, update);
				control.addListener(SWT.Move, update);
				control.addListener(SWT.Resize, update);
				control.addListener(SWT.Dispose, _ -> disposePart(toolBar));
			}
			/*
			 * Now (in the paint of the folder): the tool bar is painted after the folder, so
			 * it is not painted with the old part (e.g., when the folder is resized)!
			 */
			setPart(toolBar);
			return;
		}
		/* Only one listener of the current renderer (e.g., after another theme was activated) */
		if (control.getData(PAINTED) != this) {
			control.setData(PAINTED, this);
			control.addListener(SWT.Paint, event -> {
				if (control.getData(PAINTED) != this || picture == null || picture.isDisposed() || header == null || folder.isDisposed() || !isInFolder(control)) {
					return;
				}
				Point p = folder.toControl(control.toDisplay(0, 0));
				event.gc.drawImage(picture, header.x - p.x, header.y - p.y);
				if (control instanceof ToolBar toolBar) {
					/*
					 * The tool items (e.g., with a text) are filled with their color by the CSS
					 * engine. GTK paints the background color of the tool bar over the picture
					 * (the paint comes first), so the tool bar is transparent there!
					 */
					control.getDisplay().asyncExec(() -> {
						if (GTK) {
							transparent(toolBar);
						}
						transparentItems(toolBar);
					});
				}
			});
			control.redraw();
		}
		if (control instanceof Composite composite && !(control instanceof ToolBar)) {
			for (Control child : composite.getChildren()) {
				paintBehind(child);
			}
		}
	}

	private static void transparent(ToolBar toolBar) {
		if (toolBar.isDisposed()) {
			return;
		}
		Color color = toolBar.getBackground();
		if (color == null || color.getAlpha() != 0) {
			toolBar.setBackground(TRANSPARENT);
		}
	}

	/* Sets the part later (once for the events of a paint, nothing is changed if the part is right) */
	private void later(ToolBar toolBar) {
		/* Not for the listeners of an old renderer (e.g., after another theme was activated) */
		if (toolBar.isDisposed() || toolBar.getData(PAINTED) != this || toolBar.getData(PENDING) == this) {
			return;
		}
		toolBar.setData(PENDING, this);
		toolBar.getDisplay().asyncExec(() -> {
			if (!toolBar.isDisposed()) {
				if (toolBar.getData(PENDING) == this) {
					toolBar.setData(PENDING, null);
				}
				setPart(toolBar);
			}
		});
	}

	/*
	 * Windows: the tool bar gets the part of the picture under it as its
	 * background image (created again if the tool bar was moved or the picture
	 * changed) and the tool items get no color (they are drawn on the image)!
	 */
	private void setPart(ToolBar toolBar) {
		if (toolBar.isDisposed() || toolBar.getData(PAINTED) != this || picture == null || picture.isDisposed() || header == null || folder.isDisposed()) {
			return;
		}
		Point size = toolBar.getSize();
		if (size.x <= 0 || size.y <= 0) {
			return;
		}
		Point p = folder.toControl(toolBar.toDisplay(0, 0));
		Rectangle bounds = new Rectangle(p.x - header.x, p.y - header.y, size.x, size.y);
		if (!isInFolder(toolBar) || bounds.y < 0 || bounds.y + bounds.height > header.height + 4) {
			/* Not (anymore) in the tab row (e.g., the tool bar of a narrow view in a row below the tabs) */
			disposePart(toolBar);
			return;
		}
		Object key = bounds + "/" + pictureKey;
		Image part = (Image) toolBar.getData(PART);
		if (part == null || part.isDisposed() || !key.equals(toolBar.getData(PART_KEY))) {
			Image old = part;
			Image source = picture;
			part = new Image(toolBar.getDisplay(), (ImageDataProvider) zoom -> ToolbarArtwork.crop(source.isDisposed() ? null : source.getImageData(zoom), bounds, zoom));
			toolBar.setData(PART, part);
			toolBar.setData(PART_KEY, key);
			toolBar.setBackgroundImage(part);
			if (old != null && !old.isDisposed()) {
				old.dispose();
			}
		} else if (toolBar.getBackgroundImage() != part) {
			/* The CSS engine has set another background */
			toolBar.setBackgroundImage(part);
		}
		/* No change (and no paint) if an item has no color */
		for (ToolItem item : toolBar.getItems()) {
			item.setBackground(null);
		}
	}

	private static void disposePart(Control control) {
		Object part = control.getData(PART);
		control.setData(PART, null);
		control.setData(PART_KEY, null);
		if (part instanceof Image image && !image.isDisposed()) {
			if (!control.isDisposed() && control.getBackgroundImage() == image) {
				control.setBackgroundImage(null);
			}
			image.dispose();
		}
	}

	private static void transparentItems(ToolBar toolBar) {
		if (toolBar.isDisposed()) {
			return;
		}
		for (ToolItem item : toolBar.getItems()) {
			Color color = item.getBackground();
			if (color == null || color.getAlpha() != 0) {
				item.setBackground(TRANSPARENT);
			}
		}
	}

	/* The control is (still) in the tab row of the folder (e.g., not moved with a view) */
	private boolean isInFolder(Control control) {
		for (Control c = control; c != null; c = c.getParent()) {
			if (c.getParent() == folder) {
				return inTabRow(c, header);
			}
		}
		return false;
	}

	/* Another renderer (e.g., another theme): the picture is not painted anymore */
	@Override
	protected void dispose() {
		disposePicture();
		if (WINDOWS && !folder.isDisposed()) {
			disposeParts(folder);
		}
		super.dispose();
	}

	/* The tool bars get the background of the CSS again */
	private void disposeParts(Composite parent) {
		for (Control child : parent.getChildren()) {
			if (child.getData(PAINTED) == this) {
				disposePart(child);
			}
			if (child instanceof Composite composite) {
				disposeParts(composite);
			}
		}
	}

	private void disposePicture() {
		if (picture != null && !picture.isDisposed()) {
			picture.dispose();
		}
		picture = null;
		pictureKey = null;
	}
}
