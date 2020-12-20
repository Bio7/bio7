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