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
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
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
		super.draw(part, state, bounds, gc);
		if (part == PART_BACKGROUND) {
			paintPicture(gc, bounds);
		}
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
		Object key = size.x + "x" + height + "/" + ToolbarArtwork.getVersion() + "/" + (theme != null ? System.identityHashCode(theme) : rowColor);
		if (picture == null || picture.isDisposed() || !key.equals(pictureKey)) {
			Image old = picture;
			final int width = size.x;
			final int h = height;
			picture = new Image(folder.getDisplay(), (ImageDataProvider) zoom -> theme != null ? ArtworkRenderer.renderTabs(theme, dir, width, h, zoom)
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
