/*******************************************************************************
 * Copyright (c) 2007-2025 M. Austenfeld
 * 
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * Contributors:
 * Marcel Austenfeld - initial API and implementation
 *******************************************************************************/

package com.eco.bio7.discrete;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import com.eco.bio7.init.ButtonContainer;

public class StateButtonView extends ViewPart {

	public static final String ID = "com.eco.bio7.states";

	private Composite top = null;

	private static ScrolledComposite sc;

	private ButtonContainer butt;

	public void createPartControl(Composite parent) {
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent,
				"com.eco.bio7.plantbutton");
		getViewSite().getPage().addPartListener(new IPartListener2() {

			public void partActivated(IWorkbenchPartReference partRef) {
				if (partRef.getId().equals("com.eco.bio7.states")) {

				}

			}

			public void partBroughtToTop(IWorkbenchPartReference partRef) {

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

		sc = new ScrolledComposite(parent, SWT.H_SCROLL | SWT.V_SCROLL
				| SWT.BORDER);
		top = new Composite(sc, SWT.NONE);

		top.setBackground(parent.getBackground());
		top.setLayout(new RowLayout());

		butt = new ButtonContainer(top);

		sc.setContent(top);
		sc.setExpandHorizontal(true);
		sc.setExpandVertical(true);
		sc.setMinSize(top.computeSize(200, 50));

	}

	public void setFocus() {
		sc.setFocus();
	}

	public void dispose() {

		super.dispose();

		butt.getButtcont().clear();
		butt.disposed = true;
	}

	public static ScrolledComposite getSc() {
		return sc;
	}

	public static void setSc(ScrolledComposite sc) {
		StateButtonView.sc = sc;
	}

}
