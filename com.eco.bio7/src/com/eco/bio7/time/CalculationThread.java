/*******************************************************************************
 * Copyright (c) 2007-2025 M. Austenfeld
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

package com.eco.bio7.time;

import java.awt.Graphics;
import java.lang.reflect.InvocationTargetException;
import javax.swing.SwingUtilities;
import org.eclipse.jface.action.StatusLineContributionItem;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.PlatformUI;
import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.compile.Model;
import com.eco.bio7.discrete.Hexagon;
import com.eco.bio7.discrete.Quad2d;
import com.eco.bio7.image.PointPanel;
import com.eco.bio7.info.InfoView;
import com.eco.bio7.methods.Compiled;
import com.eco.bio7.rcp.ApplicationActionBarAdvisor;

public class CalculationThread extends Thread {

	private IPreferenceStore store;

	public CalculationThread() {

		store = Bio7Plugin.getDefault().getPreferenceStore();
	}

	public void run() {

		while (true) {

			try {

				sleep(Time.getInterval());
				if (!Time.isPause()) {

					Model model = Compiled.getModel();
					if (model != null) {
						try {
							model.run();
						} catch (RuntimeException e) {

							e.printStackTrace();
							Compiled.setModel(null);
						}
					}
					try {
						if (store.getBoolean("RECORD_VALUES")) {
							Quad2d quad = Quad2d.getQuad2dInstance();
							if (quad != null) {
								quad.feldzaehler();
							}
						}
						/* Count of the states and the amount */
					} catch (RuntimeException e2) {

						e2.printStackTrace();
					}

					try {
						SwingUtilities.invokeAndWait(new Runnable() {
							// !!
							public void run() {

								if (store.getBoolean("REPAINT_QUAD")) {

									Quad2d quad = Quad2d.getQuad2dInstance();
									if (quad != null) {
										if (quad.quadviewopenend == true) {
											if (quad.activeRendering == false) {

												quad.fieldrenderer();
												quad.repaint();
											}

											else {
												Graphics g = quad.getGraphics();
												quad.malen(g);

											}
										}

									}
								}
								if (store.getBoolean("REPAINT_HEX")) {
									Hexagon h = Hexagon.getHexagonInstance();
									if (h != null) {
										if (h.hexviewopenend == true) {
											if (h.active_rendering == false) {
												h.fieldrenderer();
												h.repaint();
											}

											else {
												h.repaint();

											}

										}
									}
								}
								if (store.getBoolean("REPAINT_POINTS")) {
									PointPanel.doPaint();
								}

							}
						});
					} catch (InterruptedException e1) {

						e1.printStackTrace();
					} catch (InvocationTargetException e1) {

						e1.printStackTrace();
					}
					Time.count();
					// InfoView.getPan().repaint();
					Time.Timeupdate();

					Display display = PlatformUI.getWorkbench().getDisplay();
					display.syncExec(new Runnable() {

						public void run() {
							updateTimeDisplays();

						}

					});

				}

			} catch (InterruptedException e) {
			}

		}
	}

	private void updateTimeDisplays() {
		StatusLineContributionItem ic = ApplicationActionBarAdvisor.getUserItem();
		Label lt = InfoView.getLblTimesteps();
		Label lm = InfoView.getLblMonth();
		Label ly = InfoView.getLblYear();

		ic.setText("Time steps: " + Time.getCounter());
		if (lt != null && lt.isDisposed() == false) {
			lt.setText("Time steps: " + Time.getTime());
		}
		if (lm != null && lm.isDisposed() == false) {
			lm.setText(InfoView.getMonth() + Time.getMonth());
		}
		if (ly != null && ly.isDisposed() == false) {
			ly.setText(InfoView.getYear() + Time.getYear());
		}
	}

}