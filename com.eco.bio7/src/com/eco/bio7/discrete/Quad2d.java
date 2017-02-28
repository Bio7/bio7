/*******************************************************************************
 * Copyright (c) 2007-2017 M. Austenfeld
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
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;
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

public class Quad2d extends JPanel implements KeyListener, MouseListener, MouseMotionListener, MouseWheelListener {

	private static final long serialVersionUID = 1L;

	public int rwidth = Field.getQuadSize();

	public int rheight = Field.getQuadSize();

	public int rx = 0;

	public int ry = 0;

	private int quaddist = 0;

	public int zeichenflaechex = (Field.getHeight() * Field.getQuadSize());

	public int zeichenflaechey = (Field.getWidth() * Field.getQuadSize());

	public static Rectangle[][] quad = new Rectangle[Field.getHeight()][Field.getWidth()];

	public JScrollPane jScrollPane = null;

	private static int value = 0;// The active value of the selected State when
	// dragging etc.
	public Graphics g = null;

	public int RGB[] = new int[3];

	public ArrayList<CounterModel> zaehlerlist = new ArrayList<CounterModel>();

	public boolean dragclick = false;

	public int exz;

	public int eyz;

	public boolean resized = false;

	public boolean quadviewopenend = false;

	private static Quad2d quad2d_instance;

	public Graphics gbuff;

	public Image offscreenimage = null;

	public boolean selectionenabled = false;

	public boolean donotdrag = false;

	public boolean activeRendering = false;

	private int rectanglex;

	private int rectangley;

	private boolean popup_trigger = false;// For Linux !

	private IPreferenceStore store;

	JScrollPane getJScrollPane() {
		if (jScrollPane == null) {
			jScrollPane = new JScrollPane();
			jScrollPane.setComponentOrientation(java.awt.ComponentOrientation.UNKNOWN);
			jScrollPane.setWheelScrollingEnabled(true);
			if (store.getBoolean("QUAD_PANEL_SCROLLBAR") == false) {
				jScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
				jScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			}

		}
		return jScrollPane;
	}

	public void createzaehler() {
		zaehlerlist.clear();// Deletes all Objects in the ArrayList !
		for (int i = 0; i < CurrentStates.getR().size(); i++) {

			zaehlerlist.add(new CounterModel());// New count Objects !
		}

	}

	/* Count the different states ! */
	public void feldzaehler() {
		reset();
		for (int i = 0; i < Field.getHeight(); i++) {
			for (int u = 0; u < Field.getWidth(); u++) {
				if (Field.getState(u, i) < CurrentStates.getStateList().size() && CurrentStates.getStateList().size() > 0) {
					/* Get the self defined class ! */
					CounterModel zahl = (CounterModel) zaehlerlist.get(Field.getState(u, i));
					zahl.setZahl();

				}

			}

		}

		for (int i = 0; i < CurrentStates.getStateList().size(); i++) {

			CounterModel zahl = (CounterModel) zaehlerlist.get(i);

			zahl.addCountList(zahl.getCount()); // Copy the counts into the List
			// !
		}

	}

	public void reset() {
		/* This is called after one iteration. Sets the counter to zero ! */
		for (int i = 0; i < zaehlerlist.size(); i++) {

			CounterModel zahl = (CounterModel) zaehlerlist.get(i);
			zahl.reset();

		}
	}

	public Quad2d() {

		super();
		quad2d_instance = this;
		store = Bio7Plugin.getDefault().getPreferenceStore();
		drawQuad();
		getJScrollPane();
		/* We use the random function to assign objects at startup ! */
		Field.chance();
		setPreferredSize(new Dimension(zeichenflaechey, zeichenflaechex));
		jScrollPane.setViewportView(this);
		jScrollPane.setFocusable(true);
		addMouseWheelListener(this);
		addMouseListener(this);
		addMouseMotionListener(this);
		setFocusable(true);
		addKeyListener(this);

		for (int i = 0; i < CurrentStates.getR().size(); i++) {
			zaehlerlist.add(new CounterModel());// initialize new Counter

		}

	}

	public void malen(Graphics g) {
		if (g != null) {

			if (dragclick == false) {
				for (int i = 0; i < Field.getHeight(); i++) {

					for (int u = 0; u < Field.getWidth(); u++) {

						if ((Field.getHeight() == Field.getStateArray().length) && (Field.getWidth() == Field.getStateArray()[0].length)) {

							if (Field.getState(u, i) < CurrentStates.getStateList().size() && CurrentStates.getStateList().size() > 0) {
								RGB = CurrentStates.getRGB(Field.getState(u, i));
								g.setColor(new Color(RGB[0], RGB[1], RGB[2]));
								if (i < quad.length && u < quad[0].length) {
									g.fillRect(((int) quad[i][u].getX()), ((int) quad[i][u].getY()), ((int) quad[i][u].getWidth()), ((int) quad[i][u].getHeight()));
								}
							}

						}
					}
				}
			}

			else {// if dragged

				if (Field.getState(exz, eyz) < CurrentStates.getStateList().size() && CurrentStates.getStateList().size() > 0) {
					RGB = CurrentStates.getRGB(Field.getState(exz, eyz));
					g.setColor(new Color(RGB[0], RGB[1], RGB[2]));

					g.fillRect(((int) quad[eyz][exz].getX()), ((int) quad[eyz][exz].getY()), ((int) quad[eyz][exz].getWidth()), ((int) quad[eyz][exz].getHeight()));

				}

			}
		}
	}

	public void drawQuad() {

		for (int u = 0; u < Field.getHeight(); u++) {

			for (int v = 0; v < Field.getWidth(); v++)

			{

				quad[u][v] = new Rectangle(rx, ry, rwidth, rheight);
				rx = rx + rwidth + quaddist;
			}
			rx = 0;
			ry = ry + rheight + quaddist;
		}
		ry = 0;

	}

	public void groesser(MouseWheelEvent e) {
		int r = Field.getQuadSize() + e.getWheelRotation();
		if (r >= 2) {
			Field.setQuadSize(r);

		} else {
			Field.setQuadSize(2);
		}

		Time.setPause(true);
		Field.setQuadSize(Field.getQuadSize() + e.getWheelRotation());
		;
		Field.setQuadSize(Field.getQuadSize());
		rwidth = Field.getQuadSize();
		rheight = Field.getQuadSize();
		drawQuad();
		zeichenflaechex = ((Field.getHeight() * Field.getQuadSize()));
		zeichenflaechey = ((Field.getWidth() * Field.getQuadSize()));

		setPreferredSize(new Dimension(zeichenflaechey, zeichenflaechex));
		SwingUtilities.invokeLater(new Runnable() {
			// !!
			public void run() {
				jScrollPane.setViewportView(quad2d_instance);
			}
		});

	}

	public void quadResize(int size) {

		Field.setQuadSize(size);
		Time.setPause(true);
		Field.setQuadSize(Field.getQuadSize());
		rwidth = Field.getQuadSize();
		rheight = Field.getQuadSize();
		drawQuad();
		zeichenflaechex = ((Field.getHeight() * Field.getQuadSize()));
		zeichenflaechey = ((Field.getWidth() * Field.getQuadSize()));

		setPreferredSize(new Dimension(zeichenflaechey, zeichenflaechex));
		SwingUtilities.invokeLater(new Runnable() {
			// !!
			public void run() {
				jScrollPane.setViewportView(quad2d_instance);
			}
		});

	}

	private void Panel1MousePressed(java.awt.event.MouseEvent evt) {

		mousepressed(evt, 1, 1);
	}

	/* This method is for Image J ! */
	public void setquads(double x, double y) {

		exz = (int) (x / Field.getQuadSize());
		eyz = (int) (y / Field.getQuadSize());
		if (exz <= Field.getWidth() && eyz <= Field.getHeight()) {

			Field.setState(exz, eyz, getValue());
			Field.setTempState(exz, eyz, getValue());

			repaint();

		}

	}

	public void mousepressed(java.awt.event.MouseEvent evt, double transformx, double transformy) {
		int ex = 0;
		int ey = 0;

		ex = (int) (evt.getX() / transformx);
		ey = (int) (evt.getY() / transformx);

		exz = ex / Field.getQuadSize();
		eyz = ey / Field.getQuadSize();
		if ((exz >= 0 && exz < Field.getWidth()) && (eyz >= 0 && eyz < Field.getHeight())) {

			if (selectionenabled) {

				if (quad[eyz][exz].contains(ex, ey)) {

					Display display = PlatformUI.getWorkbench().getDisplay();
					display.syncExec(new Runnable() {

						public void run() {
							Quadview.getQuadview().setstatusline("Current " + Field.getState(exz, eyz) + " Temp: " + Field.getTempState(exz, eyz));
						}
					});

					// }

				}

			} else {

				if (quad[eyz][exz].contains(ex, ey) && evt.getButton() != 3) {

					Field.setState(exz, eyz, getValue());
					Field.setTempState(exz, eyz, getValue());
					repaint();

				}

			}
		}

	}

	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		if (Time.isPause() == false) {

			if (!activeRendering) {

				if (rectanglex < jScrollPane.getVisibleRect().width || rectangley < jScrollPane.getVisibleRect().height) {
					offscreenimage = null;
					fieldrenderer();

				}

				else {

					g.drawImage(offscreenimage, jScrollPane.getVisibleRect().x, jScrollPane.getVisibleRect().y, this);
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
			int width = jScrollPane.getVisibleRect().width;
			int height = jScrollPane.getVisibleRect().height;
			if (width > 0 && height > 0) {
				offscreenimage = jScrollPane.createImage(width, height);
			} else {
				offscreenimage = jScrollPane.createImage(1, 1);
			}

			gbuff = offscreenimage.getGraphics();

		}
		gbuff.setColor(this.getBackground());
		gbuff.fillRect(0, 0, jScrollPane.getVisibleRect().width, jScrollPane.getVisibleRect().height);
		rectanglex = jScrollPane.getVisibleRect().width;
		rectangley = jScrollPane.getVisibleRect().height;

		malen(gbuff);
		repaint();// recursive paint !

	}

	/**
	 * @return Returns the pause.
	 */

	public static boolean isPause() {
		return Time.isPause();
	}

	/**
	 * @param pause
	 *            The pause to set.
	 */
	public static void setPause(boolean pause) {
		Time.setPause(pause);
	}

	public void keyTyped(KeyEvent e) {

	}

	public void keyPressed(KeyEvent e) {
		int r = Field.getQuadSize();
		if (e.getKeyCode() == 521) {

			r = Field.getQuadSize() + 1;
		} else if (e.getKeyCode() == 45) {
			if (Field.getQuadSize() - 1 > 0) {
				r = Field.getQuadSize() - 1;
			}
		}

		Time.setPause(true);

		;
		Field.setQuadSize(r);
		rwidth = Field.getQuadSize();
		rheight = Field.getQuadSize();
		drawQuad();
		zeichenflaechex = ((Field.getHeight() * Field.getQuadSize()));
		zeichenflaechey = ((Field.getWidth() * Field.getQuadSize()));

		setPreferredSize(new Dimension(zeichenflaechey, zeichenflaechex));

		SwingUtilities.invokeLater(new Runnable() {
			// !!
			public void run() {
				jScrollPane.setViewportView(quad2d_instance);
			}
		});
	}

	public void keyReleased(KeyEvent e) {

	}

	public void mouseReleased(final MouseEvent e) {
		if (Time.isPause()) {

			if (e.isPopupTrigger() || popup_trigger == true && selectionenabled == false) {

				JPopupMenu menu = new JPopupMenu();
				final JMenuItem it1 = new JMenuItem();
				it1.setText("Active Rendering on/off");

				it1.addActionListener(new java.awt.event.ActionListener() {
					public void actionPerformed(java.awt.event.ActionEvent e) {
						activeRendering = !activeRendering;

						Display dis = Quadview.getQuadview().getTop().getDisplay();
						dis.syncExec(new Runnable() {
							public void run() {

								MessageBox messageBox = new MessageBox(new Shell(),

										SWT.ICON_WARNING);
								if (activeRendering) {
									messageBox.setMessage("Switched rendering mode to active rendering !");
								} else {
									messageBox.setMessage("Switched rendering mode to default rendering !");
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
		popup_trigger = false;
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
			int x = e.getX();
			int y = e.getY();
			exz = x / Field.getQuadSize();
			eyz = y / Field.getQuadSize();

			if ((exz >= 0 && exz < Field.getWidth()) && (eyz >= 0 && eyz < Field.getHeight())) {// dragging

				if (quad[eyz][exz].contains(x, y)) {

					Field.setState(exz, eyz, getValue());
					Field.setTempState(exz, eyz, getValue());

					repaint(quad[eyz][exz]);

				}

			}
		}
	}

	public void resize_scrollpane_quad2d() {
		int zeichenflaechex = ((Field.getHeight() * Field.getQuadSize()));
		int zeichenflaechey = ((Field.getWidth() * Field.getQuadSize()));
		setPreferredSize(new Dimension(zeichenflaechey, zeichenflaechex));
		SwingUtilities.invokeLater(new Runnable() {
			// !!
			public void run() {
				jScrollPane.setViewportView(quad2d_instance);
			}
		});
	}

	public void mouseWheelMoved(MouseWheelEvent e) {

		if (Time.isPause()) {
			groesser(e);
		}
	}

	public void mouseMoved(MouseEvent evt) {

		/*
		 * if (this.isFocusOwner() == false) { this.requestFocus(); }
		 */

	}

	public void mouseClicked(MouseEvent e) {

		if (this.isFocusOwner() == false) {
			this.requestFocus();

		}

	}

	public void mousePressed(MouseEvent e) {
		if (this.isFocusOwner() == false) {
			this.requestFocus();

		}
		if (ApplicationWorkbenchWindowAdvisor.getOS().equals("Linux")) {
			if (e.isPopupTrigger()) {
				popup_trigger = true;// A check for Linux !
			}
		}

		if (Time.isPause()) {

			Panel1MousePressed(e);
			selectionenabled = false;
		}

	}

	public void mouseEntered(MouseEvent evt) {

	}

	public void mouseExited(MouseEvent e) {

	}

	public static Quad2d getQuad2dInstance() {
		return quad2d_instance;
	}

	public static void setValue(int value) {
		Quad2d.value = value;
	}

	public static int getValue() {
		return value;
	}

}
