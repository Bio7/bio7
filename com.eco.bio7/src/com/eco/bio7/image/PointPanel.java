/*******************************************************************************
 * Copyright (c) 2007-2012 M. Austenfeld
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/

package com.eco.bio7.image;

import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.ImageCanvas;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Ellipse2D.Double;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.Vector;
import javax.swing.JPanel;
import javax.swing.JViewport;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import com.eco.bio7.compile.Model;
import com.eco.bio7.discrete.Quad2d;
import com.eco.bio7.methods.Compiled;
import com.eco.bio7.methods.CurrentStates;
import com.eco.bio7.time.Time;
import org.locationtech.jts.awt.ShapeWriter;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;

/**
 * This class provides methods for the Points panel of the Bio7 application and
 * the points attributes inside the Points panel.
 * 
 * @author Bio7
 * 
 */
public class PointPanel extends JPanel implements KeyListener, MouseListener, MouseMotionListener, MouseWheelListener {

	private static int x = 20;

	private static int y = 20;

	private static int diameter = 5;

	// private static Graphics2D g;

	private static Vector<Double> ve = new Vector<Double>();

	private static Vector<Point2D.Double> Points = new Vector<Point2D.Double>();

	private static Vector<Integer> species = new Vector<Integer>();

	private static Vector<Float> alpha = new Vector<Float>();

	private static Individual[] individual = null;

	private int count = 0;

	private static boolean quad2dVisible = false;

	private static float compos = 255;

	private static float compos2 = 255;

	private static float compos3 = 255;

	private static BufferedImage buff = null;

	private int px = 1000;

	private int py = 1000;

	private int scalefactorx = 1000;

	private int scalefactory = 1000;

	private int scaledx = scalefactorx;

	private int scaledy = scalefactory;

	private boolean drag;

	private double transformx = 1;

	private double transformy = 1;

	private boolean scroll;

	public boolean dynamicVoronoi = true;

	public boolean dynamicDelauney = false;

	private double transformVorox;

	private double transformVoroy;

	private boolean retina;

	public static boolean showAreas = false;

	private static PointPanel pointPanel;

	private static java.awt.geom.Point2D.Double currentPoint = null;

	private static Ellipse2D.Double currentellipse;

	/* The state selected from the Buttons! */
	private static int stateIndexJp = 0;

	private static double sx = 1.0;

	private static double sy = 1.0;

	public static boolean showVoronoi = false;

	public static boolean showDelauney = false;

	private static Geometry geomDelauney;

	private static Geometry geomVoronoi;

	private static Collection<Coordinate> coords;

	private static Collection<Coordinate> coordsDelauney;

	private static double drawAreaScaled = 1.0;

	public static void setDrawAreaScaled(double drawAreaScaled) {
		PointPanel.drawAreaScaled = drawAreaScaled;
	}

	PointPanel() {
		retina = Util.isMacRetinaDisplay();
		this.addMouseWheelListener(this);

		this.addKeyListener(this);
		this.addMouseListener(this);
		this.addMouseMotionListener(this);
		pointPanel = this;

	}

	public static PointPanel getPointPanel() {
		return pointPanel;
	}

	public void paintComponent(Graphics g) {

		super.paintComponent(g);

		Graphics2D g2 = (Graphics2D) g;
		g2.scale(sx, sy);

		AffineTransform aff = g2.getTransform();
		if (retina) {
			transformx = aff.getScaleX() / 2;
			transformy = aff.getScaleY() / 2;
		} else {
			transformx = aff.getScaleX();
			transformy = aff.getScaleY();
		}

		if (quad2dVisible) {
			g2.setComposite(makeComposite(compos2 / 255.0f));
			Quad2d.getQuad2dInstance().malen(g2);
		}
		g2.setComposite(makeComposite(compos / 255.0f));
		if (buff != null) {
			g2.drawImage(buff, 0, 0, px, py, this);

		}
		g2.setComposite(makeComposite(1.0f));
		g2.setColor(new Color(0, 128, 0));
		g2.drawLine(0, ImageMethods.getFieldY(), ImageMethods.getFieldX(), ImageMethods.getFieldY());
		g2.drawLine(ImageMethods.getFieldX(), 0, ImageMethods.getFieldX(), ImageMethods.getFieldY());

		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		if (individual != null && Time.isPause() == false) {

			drawEllipse(g2);

		} else {

			drawEllipse(g2);
		}
		g2.setComposite(makeComposite(1.0f));

		if (showVoronoi) {
			g2.setColor(new Color(0, 0, 0));

			if (geomVoronoi != null) {
				drawVoronoiDelauney(g2, toShape(geomVoronoi));

			}
		}
		if (showDelauney) {
			g2.setColor(new Color(0, 0, 0));
			if (geomDelauney != null) {
				drawVoronoiDelauney(g2, toShape(geomDelauney));
			}
		}
		if (dynamicVoronoi && showVoronoi) {
			createVoronoi();
		}
		if (dynamicDelauney && showDelauney) {
			createDelauney();
		}
		if (showAreas) {
			showAreas(g2);
		}

		/* Draw from compiled context on the panel! */

		Model eco = Compiled.getModel();
		if (eco != null) {
			try {
				if (g2 != null) {
					eco.draw(g2);
				}
			} catch (RuntimeException e) {

				e.printStackTrace();
			}
		}

	}

	public void showAreas(Graphics2D g2d) {
		g2d.scale(drawAreaScaled, drawAreaScaled);
		AffineTransform aff = g2d.getTransform();
		if (retina) {
			transformVorox = aff.getScaleX() / 2;
			transformVoroy = aff.getScaleY() / 2;
		} else {
			transformVorox = aff.getScaleX();
			transformVoroy = aff.getScaleY();
		}
		if (PointPanel.getPoints().length > 1) {
			Geometry g = getGeomVoronoi();
			if (g != null) {
				for (int i = 0; i < g.getNumGeometries(); i++) {
					Geometry geom = g.getGeometryN(i);
					org.locationtech.jts.geom.Point p = geom.getCentroid();

					g2d.drawString("" + geom.getArea(), (int) ((p.getX() / transformVorox) * transformx), (int) ((p.getY() / transformVoroy) * transformy));

				}
			}
		}
	}

	public static void cleanVoronoi() {
		coords = null;
		geomVoronoi = null;
	}

	public static void cleanDelauney() {
		coordsDelauney = null;
		geomDelauney = null;
	}

	/**
	 * Creates Voronoi Geometry objects from the points in the Points panel.
	 */
	public static void createVoronoi() {
		if (PointPanel.getPoints().length > 1) {
			coords = DelaunayAndVoronoiApp.getPredefinedSites();
			geomVoronoi = DelaunayAndVoronoiApp.buildVoronoiDiagram(coords);
		}

	}

	/**
	 * Creates Delauney Geometry objects from the points in the Points panel.
	 */
	public static void createDelauney() {
		if (PointPanel.getPoints().length > 1) {
			coordsDelauney = DelaunayAndVoronoiApp.getPredefinedSites();
			geomDelauney = DelaunayAndVoronoiApp.buildDelaunayTriangulation(coordsDelauney);
		}

	}

	public static Shape toShape(Geometry geom) {

		ShapeWriter writer = new ShapeWriter();
		return writer.toShape(geom);
	}

	public static void drawVoronoiDelauney(Graphics2D g2d, final Shape... shape) {

		if (shape != null) {
			for (Shape s : shape) {
				// g2d.setColor(new Color(Color.HSBtoRGB(new
				// Random().nextFloat(), 1f, 0.6f)));
				g2d.draw(s);

			}
		}

	}

	/**
	 * Returns the JTS Geometry which contains the Voronoi Geometry objects
	 * 
	 * @return an Geometry object.
	 */
	public static Geometry getGeomDelauney() {
		return geomDelauney;
	}

	/**
	 * Returns the JTS Geometry which contains the Delauney Geometry objects
	 * 
	 * @return an Geometry object.
	 */
	public static Geometry getGeomVoronoi() {
		return geomVoronoi;
	}

	private void setfield() {
		for (int y = 0; y < individual.length; y++) {
			if (individual[y] != null) {
				diameter = individual[y].getDiameter();
				ve.add(new Ellipse2D.Double(individual[y].getX() - (int) (diameter / 2), individual[y].getY() - (int) (diameter / 2), diameter, diameter));
				species.add(individual[y].getSpecies());
				alpha.add(individual[y].getAlpha());
				Points.add(new Point2D.Double(individual[y].getX(), individual[y].getY()));

			}

		}
	}

	public static Ellipse2D.Double getCurrentEllipse() {
		return currentellipse;
	}

	public static int getSelectedX() {
		return x;
	}

	public static int getSelectedY() {
		return y;
	}

	private void drawEllipse(Graphics2D g2) {

		for (int i = 0; i < ve.size(); i++) {

			Ellipse2D.Double ellipse = (Ellipse2D.Double) ve.get(i);

			Point2D.Double p = (Point2D.Double) Points.get(i);

			Float al = (Float) alpha.get(i);
			g2.setComposite(makeComposite(al / 255.0f));

			Integer s = (Integer) species.get(i);
			int sp = s.intValue();
			String specie = CurrentStates.getStateName(sp);
			int rgb[] = CurrentStates.getRGB(sp);
			g2.setColor(new Color(rgb[0], rgb[1], rgb[2]));
			g2.fillOval((int) (ellipse.getX()), (int) (ellipse.getY()), (int) ellipse.getHeight(), (int) ellipse.getWidth());

			if (drag == true) {
				if (currentellipse.equals(ellipse)) {
					g2.setColor(new Color(0, 0, 0));

					final double px = p.getX();
					final double py = p.getY();

					final String specie2 = specie;
					Display display = PlatformUI.getWorkbench().getDisplay();
					display.syncExec(new Runnable() {

						public void run() {
							if (PointPanelView.getImj() != null) {
								PointPanelView.getImj().setstatusline("x: " + (px) + " y: " + (py + " -> " + specie2));
							}
						}
					});
				}
			}

		}

	}

	private AlphaComposite makeComposite(float alpha) {

		int type = AlphaComposite.SRC_OVER;
		return (AlphaComposite.getInstance(type, alpha));
	}

	/**
	 * Returns the points, alpha values, species and ellipses as a 2-dimensional
	 * array for the transfer to R by means of Rserve.
	 * 
	 * @return an integer array with the values.
	 */
	public static double[][] pointToArray() {// Conversion for R!
		double xydia[][] = new double[5][ve.size()];
		for (int i = 0; i < ve.size(); i++) {
			Integer spec = (Integer) species.get(i);
			int spe = spec.intValue();
			Float fl = alpha.get(i);
			float flo = fl.floatValue();
			Ellipse2D.Double elli = (Ellipse2D.Double) ve.get(i);
			Point2D.Double poi = (Point2D.Double) Points.get(i);
			xydia[0][i] = poi.x;
			xydia[1][i] = poi.y;
			xydia[2][i] = (int) elli.getHeight();
			xydia[3][i] = spe; // The species
			xydia[4][i] = (int) flo;
		}

		return xydia;

	}

	private void groesser(MouseWheelEvent e) {

		scalefactorx = scalefactorx + (e.getWheelRotation() * 50);
		scalefactory = scalefactory + (e.getWheelRotation() * 50);
		scaledx = scalefactorx;

		scaledy = scalefactory;

		repaint();
	}

	protected int getPx() {
		return px;
	}

	protected void setPx(int px) {
		this.px = px;
	}

	protected int getPy() {
		return py;
	}

	protected void setPy(int py) {
		this.py = py;
	}

	protected int getScaledx() {
		return scaledx;
	}

	protected void setScaledx(int scaledx) {
		this.scaledx = scaledx;
	}

	protected int getScaledy() {
		return scaledy;
	}

	protected void setScaledy(int scaledy) {
		this.scaledy = scaledy;
	}

	public void keyTyped(KeyEvent arg0) {

	}

	public void keyPressed(KeyEvent e) {

	}

	public void keyReleased(KeyEvent arg0) {

	}

	public void mouseClicked(MouseEvent e) {
		if (e.getClickCount() == 2) {
			x = (int) (e.getX() / transformx);// New Coordinates!
			y = (int) (e.getY() / transformy);
			if (x <= ImageMethods.getFieldX() && y <= ImageMethods.getFieldY()) {

				alpha.add(compos3);
				ve.add(new Ellipse2D.Double(x - (int) (diameter / 2), y - (int) (diameter / 2), diameter, diameter));
				Points.add(new Point2D.Double(x, y));

				species.add(stateIndexJp);

				count++;
				// If pressed the transformation is considered!
				if (quad2dVisible) {
					Quad2d.getQuad2dInstance().mousepressed(e, transformx, transformy);
					repaint();
				}

			} else {

				Display display = PlatformUI.getWorkbench().getDisplay();
				display.syncExec(new Runnable() {

					public void run() {
						MessageBox messageBox = new MessageBox(new Shell(),

								SWT.ICON_INFORMATION);
						messageBox.setMessage("Point outside Pointpanel area !");
						messageBox.open();
					}
				});

			}
			if (showVoronoi && dynamicVoronoi) {
				createVoronoi();
			}
			if (showDelauney && dynamicDelauney) {
				createDelauney();
			}

		}

		else if (e.getClickCount() == 1) {
			Quad2d.getQuad2dInstance().mouseClicked(e);

		}

		if (e.getButton() == 3) {
			if (e.getClickCount() == 2) {
				scroll = !scroll;
			}
			x = (int) (e.getX() / transformx);
			y = (int) (e.getY() / transformy);

			count--;
			for (int i = 0; i < ve.size(); i++) {
				if (ve.size() > 0) {
					Ellipse2D.Double ellipse = (Ellipse2D.Double) ve.get(i);
					if (ellipse.contains(x, y)) {

						ve.remove(i);
						Points.remove(i);
						species.remove(i);
						alpha.remove(i);
						repaint();

					}
				}

			}

		}

	}

	public void mousePressed(MouseEvent e) {
		// requestFocus();
		x = (int) (e.getX() / transformx);
		y = (int) (e.getY() / transformy);
		currentellipse = null;
		currentPoint = null;
		for (int i = 0; i < ve.size(); i++) {

			if (ve.size() > 0) {

				Ellipse2D.Double ellipse = (Ellipse2D.Double) ve.get(i);

				if (ellipse.contains(x, y)) {

					currentellipse = (Ellipse2D.Double) ve.get(i);
					currentPoint = (Point2D.Double) Points.get(i);

				}

			}

		}

	}

	public void mouseReleased(MouseEvent e) {

		drag = false;
		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {

			public void run() {
				if (PointPanelView.getImj() != null) {
					PointPanelView.getImj().setstatusline("");
				}
			}
		});

		this.repaint();
	}

	public void mouseEntered(MouseEvent arg0) {

	}

	public void mouseExited(MouseEvent arg0) {

	}

	public void mouseDragged(MouseEvent e) {
		drag = true;
		if (currentellipse != null) {
			x = (int) (e.getX() / transformx);
			y = (int) (e.getY() / transformy);

			if (x < 0 && y < 0) {
				currentPoint.x = 0;
				currentPoint.y = 0;
				currentellipse.x = 0 - (currentellipse.getHeight() / 2);
				currentellipse.y = 0 - (currentellipse.getWidth() / 2);

			} else if (x < 0 && y < ImageMethods.getFieldY()) {
				currentPoint.x = 0;
				currentPoint.y = y;
				currentellipse.x = 0 - (currentellipse.getHeight() / 2);
				currentellipse.y = y - (currentellipse.getWidth() / 2);

			}

			else if (x < ImageMethods.getFieldX() && y < 0) {
				currentPoint.x = x;
				currentPoint.y = 0;
				currentellipse.x = x - (currentellipse.getHeight() / 2);
				currentellipse.y = 0 - (currentellipse.getWidth() / 2);

			}

			else if (x >= ImageMethods.getFieldX() && y <= 0) {
				currentPoint.x = ImageMethods.getFieldX();
				currentPoint.y = 0;
				currentellipse.x = ImageMethods.getFieldX() - (currentellipse.getHeight() / 2);
				currentellipse.y = 0 - (currentellipse.getWidth() / 2);

			}

			else if (y >= ImageMethods.getFieldY() && x <= 0) {
				currentPoint.x = 0;
				currentPoint.y = ImageMethods.getFieldY();
				currentellipse.x = 0 - (currentellipse.getHeight() / 2);
				currentellipse.y = ImageMethods.getFieldY() - (currentellipse.getWidth() / 2);

			}

			else if (x <= ImageMethods.getFieldX() && y <= ImageMethods.getFieldY()) {

				currentPoint.x = x;
				currentPoint.y = y;
				currentellipse.x = x - (currentellipse.getHeight() / 2);
				currentellipse.y = y - (currentellipse.getWidth() / 2);

			} else if (x <= ImageMethods.getFieldX() && y >= ImageMethods.getFieldY()) {
				currentPoint.x = x;
				currentPoint.y = ImageMethods.getFieldY();
				currentellipse.x = x - (currentellipse.getHeight() / 2);
				currentellipse.y = ImageMethods.getFieldY() - (currentellipse.getWidth() / 2);

			} else if (x >= ImageMethods.getFieldX() && y <= ImageMethods.getFieldY()) {
				currentPoint.x = ImageMethods.getFieldX();
				currentPoint.y = y;
				currentellipse.x = ImageMethods.getFieldX() - (currentellipse.getHeight() / 2);
				currentellipse.y = y - (currentellipse.getWidth() / 2);

			} else if (x >= ImageMethods.getFieldX() && y >= ImageMethods.getFieldY()) {
				currentPoint.x = ImageMethods.getFieldX();
				currentPoint.y = ImageMethods.getFieldY();
				currentellipse.x = ImageMethods.getFieldX() - (currentellipse.getHeight() / 2);
				currentellipse.y = ImageMethods.getFieldY() - (currentellipse.getHeight() / 2);

			}
			repaint();

		}

	}

	public void mouseMoved(MouseEvent e) {
		if (scroll) {
			if (WindowManager.getCurrentWindow() != null) {
				ImageCanvas c = WindowManager.getCurrentWindow().getCanvas();
				Rectangle maxWindow = CanvasView.getCanvas_view().getCurrent().getBounds();
				ImagePlus im = WindowManager.getCurrentWindow().getImagePlus();
				int iw = im.getWidth();
				int ih = im.getHeight();
				double wx = maxWindow.getWidth();
				double wy = maxWindow.getHeight();
				double xc = e.getX();
				double yc = e.getY();

				double x = (xc / transformx);
				double y = (yc / transformy);

				if (x < iw && y < ih) {
					Rectangle rec = c.getSrcRect();
					int sx = (int) (x - ((wx / 2) / c.getMagnification()));
					int sy = (int) (y - ((wy / 2) / c.getMagnification()));

					if (sx > 0) {
						rec.x = sx;
					}
					if (sy > 0) {
						rec.y = sy;
					}

					c.repaint();
				}
			}
		}
	}

	public void mouseWheelMoved(MouseWheelEvent e) {

		groesser(e);
		Quad2d.getQuad2dInstance().mouseWheelMoved(e);

		repaint();
	}

	protected BufferedImage getBuff() {
		return buff;
	}

	protected void setBuff(BufferedImage buf) {
		buff = buf;
		py = buff.getHeight();
		px = buff.getWidth();
		this.repaint();
	}

	protected static void setBuffNull() {
		buff = null;

	}

	/**
	 * Returns the exact points (in double precision) of the Points panel.
	 * 
	 * @return an array of the type Point2D.Double.
	 */
	public static Point2D.Double[] getPoints() {
		Point2D.Double[] points = new Point2D.Double[Points.size()];
		for (int i = 0; i < Points.size(); i++) {
			points[i] = (Point2D.Double) Points.get(i);

		}

		return points;
	}

	/**
	 * Returns the ellipses in the Points panel representing the points.
	 * 
	 * @return an array of the type Ellipse2D.Double.
	 */
	public static Ellipse2D.Double[] getEllipse() {
		Ellipse2D.Double[] ellipse = new Ellipse2D.Double[ve.size()];
		for (int i = 0; i < ve.size(); i++) {
			ellipse[i] = (Ellipse2D.Double) ve.get(i);

		}

		return ellipse;
	}

	/**
	 * Returns the diameter of the points in the Points panel.
	 * 
	 * @return the diameter as an integer array.
	 */
	public static int[] getDiameter() {
		int[] diam = new int[ve.size()];

		for (int i = 0; i < diam.length; i++) {
			Ellipse2D.Double ellipse = (Ellipse2D.Double) ve.get(i);
			diam[i] = (int) ellipse.height;

		}

		return diam;
	}

	/**
	 * Returns the state numbers of the species in the Points panel.
	 * 
	 * @return the state numbers as an integer array.
	 */
	public static int[] getSpecies() {
		int[] specie = new int[species.size()];
		for (int i = 0; i < species.size(); i++) {
			specie[i] = (Integer) species.get(i);
		}

		return specie;
	}

	/**
	 * Returns the alpha value of the points in the Points panel
	 * 
	 * @return the alpha values as an float array.
	 */
	public static float[] getAlpha() {
		float[] alp = new float[alpha.size()];
		for (int i = 0; i < alpha.size(); i++) {
			alp[i] = (Float) alpha.get(i);
		}

		return alp;
	}

	/**
	 * Returns the name of the species of the given index.
	 * 
	 * @param index the index as an integer.
	 * @return the name as a string.
	 */
	public static String getName(int index) {
		String be = (String) CurrentStates.getStateList().get(index);
		return be;
	}

	/**
	 * Deletes all point values, alpha values, ellipse values and species values in
	 * the Points panel.
	 */
	public static void delete() {
		ve.clear();
		alpha.clear();
		Points.clear();
		species.clear();

	}

	/**
	 * Sets the individuals in the Points panel and repaints the Points panel.
	 * 
	 * @param individuals an array of the type individual.
	 */
	public static void setIndividual(Individual[] individuals) {
		if (individuals != null) {
			individual = individuals;
			PointPanel Jp = PointPanelView.getJp();
			Jp.setfield();
			Jp.repaint();
		}
	}

	/**
	 * A torus function for the Points panel.
	 * 
	 * @param x  the x-coordinate.
	 * @param y  the y-coordinate.
	 * @param dx the distance from the x-coordinate.
	 * @param dy the distance from the y-coordinate.
	 * @return the coordinates as an integer array for the new location calculated
	 *         from the torus function.
	 */
	public static int[] torus(int x, int y, int dx, int dy) {

		int coords[] = new int[2];
		coords[0] = ((y + dy + ImageMethods.getFieldY()) % (ImageMethods.getFieldY()));// modulo,

		coords[1] = ((x + dx + ImageMethods.getFieldX()) % (ImageMethods.getFieldX()));

		return coords;
	}

	/**
	 * Repaints the Points panel.
	 */
	public static void doPaint() {
		PointPanel Jp = PointPanelView.getJp();
		if (Jp != null) {
			Jp.repaint();
		}
	}

	/**
	 * Sets the view of the scrollpane of the Points panel to the given coordinates.
	 * 
	 * @param x the x-coordinate.
	 * @param y the y-coordinate.
	 */
	public static void setViewCoordinates(int x, int y) {
		JViewport viewport = PointPanelView.getScroll().getViewport();
		PointPanel panel = PointPanelView.getJp();
		int height = viewport.getHeight();
		int width = viewport.getWidth();
		double maxX = viewport.getVisibleRect().getMaxX();
		double maxY = viewport.getVisibleRect().getMaxY();
		double viewportmaxX = maxX / panel.transformx;
		double viewportmaxY = maxY / panel.transformy;
		if (panel != null) {

			if ((x * panel.transformx) - width / 2 >= 0 && (y * panel.transformy) - height / 2 >= 0) {
				viewport.setViewPosition(new Point((int) ((x * panel.transformx) - width / 2), (int) ((y * panel.transformy) - height / 2)));

			}

			else if ((x * panel.transformx) - width / 2 >= 0 && (y * panel.transformy) - height / 2 < 0) {
				viewport.setViewPosition(new Point((int) ((x * panel.transformx) - width / 2), viewport.getViewPosition().y));
			} else if ((x * panel.transformx) - width / 2 < 0 && (y * panel.transformy) - height / 2 >= 0) {
				viewport.setViewPosition(new Point(viewport.getViewPosition().x, (int) ((y * panel.transformy) - height / 2)));
			}

		}
	}

	public static int get_Diameter() {
		return diameter;
	}

	protected static void set_Diameter(int diameter) {
		PointPanel.diameter = diameter;
	}

	public static Vector<Float> get_Alpha() {
		return alpha;
	}

	public static Vector<Point2D.Double> get_Points() {
		return Points;
	}

	public static Vector<Integer> get_Species() {
		return species;
	}

	public static Vector<Double> getVe() {
		return ve;
	}

	protected static float getCompos() {
		return compos;
	}

	protected static void setCompos(float compos) {
		PointPanel.compos = compos;
	}

	protected static float getCompos2() {
		return compos2;
	}

	protected static void setCompos2(float compos2) {
		PointPanel.compos2 = compos2;
	}

	public static float getCompos3() {
		return compos3;
	}

	protected static void setCompos3(float compos3) {
		PointPanel.compos3 = compos3;
	}

	public static boolean isQuad2d_visible() {
		return quad2dVisible;
	}

	protected static void setQuad2d_visible(boolean quad2d_visible) {
		PointPanel.quad2dVisible = quad2d_visible;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	protected double getTransformx() {
		return transformx;
	}

	protected void setTransformx(double transformx) {
		this.transformx = transformx;
	}

	protected double getTransformy() {
		return transformy;
	}

	protected void setTransformy(double transformy) {
		this.transformy = transformy;
	}

	public static int getPlantIndexPanel() {
		return stateIndexJp;
	}

	/**
	 * Sets the plant index or active state in the Points panel.
	 * 
	 * @param state an available state as an integer.
	 */
	public static void setPlantIndexPanel(int state) {
		PointPanel.stateIndexJp = state;
	}

	protected static double getSx() {
		return sx;
	}

	protected static void setSx(double sx) {
		PointPanel.sx = sx;
	}

	protected static double getSy() {
		return sy;
	}

	protected static void setSy(double sy) {
		PointPanel.sy = sy;
	}

}
