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

import java.lang.reflect.InvocationTargetException;

import javax.swing.SwingUtilities;

import com.eco.bio7.plot.LineChart;

public class ThreadLineChart extends Thread {

	private final LineChart graph;

	public ThreadLineChart(LineChart grap) {
		this.graph = grap;
	}

	public void run() {

		while (true) {
			try {

				sleep(1000);

			} catch (InterruptedException e) {
			}

			if (!Time.isPause()) {
				try {
					SwingUtilities.invokeAndWait(new Runnable() {

						public void run() {
							graph.datenin();
						}
					});
				} catch (InterruptedException e) {

					e.printStackTrace();
				} catch (InvocationTargetException e) {

					e.printStackTrace();
				}
			}
		}
	}
}