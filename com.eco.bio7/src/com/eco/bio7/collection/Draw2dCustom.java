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

package com.eco.bio7.collection;

import java.util.Vector;
import org.eclipse.draw2d.LightweightSystem;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Display;

public class Draw2dCustom {

	private Vector ve = null;

	private LightweightSystem lws = null;

	private CustomView view;

	public Draw2dCustom(CustomView view) {
		this.view = view;
	}

	public LightweightSystem addTab(final String title) {

		Display dis = view.getCustomViewParent().getDisplay();
		dis.syncExec(new Runnable() {
			public void run() {

				view.getCustomViewParent().setData(ve);
				Canvas canvas = new Canvas(view.getCustomViewParent(), 0);

				lws = new LightweightSystem(canvas);
				view.getCustomViewParent().layout();

			}
		});
		return lws;

	}

}
