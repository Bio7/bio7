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

	private static volatile boolean busy = false;

	/* The state of an R which evaluates on its own (e.g., the console of embedded R) */
	private static volatile java.util.function.BooleanSupplier externalBusy;

	private static RSession rSession = null;

	/**
	 * A method to detect if the R interpreter is busy.
	 *
	 * @return a boolean value.
	 */
	public static boolean isBusy() {
		if (busy) {
			return true;
		}
		java.util.function.BooleanSupplier ext = externalBusy;
		return ext != null && ext.getAsBoolean();
	}

	/**
	 * Sets a supplier which reports if R is busy outside of setBusy (e.g., embedded
	 * R evaluating console input), or null.
	 */
	public static void setExternalBusy(java.util.function.BooleanSupplier supplier) {
		externalBusy = supplier;
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
	 * @param rsession an RSession.
	 */
	public static void setRSession(RSession rsession) {
		rSession = rsession;
	}

}
