/*******************************************************************************
 * Copyright (c) 2007-2026 M. Austenfeld
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * Contributors:
 * Marcel Austenfeld - initial API and implementation
 *******************************************************************************/
package com.eco.bio7.reditor.control;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.AbstractInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import com.eco.bio7.reditors.RDebugHover;

/**
 * The hover of a variable in a debug session: its class, dimension, size and a
 * preview (e.g., the first rows of a data frame) in a fixed font, with the
 * action "Open" (a data frame or matrix in the Bio7 table, a numeric matrix as
 * an image, a vector as a plot). Move the mouse into the hover to use it.
 */
public class RDebugValueInformationControl extends AbstractInformationControl {

	private StyledText text;
	private String name;

	public RDebugValueInformationControl(Shell parent) {
		super(parent, new ToolBarManager(SWT.FLAT));
		ToolBarManager tbm = getToolBarManager();
		tbm.add(new Action("Open in Table") {
			@Override
			public void run() {
				String n = name;
				dispose();
				if (n != null) {
					RDebugHover.open(n);
				}
			}
		});
		tbm.update(true);
		create();
	}

	@Override
	protected void createContent(Composite parent) {
		parent.setLayout(new FillLayout());
		text = new StyledText(parent, SWT.MULTI | SWT.READ_ONLY | SWT.H_SCROLL | SWT.V_SCROLL);
		text.setFont(JFaceResources.getTextFont());
		text.setMargins(4, 4, 4, 4);
	}

	@Override
	public void setInformation(String information) {
		String info = information == null ? "" : information;
		if (info.startsWith(RDebugHover.MARK)) {
			int nl = info.indexOf('\n');
			name = info.substring(RDebugHover.MARK.length(), nl < 0 ? info.length() : nl);
			info = nl < 0 ? "" : info.substring(nl + 1);
		}
		text.setText(info);
	}

	@Override
	public boolean hasContents() {
		return text != null && text.getCharCount() > 0;
	}

	@Override
	public Point computeSizeHint() {
		Point size = getShell().computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
		Point constraints = getSizeConstraints();
		if (constraints != null) {
			size.x = Math.min(size.x, constraints.x);
			size.y = Math.min(size.y, constraints.y);
		}
		return new Point(Math.max(size.x, 260), size.y);
	}

	@Override
	public void setForegroundColor(org.eclipse.swt.graphics.Color foreground) {
		super.setForegroundColor(foreground);
		if (text != null) {
			text.setForeground(foreground);
		}
	}

	@Override
	public void setBackgroundColor(org.eclipse.swt.graphics.Color background) {
		super.setBackgroundColor(background);
		if (text != null) {
			text.setBackground(background);
		}
	}

	/* The enriched hover (the mouse moved into it) is this control itself */
	@Override
	public IInformationControlCreator getInformationPresenterControlCreator() {
		return RDebugValueInformationControl::new;
	}
}
