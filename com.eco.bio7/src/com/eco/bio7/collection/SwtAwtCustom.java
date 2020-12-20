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


package com.eco.bio7.collection;

import java.awt.EventQueue;
import java.awt.Panel;
import java.util.Vector;

import com.jogamp.opengl.awt.GLCanvas;
import javax.swing.JApplet;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

public class SwtAwtCustom {

	private JApplet panel;

	private Composite top;

	

	private java.awt.Container contentPane;

	private java.awt.Frame frame;

	private JPanel jpanel;
	
	private Panel awtPanel;

	private Vector ve;

	private GLCanvas canvas;

	private CustomView view;

	public SwtAwtCustom(JPanel Jpanel,CustomView view) {
		 this.view=view;
		this.jpanel = Jpanel;
		ve = new Vector();
		ve.add(Jpanel);

	}
	
	public SwtAwtCustom(Panel panel,CustomView view) {
		 this.view=view;
		this.awtPanel = panel;
		ve = new Vector();
		ve.add(panel);

	}

	/*public SwtAwtCustom(GLCanvas canvas, com.sun.opengl.util.Animator anim,CustomView view) {

		ve = new Vector();
		ve.add(anim);
		this.canvas = canvas;

	}*/

	public void addTab(final String title) {
		Display dis = view.getCustomViewParent().getDisplay();
		dis.syncExec(new Runnable() {
			public void run() {

				
				top = new Composite(view.getCustomViewParent(), SWT.NO_BACKGROUND | SWT.EMBEDDED);
				try {
					System.setProperty("sun.awt.noerasebackground", "true");
				} catch (NoSuchMethodError error) {
				}
				
				view.getCustomViewParent().setData(ve);

				frame = SWT_AWT.new_Frame(top);

				 panel = new JApplet() {
				      public void update(java.awt.Graphics g) {
				        /* Do not erase the background */
				        paint(g);
				      }
				    };

				frame.add(panel);

				JRootPane root = new JRootPane();
				panel.add(root);
				contentPane = root.getContentPane();

				contentPane.add(jpanel);
				view.getCustomViewParent().layout();
				
				

			}
		});

	}
	
	public void addAWTTab(final String title) {
		Display dis = view.getCustomViewParent().getDisplay();
		dis.syncExec(new Runnable() {
			public void run() {

				

				top = new Composite(view.getCustomViewParent(), SWT.NO_BACKGROUND | SWT.EMBEDDED);
				try {
					System.setProperty("sun.awt.noerasebackground", "true");
				} catch (NoSuchMethodError error) {
				}
				
				view.getCustomViewParent().setData(ve);

				frame = SWT_AWT.new_Frame(top);

				 panel = new JApplet() {
				      public void update(java.awt.Graphics g) {
				        /* Do not erase the background */
				        paint(g);
				      }
				    };

				frame.add(panel);

				JRootPane root = new JRootPane();
				panel.add(root);
				contentPane = root.getContentPane();

				contentPane.add(awtPanel);

				view.getCustomViewParent().layout();
				

			}
		});

	}

	/*public void addCanvasTab(final String title) {
		Display dis = Custom3D.getParent2().getDisplay();
		dis.syncExec(new Runnable() {
			public void run() {

				

				top = new Composite(Custom3D.getTabFolder(), SWT.NO_BACKGROUND | SWT.EMBEDDED);
				try {
					System.setProperty("sun.awt.noerasebackground", "true");
				} catch (NoSuchMethodError error) {
				}
				
				view.getCustomViewParent().setData(ve);

				frame = SWT_AWT.new_Frame(top);

				 panel = new JApplet() {
				      public void update(java.awt.Graphics g) {
				         Do not erase the background 
				        paint(g);
				      }
				    };

				frame.add(panel);

				JRootPane root = new JRootPane();
				panel.add(root);
				contentPane = root.getContentPane();
				EventQueue.invokeLater(new Runnable() {
					public void run() {
						contentPane.add(canvas);
					}
				});

				view.getCustomViewParent().layout();
				

			}
		});
	}*/

	

}
