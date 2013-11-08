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

package com.eco.bio7.collection;

import java.util.Vector;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

public class SwtCustom {

	private Composite top;

	

	private Vector ve = null;



	private CustomView view;

	public SwtCustom(CustomView view) {
		 this.view=view;
	}

	public Composite addTab(final String title) {
		
		Display dis = view.getCustomViewParent().getDisplay();
		dis.syncExec(new Runnable() {
			public void run() {

				

				top = new Composite(view.getCustomViewParent(), SWT.NORMAL);

				

				view.getCustomViewParent().setData(ve);

				view.getCustomViewParent().layout();

			}
		});
		return top;

	}

}
