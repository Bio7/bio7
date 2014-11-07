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

package com.eco.bio7.image;

import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.ImageWindow;

import java.util.Vector;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.widgets.Display;

/**
 * A class for the control of the ImageJ tabs.
 * 
 * @author Bio7
 * 
 */
public class IJTabs {

	/**
	 * Activates the tab with the specified number.
	 * 
	 * @param number
	 *            the number of the tab.
	 */
	public static void setActive(final int number) {
		final CTabItem[] items = CanvasView.getCanvas_view().tabFolder.getItems();

		if (items.length > 0 && number < items.length) {

			Display dis = CanvasView.getParent2().getDisplay();
			dis.syncExec(new Runnable() {

				public void run() {

					CanvasView.tabFolder.showItem(items[number]);
					CanvasView.tabFolder.setSelection(items[number]);
					Vector ve = (Vector) items[number].getData();
					SwingUtilities.invokeLater(new Runnable() {
						// !!
						public void run() {
							ImagePlus plu = (ImagePlus) ve.get(0);
							ImageWindow win = (ImageWindow) ve.get(1);
							WindowManager.setTempCurrentImage(plu);
							WindowManager.setCurrentWindow(win);
							CanvasView.setCurrent((JPanel) ve.get(2));
						}
					});

				}
			});

		}

	}

	/**
	 * Returns the amount of opened tabs in the ImageJ view.
	 * 
	 * @return the amount of tabs as an integer value.
	 */
	public static int getAmountTabs() {
		final CTabItem[] items = CanvasView.getCanvas_view().tabFolder.getItems();

		return items.length;

	}

	/**
	 * Closes all tabs in the ImageJ view.
	 */
	public static void deleteAllTabs() {
		final CTabItem[] items = CanvasView.getCanvas_view().tabFolder.getItems();

		for (int i = 0; i < items.length; i++) {
			final int tabcount = i;

			Display dis = CanvasView.getParent2().getDisplay();
			dis.syncExec(new Runnable() {

				public void run() {

					items[tabcount].dispose();
				}
			});

		}
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				WindowManager.closeAllWindows();
			}
		});
	}

	/**
	 * Closes the tab with the specified number in the ImageJ view.
	 * 
	 * @param number
	 *            the number of the tab as an integer value.
	 */
	public static void deleteTab(int number) {
		final int nrdel = number;
		final CTabItem[] items = CanvasView.getCanvas_view().tabFolder.getItems();
		Display dis = CanvasView.getParent2().getDisplay();
		dis.syncExec(new Runnable() {

			public void run() {
				Vector ve = (Vector) items[nrdel].getData();
				SwingUtilities.invokeLater(new Runnable() {
					// !!
					public void run() {
						ImagePlus plu = (ImagePlus) ve.get(0);

						final ImageWindow win = (ImageWindow) ve.get(1);

						win.bio7TabClose();
					}
				});
				items[nrdel].dispose();
			}
		});

	}

	/**
	 * Hides the active tab in the ImageJ view.
	 */
	public static void hideTab() {

		final CTabItem item = CanvasView.tabFolder.getSelection();
		Display dis = CanvasView.getParent2().getDisplay();
		dis.syncExec(new Runnable() {

			public void run() {

				item.dispose();
			}
		});

	}

	/**
	 * Hides the tab with the specified number in the ImageJ view.
	 */
	public static void hideTabNumber(int number) {

		final int nrdel = number;
		final CTabItem[] items = CanvasView.getCanvas_view().tabFolder.getItems();
		Display dis = CanvasView.getParent2().getDisplay();
		dis.syncExec(new Runnable() {

			public void run() {

				items[nrdel].dispose();
			}
		});

	}

	/**
	 * Deletes the active tab in the ImageJ view.
	 */
	public static void deleteActiveTab() {

		final CTabItem item = CanvasView.tabFolder.getSelection();
		Display dis = CanvasView.getParent2().getDisplay();
		dis.syncExec(new Runnable() {

			public void run() {
				Vector ve = (Vector) item.getData();

				SwingUtilities.invokeLater(new Runnable() {
					// !!
					public void run() {

						final ImageWindow win = (ImageWindow) ve.get(1);

						win.bio7TabClose();
					}
				});
				item.dispose();
			}
		});

	}

}
