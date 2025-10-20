/*******************************************************************************
 * Copyright (c) 2007-2025 M. Austenfeld
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/

package com.eco.bio7.image;

import java.awt.EventQueue;
import java.awt.Panel;
import java.util.Vector;
import javax.swing.JApplet;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public class FXSwtAwtCustom {

	private JApplet panel;

	private Composite top;

	public Composite getTop() {
		return top;
	}

	private java.awt.Container contentPane;

	private java.awt.Frame frame;

	private JPanel jpanel;

	private Panel awtPanel;

	private Vector ve;

	private CustomDetachedImageJView view;

	protected Shell parent;

	public FXSwtAwtCustom(JPanel Jpanel, CustomDetachedImageJView view) {
		this.view = view;
		this.jpanel = Jpanel;
		ve = new Vector();
		ve.add(Jpanel);

	}

	public java.awt.Frame getFrame() {
		return frame;
	}

	public void addTab(final String title) {
		/* Add SWT_AWT to embed the ImageJ canvas! */
		Composite customViewParent = view.getCustomViewParent();
		Display dis = customViewParent.getDisplay();
		dis.syncExec(new Runnable() {
			public void run() {
				parent = new Shell(Util.getDisplay());
				top = new Composite(parent, SWT.NO_BACKGROUND | SWT.EMBEDDED);
				try {
					System.setProperty("sun.awt.noerasebackground", "true");
				} catch (NoSuchMethodError error) {
				}

				customViewParent.setData(ve);
				frame = SWT_AWT.new_Frame(top);
				/*
				 * HighDPI fix for Windows frame layout which reverts the DPIUtil settings of
				 * the default implementation of the SWT_AWT class!
				 */
				if (Util.getOS().equals("Windows")) {
					if (Util.getZoom() >= 175) {
						customViewParent.getDisplay().asyncExec(() -> {
							if (customViewParent.isDisposed())
								return;
							final Rectangle clientArea = customViewParent.getClientArea(); // To Pixels
							EventQueue.invokeLater(() -> {
								frame.setSize(clientArea.width, clientArea.height);
								frame.validate();
							});
						});
					}

				}

				// SwtAwt.setSwtAwtFocus(frame, top,Util.getDisplay());

				panel = new JApplet() {
					public void update(java.awt.Graphics g) {
						// Do not erase the background
						paint(g);
					}
				};

				frame.add(panel);

				JRootPane root = new JRootPane();
				panel.add(root);
				contentPane = root.getContentPane();

				contentPane.add(jpanel);

				customViewParent.layout();

			}
		});

	}

}
