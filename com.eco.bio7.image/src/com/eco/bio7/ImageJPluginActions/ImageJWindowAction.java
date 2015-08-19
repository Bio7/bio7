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

package com.eco.bio7.ImageJPluginActions;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MPartSashContainerElement;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
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
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

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

				IViewReference ref = null;
				IWorkbenchPartSite part = null;
				
				/*Preferences form the Bio7Plugin!*/
				IPreferencesService service = Platform.getPreferencesService();
				int xSize = service.getInt("com.eco.bio7", "IMAGE_METHODS_SIZE_X", 260, null);
				int ySize = service.getInt("com.eco.bio7", "IMAGE_METHODS_SIZE_Y", 790, null);
				
				
				try {
					part = page.showView("com.eco.bio7.image_methods").getSite();
					ref = part.getPage().findViewReference("com.eco.bio7.image_methods");
				} catch (PartInitException e1) {

					e1.printStackTrace();
				}

				EModelService s = (EModelService) part.getService(EModelService.class);

				MPartSashContainerElement p = (MPart) part.getService(MPart.class);
				
				String os = Util.getOS();
				if (os != null) {
					Point pt = CanvasView.getCanvas_view().tabFolder.getShell().getLocation();
					Point pSize = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell().getSize();
					if (os.equals("Linux")) {
						if (p.getCurSharedRef() != null) {
							p = p.getCurSharedRef();

							s.detach(p, (pt.x + pSize.x/2), pt.y + 30, xSize, ySize);
						}

					} else if (os.equals("Windows")) {
						if (p.getCurSharedRef() != null) {
							p = p.getCurSharedRef();

							s.detach(p, (pt.x + pSize.x/2), pt.y + 30, xSize, ySize);						}

					} else if (os.equals("Mac")) {
						if (p.getCurSharedRef() != null) {
							p = p.getCurSharedRef();

							s.detach(p, (pt.x + pSize.x/2), pt.y + 30, xSize, ySize);
						}

					}

				} else {
					if (p.getCurSharedRef() != null) {
						p = p.getCurSharedRef();

						s.detach(p, 100, 100, xSize, ySize);
					}

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

		IViewReference ref = null;
		IWorkbenchPartSite part = null;
		
		/*Preferences form the Bio7Plugin!*/
		IPreferencesService service = Platform.getPreferencesService();
		int xSize = service.getInt("com.eco.bio7", "IMAGEJ_TOOLBAR_SIZE_X", 600, null);
		int ySize = service.getInt("com.eco.bio7", "IMAGEJ_TOOLBAR_SIZE_Y", 135, null);
		try {
			part = page.showView("com.eco.bio7.ijtoolbar").getSite();
			ref = part.getPage().findViewReference("com.eco.bio7.ijtoolbar");
		} catch (PartInitException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		EModelService s = (EModelService) part.getService(EModelService.class);

		MPartSashContainerElement p = (MPart) part.getService(MPart.class);

		Point pt = null;
		String os = Util.getOS();
		if (os != null) {
			pt = CanvasView.getCanvas_view().tabFolder.getShell().getLocation();
			if (os.equals("Linux")) {
				if (p.getCurSharedRef() != null) {
					p = p.getCurSharedRef();

					s.detach(p, pt.x + 20, pt.y + 100, xSize, ySize);
				}

			} else if (os.equals("Windows")) {
				if (p.getCurSharedRef() != null) {
					p = p.getCurSharedRef();

					s.detach(p, pt.x + 20, pt.y + 100, xSize, ySize);
				}

			} else if (os.equals("Mac")) {
				if (p.getCurSharedRef() != null) {
					p = p.getCurSharedRef();

					s.detach(p, pt.x + 20, pt.y + 100, xSize, ySize);
				}

			}

		} else {

			ref.getView(false).getViewSite().getShell().setSize(xSize, ySize);
		}
		

	}

}