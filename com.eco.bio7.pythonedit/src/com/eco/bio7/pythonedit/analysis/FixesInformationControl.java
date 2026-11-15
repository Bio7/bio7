/*******************************************************************************
 * Copyright (c) 2007-2026 M. Austenfeld
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
package com.eco.bio7.pythonedit.analysis;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.text.AbstractInformationControl;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IInformationControlExtension2;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

/**
 * The hover of a marker: the message and the quick fixes as links (a click
 * applies the fix). The hover becomes sticky after a moment.
 */
public class FixesInformationControl extends AbstractInformationControl implements IInformationControlExtension2 {

	/** The input: the marker and the document of the fixes. */
	public record Input(PythonFixes.MarkerFixes fixes, IDocument document) {
	}

	private Composite content;
	private Object input;

	public FixesInformationControl(Shell parent, boolean sticky) {
		super(parent, sticky);
		create();
	}

	@Override
	protected void createContent(Composite parent) {
		content = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		layout.marginWidth = 8;
		layout.marginHeight = 6;
		content.setLayout(layout);
		content.setForeground(parent.getForeground());
		content.setBackground(parent.getBackground());
	}

	@Override
	public void setInput(Object value) {
		input = value instanceof Input || value instanceof String s && !s.isBlank() ? value : null;
		for (Control c : content.getChildren()) {
			c.dispose();
		}
		if (input == null) {
			return;
		}
		if (input instanceof String text) {
			/* The help and the values (a text) */
			org.eclipse.swt.widgets.Text t = new org.eclipse.swt.widgets.Text(content, SWT.MULTI | SWT.READ_ONLY | SWT.WRAP);
			t.setText(text);
			t.setFont(org.eclipse.jface.resource.JFaceResources.getTextFont());
			GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
			gd.widthHint = Math.min(620, Math.max(200, text.lines().mapToInt(String::length).max().orElse(40) * 8));
			t.setLayoutData(gd);
			t.setForeground(content.getForeground());
			t.setBackground(content.getBackground());
			content.layout(true, true);
			return;
		}
		Input in = (Input) input;
		PythonFixes.MarkerFixes fixes = in.fixes();
		Label icon = new Label(content, SWT.NONE);
		String image = fixes.severity() == IMarker.SEVERITY_ERROR ? ISharedImages.IMG_OBJS_ERROR_TSK
				: fixes.severity() == IMarker.SEVERITY_WARNING ? ISharedImages.IMG_OBJS_WARN_TSK : ISharedImages.IMG_OBJS_INFO_TSK;
		icon.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(image));
		Label message = new Label(content, SWT.WRAP);
		message.setText(fixes.message());
		message.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		if (!fixes.proposals().isEmpty()) {
			Label header = new Label(content, SWT.NONE);
			header.setText(fixes.proposals().size() == 1 ? "Quick fix:" : fixes.proposals().size() + " quick fixes:");
			header.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
			for (ICompletionProposal p : fixes.proposals()) {
				new Label(content, SWT.NONE);
				Link link = new Link(content, SWT.NONE);
				link.setText("<a>" + p.getDisplayString().replace("&", "&&").replace("<", "&lt;") + "</a>");
				link.addListener(SWT.Selection, _ -> {
					IDocument doc = in.document();
					dispose();
					p.apply(doc);
				});
			}
		}
		for (Control c : content.getChildren()) {
			c.setForeground(content.getForeground());
			c.setBackground(content.getBackground());
		}
		content.layout(true, true);
	}

	@Override
	public void setInformation(String information) {
		setInput(information);
	}

	@Override
	public boolean hasContents() {
		return input != null;
	}

	@Override
	public Point computeSizeHint() {
		Point size = content.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
		Rectangle trim = computeTrim();
		return new Point(Math.min(size.x + trim.width, 640), size.y + trim.height);
	}

	@Override
	public void setForegroundColor(org.eclipse.swt.graphics.Color foreground) {
		super.setForegroundColor(foreground);
		content.setForeground(foreground);
	}

	@Override
	public void setBackgroundColor(org.eclipse.swt.graphics.Color background) {
		super.setBackgroundColor(background);
		content.setBackground(background);
	}

	@Override
	public IInformationControlCreator getInformationPresenterControlCreator() {
		return parent -> new FixesInformationControl(parent, true);
	}
}
