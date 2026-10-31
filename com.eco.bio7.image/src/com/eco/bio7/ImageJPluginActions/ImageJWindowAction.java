/*******************************************************************************
 * Copyright (c) 2005-2017 M. Austenfeld
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/

package com.eco.bio7.ImageJPluginActions;



import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MPartSashContainerElement;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import com.eco.bio7.image.ui.Activator;
import com.eco.bio7.image.ui.CanvasView;
import com.eco.bio7.image.ui.CustomDetachedImageJView;
import com.eco.bio7.image.ui.Fullscreen;
import com.eco.bio7.image.ui.IJTabs;
import com.eco.bio7.image.ui.Util;

import ij.ImagePlus;
import ij.WindowManager;

public class ImageJWindowAction extends Action implements IMenuCreator {

	private Menu fMenu;

	protected Point p;

	private MenuItem menuItemFx;

	private boolean selected;

	private static boolean bio7RCP = false;

	public static void setBio7RCP(boolean bio7rcp) {
		bio7RCP = bio7rcp;
	}

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

		fMenu.addMenuListener(new MenuListener() {
			public void menuHidden(MenuEvent e) {

			}

			@Override
			public void menuShown(MenuEvent e) {

				MenuItem menuItem = new MenuItem(fMenu, SWT.PUSH);

				menuItem.setText("ImageJ-Toolbar");

				menuItem.addSelectionListener(new SelectionListener() {

					public void widgetSelected(SelectionEvent e) {

						showtoolbar();

					}

					public void widgetDefaultSelected(SelectionEvent e) {

					}
				});

				if (bio7RCP) {

					MenuItem menuItem1 = new MenuItem(fMenu, SWT.PUSH);

					menuItem1.setText("Bio7-Toolbar");
					menuItem1.addSelectionListener(new SelectionListener() {

						public void widgetSelected(SelectionEvent e) {

							IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();

							IViewReference ref = null;
							IWorkbenchPartSite part = null;

							IPreferenceStore store = Activator.getDefault().getPreferenceStore();
							int xSize = store.getInt("IMAGE_METHODS_SIZE_X");
							int ySize = store.getInt("IMAGE_METHODS_SIZE_Y");

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

										s.detach(p, (pt.x + pSize.x / 2), pt.y + 30, xSize, ySize);
									}

								} else if (os.equals("Windows")) {
									if (p.getCurSharedRef() != null) {
										p = p.getCurSharedRef();

										s.detach(p, (pt.x + pSize.x / 2), pt.y + 30, xSize, ySize);
									}

								} else if (os.equals("Mac")) {
									if (p.getCurSharedRef() != null) {
										p = p.getCurSharedRef();

										s.detach(p, (pt.x + pSize.x / 2), pt.y + 30, xSize, ySize);
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
				}
				new MenuItem(fMenu, SWT.SEPARATOR);

				MenuItem menuItemDetachTab = new MenuItem(fMenu, SWT.PUSH);

				menuItemDetachTab.setText("Open Current Image as View");

				menuItemDetachTab.addSelectionListener(new SelectionListener() {
					public void widgetSelected(SelectionEvent e) {
						ImagePlus plu = IJTabs.getSelectedImagePlus();
						if (plu != null) {
							CustomDetachedImageJView.detach(plu, null);
						}
					}

					public void widgetDefaultSelected(SelectionEvent e) {

					}
				});
				MenuItem menuItemDetachAllTab = new MenuItem(fMenu, SWT.PUSH);

				menuItemDetachAllTab.setText("Open All Images As Views ");

				menuItemDetachAllTab.addSelectionListener(new SelectionListener() {
					public void widgetSelected(SelectionEvent e) {
						openDetachedView();
					}

					public void widgetDefaultSelected(SelectionEvent e) {

					}
				});
				/* Also without the F2 key (e.g., if the focus is not on the image)! */
				MenuItem menuItemFullscreen = new MenuItem(fMenu, SWT.PUSH);
				menuItemFullscreen.setText("Fullscreen Image (F2)");
				menuItemFullscreen.addSelectionListener(new SelectionListener() {
					public void widgetSelected(SelectionEvent e) {
						ImagePlus plu = IJTabs.getSelectedImagePlus();
						if (plu == null) {
							plu = WindowManager.getCurrentImage();
						}
						if (plu != null) {
							/* On the primary monitor like F2! */
							org.eclipse.swt.widgets.Monitor[] monitors = Fullscreen.getMonitors(e.display);
							Fullscreen.enter(plu, monitors.length > 0 ? monitors[0] : null);
						}
					}

					public void widgetDefaultSelected(SelectionEvent e) {

					}
				});
				/* New images are opened as views (see Bio7ImageJ.imageOpened)! */
				MenuItem menuItemImagesAsViews = new MenuItem(fMenu, SWT.CHECK);
				menuItemImagesAsViews.setText(IMAGES_AS_VIEWS_TEXT);
				menuItemImagesAsViews.addSelectionListener(new SelectionListener() {
					public void widgetSelected(SelectionEvent e) {
						Activator.getDefault().getPreferenceStore().setValue(OPEN_IMAGES_AS_VIEWS, menuItemImagesAsViews.getSelection());
					}

					public void widgetDefaultSelected(SelectionEvent e) {

					}
				});
				MenuItem[] menuItems = fMenu.getItems();
				int defaultMenuEntries = 6;
				/*If we use the plugin in the Bio7 RCP!*/
				if (bio7RCP) {
					defaultMenuEntries = 7;
				}
				/* The kept check item shows the current preference (e.g., changed in the preferences)! */
				for (int i = 0; i < defaultMenuEntries && i < menuItems.length; i++) {
					if ((menuItems[i].getStyle() & SWT.CHECK) != 0 && IMAGES_AS_VIEWS_TEXT.equals(menuItems[i].getText())) {
						menuItems[i].setSelection(isOpenImagesAsViews());
					}
				}
				// Only delete the plugins menu items and menus (seperator menu items counting, too!)!
				for (int i = defaultMenuEntries; i < menuItems.length; i++) {
					if (menuItems[i] != null) {
						menuItems[i].dispose();
					}
				}
				// new MenuItem(fMenu, SWT.SEPARATOR);
				new ImageJSubmenu().addSubMenus(fMenu, "Window");

			}
		});
		// new MenuItem(fMenu, SWT.SEPARATOR);
		/*
		 * Create the dynamic menu which is later disposed and recreated every time the
		 * menu is shown!
		 */
		// menuItemFx = new MenuItem(fMenu, SWT.CHECK);

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

		/* Preferences form the Bio7Plugin! */
		// IPreferencesService service = Platform.getPreferencesService();
		// int xSize = service.getInt("com.eco.bio7", "IMAGEJ_TOOLBAR_SIZE_X",
		// 600, null);
		// int ySize = service.getInt("com.eco.bio7", "IMAGEJ_TOOLBAR_SIZE_Y",
		// 135, null);

		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		int xSize = store.getInt("IMAGEJ_TOOLBAR_SIZE_X");
		int ySize = store.getInt("IMAGEJ_TOOLBAR_SIZE_Y");

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

	/** The preference to open new images as views by default. */
	public static final String OPEN_IMAGES_AS_VIEWS = "OPEN_IMAGES_AS_VIEWS";

	private static final String IMAGES_AS_VIEWS_TEXT = "Open Images As Views By Default";

	/** Returns true if new images are opened as views by default. */
	public static boolean isOpenImagesAsViews() {
		return Activator.getDefault().getPreferenceStore().getBoolean(OPEN_IMAGES_AS_VIEWS);
	}

	/**
	 * Opens the image of the ImageJ-Canvas view in a view (see the preferences),
	 * placed after the already opened image views. Must be called in the SWT UI
	 * thread.
	 */
	public static void openAsView(ImagePlus imp) {
		if (!Activator.getDefault().getPreferenceStore().getBoolean("ENABLE_DETACHED_VIEW_WINDOWS")) {
			CustomDetachedImageJView.detach(imp, null);
			return;
		}
		int openViews = 0;
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		IWorkbenchPage page = window != null ? window.getActivePage() : null;
		if (page != null) {
			for (IViewReference ref : page.getViewReferences()) {
				if (CustomDetachedImageJView.ID.equals(ref.getId())) {
					openViews++;
				}
			}
		}
		CustomDetachedImageJView.detach(imp, floatingBounds(openViews));
	}

	/* The location and size of the n-th floating image view (see the preferences)! */
	private static Rectangle floatingBounds(int index) {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		int xPosDefault = store.getInt("DETACHED_IMAGE_POSITION_X");
		int yPosDefault = store.getInt("DETACHED_IMAGE_POSITION_Y");
		int xSpacing = store.getInt("DETACHED_IMAGE_SPACING_X");
		int ySpacing = store.getInt("DETACHED_IMAGE_SPACING_Y");
		int xMaxSpace = store.getInt("DETACHED_IMAGE_SPACING_X_MAX");
		int yMaxSpace = store.getInt("DETACHED_IMAGE_SPACING_Y_MAX");
		int xPos = xPosDefault;
		int yPos = yPosDefault;
		for (int i = 0; i < index; i++) {
			if (xMaxSpace > 0) {
				if ((xPos + xSpacing) > xMaxSpace) {
					xPos = xPosDefault;
					yPos = yPos + ySpacing;
				} else {
					xPos = xPos + xSpacing;
				}
			}
			if (yMaxSpace > 0) {
				if ((yPos + ySpacing) > yMaxSpace) {
					yPos = yPosDefault;
					xPos = xPos + xSpacing;
				} else {
					yPos = yPos + ySpacing;
				}
			}
		}
		return new Rectangle(xPos, yPos, store.getInt("DETACHED_IMAGE_WIDTH"), store.getInt("DETACHED_IMAGE_HEIGHT"));
	}

	/**
	 * Opens all images of the ImageJ-Canvas view in detached views.
	 */
	public static void openDetachedView() {
		boolean detWindow = Activator.getDefault().getPreferenceStore().getBoolean("ENABLE_DETACHED_VIEW_WINDOWS");
		int index = 0;
		for (ImagePlus plu : IJTabs.getImagePlusList()) {
			CustomDetachedImageJView.detach(plu, detWindow ? floatingBounds(index++) : null);
		}
	}
}