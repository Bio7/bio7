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

package com.eco.bio7.rbridge;

/*import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;*/
import org.rosuda.REngine.Rserve.RSession;

//import com.eco.bio7.rbridge.RShellView;

/**
 * A class to detect the R interpreter state (if a R job is running).
 * 
 * @author Bio7
 */
public class RState {

	private static boolean busy = false;

	private static RSession rSession = null;

	/**
	 * A method to detect if the R interpreter is busy.
	 * 
	 * @return a boolean value.
	 */
	public static boolean isBusy() {
		return busy;

	}

	/**
	 * Sets the boolean value to indicate that the R interpreter is busy.
	 * 
	 * @param b
	 *            a boolean value
	 */
	public static void setBusy(boolean b) {
		busy = b;
		/*Display display = PlatformUI.getWorkbench().getDisplay();
		if (b) {
           Mark the textfield of the R console!
			display.syncExec(new Runnable() {

				public void run() {
					RShellView rsv = RShellView.getInstance();
					rsv.text.setBackground(new Color(Display.getCurrent(), 0, 0, 0));
					rsv.text.setForeground(new Color(Display.getCurrent(), 255, 255, 255));

				}
			});

		} else {
			 Mark the textfield of the R console!
			display.syncExec(new Runnable() {

				public void run() {
					RShellView rsv = RShellView.getInstance();

					rsv.text.setBackground(new Color(Display.getCurrent(), 255, 255, 255));
					rsv.text.setForeground(new Color(Display.getCurrent(), 0, 0, 0));
				}
			});

		}
*/
	}

	/**
	 * A method to detect if a R session is available.
	 * 
	 * @return a boolean value.
	 */
	public RSession getRSession() {
		return rSession;
	}

	/**
	 * Sets an RSession object.
	 * 
	 * @param b
	 *            an RSession
	 */
	public static void setRSession(RSession rsession) {
		rSession = rsession;
	}

}
