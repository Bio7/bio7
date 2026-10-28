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
		if (theme == null || folder.isDisposed()) {
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
		Object key = size.x + "x" + height + "/" + ToolbarArtwork.getVersion() + "/" + System.identityHashCode(theme);
		if (picture == null || picture.isDisposed() || !key.equals(pictureKey)) {
			Image old = picture;
			final int width = size.x;
			final int h = height;
			picture = new Image(folder.getDisplay(), (ImageDataProvider) zoom -> ArtworkRenderer.renderTabs(theme, dir, width, h, zoom));
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

	private static boolean inTabRow(Control child, Rectangle header) {
		return child.getBounds().y < header.height && child.getBounds().height <= header.height + 4;
	}

	/*
	 * The controls in the tab row (e.g., the view menu, minimize and maximize and
	 * the tool bars of a view in the tab row, also in a composite) paint the part
	 * of the picture under them as their background: the colors set by the CSS
	 * engine (also again after a view was moved) are painted over and a background
	 * image of the folder would be drawn shifted by macOS (as a pattern). Windows
	 * fills the tool items with their background color, so the tool bars keep
	 * their color there!
	 */
	private void paintBehind(Control control) {
		if (WINDOWS && control instanceof ToolBar) {
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
					/* The tool items (e.g., with a text) are filled with their color by the CSS engine */
					control.getDisplay().asyncExec(() -> transparentItems(toolBar));
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
		super.dispose();
	}

	private void disposePicture() {
		if (picture != null && !picture.isDisposed()) {
			picture.dispose();
		}
		picture = null;
		pictureKey = null;
	}
}
