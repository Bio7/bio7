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

import java.util.List;

import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MPartSashContainer;
import org.eclipse.e4.ui.model.application.ui.basic.MPartSashContainerElement;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
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
		/* Enlarge the view once if the toolbar, status line and progress bar don't fit! */
		parent.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent e) {
				if (parent.getClientArea().height <= 0) {
					return;
				}
				parent.removeControlListener(this);
				parent.getDisplay().asyncExec(() -> ensureToolbarHeight(parent));
			}
		});
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

	/*
	 * Increases the weight of the part stack of this view in its (vertical) sash
	 * container so that the complete ImageJ toolbar is visible.
	 */
	private void ensureToolbarHeight(Composite parent) {
		if (parent.isDisposed() || ijComposite == null || ijComposite.isDisposed()) {
			return;
		}
		int width = parent.getClientArea().width;
		int needed = ijComposite.computeSize(width > 0 ? width : SWT.DEFAULT, SWT.DEFAULT).y;
		int missing = needed - parent.getClientArea().height;
		if (missing <= 0) {
			return;
		}
		MPart part = getSite().getService(MPart.class);
		if (part == null) {
			return;
		}
		MUIElement element = part.getCurSharedRef() != null ? part.getCurSharedRef() : part;
		/* The part is in a part stack which is a child of a sash container! */
		MUIElement stack = element.getParent();
		Object container = stack != null ? stack.getParent() : null;
		if (!(container instanceof MPartSashContainer)) {
			return;
		}
		MPartSashContainer sash = (MPartSashContainer) container;
		if (sash.isHorizontal() || !(stack.getWidget() instanceof Control)) {
			return;
		}
		List<MPartSashContainerElement> children = sash.getChildren();
		int sashHeight = 0;
		double otherWeights = 0;
		for (MUIElement child : children) {
			if (!child.isToBeRendered() || !child.isVisible() || !(child.getWidget() instanceof Control)) {
				continue;
			}
			sashHeight += ((Control) child.getWidget()).getSize().y;
			if (child != stack) {
				otherWeights += weight(child);
			}
		}
		int stackHeight = ((Control) stack.getWidget()).getSize().y;
		if (sashHeight <= 0 || otherWeights <= 0) {
			return;
		}
		double fraction = (double) (stackHeight + missing) / sashHeight;
		if (fraction >= 0.9) {
			/* The window is too small. Don't hide the other views! */
			return;
		}
		double newWeight = fraction * otherWeights / (1 - fraction);
		stack.setContainerData(Integer.toString((int) Math.ceil(newWeight)));
	}

	private static double weight(MUIElement element) {
		try {
			return Double.parseDouble(element.getContainerData());
		} catch (NumberFormatException | NullPointerException e) {
			return 0;
		}
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
