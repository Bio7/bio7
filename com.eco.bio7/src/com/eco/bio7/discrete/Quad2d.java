/*******************************************************************************
 * Copyright (c) 2007-2025 M. Austenfeld
 * 
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * Contributors:
 * Marcel Austenfeld - initial API and implementation
 *******************************************************************************/

package com.eco.bio7.discrete;

import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import com.eco.bio7.methods.CurrentStates;
import com.eco.bio7.rcp.ApplicationWorkbenchWindowAdvisor;
import com.eco.bio7.time.Time;
import com.eco.bio7.util.Util;

/*
 * Faster Quad2d that makes resizing (zoom) very fast by:
 * - Maintaining a small "cell image" (smallImage) where each pixel == one cell.
 * - On paint, scaling smallImage to the desired zoom (quad size) via GC.drawImage.
 * - For incremental updates (click/drag), we only update the smallImage pixel(s) and recreate smallImage (cheap because it's small),
 *   then redraw the scaled region. No per-cell fillRect loops for the full-sized image.
 *
 * This approach drastically speeds up resizing because we don't rebuild a huge image with many pixels.
 */

public class Quad2d extends org.eclipse.swt.widgets.Composite implements KeyListener {

	/* Quad dimensions (in pixels) */
	public int rwidth = Field.getQuadSize();
	public int rheight = Field.getQuadSize();
	public int rx = 0;
	public int ry = 0;
	private int quaddist = 0;

	/* drawing area size in pixels */
	public int zeichenflaechex = (Field.getHeight() * Field.getQuadSize());
	public int zeichenflaechey = (Field.getWidth() * Field.getQuadSize());

	/* grid rectangles - using SWT Rectangle */
	public Rectangle[][] quad = new Rectangle[Field.getHeight()][Field.getWidth()];

	/* Scrolling */
	private ScrolledComposite scrolledComposite;
	public Canvas canvas;

	private static int value = 0; // The active value of the selected State when dragging etc.

	public int RGB[] = new int[3];

	public ArrayList<CounterModel> zaehlerlist = new ArrayList<CounterModel>();

	public boolean dragclick = false;

	public int exz;
	public int eyz;

	public boolean resized = false;

	public boolean quadviewopenend = false;

	private static Quad2d quad2d_instance;

	public boolean selectionenabled = false;

	public boolean donotdrag = false;

	public boolean activeRendering = false;

	private boolean popup_trigger = false; // Linux check

	/* Small image (one pixel per cell) and its ImageData */
	private ImageData smallImageData = null;
	private Image smallImage = null;
	private PaletteData palette = new PaletteData(0xFF0000, 0xFF00, 0xFF);
	private int[] palettePixel = null; // cached palette pixel value per state
	private int lastStateCount = 0;

	/* Batched update flags (coalescing many cell updates) */
	private boolean updateScheduled = false;
	private int dirtyCellX1 = Integer.MAX_VALUE, dirtyCellY1 = Integer.MAX_VALUE, dirtyCellX2 = Integer.MIN_VALUE,
			dirtyCellY2 = Integer.MIN_VALUE;
	private org.eclipse.swt.graphics.Color[] swtColors = null;

	public Quad2d(org.eclipse.swt.widgets.Composite parent, int style) {
		super(parent, style);
		quad2d_instance = this;
		createScrolledComposite();
		drawQuad();
		ensurePixelCache();
		createSmallImageFromField(); // initial small image (one pixel per cell)
		Field.chance();

		setPreferredSize(zeichenflaechey, zeichenflaechex);

		for (int i = 0; i < CurrentStates.getR().size(); i++) {
			zaehlerlist.add(new CounterModel());
		}

		// Paint listener
		canvas.addListener(SWT.Paint, evt -> {
			GC gc = evt.gc;
			paintCanvas(gc, evt.getBounds());
		});

		// Mouse events (SWT Event objects)
		// canvas.addListener(SWT.MouseDown, evt -> mousePressedSWT(evt));
		canvas.addListener(SWT.MouseUp, evt -> mouseReleasedSWT(evt));
		canvas.addListener(SWT.MouseMove, evt -> {
			if ((evt.stateMask & SWT.BUTTON1) != 0) {
				mouseDraggedSWT(evt);
			}
		});
		canvas.addListener(SWT.MouseWheel, evt -> {
			if (Time.isPause()) {
				groesser(evt.count);
			}
		});

		// Key handling
		canvas.addKeyListener(new org.eclipse.swt.events.KeyAdapter() {
			@Override
			public void keyPressed(org.eclipse.swt.events.KeyEvent e) {
				// KeyEvent ke = new KeyEvent(null);
				keyPressed(e);
			}
		});

		// Dispose listener to free resources
		this.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				disposeResources();
			}
		});
	}

	private void createScrolledComposite() {
		scrolledComposite = new ScrolledComposite(this, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		scrolledComposite.setExpandHorizontal(true);
		scrolledComposite.setExpandVertical(true);
		canvas = new Canvas(scrolledComposite, SWT.TRANSPARENT | SWT.DOUBLE_BUFFERED);
		scrolledComposite.setContent(canvas);
		scrolledComposite.setMinSize(zeichenflaechey, zeichenflaechex);

		// layout the scrolledComposite to fill this Composite
		scrolledComposite.setBounds(0, 0, Math.max(1, this.getSize().x), Math.max(1, this.getSize().y));
		this.addListener(SWT.Resize, e -> {
			org.eclipse.swt.graphics.Point s = Quad2d.this.getSize();
			scrolledComposite.setBounds(0, 0, Math.max(1, s.x), Math.max(1, s.y));
		});
	}

	public ScrolledComposite getScrolledComposite() {
		return scrolledComposite;
	}

	public void createzaehler() {
		zaehlerlist.clear();
		for (int i = 0; i < CurrentStates.getR().size(); i++) {
			zaehlerlist.add(new CounterModel());
		}
	}

	public void feldzaehler() {
		reset();
		for (int i = 0; i < Field.getHeight(); i++) {
			for (int u = 0; u < Field.getWidth(); u++) {
				if (Field.getState(u, i) < CurrentStates.getStateList().size()
						&& CurrentStates.getStateList().size() > 0) {
					CounterModel zahl = (CounterModel) zaehlerlist.get(Field.getState(u, i));
					zahl.setZahl();
				}
			}
		}
		for (int i = 0; i < CurrentStates.getStateList().size(); i++) {
			CounterModel zahl = (CounterModel) zaehlerlist.get(i);
			zahl.addCountList(zahl.getCount());
		}
	}

	public void reset() {
		for (int i = 0; i < zaehlerlist.size(); i++) {
			CounterModel zahl = (CounterModel) zaehlerlist.get(i);
			zahl.reset();
		}
	}

	/*
	 * ------------------ Small image (one pixel per cell) logic ------------------
	 */

	/**
	 * Create or update palettePixel[] mapping state -> pixel and mark for
	 * recreation.
	 */
	private void ensurePixelCache() {
		int stateCount = Math.max(0, CurrentStates.getStateList().size());
		if (palettePixel == null || stateCount != lastStateCount) {
			palettePixel = null;
			if (stateCount > 0) {
				palettePixel = new int[stateCount];
				for (int i = 0; i < stateCount; i++) {
					int[] rgb = CurrentStates.getRGB(i);
					int r = Math.max(0, Math.min(255, rgb[0]));
					int g = Math.max(0, Math.min(255, rgb[1]));
					int b = Math.max(0, Math.min(255, rgb[2]));
					palettePixel[i] = palette.getPixel(new RGB(r, g, b));
				}
			}
			lastStateCount = stateCount;
		}
	}

	/**
	 * Build smallImageData (width = number of columns, height = number of rows) and
	 * create smallImage (Image) from it. Each pixel in smallImage represents one
	 * cell.
	 */
	private void createSmallImageFromField() {
		Display d = Display.getCurrent();
		if (d == null) {
			d = Display.getDefault();
		}
		ensurePixelCache();

		int cols = Math.max(1, Field.getWidth());
		int rows = Math.max(1, Field.getHeight());

		smallImageData = new ImageData(cols, rows, 24, palette);

		// background pixel

		int bgPixel = palette.getPixel(new RGB(canvas.getBackground().getRed(), canvas.getBackground().getGreen(),
				canvas.getBackground().getBlue()));

		// fill with background
		for (int y = 0; y < rows; y++) {
			for (int x = 0; x < cols; x++) {
				smallImageData.setPixel(x, y, bgPixel);
			}
		}

		// set pixels per cell state
		if (palettePixel != null) {
			for (int row = 0; row < rows; row++) {
				for (int col = 0; col < cols; col++) {
					int state = Field.getState(col, row);
					int px = (state >= 0 && state < palettePixel.length) ? palettePixel[state] : bgPixel;
					smallImageData.setPixel(col, row, px);
				}
			}
		}

		// create SWT Image (small) from data
		disposeSmallImage();
		try {
			smallImage = new Image(d, smallImageData);
		} catch (IllegalArgumentException iae) {
			// fallback: ignore, smallImage remains null and will be recreated later
			smallImage = null;
		}
	}

	private void disposeSmallImage() {
		if (smallImage != null && !smallImage.isDisposed()) {
			try {
				smallImage.dispose();
			} catch (Exception ex) {
				// ignore
			}
			smallImage = null;
		}
	}

	// call this to (re)build the SWT Color and palette pixel caches from
	// CurrentStates
	public void ensureColorAndPixelCache() {
		// must run on UI thread
		Display d = Display.getCurrent();
		if (d == null) {
			d = Display.getDefault();
		}

		int stateCount = CurrentStates.getStateList().size();

		// dispose old colors
		disposeColorCache();

		if (stateCount > 0) {
			swtColors = new org.eclipse.swt.graphics.Color[stateCount];
			palettePixel = new int[stateCount];
			for (int i = 0; i < stateCount; i++) {
				int[] rgb = CurrentStates.getRGB(i); // assume returns int[3]
				int r = Math.max(0, Math.min(255, rgb[0]));
				int g = Math.max(0, Math.min(255, rgb[1]));
				int b = Math.max(0, Math.min(255, rgb[2]));
				swtColors[i] = new org.eclipse.swt.graphics.Color(d, r, g, b);
				palettePixel[i] = palette.getPixel(new org.eclipse.swt.graphics.RGB(r, g, b));
			}
		} else {
			swtColors = null;
			palettePixel = null;
		}
		lastStateCount = stateCount;
	}

	// dispose cached SWT Color objects
	private void disposeColorCache() {
		if (swtColors != null) {
			for (org.eclipse.swt.graphics.Color c : swtColors) {
				if (c != null && !c.isDisposed()) {
					try {
						c.dispose();
					} catch (Exception ex) {
						/* ignore */ }
				}
			}
			swtColors = null;
		}
		palettePixel = null;
		lastStateCount = 0;
	}

	/* ------------------ Painting (draw scaled smallImage) ------------------ */

	/**
	 * Paint handler draws the scaled smallImage to the canvas. Only the
	 * visible/clip area is drawn by computing a corresponding source rectangle in
	 * cell coordinates.
	 */
	private void paintCanvas(GC gc, Rectangle clip) {
		if (smallImage == null || smallImage.isDisposed()) {
			createSmallImageFromField();
		}
		if (smallImage == null || smallImage.isDisposed()) {
			// fallback: nothing to draw
			return;
		}

		int cols = smallImageData.width;
		int rows = smallImageData.height;
		int cellSize = Field.getQuadSize();
		int destW = cols * cellSize;
		int destH = rows * cellSize;

		// if clip bounds are bigger than image, clamp
		int clipX = Math.max(0, clip.x);
		int clipY = Math.max(0, clip.y);
		int clipW = Math.min(clip.width, Math.max(0, destW - clipX));
		int clipH = Math.min(clip.height, Math.max(0, destH - clipY));
		if (clipW <= 0 || clipH <= 0) {
			return;
		}

		// Convert clip in destination pixels to source (cell) coords.
		// Add one extra cell margin to avoid pixel gaps due to integer division.
		int srcX = clipX / cellSize;
		int srcY = clipY / cellSize;
		int srcW = (clipW + cellSize - 1) / cellSize + 1;
		int srcH = (clipH + cellSize - 1) / cellSize + 1;

		// Clamp source rectangle
		if (srcX < 0)
			srcX = 0;
		if (srcY < 0)
			srcY = 0;
		if (srcX + srcW > cols)
			srcW = cols - srcX;
		if (srcY + srcH > rows)
			srcH = rows - srcY;
		if (srcW <= 0 || srcH <= 0) {
			return;
		}

		// Destination rectangle corresponding to the source rectangle
		int dstX = srcX * cellSize;
		int dstY = srcY * cellSize;
		int dstW = srcW * cellSize;
		int dstH = srcH * cellSize;
		try {
			gc.setAntialias(SWT.OFF); // optional, disables antialiasing
		} catch (NoSuchMethodError ignore) {
		}
		// set interpolation to none (nearest-neighbor)
		if (Util.getOS().equals("Mac") || Util.getOS().equals("Linux")) {
			/* On Windows drastically reduces the speed of display! */
			gc.setInterpolation(SWT.NONE);
		}
		// Finally draw the required portion of the small image scaled up.
		try {
			gc.drawImage(smallImage, srcX, srcY, srcW, srcH, dstX, dstY, dstW, dstH);
		} catch (IllegalArgumentException iae) {
			// Fallback: draw full scaled image if the partial draw fails
			gc.drawImage(smallImage, 0, 0, cols, rows, 0, 0, destW, destH);
		}
	}

	/*
	 * ------------------ Incremental updates (update smallImage pixel and recreate
	 * smallImage) ------------------
	 */

	/**
	 * Update one cell in the smallImageData and schedule a batched recreation of
	 * smallImage + redraw. This is cheap because smallImage dimensions == number of
	 * cells.
	 */
	private void updateCellInSmallImage(final int cellX, final int cellY) {
		if (cellX < 0 || cellY < 0 || cellY >= quad.length || cellX >= quad[0].length) {
			return;
		}

		ensurePixelCache();

		Display d = Display.getCurrent();
		if (d == null) {
			d = Display.getDefault();
		}
		final Display disp = d;

		Runnable writeAndSchedule = () -> {
			if (smallImageData == null) {
				createSmallImageFromField();
				if (!canvas.isDisposed())
					canvas.redraw();
				return;
			}

			int cols = smallImageData.width;
			int rows = smallImageData.height;
			if (cellX >= cols || cellY >= rows)
				return;

			int state = Field.getState(cellX, cellY);
			int pixel = (palettePixel != null && state >= 0 && state < palettePixel.length) ? palettePixel[state]
					: palette.getPixel(new RGB(canvas.getBackground().getRed(), canvas.getBackground().getGreen(),
							canvas.getBackground().getBlue()));

			smallImageData.setPixel(cellX, cellY, pixel);

			// mark dirty cell extents
			dirtyCellX1 = Math.min(dirtyCellX1, cellX);
			dirtyCellY1 = Math.min(dirtyCellY1, cellY);
			dirtyCellX2 = Math.max(dirtyCellX2, cellX + 1);
			dirtyCellY2 = Math.max(dirtyCellY2, cellY + 1);

			// schedule recreation and redraw (coalesced)
			if (!updateScheduled) {
				updateScheduled = true;
				disp.asyncExec(() -> {
					updateScheduled = false;
					// recreate smallImage from smallImageData
					disposeSmallImage();
					try {
						smallImage = new Image(disp, smallImageData);
					} catch (IllegalArgumentException iae) {
						// ignore — will try again later
						smallImage = null;
					}
					// compute redraw rectangle in destination (scaled) coords
					if (!canvas.isDisposed()) {
						int cellSize = Field.getQuadSize();
						int dx = (dirtyCellX1 == Integer.MAX_VALUE) ? 0 : dirtyCellX1 * cellSize;
						int dy = (dirtyCellY1 == Integer.MAX_VALUE) ? 0 : dirtyCellY1 * cellSize;
						int dw = ((dirtyCellX2 == Integer.MIN_VALUE) ? canvas.getBounds().width
								: (dirtyCellX2 - dirtyCellX1) * cellSize);
						int dh = ((dirtyCellY2 == Integer.MIN_VALUE) ? canvas.getBounds().height
								: (dirtyCellY2 - dirtyCellY1) * cellSize);
						if (dw <= 0)
							dw = 1;
						if (dh <= 0)
							dh = 1;
						canvas.redraw(dx, dy, dw, dh, true);
					}
					// reset dirty extents
					dirtyCellX1 = Integer.MAX_VALUE;
					dirtyCellY1 = Integer.MAX_VALUE;
					dirtyCellX2 = Integer.MIN_VALUE;
					dirtyCellY2 = Integer.MIN_VALUE;
				});
			}
		};

		if (disp.getThread() == Thread.currentThread()) {
			writeAndSchedule.run();
		} else {
			disp.syncExec(writeAndSchedule);
		}
	}

	/**
	 * Recreate the entire smallImage from current field (use when many cells
	 * change).
	 */
	public void fullRedrawAll() {
		Display d = Display.getCurrent();
		if (d == null) {
			d = Display.getDefault();
		}
		final Display disp = d;
		Runnable r = () -> {
			createSmallImageFromField();
			if (!canvas.isDisposed()) {
				canvas.redraw();
			}
		};
		if (disp.getThread() == Thread.currentThread()) {
			r.run();
		} else {
			disp.asyncExec(r);
		}
	}

	/* ------------------ Original logic adapted ------------------ */

	public void drawQuad() {
		rx = 0;
		ry = 0;
		for (int u = 0; u < Field.getHeight(); u++) {
			for (int v = 0; v < Field.getWidth(); v++) {
				quad[u][v] = new Rectangle(rx, ry, rwidth, rheight);
				rx = rx + rwidth + quaddist;
			}
			rx = 0;
			ry = ry + rheight + quaddist;
		}
		ry = 0;
		if (scrolledComposite != null) {
			scrolledComposite.setMinSize(zeichenflaechey, zeichenflaechex);
		}
		// Recreate the small image to match the (possibly) new grid size
		createSmallImageFromField();
	}

	public void groesser(int wheelRotation) {
		Time.setPause(true);
		if (wheelRotation >= 1) {
			Field.setQuadSize(Field.getQuadSize() + 1);
		} else {
			if (Field.getQuadSize() > 1) {
				Field.setQuadSize(Field.getQuadSize() - 1);
			}
		}
		rwidth = Field.getQuadSize();
		rheight = Field.getQuadSize();
		zeichenflaechex = ((Field.getHeight() * Field.getQuadSize()));
		zeichenflaechey = ((Field.getWidth() * Field.getQuadSize()));
		drawQuad();
		setPreferredSize(zeichenflaechey, zeichenflaechex);
		// No heavy per-pixel rebuild needed here — smallImage will be scaled at paint
		// time.
		if (!canvas.isDisposed()) {
			canvas.redraw();
		}
	}

	public void quadResize(int size) {
		Field.setQuadSize(size);
		Time.setPause(true);
		rwidth = Field.getQuadSize();
		rheight = Field.getQuadSize();
		zeichenflaechex = ((Field.getHeight() * Field.getQuadSize()));
		zeichenflaechey = ((Field.getWidth() * Field.getQuadSize()));
		drawQuad();
		setPreferredSize(zeichenflaechey, zeichenflaechex);
		if (!canvas.isDisposed()) {
			canvas.redraw();
		}
	}

	private void Panel1MousePressed(org.eclipse.swt.widgets.Event evt) {
		// org.eclipse.swt.events.MouseEvent me = new
		// org.eclipse.swt.events.MouseEvent(evt);
		mousepressedSWT(evt);
	}

	/* This method is for Image J ! */
	public void setquads(double x, double y) {
		exz = (int) (x / Field.getQuadSize());
		eyz = (int) (y / Field.getQuadSize());
		if (exz <= Field.getWidth() && eyz <= Field.getHeight()) {
			Field.setState(exz, eyz, getValue());
			Field.setTempState(exz, eyz, getValue());
			updateCellInSmallImage(exz, eyz); // update the one-pixel representation
		}
	}

	/* SWT-style mouse handlers */

	private void mousepressedSWT(org.eclipse.swt.widgets.Event evt) {
		org.eclipse.swt.events.MouseEvent e = new org.eclipse.swt.events.MouseEvent(evt);
		int ex = e.x;
		int ey = e.y;

		exz = ex / Field.getQuadSize();
		eyz = ey / Field.getQuadSize();
		if ((exz >= 0 && exz < Field.getWidth()) && (eyz >= 0 && eyz < Field.getHeight())) {

			if (selectionenabled) {

				if (rectContains(quad[eyz][exz], ex, ey)) {

					Display display = PlatformUI.getWorkbench().getDisplay();
					display.syncExec(new Runnable() {

						public void run() {
							Quadview.getQuadview().setstatusline(
									"Current " + Field.getState(exz, eyz) + " Temp: " + Field.getTempState(exz, eyz));
						}
					});

				}

			} else {

				if (rectContains(quad[eyz][exz], ex, ey) && e.button != 3) {

					Field.setState(exz, eyz, getValue());
					Field.setTempState(exz, eyz, getValue());
					updateCellInSmallImage(exz, eyz);

				}

			}
		}
	}

	private boolean rectContains(Rectangle r, int x, int y) {
		if (r == null)
			return false;
		return (x >= r.x && x < (r.x + r.width) && y >= r.y && y < (r.y + r.height));
	}

	private void mouseReleasedSWT(org.eclipse.swt.widgets.Event event) {
		org.eclipse.swt.events.MouseEvent e = new org.eclipse.swt.events.MouseEvent(event);
		if (Time.isPause()) {

			boolean isPopup = (e.button == 3) || popup_trigger;
			if (isPopup && selectionenabled == false) {

				Menu menu = new Menu(canvas);

				new MenuItem(menu, SWT.SEPARATOR);
				final MenuItem it2 = new MenuItem(menu, SWT.PUSH);
				it2.setText("Random");

				it2.addListener(SWT.Selection, ev -> {
					Field.chance();
					fullRedrawAll(); // rebuild the small image and redraw
				});

				org.eclipse.swt.graphics.Point pt = new org.eclipse.swt.graphics.Point(e.x, e.y);
				menu.setLocation(canvas.toDisplay(pt));
				menu.setVisible(true);

			} else {

				canvas.setCursor(Display.getCurrent().getSystemCursor(SWT.CURSOR_ARROW));
				donotdrag = false;

			}

		}
		popup_trigger = false;
	}

	private void mouseDraggedSWT(org.eclipse.swt.widgets.Event event) {
		org.eclipse.swt.events.MouseEvent e = new org.eclipse.swt.events.MouseEvent(event);
		if (Time.isPause()) {
			if (selectionenabled == false && donotdrag == false) {
				mousedragSWT(e);
			}
		}
	}

	private void mousedragSWT(org.eclipse.swt.events.MouseEvent e) {
		if (e.button != 3) {
			int x = e.x;
			int y = e.y;
			exz = x / Field.getQuadSize();
			eyz = y / Field.getQuadSize();

			if ((exz >= 0 && exz < Field.getWidth()) && (eyz >= 0 && eyz < Field.getHeight())) {// dragging

				if (rectContains(quad[eyz][exz], x, y)) {

					Field.setState(exz, eyz, getValue());
					Field.setTempState(exz, eyz, getValue());

					updateCellInSmallImage(exz, eyz);

				}

			}
		}
	}

	public void resize_scrollpane_quad2d() {
		zeichenflaechex = ((Field.getHeight() * Field.getQuadSize()));
		zeichenflaechey = ((Field.getWidth() * Field.getQuadSize()));
		setPreferredSize(zeichenflaechey, zeichenflaechex);
		// smallImage scales instantly at paint time; no expensive rebuild here
		if (!canvas.isDisposed()) {
			canvas.redraw();
		}
	}

	public void mouseMoved(org.eclipse.swt.events.MouseEvent evt) {
		// no-op
	}

	public void mouseClicked(org.eclipse.swt.events.MouseEvent e) {
		if (!canvas.isFocusControl()) {
			canvas.setFocus();
		}
	}

	public void mousePressed(org.eclipse.swt.events.MouseEvent e) {
		if (!canvas.isFocusControl()) {
			canvas.setFocus();
		}
		if (ApplicationWorkbenchWindowAdvisor.getOS().equals("Linux")) {
			if (e.button == 3) {
				popup_trigger = true;
			}
		}

		if (Time.isPause()) {

			Panel1MousePressed(new org.eclipse.swt.widgets.Event());
			selectionenabled = false;
		}
	}

	public void mouseEntered(org.eclipse.swt.events.MouseEvent evt) {
		// no-op
	}

	public void mouseExited(org.eclipse.swt.events.MouseEvent e) {
		// no-op
	}

	/* KeyListener implementation */
	@Override
	public void keyPressed(KeyEvent e) {
		int r = Field.getQuadSize();
		// plus/minus handling via character
		if (e.character == '+' || e.keyCode == SWT.KEYPAD_ADD) {
			r = Field.getQuadSize() + 1;
		} else if (e.character == '-' || e.keyCode == SWT.KEYPAD_SUBTRACT) {
			if (Field.getQuadSize() - 1 > 0) {
				r = Field.getQuadSize() - 1;
			}
		}
		Time.setPause(true);
		Field.setQuadSize(r);
		rwidth = Field.getQuadSize();
		rheight = Field.getQuadSize();
		zeichenflaechex = ((Field.getHeight() * Field.getQuadSize()));
		zeichenflaechey = ((Field.getWidth() * Field.getQuadSize()));
		drawQuad();
		setPreferredSize(zeichenflaechey, zeichenflaechex);
	}

	@Override
	public void keyReleased(KeyEvent e) {
		// no-op
	}

	/* ------------------ Utilities and cleanup ------------------ */

	private void disposeResources() {
		disposeSmallImage();
		smallImageData = null;
		palettePixel = null;
		lastStateCount = 0;
	}

	/* Public helpers */

	public static Quad2d getQuad2dInstance() {
		return quad2d_instance;
	}

	public static void setValue(int value) {
		Quad2d.value = value;
	}

	public static int getValue() {
		return value;
	}

	/* Helper to set preferred size of the scrolled content */
	public void setPreferredSize(int width, int height) {
		this.zeichenflaechey = width;
		this.zeichenflaechex = height;
		if (scrolledComposite != null) {
			scrolledComposite.setMinSize(width, height);
		}
		if (canvas != null) {
			canvas.setSize(width, height);
		}
	}

	/* redraw the whole canvas */
	public void redrawCanvas() {

		Display.getDefault().syncExec(() -> {
			canvas.redraw();

		});

	}

	/* redraw a single cell (requests redraw of the scaled region) */
	private void redrawCell(int cellX, int cellY) {
		if (cellX < 0 || cellY < 0 || cellY >= quad.length || cellX >= quad[0].length)
			return;
		Rectangle r = quad[cellY][cellX];
		if (r != null && canvas != null && !canvas.isDisposed()) {
			canvas.redraw(r.x, r.y, r.width, r.height, true);
		} else {
			redrawCanvas();
		}
	}
}