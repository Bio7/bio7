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
import ij.gui.ImageCanvas;
import ij.gui.ImageLayout;
import ij.gui.ImageWindow;
import ij.gui.ScrollbarWithLabel;

import java.lang.reflect.InvocationTargetException;
import java.util.Vector;
import javax.swing.JApplet;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;

import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

public class SwtAwtImageJ {

	private JPanel a;

	private JApplet panel;

	private Composite top;

	private CTabItem ci;

	private java.awt.Container contentPane;

	private java.awt.Frame frame;

	private ImageCanvas im;

	private ImagePlus plus;

	private ImageWindow win;

	private Vector ve;

	private ScrollbarWithLabel sliceselect;
	/* For Hyperstacks! */
	private ScrollbarWithLabel channelselect;

	private ScrollbarWithLabel frameselect;

	public SwtAwtImageJ(ScrollbarWithLabel channelSelector, ScrollbarWithLabel sliceSelector, ScrollbarWithLabel frameSelector, ImageCanvas ic, ImagePlus plusin, ImageWindow window) {
		this.im = ic;
		this.plus = plusin;
		this.win = window;
		this.channelselect = channelSelector;
		this.frameselect = frameSelector;

		this.sliceselect = sliceSelector;

		ve = new Vector();
		ve.add(plus);
		ve.add(win);

	}

	public void addTab(final String title) {

		Display dis = CanvasView.getParent2().getDisplay();
		dis.syncExec(new Runnable() {

			public void run() {

				ci = new CTabItem(CanvasView.tabFolder, SWT.CLOSE, CanvasView.insertMark + 1);
				//ci.setData(plus);// add a reference to the image for use as
				// selected tab
				ci.setData(ve);// add a vector with the data from the
				// ImageWindow and the Image!!
				ci.setText(title);
				ci.isShowing();

				top = new Composite(CanvasView.tabFolder, SWT.NO_BACKGROUND | SWT.EMBEDDED);
				try {
					System.setProperty("sun.awt.noerasebackground", "true");
				} catch (NoSuchMethodError error) {
				}
				ci.setControl(top);

				frame = SWT_AWT.new_Frame(top);

				panel = new JApplet() {
					public void update(java.awt.Graphics g) {
						/* Do not erase the background */
						paint(g);
					}
				};
				// panel.addLayout(new java.awt.BorderLayout());
				frame.add(panel);

				JRootPane root = new JRootPane();
				panel.add(root);
				contentPane = root.getContentPane();

				a = new JPanel();
				contentPane.add(a);
				a.add(im);// Add the Image canvas to the JPanel
				a.setLayout(new ImageLayout(im));
				// a.setLayout(new java.awt.BorderLayout());
				if (channelselect != null) {
					a.add(channelselect);
				}
				if (sliceselect != null) {
					a.add(sliceselect);
				}
				if (frameselect != null) {
					a.add(frameselect);
				}

				ve.add(a); // Add to the vector for access
				CanvasView.tabFolder.setLayout(null);
				CanvasView.tabFolder.showItem(ci);
				CanvasView.tabFolder.setSelection(ci);
				CanvasView.setCurrent(a);

			}
		});

	}

	public JPanel getPanel() {
		return a;
	}

	public JApplet getApplet() {
		return panel;
	}

}
