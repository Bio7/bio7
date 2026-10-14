/*******************************************************************************
 * Copyright (c) 2007-2026 M. Austenfeld
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/
package com.eco.bio7.image.thumbs;

import java.io.File;
import java.lang.ref.SoftReference;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageDataProvider;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.ScrollBar;

/**
 * A canvas which displays thousands of thumbnails without operating system
 * resources per thumbnail. The thumbnails are kept as image data and only the
 * visible area is composed into one temporary image when the canvas is painted
 * (one image handle at a time). Must be used in the UI thread, except
 * {@link #setData(int, Item, ThumbData)}.
 */
class ThumbnailGrid extends Canvas {

	/**
	 * A file of the grid and its thumbnail. The pixels are softly referenced (the
	 * garbage collector can release them if the memory is low, they are reloaded
	 * from the cache when they are painted again), the information is kept.
	 */
	static final class Item {
		final File file;
		private volatile SoftReference<ThumbData> thumb;
		/* True when the thumbnail has been loaded once! */
		volatile boolean loaded;
		volatile boolean stack;
		volatile String info;
		volatile int width, height;
		boolean reloading;
		/* The thumbnail scaled to the zoom and to the overview (cached)! */
		SoftReference<ImageData> zoomed, tiny;
		volatile ImageData mini;
		int zoomedSize, tinySize;

		Item(File file) {
			this.file = file;
		}

		/** The thumbnail or null if it is not loaded (or released). */
		ThumbData thumb() {
			SoftReference<ThumbData> r = thumb;
			return r == null ? null : r.get();
		}

		void set(ThumbData d) {
			/* A small copy for the overview is kept (never released)! */
			ImageData src = d.data;
			double t = Math.min(1.0, (double) MINI / Math.max(src.width, src.height));
			mini = t < 1 ? src.scaledTo(Math.max(1, (int) Math.round(src.width * t)), Math.max(1, (int) Math.round(src.height * t))) : src;
			stack = d.stack;
			info = d.info;
			width = d.width;
			height = d.height;
			thumb = new SoftReference<ThumbData>(d);
			loaded = true;
		}
	}

	/* Loads a released thumbnail again (from the cache)! */
	private Function<File, ThumbData> reloader;
	private final ExecutorService reloadPool = Executors.newSingleThreadExecutor(r -> {
		Thread t = new Thread(r, "Thumbnail reload");
		t.setDaemon(true);
		return t;
	});

	private static final int MARGIN = 10;
	/* The size of the copies for the overview in pixels! */
	private static final int MINI = 32;

	private Item[] items = new Item[0];
	private int generation;
	/* The thumbnail size in points and the size of the thumbnail pixels! */
	private int thumbSize = 50;
	private int pixelSize = 50;
	/* The number of thumbnails in a row (0 to fit the width of the canvas)! */
	private int columns = 15;
	private double zoom = 1.0;
	private int originX, originY;
	private int hover = -1;
	/* The selected item (selected with a click, null if none)! */
	private Item selected;
	private boolean redrawScheduled;
	private Consumer<Item> openListener, folderListener;
	private Runnable viewportListener;
	private Color hoverColor, frameColor, textColor;
	/* The information of the thumbnail under the mouse! */
	private final InfoPopup info;

	ThumbnailGrid(Composite parent) {
		super(parent, SWT.V_SCROLL | SWT.H_SCROLL | SWT.DOUBLE_BUFFERED | SWT.NO_BACKGROUND);
		updateColors();
		addListener(SWT.Paint, this::paint);
		addListener(SWT.Resize, e -> {
			updateScrollBars();
			if (columns == 0) {
				/* The thumbnails are arranged again! */
				redraw();
				fireViewport();
			}
		});
		getVerticalBar().addListener(SWT.Selection, e -> scrollTo(originX, getVerticalBar().getSelection()));
		getHorizontalBar().addListener(SWT.Selection, e -> scrollTo(getHorizontalBar().getSelection(), originY));
		addListener(SWT.MouseMove, e -> setHover(indexAt(e.x, e.y)));
		addListener(SWT.MouseExit, e -> setHover(-1));
		addListener(SWT.MouseUp, this::mouseUp);
		addListener(SWT.MouseDoubleClick, this::mouseDoubleClick);
		info = new InfoPopup(this);
		addListener(SWT.MouseDown, e -> {
			info.hide();
			setFocus();
		});
		addListener(SWT.KeyDown, this::keyDown);
		/* Also called if the parent is disposed (dispose() of this control is not called then)! */
		addListener(SWT.Dispose, e -> {
			items = new Item[0];
			reloadPool.shutdownNow();
		});
	}

	private void updateColors() {
		Color bg = getParent().getBackground();
		setBackground(bg);
		RGB b = bg.getRGB();
		boolean dark = (b.red + b.green + b.blue) / 3 < 128;
		hoverColor = new Color(dark ? new RGB(90, 140, 210) : new RGB(60, 120, 215));
		frameColor = new Color(dark ? new RGB(85, 85, 85) : new RGB(200, 200, 200));
		textColor = new Color(dark ? new RGB(220, 220, 220) : new RGB(40, 40, 40));
	}

	/** Sets the function which loads a thumbnail again if it was released (in a background thread). */
	void setReloader(Function<File, ThumbData> reloader) {
		this.reloader = reloader;
	}

	/* Reloads a released thumbnail in the background (a deleted file is removed)! */
	private void reload(Item item) {
		Function<File, ThumbData> r = reloader;
		if (r == null || item.reloading) {
			return;
		}
		item.reloading = true;
		final int gen = generation;
		reloadPool.execute(() -> {
			if (!item.file.exists()) {
				getDisplay().asyncExec(() -> {
					if (!isDisposed()) {
						removeDeleted(item);
					}
				});
				return;
			}
			ThumbData d = r.apply(item.file);
			getDisplay().asyncExec(() -> item.reloading = false);
			setData(gen, item, d);
		});
	}

	/** Removes the item of a deleted file and reports it in the Bio7 console. */
	void removeDeleted(Item item) {
		int index = java.util.Arrays.asList(items).indexOf(item);
		if (index < 0) {
			return;
		}
		Item[] newItems = new Item[items.length - 1];
		System.arraycopy(items, 0, newItems, 0, index);
		System.arraycopy(items, index + 1, newItems, index, items.length - index - 1);
		items = newItems;
		hover = -1;
		if (selected == item) {
			selected = null;
		}
		info.hide();
		System.out.println("Thumbnails: " + item.file.getAbsolutePath() + ": the file has been deleted, the thumbnail is removed");
		updateScrollBars();
		redraw();
		fireViewport();
	}

	/** Called with an item when it is double clicked (left button). */
	void setOpenListener(Consumer<Item> listener) {
		this.openListener = listener;
	}

	/** Called with an item when it is double clicked with the right button. */
	void setFolderListener(Consumer<Item> listener) {
		this.folderListener = listener;
	}

	/** Called when the visible area or the content changes (for the overview). */
	void setViewportListener(Runnable listener) {
		this.viewportListener = listener;
	}

	/**
	 * Displays the files (thumbnails are added with setData). Returns the
	 * generation of this content, which setData needs. The scroll position is kept
	 * if keepPosition is true (e.g., for a refresh).
	 */
	int setFiles(File[] files, int thumbSize, int pixelSize, boolean keepPosition) {
		Item[] newItems = new Item[files.length];
		for (int i = 0; i < files.length; i++) {
			newItems[i] = new Item(files[i]);
		}
		this.items = newItems;
		this.thumbSize = thumbSize;
		this.pixelSize = pixelSize;
		this.hover = -1;
		info.hide();
		/* A refresh keeps the selected file! */
		Item old = selected;
		this.selected = null;
		if (keepPosition && old != null) {
			for (Item it : newItems) {
				if (it.file.equals(old.file)) {
					selected = it;
					break;
				}
			}
		}
		if (!keepPosition) {
			originX = originY = 0;
		}
		generation++;
		updateScrollBars();
		redraw();
		return generation;
	}

	Item[] getItems() {
		return items;
	}

	/**
	 * Sets the thumbnail of an item (of the array returned by getItems after
	 * setFiles). Can be called in any thread.
	 */
	void setData(int gen, Item item, ThumbData data) {
		if (gen != generation) {
			return;
		}
		item.set(data);
		scheduleRedraw();
	}

	/* Coalesces the redraws of many thumbnails which are loaded in parallel! */
	private void scheduleRedraw() {
		synchronized (this) {
			if (redrawScheduled) {
				return;
			}
			redrawScheduled = true;
		}
		getDisplay().asyncExec(() -> {
			synchronized (ThumbnailGrid.this) {
				redrawScheduled = false;
			}
			if (!isDisposed()) {
				redraw();
				fireViewport();
			}
		});
	}

	/** Sets the number of thumbnails in a row (0 to fit the width of the canvas). */
	void setColumns(int columns) {
		this.columns = Math.max(0, columns);
		updateScrollBars();
		redraw();
		fireViewport();
	}

	void setZoom(double zoom) {
		this.zoom = zoom;
		for (Item it : items) {
			it.zoomed = null;
		}
		updateScrollBars();
		redraw();
		fireViewport();
	}

	/* --- Geometry (in points) --- */

	private int box() {
		return Math.max(8, (int) Math.round(thumbSize * zoom));
	}

	private int labelHeight() {
		return box() >= 80 ? 18 : 0;
	}

	int cellWidth() {
		return box() + Math.max(10, box() / 5);
	}

	int cellHeight() {
		return box() + labelHeight() + Math.max(10, box() / 5);
	}

	/* The number of thumbnails in a row (computed from the width if they fit)! */
	private int cols() {
		if (columns > 0) {
			return columns;
		}
		return Math.max(1, (getClientArea().width - 2 * MARGIN) / cellWidth());
	}

	/** The size of the whole content in points. */
	Point getContentSize() {
		int columns = cols();
		int rows = (items.length + columns - 1) / columns;
		return new Point(2 * MARGIN + Math.min(columns, Math.max(1, items.length)) * cellWidth(), 2 * MARGIN + rows * cellHeight());
	}

	/** The visible part of the content in content coordinates. */
	Rectangle getViewport() {
		Rectangle ca = getClientArea();
		return new Rectangle(originX, originY, ca.width, ca.height);
	}

	/** The bounds of the cell of an item in content coordinates. */
	Rectangle cellBounds(int index) {
		int columns = cols();
		return new Rectangle(MARGIN + (index % columns) * cellWidth(), MARGIN + (index / columns) * cellHeight(), cellWidth(), cellHeight());
	}

	private int indexAt(int x, int y) {
		int cx = x + originX - MARGIN, cy = y + originY - MARGIN;
		if (cx < 0 || cy < 0) {
			return -1;
		}
		int columns = cols();
		int col = cx / cellWidth(), row = cy / cellHeight();
		int i = row * columns + col;
		return col < columns && i < items.length ? i : -1;
	}

	/** Scrolls to the given origin (content coordinates). */
	void scrollTo(int x, int y) {
		Point content = getContentSize();
		Rectangle ca = getClientArea();
		int nx = Math.max(0, Math.min(x, content.x - ca.width));
		int ny = Math.max(0, Math.min(y, content.y - ca.height));
		if (nx != originX || ny != originY) {
			originX = nx;
			originY = ny;
			getHorizontalBar().setSelection(nx);
			getVerticalBar().setSelection(ny);
			redraw();
			fireViewport();
		}
	}

	private void updateScrollBars() {
		Point content = getContentSize();
		Rectangle ca = getClientArea();
		ScrollBar v = getVerticalBar(), h = getHorizontalBar();
		v.setValues(Math.min(originY, Math.max(0, content.y - ca.height)), 0, content.y, Math.max(1, ca.height), cellHeight() / 2, Math.max(1, ca.height - 20));
		h.setValues(Math.min(originX, Math.max(0, content.x - ca.width)), 0, content.x, Math.max(1, ca.width), cellWidth() / 2, Math.max(1, ca.width - 20));
		v.setVisible(content.y > ca.height);
		h.setVisible(content.x > ca.width);
		originX = h.getSelection();
		originY = v.getSelection();
		if (content.y <= ca.height) {
			originY = 0;
		}
		if (content.x <= ca.width) {
			originX = 0;
		}
	}

	private void fireViewport() {
		if (viewportListener != null) {
			viewportListener.run();
		}
	}

	private void setHover(int index) {
		if (index == hover) {
			return;
		}
		hover = index;
		info.hoverChanged(index < 0 ? null : items[index]);
		redraw();
	}

	/* A click selects a thumbnail (a click on the background removes the selection)! */
	private void mouseUp(Event e) {
		if (e.button != 1 && e.button != 3) {
			return;
		}
		int i = indexAt(e.x, e.y);
		Item item = i < 0 ? null : items[i];
		if (item != selected) {
			selected = item;
			redraw();
		}
	}

	/* A double click opens the image, a double click with the right button the folder! */
	private void mouseDoubleClick(Event e) {
		int i = indexAt(e.x, e.y);
		if (i < 0) {
			return;
		}
		if (e.button == 1 && openListener != null) {
			openListener.accept(items[i]);
		} else if (e.button == 3 && folderListener != null) {
			folderListener.accept(items[i]);
		}
	}

	/*
	 * The arrow keys, Home, End, Page Up and Page Down move the selection, Enter
	 * opens the selected image!
	 */
	private void keyDown(Event e) {
		if (items.length == 0) {
			return;
		}
		info.hide();
		int current = selected == null ? -1 : java.util.Arrays.asList(items).indexOf(selected);
		int columns = cols();
		int page = Math.max(1, getClientArea().height / cellHeight()) * columns;
		int next;
		switch (e.keyCode) {
		case SWT.CR:
		case SWT.KEYPAD_CR:
			if (current >= 0 && openListener != null) {
				openListener.accept(items[current]);
			}
			return;
		case SWT.ARROW_RIGHT:
			next = current + 1;
			break;
		case SWT.ARROW_LEFT:
			next = current - 1;
			break;
		case SWT.ARROW_DOWN:
			next = current < 0 ? 0 : current + columns;
			break;
		case SWT.ARROW_UP:
			next = current - columns;
			break;
		case SWT.PAGE_DOWN:
			next = current < 0 ? 0 : current + page;
			break;
		case SWT.PAGE_UP:
			next = current - page;
			break;
		case SWT.HOME:
			next = 0;
			break;
		case SWT.END:
			next = items.length - 1;
			break;
		default:
			return;
		}
		e.doit = false;
		/* Without a selection the first thumbnail is selected! */
		if (current < 0) {
			next = 0;
		}
		if (next < 0 || next >= items.length) {
			/* Page keys and the last row move to the first or last thumbnail! */
			if (e.keyCode == SWT.PAGE_DOWN || e.keyCode == SWT.ARROW_DOWN) {
				next = items.length - 1;
			} else if (e.keyCode == SWT.PAGE_UP || e.keyCode == SWT.ARROW_UP) {
				next = 0;
			} else {
				return;
			}
		}
		select(next);
	}

	/* Selects the thumbnail and scrolls it into the visible area! */
	private void select(int index) {
		selected = items[index];
		Rectangle cell = cellBounds(index);
		Rectangle v = getViewport();
		int x = originX, y = originY;
		if (cell.y < v.y) {
			y = cell.y - MARGIN;
		} else if (cell.y + cell.height > v.y + v.height) {
			y = cell.y + cell.height + MARGIN - v.height;
		}
		if (cell.x < v.x) {
			x = cell.x - MARGIN;
		} else if (cell.x + cell.width > v.x + v.width) {
			x = cell.x + cell.width + MARGIN - v.width;
		}
		scrollTo(x, y);
		info.hide();
		redraw();
	}

	/** The selected item (null if none). */
	Item getSelection() {
		return selected;
	}

	/** The tooltip of an item: the image information or the file information. */
	static String tooltip(Item item) {
		if (item.info != null) {
			return item.info;
		}
		File f = item.file;
		double kb = f.length() / 1024.0;
		String s = " Directory:  " + f.getParent() + "\n File name: " + f.getName() + "\n File size:    " + String.format("%.2f kb | %.2f mb", kb, kb / 1024);
		if (item.width > 0) {
			s += "\n Width:       " + item.width + "\n Height:      " + item.height;
		}
		return s;
	}

	/* --- Painting --- */

	/* The device pixels per point (2 on a Retina display)! */
	private double deviceScale() {
		return Math.max(1, getMonitor().getZoom() / 100.0);
	}

	/* The thumbnail in the size of the zoomed box in device pixels! */
	private ImageData pixels(int index, int boxPx) {
		Item item = items[index];
		ThumbData d = item.thumb();
		if (d == null) {
			if (item.loaded) {
				reload(item);
			}
			return null;
		}
		ImageData src = d.data;
		if (boxPx == pixelSize) {
			return src;
		}
		ImageData z = item.zoomed == null ? null : item.zoomed.get();
		if (z == null || item.zoomedSize != boxPx) {
			double f = (double) boxPx / pixelSize;
			z = src.scaledTo(Math.max(1, (int) Math.round(src.width * f)), Math.max(1, (int) Math.round(src.height * f)));
			item.zoomed = new SoftReference<ImageData>(z);
			item.zoomedSize = boxPx;
		}
		return z;
	}

	private void paint(Event e) {
		GC gc = e.gc;
		Rectangle clip = new Rectangle(e.x, e.y, e.width, e.height);
		gc.setBackground(getBackground());
		gc.fillRectangle(clip);
		if (items.length == 0 || clip.width <= 0 || clip.height <= 0) {
			return;
		}
		double scale = deviceScale();
		int box = box();
		/* The box in device pixels (the thumbnails have pixelSize pixels at zoom 1)! */
		int boxPx = (int) Math.round(box * pixelSize / (double) thumbSize);
		/* The cells which intersect the clip area! */
		int cw = cellWidth(), ch = cellHeight();
		int columns = cols();
		int firstRow = Math.max(0, (clip.y + originY - MARGIN) / ch);
		int lastRow = (clip.y + clip.height + originY - MARGIN) / ch;
		int firstCol = Math.max(0, (clip.x + originX - MARGIN) / cw);
		int lastCol = Math.min(columns - 1, (clip.x + clip.width + originX - MARGIN) / cw);

		/* All visible thumbnails are composed into one image (one handle)! */
		int vw = (int) Math.ceil(clip.width * scale), vh = (int) Math.ceil(clip.height * scale);
		ImageData vp = new ImageData(vw, vh, 24, ThumbData.PALETTE);
		RGB bg = getBackground().getRGB();
		int[] bgRow = new int[vw];
		java.util.Arrays.fill(bgRow, (bg.red << 16) | (bg.green << 8) | bg.blue);
		for (int y = 0; y < vh; y++) {
			vp.setPixels(0, y, vw, bgRow, 0);
		}
		boolean any = false;
		for (int row = firstRow; row <= lastRow; row++) {
			for (int col = firstCol; col <= lastCol; col++) {
				int i = row * columns + col;
				if (i >= items.length) {
					break;
				}
				ImageData px = pixels(i, boxPx);
				if (px == null) {
					continue;
				}
				Rectangle cell = cellBounds(i);
				/* The thumbnail is centered in its box (device pixels relative to the clip)! */
				double bx = (cell.x - originX - clip.x + (cw - box) / 2.0) * scale;
				double by = (cell.y - originY - clip.y + (ch - labelHeight() - box) / 2.0) * scale;
				int dx = (int) Math.round(bx + (box * scale - px.width) / 2);
				int dy = (int) Math.round(by + (box * scale - px.height) / 2);
				blit(px, vp, dx, dy);
				any = true;
			}
		}
		if (any) {
			final ImageData data = vp;
			final double s = scale;
			ImageDataProvider provider = zoomLevel -> {
				if (Math.abs(zoomLevel / 100.0 - s) < 0.01) {
					return data;
				}
				double f = zoomLevel / 100.0 / s;
				return data.scaledTo(Math.max(1, (int) Math.round(data.width * f)), Math.max(1, (int) Math.round(data.height * f)));
			};
			Image image = new Image(getDisplay(), provider);
			try {
				gc.drawImage(image, clip.x, clip.y);
			} finally {
				image.dispose();
			}
		}
		/* Frames, labels and stack marks are drawn with the GC! */
		gc.setAntialias(SWT.ON);
		gc.setTextAntialias(SWT.ON);
		int lh = labelHeight();
		for (int row = firstRow; row <= lastRow; row++) {
			for (int col = firstCol; col <= lastCol; col++) {
				int i = row * columns + col;
				if (i >= items.length) {
					break;
				}
				Item item = items[i];
				Rectangle cell = cellBounds(i);
				int bx = cell.x - originX + (cw - box) / 2;
				int by = cell.y - originY + (ch - lh - box) / 2;
				ThumbData d = item.thumb();
				if (!item.loaded) {
					/* Still loading! */
					gc.setForeground(frameColor);
					gc.setLineDash(new int[] { 3, 3 });
					gc.drawRoundRectangle(bx, by, box, box, 6, 6);
					gc.setLineDash(null);
				} else if (d != null && d.stack) {
					/* A stack: two frames behind the thumbnail (in the size of the image)! */
					int iw = (int) Math.round(d.data.width * box / (double) pixelSize);
					int ih = (int) Math.round(d.data.height * box / (double) pixelSize);
					int ix = bx + (box - iw) / 2, iy = by + (box - ih) / 2;
					gc.setForeground(frameColor);
					gc.drawRectangle(ix + 3, iy - 3, iw - 1, ih - 1);
					gc.drawRectangle(ix + 6, iy - 6, iw - 1, ih - 1);
				}
				boolean isSelected = item == selected;
				if (isSelected || i == hover) {
					/* The selection has a thick frame, the hover a thin one! */
					gc.setForeground(hoverColor);
					gc.setLineWidth(isSelected ? 3 : 1);
					gc.drawRoundRectangle(bx - 4, by - 4, box + 8, box + 8 + lh, 8, 8);
					gc.setLineWidth(1);
				}
				if (lh > 0) {
					String name = shorten(gc, item.file.getName(), cw - 6);
					int tw = gc.textExtent(name).x;
					int tx = cell.x - originX + (cw - tw) / 2;
					if (isSelected) {
						/* The name of the selected thumbnail is highlighted! */
						gc.setBackground(hoverColor);
						gc.fillRoundRectangle(tx - 3, by + box + 2, tw + 6, lh - 2, 6, 6);
						gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WHITE));
					} else {
						gc.setForeground(textColor);
					}
					gc.drawText(name, tx, by + box + 3, true);
				}
			}
		}
	}

	/* Copies the source rows into the destination (same pixel format), clipped! */
	private static void blit(ImageData src, ImageData dst, int dx, int dy) {
		int sx0 = Math.max(0, -dx), sy0 = Math.max(0, -dy);
		int w = Math.min(src.width - sx0, dst.width - Math.max(0, dx));
		int h = Math.min(src.height - sy0, dst.height - Math.max(0, dy));
		if (w <= 0 || h <= 0) {
			return;
		}
		int bpp = 3;
		for (int y = 0; y < h; y++) {
			System.arraycopy(src.data, (sy0 + y) * src.bytesPerLine + sx0 * bpp, dst.data, (Math.max(0, dy) + y) * dst.bytesPerLine + Math.max(0, dx) * bpp, w * bpp);
		}
	}

	private static String shorten(GC gc, String text, int width) {
		if (gc.textExtent(text).x <= width) {
			return text;
		}
		String ell = "…";
		int lo = 0, hi = text.length();
		while (lo < hi) {
			int mid = (lo + hi + 1) / 2;
			if (gc.textExtent(text.substring(0, mid) + ell).x <= width) {
				lo = mid;
			} else {
				hi = mid - 1;
			}
		}
		return text.substring(0, lo) + ell;
	}

	/** Paints the whole content scaled into the given area (for the overview, one image handle). */
	void paintOverview(GC gc, Rectangle area) {
		Point content = getContentSize();
		if (items.length == 0 || area.width <= 0 || area.height <= 0) {
			return;
		}
		double scale = deviceScale();
		double f = Math.min((double) area.width / content.x, (double) area.height / content.y);
		int box = box();
		int cw = cellWidth(), ch = cellHeight();
		int vw = (int) Math.ceil(area.width * scale), vh = (int) Math.ceil(area.height * scale);
		ImageData vp = new ImageData(vw, vh, 24, ThumbData.PALETTE);
		RGB bg = getBackground().getRGB();
		int[] bgRow = new int[vw];
		java.util.Arrays.fill(bgRow, (bg.red << 16) | (bg.green << 8) | bg.blue);
		for (int y = 0; y < vh; y++) {
			vp.setPixels(0, y, vw, bgRow, 0);
		}
		int sizePx = Math.max(1, (int) Math.round(box * f * scale));
		for (int i = 0; i < items.length; i++) {
			Item item = items[i];
			ImageData tiny = item.tiny == null ? null : item.tiny.get();
			if (tiny == null || item.tinySize != sizePx) {
				/* The kept small copy is used if it is big enough, else the thumbnail! */
				ImageData m = item.mini;
				ThumbData d = sizePx > MINI ? item.thumb() : null;
				ImageData src = d != null ? d.data : m;
				if (src == null) {
					continue;
				}
				double t = (double) sizePx / Math.max(src.width, src.height);
				tiny = src.scaledTo(Math.max(1, (int) Math.round(src.width * t)), Math.max(1, (int) Math.round(src.height * t)));
				item.tiny = new SoftReference<ImageData>(tiny);
				item.tinySize = sizePx;
			}
			Rectangle cell = cellBounds(i);
			int x = (int) Math.round(((cell.x + (cw - box) / 2.0) * f) * scale + (sizePx - tiny.width) / 2.0);
			int y = (int) Math.round(((cell.y + (ch - labelHeight() - box) / 2.0) * f) * scale + (sizePx - tiny.height) / 2.0);
			blit(tiny, vp, x, y);
		}
		final double s = scale;
		Image image = new Image(getDisplay(), (ImageDataProvider) zoomLevel -> Math.abs(zoomLevel / 100.0 - s) < 0.01 ? vp
				: vp.scaledTo(Math.max(1, (int) Math.round(vp.width * zoomLevel / 100.0 / s)), Math.max(1, (int) Math.round(vp.height * zoomLevel / 100.0 / s))));
		try {
			gc.drawImage(image, area.x, area.y);
		} finally {
			image.dispose();
		}
		Rectangle v = getViewport();
		gc.setForeground(hoverColor);
		gc.setLineWidth(2);
		gc.drawRectangle(area.x + (int) (v.x * f), area.y + (int) (v.y * f), Math.max(2, (int) (v.width * f)), Math.max(2, (int) (v.height * f)));
		gc.setLineWidth(1);
	}

	/** Scrolls so that the content point which corresponds to the overview point is centered. */
	void scrollFromOverview(Rectangle area, int x, int y) {
		Point content = getContentSize();
		double f = Math.min((double) area.width / content.x, (double) area.height / content.y);
		Rectangle v = getViewport();
		scrollTo((int) ((x - area.x) / f) - v.width / 2, (int) ((y - area.y) / f) - v.height / 2);
	}


}
