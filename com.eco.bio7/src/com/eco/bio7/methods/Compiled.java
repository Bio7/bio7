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
	 * ecomain method.
	 * 
	 * @return a reference to the compiled class.
	 */
	public static Model getModel() {
		return model;
	}

	/**
	 * Set the main class.
	 * 
	 * 
	 */
	public static void setModel(Model ecoclass) {
		Compiled.model = ecoclass;
	}

}
