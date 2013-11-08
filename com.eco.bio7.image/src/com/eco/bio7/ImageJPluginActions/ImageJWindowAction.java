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

package com.eco.bio7.ImageJPluginActions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.WorkbenchPage;
import com.eco.bio7.image.CanvasView;
import com.eco.bio7.util.Util;

public class ImageJWindowAction extends Action implements IMenuCreator {

	private Menu fMenu;

	private Shell shell;

	protected Point p;

	public ImageJWindowAction() {
		setId("Window");
		setToolTipText("ImageJ");
		setText("Window");

		setMenuCreator(this);
	}

	public Menu getMenu(Control parent) {
		if (fMenu != null) {
			fMenu.dispose();
		}
		fMenu = new Menu(parent);

		MenuItem menuItem = new MenuItem(fMenu, SWT.PUSH);

		menuItem.setText("ImageJ-Toolbar");

		MenuItem menuItem1 = new MenuItem(fMenu, SWT.PUSH);

		menuItem1.setText("Bio7-Toolbar");

		menuItem.addSelectionListener(new SelectionListener() {

			public void selectionChanged(SelectionChangedEvent event) {

			}

			public void widgetSelected(SelectionEvent e) {

				showtoolbar();

			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});
		menuItem1.addSelectionListener(new SelectionListener() {

			public void selectionChanged(SelectionChangedEvent event) {

			}

			public void widgetSelected(SelectionEvent e) {

				IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
				try {
					IViewReference ref = page.showView("com.eco.bio7.image_methods").getSite().getPage().findViewReference("com.eco.bio7.image_methods");
					//((WorkbenchPage) page.showView("com.eco.bio7.image_methods").getSite().getPage()).getActivePerspective().getPresentation().detachPart(ref);
					Util ut = new Util();
					String os = ut.getOS();
					if (os != null) {

						if (os.equals("Linux")) {

							ref.getView(false).getViewSite().getShell().setSize(260, 760);

						} else if (os.equals("Windows")) {

							ref.getView(false).getViewSite().getShell().setSize(260, 790);

						} else if (os.equals("Mac")) {

							ref.getView(false).getViewSite().getShell().setSize(260, 760);

						}

					} else {

						ref.getView(false).getViewSite().getShell().setSize(260, 760);
					}
					p = CanvasView.getCanvas_view().tabFolder.getShell().getLocation();
					if (p != null) {
						Point pSize = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell().getSize();
						if (pSize != null) {
							ref.getView(false).getViewSite().getShell().setLocation((p.x + pSize.x) - 230, p.y + 30);
						} else {
							ref.getView(false).getViewSite().getShell().setLocation(p.x, p.y);
						}
					} else {
						ref.getView(false).getViewSite().getShell().setLocation(10, 50);
					}
					//

				} catch (PartInitException e1) {

					e1.printStackTrace();
				}

			}

			public void widgetDefaultSelected(SelectionEvent e) {

			}
		});

		return fMenu;
	}

	public void dispose() {

	}

	public Menu getMenu(Menu parent) {

		return null;
	}

	public void showtoolbar() {
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		try {
			IViewReference ref = page.showView("com.eco.bio7.ijtoolbar").getSite().getPage().findViewReference("com.eco.bio7.ijtoolbar");

			//((WorkbenchPage) page.showView("com.eco.bio7.ijtoolbar").getSite().getPage()).getActivePerspective().getPresentation().detachPart(ref);
			Util ut = new Util();
			String os = ut.getOS();
			if (os != null) {

				if (os.equals("Linux")) {

					ref.getView(false).getViewSite().getShell().setSize(570, 130);

				} else if (os.equals("Windows")) {

					ref.getView(false).getViewSite().getShell().setSize(570, 115);

				} else if (os.equals("Mac")) {

					ref.getView(false).getViewSite().getShell().setSize(570, 120);

				}

			} else {

				ref.getView(false).getViewSite().getShell().setSize(570, 120);
			}
			p = CanvasView.getCanvas_view().tabFolder.getShell().getLocation();
			if (p != null) {

				ref.getView(false).getViewSite().getShell().setLocation(p.x, p.y);
			} else {
				ref.getView(false).getViewSite().getShell().setLocation(10, 50);
			}

		} catch (PartInitException e1) {

			e1.printStackTrace();
		}

	}

}