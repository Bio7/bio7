/*******************************************************************************
 * Copyright (c) 2005-2017 M. Austenfeld
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/

package com.eco.bio7.discrete;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.methods.CurrentStates;
import com.eco.bio7.rcp.ApplicationWorkbenchWindowAdvisor;
import com.eco.bio7.time.Time;

public class Hexagon extends JPanel implements KeyListener, MouseListener, MouseMotionListener, MouseWheelListener {

	private static final long serialVersionUID = 1L;

	public JScrollPane jScrollPanehex = null;

	public int counterhex = 0;

	public int value = 0;

	private int RGBhex[] = new int[3];

	private final int ecken = 6;

	private int[] x = new int[7];

	private int[] y = new int[7];

	private int r = 6;// Radius !

	private int ehex = (int) (r * (Math.cos(Math.PI / 6)));

	private int startx = ehex;

	private int starty = r;

	public Polygon[][] poly = new Polygon[Field.getHeight()][Field.getWidth()];

	public boolean hexviewopenend = false;

	private static Hexagon hexagon_instance;

	public Graphics gbuff;

	public Image offscreenimage = null;

	public boolean active_rendering = false;

	private int rectanglex;

	private int rectangley;

	public boolean selectionenabled = false;

	public boolean donotdrag = false;

	public int exz;

	public int eyz;

	private boolean popup_trigger = false;// For Linux !

	private IPreferenceStore store;

	JScrollPane getJScrollPane() {
		if (jScrollPanehex == null) {
			jScrollPanehex = new JScrollPane();
			jScrollPanehex.setComponentOrientation(java.awt.ComponentOrientation.UNKNOWN);
			jScrollPanehex.setWheelScrollingEnabled(true);
			if (store.getBoolean("QUAD_PANEL_SCROLLBAR") == false) {
				jScrollPanehex.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
				jScrollPanehex.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			}

		}
		return jScrollPanehex;
	}

	public Hexagon() {

		super();
		hexagon_instance = this;
		store = Bio7Plugin.getDefault().getPreferenceStore();
		drawHex();
		setFocusable(true);
		requestFocus();
		// setBackground(java.awt.Color.white);
		getJScrollPane();
		Field.setHeight(Field.getHeight());
		Field.setWidth(Field.getWidth());
		Rectangle recty = poly[Field.getHeight() - 1][0].getBounds();
		Rectangle rectx = poly[0][Field.getWidth() - 1].getBounds();
		int y = recty.y + (int) recty.getHeight();
		int x = (rectx.x + (int) rectx.getWidth()) + r;
		setPreferredSize(new Dimension(x, y));
		jScrollPanehex.setViewportView(this);
		addMouseWheelListener(this);
		addKeyListener(this);
		addMouseListener(this);
		addMouseMotionListener(this);
		requestFocus();

	}

	public void malen(Graphics g) {
		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		for (int i = 0; i < Field.getHeight(); i++) {

			for (int u = 0; u < Field.getWidth(); u++) {

				if (Field.getState(u, i) < CurrentStates.getStateList().size() && CurrentStates.getStateList().size() > 0) {
					RGBhex = CurrentStates.getRGB(Field.getState(u, i));
					g2.setColor(new Color(RGBhex[0], RGBhex[1], RGBhex[2]));
					if (i < poly.length && u < poly[0].length) {
						g2.fillPolygon(poly[i][u]);
					}
				}

			}
		}

	}

	public void drawHex() {
		for (int u = 0; u < Field.getHeight(); u++) {

			calculate_hexrow(u);

			for (int v = 0; v < Field.getWidth(); v++)

			{

				calculate_hexagon();// Calculates a hexagon !
				/* A new object of the class hexagon ! */
				poly[u][v] = new Polygon(x, y, 6);

				startx = (int) (startx + 2 * r * (Math.cos(Math.PI / 6)) + 3);
				// The hexagons in x-direction !
			}

			startx = ehex; // Reset position x !

		}

		starty = r;// Reset position y !

	}

	private void calculate_hexrow(int u) {
		if (u > 0) {
			if (u % 2 == 0) {
				startx = (int) (r * (Math.cos(Math.PI / 6)));
				starty = starty + (r + (r / 2)) + 1;

			}

			else {

				starty = starty + (r + (r / 2)) + 1;
				startx = ehex + ehex + 2;
			}

		}
	}

	private void calculate_hexagon() {
		for (int i = 0; i < ecken; i++) {

			x[i] = (int) (startx + r * Math.cos(i * 2 * Math.PI / ecken + (Math.PI / 2)));
			y[i] = (int) (starty + r * Math.sin(i * 2 * Math.PI / ecken + (Math.PI / 2)));

		}
	}

	private void groesser(MouseWheelEvent e) {
		if (Time.isPause()) {
			r = r + (int) e.getPreciseWheelRotation();
			System.out.println(r);
			if (r < 1) {
				r = 1;

			}

			ehex = (int) (r * (Math.cos(Math.PI / 6)));
			startx = ehex;

			starty = r;

			drawHex();
			/*
			 * Only calculate the scrollpane if the size of the field is x>0,y>0 !
			 */
			if (Field.getHeight() != 0 && Field.getWidth() != 0) {
				Rectangle recty = poly[Field.getHeight() - 1][0].getBounds();
				Rectangle rectx = poly[0][Field.getWidth() - 1].getBounds();
				int y = recty.y + (int) recty.getHeight();
				int x = (rectx.x + (int) rectx.getWidth()) + r;

				this.setPreferredSize(new Dimension(x, y));
				SwingUtilities.invokeLater(new Runnable() {
					// !!
					public void run() {
						jScrollPanehex.setViewportView(getHexagonInstance());
					}
				});
			}
		}
	}

	private void PanelhexMousePressed(java.awt.event.MouseEvent evt) {
		int ex = evt.getX();
		int ey = evt.getY();

		if (selectionenabled) {
			for (int y = 0; y < Field.getHeight(); y++) {

				for (int x = 0; x < Field.getWidth(); x++) {

					if (poly[y][x].contains(ex, ey)) {

						Display display = PlatformUI.getWorkbench().getDisplay();
						display.syncExec(new Runnable() {

							public void run() {
								Hexview.getHexview().setstatusline("Current " + Field.getState(exz, eyz) + " Temp: " + Field.getTempState(exz, eyz));
							}
						});

					}
				}
			}

		} else {
			for (int y = 0; y < Field.getHeight(); y++) {

				for (int x = 0; x < Field.getWidth(); x++) {

					if (poly[y][x].contains(ex, ey) && evt.getButton() != 3) {

						Field.setState(x, y, value);
						Field.setTempState(x, y, value);

						repaint();

					}
				}
			}

		}

	}

	public final void paintComponent(Graphics g) {
		super.paintComponent(g);
		if (Time.isPause() == false) {

			if (!active_rendering) {

				if (rectanglex < jScrollPanehex.getVisibleRect().width || rectangley < jScrollPanehex.getVisibleRect().y) {
					offscreenimage = null;
					fieldrenderer();

				}

				else {

					g.drawImage(offscreenimage, jScrollPanehex.getVisibleRect().x, jScrollPanehex.getVisibleRect().y, this);
				}
			} else {
				malen(g);
			}
		} else {
			malen(g);
		}

	}

	public void fieldrenderer() {
		if (offscreenimage == null) {
			int width = jScrollPanehex.getVisibleRect().width;
			int height = jScrollPanehex.getVisibleRect().height;
			if (width > 0 && height > 0) {
				offscreenimage = jScrollPanehex.createImage(width, height);
			} else {
				offscreenimage = jScrollPanehex.createImage(1, 1);
			}

			gbuff = offscreenimage.getGraphics();
		}
		gbuff.setColor(this.getBackground());
		gbuff.fillRect(0, 0, jScrollPanehex.getVisibleRect().width, jScrollPanehex.getVisibleRect().height);
		rectanglex = jScrollPanehex.getVisibleRect().width;
		rectangley = jScrollPanehex.getVisibleRect().height;

		malen(gbuff);
		repaint();
	}

	public void keyTyped(KeyEvent arg0) {

	}

	public void keyPressed(KeyEvent e) {

		requestFocus();
	}

	public void keyReleased(KeyEvent arg0) {
		repaint();

	}

	public void mouseReleased(MouseEvent e) {
		if (Time.isPause()) {
			if (e.isPopupTrigger() || popup_trigger == true && selectionenabled == false) {

				JPopupMenu menu = new JPopupMenu();
				final JMenuItem it1 = new JMenuItem();
				it1.setText("Unsafe Rendering on/off");

				it1.addActionListener(new java.awt.event.ActionListener() {
					public void actionPerformed(java.awt.event.ActionEvent e) {
						active_rendering = !active_rendering;

						Display dis = Quadview.getQuadview().getTop().getDisplay();
						dis.syncExec(new Runnable() {
							public void run() {

								MessageBox messageBox = new MessageBox(new Shell(),

										SWT.ICON_WARNING);
								if (active_rendering) {
									messageBox.setMessage("Switched rendering mode to unsafe rendering !");
								} else {
									messageBox.setMessage("Switched rendering mode to safe rendering !");
								}
								messageBox.open();
							}
						});

					}
				});
				menu.add(it1);
				menu.addSeparator();
				final JMenuItem it2 = new JMenuItem();
				it2.setText("Random");

				it2.addActionListener(new java.awt.event.ActionListener() {
					public void actionPerformed(java.awt.event.ActionEvent e) {

						Field.chance();
						repaint();
					}
				});
				menu.add(it2);
				menu.addSeparator();
				final JMenuItem it3 = new JMenuItem();
				it3.setText("Select State");

				it3.addActionListener(new java.awt.event.ActionListener() {
					public void actionPerformed(java.awt.event.ActionEvent e) {

						setCursor(new Cursor(1));
						selectionenabled = true;
						donotdrag = true;
					}
				});
				menu.add(it3);

				Point pt = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), this);

				menu.show(this, pt.x, pt.y);

			} else {

				setCursor(new Cursor(0));
				donotdrag = false;

			}

		}
		popup_trigger = false;// For Linux!
	}

	public void mouseDragged(MouseEvent e) {
		if (Time.isPause()) {
			if (selectionenabled == false && donotdrag == false) {
				mousedrag(e);
			}
		}
	}

	public void mousedrag(MouseEvent e) {
		if (SwingUtilities.isRightMouseButton(e) == false) {
			int ex = e.getX();
			int ey = e.getY();

			for (int y = 0; y < Field.getHeight(); y++) {

				for (int x = 0; x < Field.getWidth(); x++) {

					if (poly[y][x].contains(ex, ey)) {

						Field.setState(x, y, value);
						Field.setTempState(x, y, value);
						repaint();

					}
				}

			}

		}

	}

	public void resize_scrollpane_hex2d() {
		/*
		 * Only calculate the scrollpane if the size of the field is x>0,y>0. If a discrete pattern is loaded the field is resized to 0,0 !
		 */
		if (Field.getHeight() > 0 && Field.getWidth() > 0) {
			Rectangle recty = poly[Field.getHeight() - 1][0].getBounds();
			Rectangle rectx = poly[0][Field.getWidth() - 1].getBounds();
			int y = recty.y + (int) recty.getHeight();
			int x = (rectx.x + (int) rectx.getWidth()) + r;

			setPreferredSize(new Dimension(x, y));
			SwingUtilities.invokeLater(new Runnable() {
				// !!
				public void run() {
					jScrollPanehex.setViewportView(getHexagonInstance());
				}
			});
		}
	}

	public void mouseWheelMoved(MouseWheelEvent e) {
		groesser(e);

	}

	public void mouseMoved(MouseEvent arg0) {

	}

	public void mouseClicked(MouseEvent arg0) {
		if (jScrollPanehex.isFocusOwner() == false) {
			jScrollPanehex.requestFocus();

		}

	}

	public void mousePressed(MouseEvent e) {
		if (jScrollPanehex.isFocusOwner() == false) {
			jScrollPanehex.requestFocus();

		}
		if (ApplicationWorkbenchWindowAdvisor.getOS().equals("Linux")) {
			if (e.isPopupTrigger()) {
				popup_trigger = true;// A check for Linux!
			}
		}
		if (Time.isPause()) {

			PanelhexMousePressed(e);
			selectionenabled = false;
		}

	}

	public void mouseEntered(MouseEvent arg0) {
		/*
		 * if (jScrollPanehex.isFocusOwner() == false) { jScrollPanehex.requestFocus(); }
		 */

	}

	public void mouseExited(MouseEvent arg0) {

	}

	public static Hexagon getHexagonInstance() {
		return hexagon_instance;
	}

	public static void setHexagon_instance() {
		Hexagon.hexagon_instance = null;
	}
}
