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

package com.eco.bio7.plot;

import javax.swing.JPanel;
import javax.swing.JRootPane;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.jfree.chart.ChartPanel;

public class SwtAwt {
	/* Class for custom chart panels ! */
	private java.awt.Panel panel;

	private JPanel a;

	private Composite top;

	private CTabItem ci;

	private java.awt.Container contentPane;

	private java.awt.Frame frame;

	private ChartPanel panelchart;

	public SwtAwt(ChartPanel chartpanel) {
		this.panelchart = chartpanel;

	}

	public void addTab(final String title) {
		Display dis = ChartView.getParent2().getDisplay();
		dis.syncExec(new Runnable() {
			public void run() {

				ci = new CTabItem(ChartView.getTabFolder(), SWT.NONE,
						ChartView.insertMark + 1);

				ci.setText(title);
				ci.isShowing();

				top = new Composite(ChartView.getTabFolder(), SWT.NO_BACKGROUND
						| SWT.EMBEDDED);
				try {
					System.setProperty("sun.awt.noerasebackground", "true");
				} catch (NoSuchMethodError error) {
				}
				ci.setControl(top);

				frame = SWT_AWT.new_Frame(top);

				panel = new java.awt.Panel(new java.awt.BorderLayout()) {
					public void update(java.awt.Graphics g) {

						paint(g);
					}
				};

				frame.add(panel);

				JRootPane root = new JRootPane();
				panel.add(root);
				contentPane = root.getContentPane();

				contentPane.add(panelchart);

				ChartView.getTabFolder().showItem(ci);
				ChartView.getTabFolder().setSelection(ci);
				ChartView.setCurrent(a);

			}
		});

	}

	public JPanel getPanel() {
		return a;
	}

}
