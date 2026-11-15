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
package com.eco.bio7.ijmacro.editor.hoover;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.text.AbstractInformationControl;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IInformationControlExtension2;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import com.eco.bio7.ijmacro.editors.IJMacroQuickFixProcessor;

/**
 * The hover of a problem marker of the ImageJ macro editor: the message and the
 * quick fixes as links (a click applies the fix). The hover becomes sticky
 * after a moment (then it can be clicked).
 */
public class IJMacroMarkerInformationControl extends AbstractInformationControl implements IInformationControlExtension2 {

	/** The input of the control: the marker and the document of the fixes. */
	public record Input(IJMacroQuickFixProcessor.MarkerFixes fixes, IDocument document) {
	}

	private Composite content;
	private Input input;

	public IJMacroMarkerInformationControl(Shell parent, boolean sticky) {
		super(parent, sticky);
		create();
	}

	@Override
	protected void createContent(Composite parent) {
		content = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		layout.marginWidth = 8;
		layout.marginHeight = 6;
		layout.verticalSpacing = 4;
		content.setLayout(layout);
		content.setForeground(parent.getForeground());
		content.setBackground(parent.getBackground());
	}

	@Override
	public void setInput(Object value) {
		input = value instanceof Input in ? in : null;
		for (Control c : content.getChildren()) {
			c.dispose();
		}
		if (input == null) {
			return;
		}
		IJMacroQuickFixProcessor.MarkerFixes fixes = input.fixes();
		/* The message with the icon of the severity */
		Label icon = new Label(content, SWT.NONE);
		String image = fixes.severity() == IMarker.SEVERITY_ERROR ? ISharedImages.IMG_OBJS_ERROR_TSK
				: fixes.severity() == IMarker.SEVERITY_WARNING ? ISharedImages.IMG_OBJS_WARN_TSK : ISharedImages.IMG_OBJS_INFO_TSK;
		icon.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(image));
		Label message = new Label(content, SWT.WRAP);
		message.setText(fixes.message());
		message.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		if (fixes.proposals().length > 0) {
			Label header = new Label(content, SWT.NONE);
			header.setText(fixes.proposals().length == 1 ? "Quick fix:" : fixes.proposals().length + " quick fixes:");
			header.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
			for (ICompletionProposal p : fixes.proposals()) {
				new Label(content, SWT.NONE);
				Link link = new Link(content, SWT.NONE);
				link.setText("<a>" + p.getDisplayString().replace("&", "&&") + "</a>");
				link.addListener(SWT.Selection, _ -> {
					IDocument doc = input.document();
					/* The hover is closed first (the document changes) */
					dispose();
					p.apply(doc);
				});
			}
		} else {
			Label none = new Label(content, SWT.NONE);
			none.setText("");
		}
		for (Control c : content.getChildren()) {
			c.setForeground(content.getForeground());
			c.setBackground(content.getBackground());
		}
		content.layout(true, true);
	}

	@Override
	public void setInformation(String information) {
		/* The input is set with setInput */
	}

	@Override
	public boolean hasContents() {
		return input != null;
	}

	@Override
	public Point computeSizeHint() {
		Point size = content.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
		org.eclipse.swt.graphics.Rectangle trim = computeTrim();
		return new Point(Math.min(size.x + trim.width, 600), size.y + trim.height);
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

	/* The sticky control (the mouse can move into it and click a fix) */
	@Override
	public IInformationControlCreator getInformationPresenterControlCreator() {
		return parent -> new IJMacroMarkerInformationControl(parent, true);
	}

	/** A creator of the hover control. */
	public static IInformationControlCreator creator() {
		return parent -> (IInformationControl) new IJMacroMarkerInformationControl(parent, false);
	}
}
