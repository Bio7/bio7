package com.eco.bio7.ImageJPluginActions;

import java.awt.CheckboxMenuItem;
import java.awt.MenuItem;
import java.awt.Point;
import java.awt.PopupMenu;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Menu;

import com.eco.bio7.image.Util;
import ij.Executer;
import ij.ImagePlus;
import ij.Menus;
import ij.gui.ImageCanvas;
import ij.gui.PlotWindow;
import ij.gui.Toolbar;
import ij.plugin.frame.RoiManager;

public class ImageJContextMenuAction {

	String delay = Messages.LinuxContextMenuDelay;
	protected Menu menu;

	/*Changed for Bio7! SWT Popup Menu!*/
	public void swtPopupMenu(ActionEvent e, PopupMenu popUp, Point p, Object fromInstance) {

		swtPopupMenu(popUp, p, fromInstance, e);
	}

	/*Changed for Bio7! SWT Popup Menu!*/
	public void swtPopupMenu(MouseEvent e, PopupMenu popUp, Object fromInstance) {
		Point p = e.getLocationOnScreen();
		swtPopupMenu(popUp, p, fromInstance, null);
	}

	/*Changed for Bio7! SWT Popup Menu!*/
	public void swtPopupMenu(PopupMenu popUp, Point p, Object from, ActionEvent awtEvent) {
		// Point p = e.getLocationOnScreen();
		int count = popUp.getItemCount();
		Util.getDisplay().asyncExec(new Runnable() {
			public void run() {
				if (menu != null) {
					menu.dispose();
				}
				menu = new org.eclipse.swt.widgets.Menu(Util.getShell(), SWT.POP_UP);
				menu.addMenuListener(new MenuListener() {
					public void menuHidden(MenuEvent e) {

					}

					public void menuShown(MenuEvent e) {

						/*org.eclipse.swt.widgets.MenuItem[] menuItems = menu.getItems();
						for (int i = 0; i < menuItems.length; i++) {
							if (menuItems[i] != null) {
								menuItems[i].dispose();
							}
						}*/

						for (int i = 0; i < count; i++) {
							MenuItem switchPopupItem = popUp.getItem(i);
							String label = switchPopupItem.getLabel();
							/* If we have a menu seperator! */
							if (label.equals("-")) { //$NON-NLS-1$
								new org.eclipse.swt.widgets.MenuItem(menu, SWT.SEPARATOR);
								/* If we have a menu item! */
							} else if (!(label.equals("-"))) { //$NON-NLS-1$
								if (switchPopupItem instanceof CheckboxMenuItem) {
									final org.eclipse.swt.widgets.MenuItem item = new org.eclipse.swt.widgets.MenuItem(
											menu, SWT.CHECK);
									CheckboxMenuItem checkBoxItem = (CheckboxMenuItem) switchPopupItem;
									item.setSelection(checkBoxItem.getState());
									item.setText(label);
									item.addSelectionListener(new SelectionListener() {
										public void widgetSelected(SelectionEvent e) {
											if (from instanceof Toolbar) {
												((Toolbar) from).itemEvent(checkBoxItem);
											}
											if (checkBoxItem.getState()) {
												item.setSelection(true);
											} else {
												item.setSelection(false);
											}

										}

										public void widgetDefaultSelected(SelectionEvent e) {

										}
									});
								} else if (switchPopupItem instanceof MenuItem) {
									final org.eclipse.swt.widgets.MenuItem item = new org.eclipse.swt.widgets.MenuItem(
											menu, SWT.NONE);
									item.setText(label);
									item.addSelectionListener(new SelectionListener() {

										public void widgetSelected(SelectionEvent e) {
											if (from instanceof RoiManager) {
												String cmd = switchPopupItem.getActionCommand();
												java.awt.EventQueue.invokeLater(new Runnable() {
													public void run() {
														((RoiManager) from).actionPerformed(cmd);
													}
												});
											} else if (from instanceof PlotWindow) {

												// String cmd = switchPopupItem.getActionCommand();
												java.awt.EventQueue.invokeLater(new Runnable() {
													public void run() {
														Object b = switchPopupItem;
														((PlotWindow) from).actionPerformed(b);
													}
												});

											} else if (from instanceof Toolbar) {
												java.awt.EventQueue.invokeLater(new Runnable() {
													public void run() {
														// Object b = switchPopupItem;
														((Toolbar) from).actionPerformed(switchPopupItem);
													}
												});
											}

											else {
												/*Extra for Bio7!*/
												/** Handle menu events. */
												String cmd = switchPopupItem.getActionCommand();

												ImagePlus imp = null;

												Object parent = Menus.getPopupMenu().getParent();
												if (parent instanceof ImageCanvas)
													imp = ((ImageCanvas) parent).getImage();
												new Executer(cmd, imp);
											}

										}

										public void widgetDefaultSelected(SelectionEvent e) {

										}
									});
								}

							}

						}

					}
				});

				menu.setLocation(p.x, p.y);
				if (Util.getOS().equals("Linux")) { //$NON-NLS-1$
					try {
						Thread.sleep(Integer.parseInt(delay));
					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					menu.setVisible(true);

				} else {
					menu.setVisible(true);
				}
			}
		});
	}

}
