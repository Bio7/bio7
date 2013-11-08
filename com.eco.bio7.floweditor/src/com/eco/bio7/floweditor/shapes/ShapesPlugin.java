/*******************************************************************************
 * Copyright (c) 2004, 2005 Elias Volanakis and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Elias Volanakis - initial API and implementation
 *******************************************************************************/
package com.eco.bio7.floweditor.shapes;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.ui.plugin.AbstractUIPlugin;

/**
 * The plugin class (singleton).
 * <p>
 * This instance can be shared between all extensions in the plugin. Information
 * shared between extensions can be persisted by using the PreferenceStore.
 * </p>
 * 
 * @see org.eclipse.ui.plugin.AbstractUIPlugin#getPreferenceStore()
 * @author Elias Volanakis
 */
public class ShapesPlugin extends AbstractUIPlugin {

	/** Single plugin instance. */
	private static ShapesPlugin singleton;

	/**
	 * Returns the shared plugin instance.
	 */
	public static ShapesPlugin getDefault() {
		return singleton;
	}

	// get the root of the plugin
	public static IWorkspace getWorkspace() {
		return ResourcesPlugin.getWorkspace();
	}

	/**
	 * The constructor.
	 */
	public ShapesPlugin() {
		if (singleton == null) {
			singleton = this;
		}
	}

}