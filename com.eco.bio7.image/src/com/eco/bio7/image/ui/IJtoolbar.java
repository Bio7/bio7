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

package com.eco.bio7.image.ui;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.part.ViewPart;

import ij.IJ;
import ij.ImageJ;

/**
 * The ImageJ-Toolbar view. Displays the SWT toolbar, status line and progress
 * bar of the ImageJ instance. The composite is moved back to the (hidden)
 * ImageJ shell when the view is closed.
 */
public class IJtoolbar extends ViewPart {

	public static IJtoolbar instance;

	private Composite ijComposite;

	public IJtoolbar() {
		instance = this;
	}

	public void createPartControl(Composite parent) {
		Bio7ImageJ.install();
		parent.setLayout(new FillLayout());
		ImageJ ij = IJ.getInstance();
		if (ij == null || ij.getComposite() == null || ij.getComposite().isDisposed()) {
			return;
		}
		ijComposite = ij.getComposite();
		ijComposite.setParent(parent);
		parent.layout(true, true);
		/* The CSS theme is applied to the view after creation! */
		parent.getDisplay().asyncExec(() -> applyThemeColors(parent));
		/* The dispose event is sent before the children are disposed! */
		parent.addDisposeListener(e -> {
			if (ijComposite != null && !ijComposite.isDisposed() && ij.getShell() != null && !ij.getShell().isDisposed()) {
				ijComposite.setParent(ij.getShell());
			}
		});
	}

	public void setFocus() {
		if (ijComposite != null && !ijComposite.isDisposed()) {
			applyThemeColors(ijComposite.getParent());
			ijComposite.setFocus();
		}
	}

	/*
	 * The ImageJ toolbar uses the colors of its control. Apply the colors of the
	 * (themed) view, e.g., for the Bio7 dark theme!
	 */
	private void applyThemeColors(Composite parent) {
		if (parent.isDisposed() || ijComposite == null || ijComposite.isDisposed()) {
			return;
		}
		Color background = parent.getBackground();
		Color foreground = parent.getForeground();
		RGB b = background.getRGB();
		boolean darkBackground = (0.299 * b.red + 0.587 * b.green + 0.114 * b.blue) < 128;
		if (Util.isThemeBlack() && !darkBackground) {
			/* The view is not styled (yet). Use dark default colors! */
			background = new Color(47, 47, 47);
			foreground = new Color(220, 220, 220);
		}
		setColors(ijComposite, background, foreground);
		ijComposite.redraw();
		ijComposite.update();
	}

	private static void setColors(Control control, Color background, Color foreground) {
		control.setBackground(background);
		control.setForeground(foreground);
		control.redraw();
		if (control instanceof Composite) {
			for (Control child : ((Composite) control).getChildren()) {
				setColors(child, background, foreground);
			}
		}
	}

	public void dispose() {
		if (instance == this) {
			instance = null;
		}
		super.dispose();
	}
}
