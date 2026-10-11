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

import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
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
		/* The dispose event is sent before the children are disposed! */
		parent.addDisposeListener(e -> {
			if (ijComposite != null && !ijComposite.isDisposed() && ij.getShell() != null && !ij.getShell().isDisposed()) {
				ijComposite.setParent(ij.getShell());
			}
		});
	}

	public void setFocus() {
		if (ijComposite != null && !ijComposite.isDisposed()) {
			ijComposite.setFocus();
		}
	}

	public void dispose() {
		if (instance == this) {
			instance = null;
		}
		super.dispose();
	}
}
