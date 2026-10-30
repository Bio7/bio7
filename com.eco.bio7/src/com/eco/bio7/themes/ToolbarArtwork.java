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
import org.eclipse.swt.graphics.RGB;
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
	/* The continuous artwork: the main toolbar and the size of the band (toolbar and the first tab rows, see bandOf) */
	private static Composite bandBar;
	private static Point bandSize;
	/* The top of the first tab rows (in the coordinates of the toolbar) */
	private static int bandRowTop;
	/* A view container which paints the band in the gaps between the views (see hookSashes) */
	private static final String SASHES = "bio7.themes.bandSashes";
	/* The picture of the whole band for the gaps (with the colors of the tab rows) and its version */
	private static Image sashPicture;
	private static int sashVersion = -1;
	/* The number of the paints of the gaps (for the tests) */
	static int sashPaints;
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

	/** The folder of the active custom theme (with the artwork) or null. */
	public static File getThemeDir() {
		return themeDir;
	}

	/** A number which changes when the theme or its pictures change (e.g., to render the tab rows again). */
	public static int getVersion() {
		return version;
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
		/* The band of the continuous artwork: from the top of the toolbar to the bottom of the tab rows below it */
		Point band = theme.isArtworkContinuous() ? measureBand(bar, size) : null;
		boolean bandChanged = band == null ? bandSize != null : !band.equals(bandSize);
		if (!size.equals(pictureSize) || bandChanged) {
			resetPicture();
			pictureSize = size;
			bandSize = band;
			bandBar = band != null ? bar : null;
			if (bandChanged) {
				/* The tab rows paint their parts of the band again */
				redrawTabFolders(bar.getShell());
			}
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
		Point band = bandSize;
		return PIXELS.computeIfAbsent(zoom, z -> ArtworkRenderer.render(theme, themeDir, pictureSize.x, pictureSize.y, z,
				band != null ? new ArtworkRenderer.Band(0, 0, band.x, band.y) : null));
	}

	/*
	 * The height of the band: the toolbar and the tab rows of the view stacks
	 * directly below the toolbar (the first row of the views), the width of the
	 * toolbar.
	 */
	private static Point measureBand(Composite bar, Point size) {
		int[] bottom = { size.y, Integer.MAX_VALUE };
		measureFolders(bar, bar.getShell(), size.y, bottom);
		bandRowTop = bottom[1] == Integer.MAX_VALUE ? size.y : bottom[1];
		return new Point(size.x, bottom[0]);
	}

	private static void measureFolders(Composite bar, Control control, int barHeight, int[] bottom) {
		if (control.isDisposed() || !control.isVisible()) {
			return;
		}
		if (control instanceof org.eclipse.swt.custom.CTabFolder folder && folder.getTabPosition() == SWT.TOP) {
			Point p = control.getDisplay().map(folder, bar, 0, 0);
			/* Directly below the toolbar (e.g., a few points of the trim and the sash) */
			if (p.y >= barHeight - 2 && p.y <= barHeight + 24) {
				bottom[0] = Math.max(bottom[0], p.y + folder.getTabHeight() + 2);
				bottom[1] = Math.min(bottom[1], p.y);
				/* The view container paints the band in the gaps between the tab rows (the sashes) */
				if (folder.getParent() != null) {
					hookSashes(folder.getParent());
				}
			}
			return;
		}
		if (control instanceof Composite composite && control != bar) {
			for (Control child : composite.getChildren()) {
				measureFolders(bar, child, barHeight, bottom);
			}
		}
	}

	private static void redrawTabFolders(Control control) {
		if (control.isDisposed()) {
			return;
		}
		if (control instanceof org.eclipse.swt.custom.CTabFolder folder) {
			folder.redraw();
		}
		if (control instanceof Composite composite) {
			for (Control child : composite.getChildren()) {
				redrawTabFolders(child);
			}
		}
	}

	/**
	 * The part of the band of the continuous artwork at the point (x, y) of the
	 * control (e.g., the tab row of a view stack) or null if the artwork is not
	 * continuous (or the control is in another window).
	 */
	/* The gaps between the views (the sashes of the container) show the band (the parts of the views paint over it) */
	private static void hookSashes(Composite container) {
		if (container.getData(SASHES) != null) {
			return;
		}
		container.setData(SASHES, Boolean.TRUE);
		container.addListener(SWT.Paint, e -> paintSashes(container, e.gc));
		Point size = container.getSize();
		container.redraw(0, 0, size.x, size.y, false);
	}

	private static void paintSashes(Composite container, org.eclipse.swt.graphics.GC gc) {
		Composite bar = bandBar;
		Point band = bandSize;
		if (theme == null || !theme.isArtworkContinuous() || bar == null || bar.isDisposed() || band == null
				|| container.getShell() != bar.getShell()) {
			return;
		}
		if (sashPicture == null || sashPicture.isDisposed() || sashVersion != version) {
			if (sashPicture != null && !sashPicture.isDisposed()) {
				sashPicture.dispose();
			}
			CustomTheme t = theme;
			File dir = themeDir;
			sashPicture = new Image(container.getDisplay(), (ImageDataProvider) zoom -> ArtworkRenderer.renderTabs(t, dir, band.x, band.y, zoom,
					new ArtworkRenderer.Band(0, 0, band.x, band.y)));
			sashVersion = version;
		}
		Point o = container.getDisplay().map(container, bar, 0, 0);
		/* The rows of the band below the toolbar (in the coordinates of the container) */
		int top = bar.getSize().y - o.y;
		Rectangle rows = new Rectangle(0, Math.max(0, top), container.getSize().x, band.y - o.y - Math.max(0, top));
		Rectangle clipping = gc.getClipping();
		Rectangle area = clipping.intersection(rows);
		if (area.isEmpty()) {
			return;
		}
		sashPaints++;
		gc.setClipping(area);
		gc.drawImage(sashPicture, -o.x, -o.y);
		gc.setClipping(clipping);
	}

	/** True if the control (e.g., a tab folder) is directly below the main toolbar (no gap). */
	static boolean isBelowToolbar(Control control) {
		Composite bar = bandBar;
		if (bar == null || bar.isDisposed() || control.getShell() != bar.getShell()) {
			return false;
		}
		return control.getDisplay().map(control, bar, 0, 0).y <= bar.getSize().y;
	}

	/** True if the control (e.g., a tab folder) is below the band of the continuous artwork. */
	static boolean isBelowBand(Control control) {
		Composite bar = bandBar;
		Point band = bandSize;
		if (bar == null || bar.isDisposed() || band == null || control.getShell() != bar.getShell()) {
			return false;
		}
		return control.getDisplay().map(control, bar, 0, 0).y >= band.y;
	}

	static ArtworkRenderer.Band bandOf(Control control, int x, int y) {
		Composite bar = bandBar;
		Point band = bandSize;
		if (theme == null || !theme.isArtworkContinuous() || bar == null || bar.isDisposed() || band == null
				|| control.getShell() != bar.getShell()) {
			return null;
		}
		Point p = control.getDisplay().map(control, bar, x, y);
		/* A tab row below the band (e.g., the second row of the views): the part of the first tab rows (not wrapped) */
		int partY = p.y >= band.y ? bandRowTop : p.y;
		return new ArtworkRenderer.Band(p.x, partY, band.x, band.y);
	}

	/* The part of the picture (in pixels of the zoom)! */
	static ImageData crop(ImageData source, Rectangle bounds, int zoom) {
		int x = bounds.x * zoom / 100, y = bounds.y * zoom / 100;
		int w = Math.max(1, bounds.width * zoom / 100), h = Math.max(1, bounds.height * zoom / 100);
		ImageData out = new ImageData(w, h, 24, new PaletteData(0xFF0000, 0xFF00, 0xFF));
		if (source == null) {
			return out;
		}
		/* The pixels of an image of SWT (e.g., Image.getImageData on Windows) have another order of the colors! */
		PaletteData palette = source.palette;
		boolean same = palette.isDirect && source.depth == 24 && palette.redMask == 0xFF0000 && palette.greenMask == 0xFF00 && palette.blueMask == 0xFF;
		int[] row = new int[w];
		for (int j = 0; j < h; j++) {
			int sy = Math.max(0, Math.min(source.height - 1, y + j));
			for (int i = 0; i < w; i++) {
				int sx = Math.max(0, Math.min(source.width - 1, x + i));
				int pixel = source.getPixel(sx, sy);
				if (same) {
					row[i] = pixel;
				} else {
					RGB rgb = palette.getRGB(pixel);
					row[i] = (rgb.red << 16) | (rgb.green << 8) | rgb.blue;
				}
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
