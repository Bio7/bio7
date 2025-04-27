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

import java.util.UUID;
import java.util.Vector;

import javax.swing.JPanel;

import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MPartSashContainerElement;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import com.eco.bio7.image.Activator;
import com.eco.bio7.image.CanvasView;
import com.eco.bio7.image.CustomDetachedImageJView;
import com.eco.bio7.image.IJTabs;
import com.eco.bio7.image.Util;

import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.ImageWindow;

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
						CTabFolder ctab = CanvasView.getCanvas_view().tabFolder;
						if (ctab.getItemCount() > 0) {
							Vector ve = (Vector) ctab.getSelection().getData();
							ImagePlus plu = (ImagePlus) ve.get(0);

							ImageWindow win = (ImageWindow) ve.get(1);
							/*In the menu action we also need to set the panel! Not necessary in the
							 *CanvasView action!*/
							// Wrap to avoid deadlock of awt frame access!
							java.awt.EventQueue.invokeLater(new Runnable() {
								public void run() {
									WindowManager.setTempCurrentImage(plu);
									WindowManager.setCurrentWindow(win);
								}
							});

							/* import to set current Panel for the menu action (as if we select a tab)! */
							CanvasView.setCurrent((JPanel) ve.get(2));

							// JPanel current = (JPanel) ve.get(2);

							CustomDetachedImageJView custom = new CustomDetachedImageJView();
							/* Create ImageJ view with unique ID! */
							//String id = UUID.randomUUID().toString();
							/* Create ImageJ view with unique image ID of the ImagePlus image! */
							String id = Integer.toString(plu.getID());
							// detachedSecViewIDs.add(id);
							custom.setPanel(CanvasView.getCurrent(), id, plu.getTitle());
							custom.setData(plu, win);
							/*
							 * Only hide the tab without to close the ImagePlus object!
							 */
							IJTabs.hideTab();
						}

					}

					public void widgetDefaultSelected(SelectionEvent e) {

					}
				});
				MenuItem menuItemDetachAllTab = new MenuItem(fMenu, SWT.PUSH);

				menuItemDetachAllTab.setText("Open All Images As Views ");

				menuItemDetachAllTab.addSelectionListener(new SelectionListener() {

					public void widgetSelected(SelectionEvent e) {
						CTabFolder ctab = CanvasView.getCanvas_view().tabFolder;
						int itemCount = ctab.getItemCount();
						if (itemCount > 0) {
							IPreferenceStore store = Activator.getDefault().getPreferenceStore();
							boolean detWindow = store.getBoolean("ENABLE_DETACHED_VIEW_WINDOWS");
							int xPosDefault = store.getInt("DETACHED_IMAGE_POSITION_X");
							int yPosDefault = store.getInt("DETACHED_IMAGE_POSITION_Y");
							int xWidth = store.getInt("DETACHED_IMAGE_WIDTH");
							int yHeight = store.getInt("DETACHED_IMAGE_HEIGHT");
							int xSpacing = store.getInt("DETACHED_IMAGE_SPACING_X");
							int ySpacing = store.getInt("DETACHED_IMAGE_SPACING_Y");
							int xMaxSpace = store.getInt("DETACHED_IMAGE_SPACING_X_MAX");
							int yMaxSpace = store.getInt("DETACHED_IMAGE_SPACING_Y_MAX");

							int xPos = xPosDefault;
							int yPos = yPosDefault;
                            String tempId = "";
							for (int i = 0; i < itemCount; i++) {

								IJTabs.setActive(i);

								Vector ve = (Vector) ctab.getSelection().getData();
								ImagePlus plu = (ImagePlus) ve.get(0);

								ImageWindow win = (ImageWindow) ve.get(1);
								/*In the menu action we also need to set the panel! Not necessary in the
								 *CanvasView action!*/
								// Wrap to avoid deadlock of awt frame access!
								java.awt.EventQueue.invokeLater(new Runnable() {
									public void run() {
										WindowManager.setTempCurrentImage(plu);
										WindowManager.setCurrentWindow(win);
									}
								});

								/* import to set current Panel for the menu action (as if we select a tab)! */
								CanvasView.setCurrent((JPanel) ve.get(2));

								// JPanel current = (JPanel) ve.get(2);

								CustomDetachedImageJView custom = new CustomDetachedImageJView();
								/* Create ImageJ view with unique ID! */
								//String id = UUID.randomUUID().toString();
								/* Create ImageJ view with unique image ID of the ImagePlus image! */
								String id = Integer.toString(plu.getID());
								tempId=id;//for the last detached view which nedds to be activated!
								// detachedSecViewIDs.add(id);
								if (detWindow) {
									custom.setPanelFloatingDetached(CanvasView.getCurrent(), id, plu.getTitle(),
											new Rectangle(xPos, yPos, xWidth, yHeight));
									custom.setData(plu, win);
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
								} else {
									custom.setPanel(CanvasView.getCurrent(), id, plu.getTitle());
									custom.setData(plu, win);
								}
								

								/*
								 * Only hide the tab without to close the ImagePlus object!
								 */
								IJTabs.hideTab();
							}
							/*Import for Windows to activate the detached views for the close event!*/
							IJTabs.setSecondaryViewShellLocAndSize(tempId,null);
							//SIJTabs.doSecondaryViewLayout();

							/*for (int i = 0; i < itemCount; i++) {
								IJTabs.setActive(i);
								IJTabs.hideTab();
							}*/
						}

					}

					public void widgetDefaultSelected(SelectionEvent e) {

					}
				});

				MenuItem[] menuItems = fMenu.getItems();
				int defaultMenuEntries = 4;
				/*If we use the plugin in the Bio7 RCP!*/
				if (bio7RCP) {
					defaultMenuEntries = 5;
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

	public static void openDetachedView() {
		Display dis = CanvasView.getParent2().getDisplay();
		dis.syncExec(new Runnable() {
			public void run() {
				CTabFolder ctab = CanvasView.getCanvas_view().tabFolder;
				int itemCount = ctab.getItemCount();
				if (itemCount > 0) {
					IPreferenceStore store = Activator.getDefault().getPreferenceStore();
					boolean detWindow = store.getBoolean("ENABLE_DETACHED_VIEW_WINDOWS");
					int xPosDefault = store.getInt("DETACHED_IMAGE_POSITION_X");
					int yPosDefault = store.getInt("DETACHED_IMAGE_POSITION_Y");
					int xWidth = store.getInt("DETACHED_IMAGE_WIDTH");
					int yHeight = store.getInt("DETACHED_IMAGE_HEIGHT");
					int xSpacing = store.getInt("DETACHED_IMAGE_SPACING_X");
					int ySpacing = store.getInt("DETACHED_IMAGE_SPACING_Y");
					int xMaxSpace = store.getInt("DETACHED_IMAGE_SPACING_X_MAX");
					int yMaxSpace = store.getInt("DETACHED_IMAGE_SPACING_Y_MAX");

					int xPos = xPosDefault;
					int yPos = yPosDefault;

					for (int i = 0; i < itemCount; i++) {

						IJTabs.setActive(i);

						Vector ve = (Vector) ctab.getSelection().getData();
						ImagePlus plu = (ImagePlus) ve.get(0);

						ImageWindow win = (ImageWindow) ve.get(1);
						/*In the menu action we also need to set the panel! Not necessary in the
						 *CanvasView action!*/
						// Wrap to avoid deadlock of awt frame access!
						java.awt.EventQueue.invokeLater(new Runnable() {
							public void run() {
								WindowManager.setTempCurrentImage(plu);
								WindowManager.setCurrentWindow(win);
							}
						});

						/* import to set current Panel for the menu action (as if we select a tab)! */
						CanvasView.setCurrent((JPanel) ve.get(2));

						// JPanel current = (JPanel) ve.get(2);

						CustomDetachedImageJView custom = new CustomDetachedImageJView();
						/* Create ImageJ view with unique ID! */
						//String id = UUID.randomUUID().toString();
						/* Create ImageJ view with unique image ID of the ImagePlus image! */
						String id = Integer.toString(plu.getID());
						// detachedSecViewIDs.add(id);
						if (detWindow) {
							custom.setPanelFloatingDetached(CanvasView.getCurrent(), id, plu.getTitle(),
									new Rectangle(xPos, yPos, xWidth, yHeight));
							custom.setData(plu, win);
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
						} else {
							custom.setPanel(CanvasView.getCurrent(), id, plu.getTitle());
							custom.setData(plu, win);
						}

						/*
						 * Only hide the tab without to close the ImagePlus object!
						 */
						IJTabs.hideTab();
					}
					//SIJTabs.doSecondaryViewLayout();

					/*for (int i = 0; i < itemCount; i++) {
						IJTabs.setActive(i);
						IJTabs.hideTab();
					}*/
				}


				/*
				 * Only hide the tab without to close the ImagePlus object!
				 */

			}
		});
		
		
	}
}