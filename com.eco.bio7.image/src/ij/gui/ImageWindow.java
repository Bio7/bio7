package ij.gui;

import java.awt.*;
import java.awt.image.*;
import java.util.Properties;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import java.awt.event.*;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.PlatformUI;

import com.eco.bio7.image.Activator;
import com.eco.bio7.image.CanvasView;
import com.eco.bio7.image.CustomDetachedImageJView;
import com.eco.bio7.image.IJTabs;
import com.eco.bio7.image.SwtAwtImageJ;
import com.eco.bio7.image.Util;

import ij.*;
import ij.process.*;
import ij.io.*;
import ij.measure.*;
import ij.plugin.frame.*;
import ij.plugin.PointToolOptions;
import ij.macro.Interpreter;
import ij.util.*;

/** A frame for displaying images. */
public class ImageWindow extends JFrame
		implements FocusListener, WindowListener, WindowStateListener, MouseWheelListener {

	public static final int MIN_WIDTH = 128;
	public static final int MIN_HEIGHT = 32;
	public static final int HGAP = 5;
	public static final int VGAP = 5;
	public static final String LOC_KEY = "image.loc";

	protected ImagePlus imp;
	protected ImageJ ij;
	protected ImageCanvas ic;
	private double initialMagnification = 1;
	private int newWidth, newHeight;
	protected boolean closed;
	private boolean newCanvas;
	private boolean unzoomWhenMinimizing = true;
	Rectangle maxWindowBounds; // largest possible window on this screen
	Rectangle maxBounds; // Size of this window after it is maximized
	long setMaxBoundsTime;
	private int sliderHeight;
	private static final int XINC = 12;
	private static final int YINC = 16;
	private final double SCALE = Prefs.getGuiScale();
	private int TEXT_GAP = 11;
	private static int xbase = -1;
	private static int ybase;
	private static int xloc;
	private static int yloc;
	private static int count;
	private static boolean centerOnScreen;
	private static Point nextLocation;
	public static long setMenuBarTime;
	private int textGap = centerOnScreen ? 0 : TEXT_GAP;
	private int screenHeight, screenWidth;
	public SwtAwtImageJ swtAwtMain;// Changed for Bio7!
	public boolean running2;
	protected boolean imageInTab;
	protected ImageWindow win2;
	/**
	 * This variable is set false if the user presses the escape key or closes the
	 * window.
	 */
	public boolean running;

	/**
	 * This variable is set false if the user clicks in this window, presses the
	 * escape key, or closes the window.
	 */

	// private ImageWindow windowInstance;// Changed for Bio7!
	public SwtAwtImageJ getSwtAwtMain() {
		return swtAwtMain;
	}

	public void setSwtAwtMain(SwtAwtImageJ swtAwtMainFrame) {
		this.swtAwtMain = swtAwtMainFrame;
	}

	public ImageWindow(String title) {
		super(title);
	}

	public ImageWindow(ImagePlus imp) {
		this(imp, null);
		/// * Changed for Bio7! */
		// windowInstance = this;
	}

	/* Changed for Bio7 setting the tab title and not the JFrame title! */
	public void setTitle(String title) {

		final CTabItem[] items = CanvasView.getCanvas_view().tabFolder.getItems();
		Display dis = CanvasView.getParent2().getDisplay();
		dis.syncExec(new Runnable() {

			public void run() {
				for (int i = 0; i < items.length; i++) {

					Vector ve = (Vector) items[i].getData();

					final ImageWindow win2 = (ImageWindow) ve.get(1);

					/* Search for the tab which embeds this instance! */
					if (ImageWindow.this.equals(win2)) {
						// Rename the tab!
						CanvasView.tabFolder.getItem(i).setText(title);

						return;

					}

				}

				/*
				 * CustomDetachedImageJView customImageJView = ImageJ.customImageJView; if
				 * (customImageJView != null) { customImageJView.setPartName(title); }
				 */

				IViewReference[] viewRefs = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
						.getViewReferences();
				for (int i = 0; i < viewRefs.length; i++) {
					String id = viewRefs[i].getId();
					if (id.equals("com.eco.bio7.image.detachedImage")) {
						IViewPart view = viewRefs[i].getView(false);
						String secId = viewRefs[i].getSecondaryId();
						CustomDetachedImageJView cdview = (CustomDetachedImageJView) view;
						/* Get the image from the detached secondary view id (same id)! */
						ImagePlus ip = WindowManager.getImage(Integer.valueOf(secId));
						if (ip != null) {
							if (ImageWindow.this.equals(ip.getWindow())) {
								cdview.setPartName(title);
							}
						}

					}
				}

			}
		});

	}

	public ImageWindow(ImagePlus imp, ImageCanvas ic) {
		super(imp.getTitle());
		if (SCALE > 1.0) {
			TEXT_GAP = (int) (TEXT_GAP * SCALE);
			textGap = centerOnScreen ? 0 : TEXT_GAP;
		}
		boolean openAsHyperStack = imp.getOpenAsHyperStack();
		ij = IJ.getInstance();
		this.imp = imp;
		if (ic == null) {
			ic = (this instanceof PlotWindow) ? new PlotCanvas(imp) : new ImageCanvas(imp);
			newCanvas = true;
		}
		this.ic = ic;
		ImageWindow previousWindow = imp.getWindow();

		setLayout(new ImageLayout(ic));
		add(ic);
		addFocusListener(this);
		addWindowListener(this);
		addWindowStateListener(this);
		addKeyListener(ij);

		setFocusTraversalKeysEnabled(false);
		if (!(this instanceof StackWindow))
			addMouseWheelListener(this);

		setResizable(true);
		if (!(this instanceof HistogramWindow && IJ.isMacro() && Interpreter.isBatchMode())) {
			WindowManager.addWindow(this);
			imp.setWindow(this);
		}
		if (previousWindow != null) {
			/* Hide the tab without to close the image! */
			IJTabs.hideTab();
			if (newCanvas)
				setLocationAndSize(false);
			else
				ic.update(previousWindow.getCanvas());

			if (!(this instanceof StackWindow || this instanceof PlotWindow)) { // layout now unless components will be
																				// added later
				swtAwtMain = new SwtAwtImageJ(null, null, null, ic, imp, this);
				/* Add the panel to the tab folder with the image */
				swtAwtMain.addTab(imp.getTitle());
				imp.setWindow(this);

				/*
				 * The next seems to be important for Mac. Else at the first zoom level the
				 * image disapears!
				 */
				java.awt.EventQueue.invokeLater(new Runnable() {
					public void run() {
						pack();
					}
				});
				/* Necessary for highDPI layout SWT_AWT! */
				CanvasView canvasView = CanvasView.getCanvas_view();
				canvasView.recalculateLayout();
				/*
				 * The next seems to be important for Mac. Else at the first zoom level the
				 * image disapears!
				 */
				if (IJ.isMacro())
					imp.setDeactivated(); // prepare for waitTillActivated (imp may have been activated before if it
											// gets a new Window now)
				// show();
			}
			if (ic.getMagnification() != 0.0)
				imp.setTitle(imp.getTitle());
			boolean unlocked = imp.lockSilently();
			boolean changes = imp.changes;
			imp.changes = false;
			/*
			 * Changed for Bio7. We have to call the default ImageJ close function which is
			 * renamed to bio7TabClose because we have hidden the tab already (close
			 * searches for the tab and closes it)!
			 */
			previousWindow.bio7TabClose(true);
			imp.changes = changes;
			if (unlocked)
				imp.unlock();
			if (this.imp != null)
				this.imp.setOpenAsHyperStack(openAsHyperStack);
			WindowManager.setCurrentWindow(this);
		} else {
			// setLocationAndSize(false);
			/*
			 * if (ij != null && !IJ.isMacintosh()) { Image img = ij.getIconImage(); if (img!=null) try {
					setIconImage(img);
				} catch (Exception e) {}
			 */

			if (Interpreter.isBatchMode() || (IJ.getInstance() == null && this instanceof HistogramWindow)) {
				WindowManager.setTempCurrentImage(imp);
				Interpreter.addBatchModeImage(imp);
			}

			else {
				if (IJ.isMacro())
					imp.setDeactivated(); // prepare for waitTillActivated (imp may have been activated previously and
											// gets a new Window now)

				if (!(this instanceof StackWindow)) {
					swtAwtMain = new SwtAwtImageJ(null, null, null, ic, imp, this);
					/* Add the panel to the tab folder with the image */
					swtAwtMain.addTab(imp.getTitle());
					imp.setWindow(this);
					/*
					 * The next seems to be important for Mac. Else at the first zoom level the
					 * image disapears!
					 */
					java.awt.EventQueue.invokeLater(new Runnable() {
						public void run() {
							pack();
						}
					});
					/* Necessary for highDPI layout SWT_AWT! */
					CanvasView canvasView = CanvasView.getCanvas_view();
					canvasView.recalculateLayout();

				}

			}

		}

	}

	private void setLocationAndSize(boolean updating) {
		if (imp == null)
			return;
		int width = imp.getWidth();
		int height = imp.getHeight();

		// load preferences file location
		Point loc = Prefs.getLocation(LOC_KEY);
		Rectangle bounds = null;
		if (loc != null) {
			bounds = GUI.getMaxWindowBounds(loc);
			if (bounds != null && (loc.x > bounds.x + bounds.width / 3 || loc.y > bounds.y + bounds.height / 3)
					&& (loc.x + width > bounds.x + bounds.width || loc.y + height > bounds.y + bounds.height)) {
				loc = null;
				bounds = null;
			}
		}
		// if loc not valid, use screen bounds of visible window (this) or of main
		// window (ij) if not visible yet (updating == false)
		Rectangle maxWindow = bounds != null ? bounds : GUI.getMaxWindowBounds(updating ? this : ij);

		if (WindowManager.getWindowCount() <= 1)
			xbase = -1;
		if (width > maxWindow.width / 2 && xbase > maxWindow.x + 5 + XINC * 6)
			xbase = -1;
		if (xbase == -1) {
			count = 0;
			if (loc != null) {
				xbase = loc.x;
				ybase = loc.y;
			} else if (ij != null) {
				Rectangle ijBounds = ij.getBounds();
				if (ijBounds.y - maxWindow.x < maxWindow.height / 8) {
					xbase = ijBounds.x;
					if (xbase + width > maxWindow.x + maxWindow.width) {
						xbase = maxWindow.x + maxWindow.width - width - 10;
						if (xbase < maxWindow.x)
							xbase = maxWindow.x + 5;
						;
					}
					ybase = ijBounds.y + ijBounds.height + 5;
				} else {
					xbase = maxWindow.x + (maxWindow.width - width) / 2;
					ybase = maxWindow.y + (maxWindow.height - height) / 4;
				}
			} else {
				xbase = maxWindow.x + (maxWindow.width - width) / 2;
				ybase = maxWindow.y + (maxWindow.height - height) / 4;
			}
			xbase = Math.max(xbase, maxWindow.x);
			ybase = Math.max(ybase, maxWindow.y);
			//if (IJ.debugMode)
				//IJ.log("ImageWindow.xbase: " + xbase);
			xloc = xbase;
			yloc = ybase;
		}
		int x = xloc;
		int y = yloc;
		xloc += XINC;
		yloc += YINC;
		count++;
		if (count % 6 == 0) {
			xloc = xbase;
			yloc = ybase;
		}

		screenHeight = maxWindow.y + maxWindow.height - sliderHeight;
		screenWidth = maxWindow.x + maxWindow.width;
		double mag = 1;
		if (!(this instanceof PlotWindow)) { // unless a plot (always at 100%), zoom out to show all of image
			while (xbase + width * mag > screenWidth || ybase + height * mag >= screenHeight) {
				double mag2 = ImageCanvas.getLowerZoomLevel(mag);
				if (mag2 == mag)
					break;
				mag = mag2;
			}
		}

		if (mag < 1.0) {
			initialMagnification = mag;
			ic.setSize((int) (width * mag), (int) (height * mag));
		}
		ic.setMagnification(mag);
		if (y + height * mag > screenHeight)
			y = ybase;
		if (Prefs.open100Percent && ic.getMagnification() < 1.0) {
			while (ic.getMagnification() < 1.0)
				ic.zoomIn(0, 0);
			setSize(Math.min(width, screenWidth - x), Math.min(height, screenHeight - y));
			validate();
		} else
			pack();
		if (!updating)
			setLocation(x, y);
	}

	Rectangle getMaxWindow(int xloc, int yloc) {

		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		/* Changed for Bio7! */
		Rectangle bounds = CanvasView.getCanvas_view().getCurrent().getBounds();

		// return GUI.getMaxWindowBounds(new Point(xloc, yloc));

		return bounds;
	}

	/*
	 * public Rectangle getBounds() {
	 * 
	 * Changed for Bio7! Rectangle bounds =
	 * CanvasView.getCanvas_view().getCurrent().getBounds();
	 * 
	 * return bounds; }
	 */

	public double getInitialMagnification() {
		return initialMagnification;
	}

	/**
	 * Override Container getInsets() to make room for some text above the image.
	 */
	/*
	 * public Insets getInsets() { Insets insets = super.getInsets(); if (imp ==
	 * null) return insets; double mag = ic.getMagnification(); int extraWidth =
	 * (int) ((MIN_WIDTH - imp.getWidth() * mag) / 2.0); if (extraWidth < 0)
	 * extraWidth = 0; int extraHeight = (int) ((MIN_HEIGHT - imp.getHeight() * mag)
	 * / 2.0); if (extraHeight < 0) extraHeight = 0; insets = new Insets(insets.top
	 * + textGap + extraHeight, insets.left + extraWidth, insets.bottom +
	 * extraHeight, insets.right + extraWidth); return insets; }
	 */ 

	/** Draws the subtitle. */
	public void drawInfo(Graphics g) {
		if (imp == null)
			return;
		if (textGap != 0) {
			Insets insets = super.getInsets();
			Color savec = null;
			if (imp.isComposite()) {
				CompositeImage ci = (CompositeImage) imp;
				if (ci.getMode() == IJ.COMPOSITE) {
					savec = g.getColor();
					Color c = ci.getChannelColor();
					if (Color.green.equals(c))
						c = new Color(0, 180, 0);
					g.setColor(c);
				}
			}
			Java2.setAntialiasedText(g, true);
			if (SCALE > 1.0) {
				Font font = new Font("SansSerif", Font.PLAIN, (int) (12 * SCALE));
				g.setFont(font);
			}
			g.drawString(createSubtitle(), insets.left + 5, insets.top + TEXT_GAP);
			if (savec != null)
				g.setColor(savec);
		}
	}

	/** Creates the subtitle. */
	public String createSubtitle() {
		String s = "";
		if (imp == null)
			return s;
		int stackSize = imp.getStackSize();
		if (stackSize > 1) {
			ImageStack stack = imp.getStack();
			int currentSlice = imp.getCurrentSlice();
			s += currentSlice + "/" + stackSize;
			String label = stack.getShortSliceLabel(currentSlice);
			if (label != null && label.length() > 0) {
				if (imp.isHyperStack())
					label = label.replace(';', ' ');
				s += " (" + label + ")";
			}
			if ((this instanceof StackWindow) && running2) {
				return s;
			}
			s += "; ";
		} else {
			String label = imp.getProp("Slice_Label");
			if (label==null && imp.hasImageStack())
				label = imp.getStack().getSliceLabel(1);
			if (label != null && label.length() > 0) {
				int newline = label.indexOf('\n');
				if (newline > 0)
					label = label.substring(0, newline);
				int len = label.length();
				if (len > 4 && label.charAt(len - 4) == '.' && !Character.isDigit(label.charAt(len - 1)))
					label = label.substring(0, len - 4);
				if (label.length() > 60)
					label = label.substring(0, 60) + "...";
				s = "\"" + label + "\"; ";
			}
		}
		int type = imp.getType();
		Calibration cal = imp.getCalibration();
		if (cal.scaled()) {
			boolean unitsMatch = cal.getXUnit().equals(cal.getYUnit());
			double cwidth = imp.getWidth() * cal.pixelWidth;
			double cheight = imp.getHeight() * cal.pixelHeight;
			int digits = Tools.getDecimalPlaces(cwidth, cheight);
			if (digits > 2)
				digits = 2;
			if (unitsMatch) {
				s += IJ.d2s(cwidth, digits) + "x" + IJ.d2s(cheight, digits) + " " + cal.getUnits() + " ("
						+ imp.getWidth() + "x" + imp.getHeight() + "); ";
			} else {
				s += d2s(cwidth) + " " + cal.getXUnit() + " x " + d2s(cheight) + " " + cal.getYUnit() + " ("
						+ imp.getWidth() + "x" + imp.getHeight() + "); ";
			}
		} else
			s += imp.getWidth() + "x" + imp.getHeight() + " pixels; ";
		switch (type) {
		case ImagePlus.GRAY8:
		case ImagePlus.COLOR_256:
			s += "8-bit";
			break;
		case ImagePlus.GRAY16:
			s += "16-bit";
			break;
		case ImagePlus.GRAY32:
			s += "32-bit";
			break;
		case ImagePlus.COLOR_RGB:
			s += imp.isRGB() ? "RGB" : "32-bit (int)";
			break;
		}
		if (imp.isInvertedLut())
			s += " (inverting LUT)";
		return s + "; " + getImageSize(imp);
	}

	public static String getImageSize(ImagePlus imp) {
		if (imp == null)
			return null;
		double size = imp.getSizeInBytes() / 1024.0;
		String s2 = null, s3 = null;
		if (size < 1024.0) {
			s2 = IJ.d2s(size, 0);
			s3 = "K";
		} else if (size < 10000.0) {
			s2 = IJ.d2s(size / 1024.0, 1);
			s3 = "MB";
		} else if (size < 1048576.0) {
			s2 = IJ.d2s(Math.round(size / 1024.0), 0);
			s3 = "MB";
		} else {
			s2 = IJ.d2s(size / 1048576.0, 1);
			s3 = "GB";
		}
		if (s2.endsWith(".0"))
			s2 = s2.substring(0, s2.length() - 2);
		return s2 + s3;
	}

	private String d2s(double n) {
		int digits = Tools.getDecimalPlaces(n);
		if (digits > 2)
			digits = 2;
		return IJ.d2s(n, digits);
	}

	/* Changed for Bio7! */
	/*
	 * public Rectangle getBounds(){ GraphicsEnvironment ge =
	 * GraphicsEnvironment.getLocalGraphicsEnvironment(); Changed for Bio7!
	 * Rectangle bounds = CanvasView.getCanvas_view().getCurrent().getBounds();
	 * return bounds; }
	 */

	public void paintComponents(Graphics g) {
		super.paintComponents(g);
		drawInfo(g);
		Rectangle r = ic.getBounds();
		int extraWidth = MIN_WIDTH - r.width;
		int extraHeight = MIN_HEIGHT - r.height;
		if (extraWidth <= 0 && extraHeight <= 0 && !Prefs.noBorder && !IJ.isLinux())
			g.drawRect(r.x - 1, r.y - 1, r.width + 1, r.height + 1);
	}

	/**
	 * Removes this window from the window list and disposes of it. Returns false if
	 * the user cancels the "save changes" dialog.
	 */
	public boolean close() {
		/*
		 * boolean isRunning = running || running2; running = running2 = false; if
		 * (imp==null) return true; boolean virtual = imp.getStackSize()>1 &&
		 * imp.getStack().isVirtual(); if (isRunning) IJ.wait(500); if (ij==null ||
		 * IJ.getApplet()!=null || Interpreter.isBatchMode() || IJ.macroRunning() ||
		 * virtual) imp.changes = false; if (imp.changes) { String msg; String name =
		 * imp.getTitle(); if (name.length()>22) msg = "Save changes to\n" + "\"" + name
		 * + "\"?"; else msg = "Save changes to \"" + name + "\"?"; YesNoCancelDialog d
		 * = new YesNoCancelDialog(this, "ImageJ", msg); if (d.cancelPressed()) return
		 * false; else if (d.yesPressed()) { FileSaver fs = new FileSaver(imp); if
		 * (!fs.save()) return false; } } closed = true; if
		 * (WindowManager.getWindowCount()==0) {xloc = 0; yloc = 0;} if
		 * (firstSmallWindow) Prefs.saveLocation(LOC_KEY, getLocation());
		 * WindowManager.removeWindow(this);
		if (ij!=null && ij.quitting())  // this may help avoid thread deadlocks
			return true;
		Rectangle bounds = getBounds();
		if (initialLoc!=null && !bounds.equals(initialLoc) && !IJ.isMacro()
		&& bounds.y<screenHeight/3 && (bounds.y+bounds.height)<=screenHeight
		&& (bounds.x+bounds.width)<=screenWidth) {
			Prefs.saveLocation(LOC_KEY, new Point(bounds.x,bounds.y));
			xbase = -1;
		} dispose(); if (imp!=null)
		 * imp.flush(); imp = null; return true;
		 */

		/*if (imp.isLocked())
				msg += "\nWARNING: This image is locked.\nProbably, processing is unfinished (slow or still previewing).";*/

		/* Changed for Bio7! */

		final CTabItem[] items = CanvasView.getCanvas_view().tabFolder.getItems();
		Display dis = CanvasView.getParent2().getDisplay();
		dis.syncExec(new Runnable() {

			public void run() {
				for (int i = 0; i < items.length; i++) {

					Vector ve = (Vector) items[i].getData();

					win2 = (ImageWindow) ve.get(1);

					/* Search for the tab which embeds this instance! */
					if (ImageWindow.this.equals(win2)) {
						imageInTab = true;
						final CTabItem[] items = CanvasView.getCanvas_view().tabFolder.getItems();
						Composite com = (Composite) items[i].getControl();
						Control compo[] = com.getChildren();
						for (int u = 0; u < compo.length; u++) {
							if (compo[u] != null && compo[u].isDisposed() == false) {
								compo[u].dispose();
							}
						}
						com.dispose();
						items[i].dispose();
						break;

					} else {
						imageInTab = false;
					}

				}

			}
		});
		// System.out.println(this.getImagePlus().getTitle());
		/*Get the specific embedded window instance and close it!*/
		if (imageInTab) {
			// ImageWindow win = (ImageWindow) ve.get(1);
			win2.bio7TabClose(true);
		} else {
			/*Check if we have images in the detached views!*/
			boolean isDetachedWin = IJTabs.closeDetachedWindowView(this);
			if (Util.getOS().equals("Mac")) {
				if (isDetachedWin) {
					/*Avoid deadlock situation by disposing the JFrame!*/
					bio7TabClose(false);
					java.awt.EventQueue.invokeLater(new Runnable() {
						public void run() {
							dispose();
						}
					});

				}
			} else {
				java.awt.EventQueue.invokeLater(new Runnable() {
					public void run() {
						dispose();
					}
				});
			}
		}

		return true;

	}

	/* Changed for Bio7! */
	public boolean bio7TabClose(boolean disposeFrame) {

		boolean isRunning = running || running2;
		running = running2 = false;
		boolean virtual = imp.getStackSize() > 1 && imp.getStack().isVirtual();
		if (isRunning)
			IJ.wait(500);
		if (ij == null || ij.quittingViaMacro() || IJ.getApplet() != null || Interpreter.isBatchMode()
				|| IJ.macroRunning() || virtual)
			imp.changes = false;
		/* Changed for Bio7! */
		/*
		 * if (imp.changes) { String msg; String name = imp.getTitle(); if
		 * (name.length()>22) msg = "Save changes to\n" + "\"" + name + "\"?"; else msg
		 * = "Save changes to \"" + name + "\"?"; YesNoCancelDialog d = new
		 * YesNoCancelDialog(this, "ImageJ", msg); if (d.cancelPressed()) return false;
		 * else if (d.yesPressed()) { FileSaver fs = new FileSaver(imp); if (!fs.save())
		 * return false; } }
		 */
		closed = true;
		if (WindowManager.getWindowCount() == 0) {
			xloc = 0;
			yloc = 0;
		}
		Point currentLoc = getLocation();
		if (!IJ.isMacro()) {
			Prefs.saveLocation(LOC_KEY, currentLoc);
			xbase = -1;
		}
		WindowManager.removeWindow(this);
		setVisible(false);
		if (ij != null && ij.quitting()) // this may help avoid thread deadlocks
			return true;
		/*Changed for Bio7! - Avoid deadlock situation by disposing the JFrame!*/
		if (disposeFrame) {
			dispose();
		}
		if (imp != null)
			imp.flush();
		imp = null;
		return true;
	}

	public ImagePlus getImagePlus() {
		return imp;
	}

	public void setImage(ImagePlus imp2) {
		ImageCanvas ic = getCanvas();
		if (ic == null || imp2 == null)
			return;
		imp = imp2;
		imp.setWindow(this);
		ic.updateImage(imp);
		ic.setImageUpdated();
		ic.repaint();
		repaint();
	}

	/*Changed for Bio7. Overwrite repaint to send a message to the swt statusline!*/
	public void repaint() {
		Display display = Util.getDisplay();
		/*
		 * MacOSX only seems to work with async method! Also on Windows async works best
		 * (avoids deadlocks when mouse moved in and out the canvas opening several
		 * images at once in the background!).
		 */

		display.asyncExec(new Runnable() {
			public void run() {
				if (imp != null) {

					if (imp.isComposite()) {
						CompositeImage ci = (CompositeImage) imp;
						if (ci.getMode() == IJ.COMPOSITE) {
							Color c = ci.getChannelColor();
							if (Color.green.equals(c))
								c = new Color(0, 180, 0);
							// g.setColor(c);
							int red = c.getRed();
							int green = c.getGreen();
							int blue = c.getBlue();
							org.eclipse.swt.graphics.Color color = new org.eclipse.swt.graphics.Color(display,
									new RGB(red, green, blue));
							// CanvasView.tabFolder.setSelectionForeground(color);
							/*On MacOSX we get NullPointerException when we call the status line first. 
							 * So we place the call to the status line here!*/
							CanvasView canvasView = CanvasView.getCanvas_view();
							if (canvasView != null) {
								canvasView.setStatusLineColor(createSubtitle(), color);
							}

						}

					} else {
						CanvasView canvasView = CanvasView.getCanvas_view();
						if (canvasView != null) {
							canvasView.setstatusline(createSubtitle());
						}
					}

				}
			}
		});
		super.repaint();
	}

	public void show() {
		/*Changed for Bio7!
		 * Empty function.
		 * The overwrite implementation avoids the opening
		 * of a JFrame ImageWindow and a reference to the image from the
		 * Functions class method: displayBatchModeImage in a macro when calling
		 * setBatchMode(false)
		 * */
		// System.out.println("Show");
	}

	public void updateImage(ImagePlus imp) {
		if (imp != this.imp)
			throw new IllegalArgumentException("imp!=this.imp");
		this.imp = imp;
		ic.updateImage(imp);
		setLocationAndSize(true);
		if (this instanceof StackWindow) {
			StackWindow sw = (StackWindow) this;
			int stackSize = imp.getStackSize();
			int nScrollbars = sw.getNScrollbars();
			if (stackSize == 1 && nScrollbars > 0)
				sw.removeScrollbars();
			else if (stackSize > 1 && nScrollbars == 0)
				sw.addScrollbars(imp);
		}
		pack();
		repaint();
		maxBounds = getMaximumBounds();
		setMaximizedBounds(maxBounds);
		setMaxBoundsTime = System.currentTimeMillis();
	}

	public ImageCanvas getCanvas() {
		return ic;
	}

	static ImagePlus getClipboard() {
		return ImagePlus.getClipboard();
	}

	public Rectangle getBounds() {
		Rectangle rec;
		JPanel current = CanvasView.getCanvas_view().getCurrent();
		JPanel pan = current;
		if (pan != null) {
			rec = current.getBounds();
		} else {
			rec = new Rectangle(400, 400);
		}

		return rec;

	}

	public Rectangle getMaximumBounds() { 
		Rectangle rec;
		JPanel pan = CanvasView.getCanvas_view().getCurrent();
		if (pan != null) {
			rec = CanvasView.getCanvas_view().getCurrent().getBounds();
		} else {
			rec = new Rectangle(400, 400);
		}

		/*
		 * Rectangle maxWindow = GUI.getMaxWindowBounds(); if (imp == null) return
		 * maxWindow; double width = imp.getWidth(); double height = imp.getHeight();
		 * double iAspectRatio = width / height;
		 * 
		 * maxWindowBounds = maxWindow; if (iAspectRatio / ((double) maxWindow.width /
		 * maxWindow.height) > 0.75) { maxWindow.y += 22; // uncover ImageJ menu bar
		 * maxWindow.height -= 22; } Dimension extraSize = getExtraSize(); double
		 * maxWidth = maxWindow.width - extraSize.width; double maxHeight =
		 * maxWindow.height - extraSize.height; double mAspectRatio = maxWidth /
		 * maxHeight; int wWidth, wHeight; double mag; if (iAspectRatio >= mAspectRatio)
		 * { mag = maxWidth / width; wWidth = maxWindow.width; wHeight = (int) (height *
		 * mag + extraSize.height); } else { mag = maxHeight / height; wHeight =
		 * maxWindow.height; wWidth = (int) (width * mag + extraSize.width); } int xloc
		 * = (int) (maxWidth - wWidth) / 2; if (xloc < 0) xloc = 0;
		 */
		return rec;
	}

	Dimension getExtraSize() {
		Insets insets = getInsets();
		int extraWidth = insets.left + insets.right + 10;
		int extraHeight = insets.top + insets.bottom + 10;
		if (extraHeight == 20)
			extraHeight = 42;
		int members = getComponentCount();
		for (int i = 1; i < members; i++) {
			Component m = getComponent(i);
			Dimension d = m.getPreferredSize();
			extraHeight += d.height + 5;
			if (IJ.debugMode)
				IJ.log(i + "  " + d.height + " " + extraHeight);
		}
		return new Dimension(extraWidth, extraHeight);
	}

	public Component add(Component comp) {
		comp = super.add(comp);
		maxBounds = getMaximumBounds();
		setMaximizedBounds(maxBounds);
		setMaxBoundsTime = System.currentTimeMillis();
		return comp;
	}

	public void maximize() {
		if (GenericDialog.getInstance() != null && IJ.isMacOSX() && IJ.isJava18())
			return; // workaround for OSX/Java 8 maximize bug
		Rectangle rect = getMaximumBounds();
		if (IJ.debugMode)
			IJ.log("maximize: " + rect);
		setLocationAndSize(rect.x, rect.y, rect.width, rect.height);
	}

	public void minimize() {
		if (IJ.debugMode)
			IJ.log("minimize: " + unzoomWhenMinimizing);
		if (unzoomWhenMinimizing)
			ic.unzoom();
		unzoomWhenMinimizing = true;
	}

	/** Has this window been closed? */
	public boolean isClosed() {
		return closed;
	}

	public void focusGained(FocusEvent e) {
		if (!Interpreter.isBatchMode() && ij != null && !ij.quitting() && imp != null) {
			//if (IJ.debugMode)
				//IJ.log("focusGained: " + imp);
			WindowManager.setCurrentWindow(this);
		}
	}

	public void windowActivated(WindowEvent e) {
		if (IJ.debugMode)
			IJ.log("windowActivated: " + imp.getTitle());
		if (IJ.isMacOSX() && Prefs.setIJMenuBar && !IJ.isMacro())
			setImageJMenuBar(this);
		if (imp == null)
			return;
		ImageJ ij = IJ.getInstance();
		if (ij != null && !closed && !ij.quitting() && !Interpreter.isBatchMode())
			WindowManager.setCurrentWindow(this);
		Roi roi = imp.getRoi();
		if (roi != null && (roi instanceof PointRoi))
			PointToolOptions.update();
		if (imp.isComposite())
			Channels.updateChannels();
		imp.setActivated(); // notify ImagePlus that image has been activated
	}

	public void windowClosing(WindowEvent e) {
		if (closed)
			return;
		if (ij != null) {
			WindowManager.setCurrentWindow(this);
			IJ.doCommand("Close");
		} else {
			dispose();
			WindowManager.removeWindow(this);
		}
	}

	public void windowStateChanged(WindowEvent e) {
		int oldState = e.getOldState();
		int newState = e.getNewState();
		if (IJ.debugMode)
			IJ.log("windowStateChanged: " + oldState + " " + newState);
		if ((oldState & Frame.MAXIMIZED_BOTH) == 0 && (newState & Frame.MAXIMIZED_BOTH) != 0)
			maximize();

	}

	public void windowClosed(WindowEvent e) {
	}

	public void windowDeactivated(WindowEvent e) {
	}

	public void focusLost(FocusEvent e) {
	}

	public void windowDeiconified(WindowEvent e) {
	}

	public void windowIconified(WindowEvent e) {
	}

	public void windowOpened(WindowEvent e) {
	}

	public synchronized void mouseWheelMoved(MouseWheelEvent e) {
		int rotation = e.getWheelRotation();
		int amount = e.getScrollAmount();
		boolean ctrl = (e.getModifiers() & Event.CTRL_MASK) != 0;
		//if (IJ.debugMode) {
				//	IJ.log("mouseWheelMoved: "+e);
				//	IJ.log("  type: "+e.getScrollType());
				//	IJ.log("  ctrl: "+ctrl);
				//	IJ.log("  rotation: "+rotation);
				//	IJ.log("  amount: "+amount);
				//}
		if (amount < 1)
			amount = 1;
		if (rotation == 0)
			return;
		int width = imp.getWidth();
		int height = imp.getHeight();
		Rectangle srcRect = ic.getSrcRect();
		int xstart = srcRect.x;
		int ystart = srcRect.y;
		if ((ctrl || IJ.shiftKeyDown()) && ic != null) {
			int ox = ic.offScreenX(e.getX());
			int oy = ic.offScreenY(e.getX());
			if (IJ.debugMode)
				IJ.log("  x,y: " + ox + "," + oy);
			if (rotation < 0)
				ic.zoomIn(ox, oy);
			else
				ic.zoomOut(ox, oy);
			return;
		} else if (IJ.spaceBarDown() || srcRect.height == height) {
			srcRect.x += rotation * amount * Math.max(width / 200, 1);
			if (srcRect.x < 0)
				srcRect.x = 0;
			if (srcRect.x + srcRect.width > width)
				srcRect.x = width - srcRect.width;
		} else {
			srcRect.y += rotation * amount * Math.max(height / 200, 1);
			if (srcRect.y < 0)
				srcRect.y = 0;
			if (srcRect.y + srcRect.height > height)
				srcRect.y = height - srcRect.height;
		}
		if (srcRect.x != xstart || srcRect.y != ystart)
			ic.repaint();
	}

	/**
	 * Copies the current ROI to the clipboard. The entire image is copied if there
	 * is no ROI.
	 */
	public void copy(boolean cut) {
		imp.copy(cut);
	}

	public void paste() {
		imp.paste();
	}

	/**
	 * This method is called by ImageCanvas.mouseMoved(MouseEvent).
	 * 
	 * @see ij.gui.ImageCanvas#mouseMoved
	 */
	public void mouseMoved(int x, int y) {
		imp.mouseMoved(x, y);
	}

	public String toString() {
		return imp != null ? imp.getTitle() : "";
	}

	/**
	 * Causes the next image to be opened to be centered on the screen and displayed
	 * without informational text above the image.
	 */
	public static void centerNextImage() {
		centerOnScreen = true;
	}

	/** Causes the next image to be displayed at the specified location. */
	public static void setNextLocation(Point loc) {
		nextLocation = loc;
	}

	/** Causes the next image to be displayed at the specified location. */
	public static void setNextLocation(int x, int y) {
		nextLocation = new Point(x, y);
	}

	/**
	 * Moves and resizes this window. Changes the magnification so the image fills
	 * the window.
	 */
	public void setLocationAndSize(int x, int y, int width, int height) {
		/*Changed for Bio7 for detached views only!*/
		String idValue = Integer.toString(getImagePlus().getID());
		IJTabs.setSecondaryViewShellLocAndSize(idValue, new org.eclipse.swt.graphics.Rectangle(x, y, width, height));
	}

	@Override
	public void setLocation(int x, int y) {
		super.setLocation(x, y);
	}

	public void setSliderHeight(int height) {
		sliderHeight = height;
	}

	public int getSliderHeight() {
		return sliderHeight;
	}

	public static void setImageJMenuBar(ImageWindow win) {
		ImageJ ij = IJ.getInstance();
		boolean setMenuBar = true;
		ImagePlus imp = win.getImagePlus();
		if (imp != null)
			setMenuBar = imp.setIJMenuBar();
		MenuBar mb = Menus.getMenuBar();
		if (mb != null && mb == win.getMenuBar())
			setMenuBar = false;
		setMenuBarTime = 0L;
		if (setMenuBar && ij!=null && !ij.quitting()) {
			IJ.wait(10); // may be needed for Java 1.4 on OS X
			long t0 = System.currentTimeMillis();
			win.setMenuBar(mb);
			long time = System.currentTimeMillis() - t0;
			setMenuBarTime = time;
			Menus.setMenuBarCount++;
			if (time > 2000L)
				Prefs.setIJMenuBar = false;

		}
		if (imp != null)
			imp.setIJMenuBar(true);
	}

} // class ImageWindow
