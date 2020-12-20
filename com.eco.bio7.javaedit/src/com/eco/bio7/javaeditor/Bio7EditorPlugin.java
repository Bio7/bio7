/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     
 *     M.Austenfeld - Minor changes for the Bio7 application.
 *******************************************************************************/
package com.eco.bio7.javaeditor;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;

public class Bio7EditorPlugin extends AbstractUIPlugin {

	public final static String JAVA_PARTITIONING = "__java_example_partitioning"; //$NON-NLS-1$

	private static Bio7EditorPlugin fgInstance;

	/**
	 * Creates a new plug-in instance.
	 */
	public Bio7EditorPlugin() {
		fgInstance = this;

	}

	/**
	 * Returns the default plug-in instance.
	 * 
	 * @return the default plug-in instance
	 */
	public static Bio7EditorPlugin getDefault() {
		return fgInstance;
	}

	/**
	 * Returns the singleton Javadoc scanner.
	 * 
	 * @return the singleton Javadoc scanner
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return AbstractUIPlugin.imageDescriptorFromPlugin("com.eco.bio7.javaedit", path);
	}
}
