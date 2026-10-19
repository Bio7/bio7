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
import java.util.HashMap;
import java.util.Map;

import org.eclipse.e4.ui.css.swt.theme.IThemeEngine;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageDataProvider;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 * Paints the background of the main toolbar of a custom theme (toolbar style
 * and artwork) as one picture over the whole toolbar. The picture is rendered
 * once for the size of the toolbar. The toolbar and each control in it (the
 * toolbars, the perspective switcher, the spacers) get their own background
 * image: the part of the picture at their position (macOS draws an inherited
 * background image of a toolbar with a flat color, an own image is drawn like
 * the gradients of the Bio7 themes). The CSS engine sets the backgrounds again
 * when the toolbar is styled again (e.g., when an editor adds a toolbar), so
 * the parts are set again when the controls are painted. The Bio7 default
 * themes are not changed (only active for a custom theme).
 */
public final class ToolbarArtwork {

	private static final String MAIN_TOOLBAR = "org.eclipse.ui.main.toolbar";
	private static final String HOOKED = "bio7.themes.hooked";
	/* The part of the picture of a control and its bounds in the toolbar! */
	private static final String PART = "bio7.themes.part";
	private static final String PART_BOUNDS = "bio7.themes.partBounds";

	private static final Color TRANSPARENT = new Color(0, 0, 0, 0);
	private static final boolean WINDOWS = "win32".equals(SWT.getPlatform());

	private static CustomTheme theme;
	private static File themeDir;
	private static boolean pending;
	/* The picture of the toolbar (size in points and the rendered pixels for each zoom)! */
	private static Point pictureSize;
	private static int version;
	private static final Map<Integer, ImageData> PIXELS = new HashMap<Integer, ImageData>();

	private ToolbarArtwork() {
	}

	/** Activates the painting for the custom theme or deactivates it (null). */
	public static void setTheme(CustomTheme customTheme, File dir) {
		Display.getDefault().asyncExec(() -> {
			theme = customTheme;
			themeDir = dir;
			resetPicture();
			for (IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
				Composite bar = mainToolbar(window);
				if (bar == null) {
					continue;
				}
				hook(bar);
				if (theme == null) {
					removeParts(bar);
					resetToolBars(bar);
					/* The CSS of the theme again (the default themes)! */
					IThemeEngine engine = ThemeStore.engine();
					if (engine != null) {
						engine.applyStyles(bar, true);
					}
				} else {
					apply(bar);
				}
				Point size = bar.getSize();
				bar.redraw(0, 0, size.x, size.y, true);
			}
		});
	}

	public static CustomTheme getTheme() {
		return theme;
	}

	private static Composite mainToolbar(IWorkbenchWindow window) {
		if (window.getShell() == null) {
			return null;
		}
		for (Control control : window.getShell().getChildren()) {
			Object element = control.getData("modelElement");
			if (control instanceof Composite && element instanceof MUIElement && MAIN_TOOLBAR.equals(((MUIElement) element).getElementId())) {
				return (Composite) control;
			}
		}
		return null;
	}

	private static void hook(Composite bar) {
		if (bar.getData(HOOKED) != null) {
			return;
		}
		bar.setData(HOOKED, Boolean.TRUE);
		bar.addListener(SWT.Paint, e -> later(bar));
		bar.addListener(SWT.Resize, e -> later(bar));
	}

	/* Checks the toolbar later (once for many events, nothing is changed if the backgrounds are right)! */
	private static void later(Composite bar) {
		if (theme == null || pending) {
			return;
		}
		pending = true;
		bar.getDisplay().asyncExec(() -> {
			pending = false;
			apply(bar);
		});
	}

	/* Sets the parts of the picture as background images of the toolbar and its controls! */
	private static void apply(Composite bar) {
		if (bar.isDisposed() || theme == null) {
			return;
		}
		Point size = bar.getSize();
		if (size.x <= 0 || size.y <= 0) {
			return;
		}
		if (!size.equals(pictureSize)) {
			resetPicture();
			pictureSize = size;
		}
		setPart(bar, bar, new Rectangle(0, 0, size.x, size.y));
		applyChildren(bar, bar);
	}

	private static void applyChildren(Composite bar, Composite parent) {
		for (Control child : parent.getChildren()) {
			if (child.isDisposed()) {
				continue;
			}
			if (child.getData(HOOKED) == null) {
				child.setData(HOOKED, Boolean.TRUE);
				/* A child is styled or moved without a paint of the toolbar (e.g., the perspective switcher)! */
				child.addListener(SWT.Paint, e -> later(bar));
				child.addListener(SWT.Move, e -> later(bar));
				child.addListener(SWT.Resize, e -> later(bar));
				child.addListener(SWT.Dispose, e -> disposePart(child));
			}
			if (child instanceof ToolBar && !WINDOWS) {
				/*
				 * A toolbar (macOS) draws its background color and no background image, so it
				 * is transparent and the part of its parent is visible! Windows draws the
				 * background image of a toolbar but fills the items with their background
				 * color without alpha (black), so a toolbar gets its own part there!
				 */
				transparent(child);
			} else {
				Point origin = child.getDisplay().map(child, bar, 0, 0);
				Point size = child.getSize();
				if (size.x > 0 && size.y > 0) {
					setPart(bar, child, new Rectangle(origin.x, origin.y, size.x, size.y));
				}
			}
			if (child instanceof Composite) {
				applyChildren(bar, (Composite) child);
			}
		}
	}

	/* The control gets the part of the picture at its bounds (created again if the bounds or the picture changed)! */
	private static void setPart(Composite bar, Control control, Rectangle bounds) {
		Image part = (Image) control.getData(PART);
		Object key = bounds + "/" + version;
		if (part == null || part.isDisposed() || !key.equals(control.getData(PART_BOUNDS))) {
			Image old = part;
			Display display = control.getDisplay();
			int v = version;
			part = new Image(display, (ImageDataProvider) zoom -> crop(pixels(zoom, v), bounds, zoom));
			control.setData(PART, part);
			control.setData(PART_BOUNDS, key);
			control.setBackgroundImage(part);
			if (old != null && !old.isDisposed()) {
				old.dispose();
			}
		} else if (control.getBackgroundImage() != part) {
			/* The CSS engine has set another background! */
			control.setBackgroundImage(part);
		}
	}

	private static void transparent(Control control) {
		if (control.getBackgroundImage() != null && control.getBackgroundImage() != control.getParent().getBackgroundImage()) {
			control.setBackgroundImage(null);
		}
		Color color = control.getBackground();
		if (color == null || color.getAlpha() != 0) {
			control.setBackground(TRANSPARENT);
		}
		/* The items fill their bounds with their background color (set by the CSS)! */
		for (ToolItem item : ((ToolBar) control).getItems()) {
			Color itemColor = item.getBackground();
			if (itemColor == null || itemColor.getAlpha() != 0) {
				item.setBackground(TRANSPARENT);
			}
		}
	}

	/* The rendered picture (pixels) for the zoom! */
	private static ImageData pixels(int zoom, int forVersion) {
		if (forVersion != version || theme == null || pictureSize == null) {
			return null;
		}
		return PIXELS.computeIfAbsent(zoom, z -> ArtworkRenderer.render(theme, themeDir, pictureSize.x, pictureSize.y, z));
	}

	/* The part of the picture (in pixels of the zoom)! */
	private static ImageData crop(ImageData source, Rectangle bounds, int zoom) {
		int x = bounds.x * zoom / 100, y = bounds.y * zoom / 100;
		int w = Math.max(1, bounds.width * zoom / 100), h = Math.max(1, bounds.height * zoom / 100);
		ImageData out = new ImageData(w, h, 24, new PaletteData(0xFF0000, 0xFF00, 0xFF));
		if (source == null) {
			return out;
		}
		int[] row = new int[w];
		for (int j = 0; j < h; j++) {
			int sy = Math.max(0, Math.min(source.height - 1, y + j));
			for (int i = 0; i < w; i++) {
				int sx = Math.max(0, Math.min(source.width - 1, x + i));
				row[i] = source.getPixel(sx, sy);
			}
			out.setPixels(0, j, w, row, 0);
		}
		return out;
	}

	private static void resetPicture() {
		PIXELS.clear();
		pictureSize = null;
		version++;
	}

	/* The default themes: the parts are removed (the CSS sets the backgrounds again)! */
	private static void removeParts(Composite bar) {
		disposePart(bar);
		for (Control child : bar.getChildren()) {
			disposePart(child);
			if (child instanceof Composite) {
				removeParts((Composite) child);
			}
		}
	}

	/* The toolbars get the color of the CSS again! */
	private static void resetToolBars(Composite parent) {
		for (Control child : parent.getChildren()) {
			if (child instanceof ToolBar) {
				if (child.getBackground() != null && child.getBackground().getAlpha() == 0) {
					child.setBackground(null);
				}
				for (ToolItem item : ((ToolBar) child).getItems()) {
					if (item.getBackground() != null && item.getBackground().getAlpha() == 0) {
						item.setBackground(null);
					}
				}
			}
			if (child instanceof Composite) {
				resetToolBars((Composite) child);
			}
		}
	}

	private static void disposePart(Control control) {
		Object part = control.getData(PART);
		control.setData(PART, null);
		control.setData(PART_BOUNDS, null);
		if (part instanceof Image && !((Image) part).isDisposed()) {
			if (!control.isDisposed() && control.getBackgroundImage() == part) {
				control.setBackgroundImage(null);
			}
			((Image) part).dispose();
		}
	}
}
