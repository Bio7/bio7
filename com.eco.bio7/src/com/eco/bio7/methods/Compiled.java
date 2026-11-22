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

package com.eco.bio7.methods;

import com.eco.bio7.compile.Model;

/**
 * This class provides some static methods for the access of the compiled
 * classes of the Bio7 application.
 * 
 * @author Bio7
 * 
 */
public class Compiled {
	// To this class the embedded Compiler compiles the classes !

	private static Model model = null;// The main compilation unit which

	// will be instantiated.

	/**
	 * Returns a reference to the main compiled class of Bio7 which provides the
	 * run method.
	 * 
	 * @return a reference to the compiled class.
	 */
	public static Model getModel() {
		return model;
	}

	/**
	 * Set the main class.
	 * @param ecoclass the Model object.
	 * 
	 */
	public static void setModel(Model ecoclass) {
		boolean newModel = ecoclass != null && ecoclass != model;
		Compiled.model = ecoclass;
		if (newModel) {
			/* A new model: the simulation is paused and the counter of the time steps starts at 0 */
			com.eco.bio7.time.Time.setPause(true);
			com.eco.bio7.time.Time.Timereset();
			com.eco.bio7.time.Time.setCounter(0);
			try {
				org.eclipse.swt.widgets.Display display = org.eclipse.ui.PlatformUI.getWorkbench().getDisplay();
				display.asyncExec(() -> {
					if (com.eco.bio7.rcp.ApplicationActionBarAdvisor.getUserItem() != null) {
						com.eco.bio7.rcp.ApplicationActionBarAdvisor.getUserItem().setText("Time steps: " + com.eco.bio7.time.Time.getCounter());
					}
				});
			} catch (RuntimeException | LinkageError e) {
				/* No workbench (e.g., a test) */
			}
		}
	}

}
