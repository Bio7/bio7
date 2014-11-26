/*******************************************************************************
 * Copyright (c) 2007-2014 M. Austenfeld
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/

package com.eco.bio7.image;

import ij.IJ;

import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Panel;
import javax.swing.JApplet;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.ViewPart;
import com.eco.bio7.swt.SwtAwt;

public class IJtoolbar extends ViewPart {

	private static Frame frame;
	private static Panel panel;
	private JPanel jpp;

	public IJtoolbar() {

	}

	public void createPartControl(Composite parent) {
		Composite top = new Composite(parent, SWT.NO_BACKGROUND | SWT.EMBEDDED);
		getViewSite().getPage().addPartListener(new IPartListener2() {

			public void partActivated(IWorkbenchPartReference partRef) {
				if (partRef.getId().equals("com.eco.bio7.ijtoolbar")) {
					SwingUtilities.invokeLater(new Runnable() {
						// !!
						public void run() {
							jpp.repaint();
						}
					});

				}

			}

			public void partBroughtToTop(IWorkbenchPartReference partRef) {
				if (partRef.getId().equals("com.eco.bio7.ijtoolbar")) {
					SwingUtilities.invokeLater(new Runnable() {
						// !!
						public void run() {
							jpp.repaint();
						}
					});

				}
			}

			public void partClosed(IWorkbenchPartReference partRef) {

			}

			public void partDeactivated(IWorkbenchPartReference partRef) {

			}

			public void partHidden(IWorkbenchPartReference partRef) {

			}

			public void partInputChanged(IWorkbenchPartReference partRef) {

			}

			public void partOpened(IWorkbenchPartReference partRef) {

			}

			public void partVisible(IWorkbenchPartReference partRef) {

			}
		});
		frame = SWT_AWT.new_Frame(top);
		/*final sun.awt.EmbeddedFrame ef = (sun.awt.EmbeddedFrame) frame;
		ef.addWindowListener(new WindowAdapter() {
			public void windowActivated(WindowEvent e) {
				ef.synthesizeWindowActivation(true);
			}
		});
*/      SwtAwt.setSwtAwtFocus(frame, top);
		panel = new JApplet() {
			public void update(java.awt.Graphics g) {

				paint(g);
			}
		};

		frame.add(panel);
		JRootPane roote = new JRootPane();
		panel.add(roote);
		java.awt.Container contentPane = roote.getContentPane();
		jpp = new JPanel();
		jpp.setLayout(new GridLayout(2, 1));
		jpp.add(IJ.getInstance().toolbar);
		jpp.add(IJ.getInstance().statusBar);

		contentPane.add(jpp);

	}

	public void setFocus() {

	}

	public static Frame getFrame() {
		return frame;
	}

	public static Panel getPanel() {
		return panel;
	}

}
