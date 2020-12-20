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
 *     
 *     M.Austenfeld - Minor changes for the Bio7 application.
 *******************************************************************************/
package com.eco.bio7.editor;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import com.eco.bio7.editors.ScriptCodeScanner;
import com.eco.bio7.editors.ScriptColorProvider;
import com.eco.bio7.editors.ScriptPartitionScanner;

/**
 * The script editor plug-in class.
 * 
 * @since 3.0
 */
public class BeanshellEditorPlugin extends AbstractUIPlugin {

	public final static String SCRIPT_PARTITIONING = "__script_example_partitioning"; //$NON-NLS-1$

	private static BeanshellEditorPlugin fgInstance;

	private ScriptColorProvider fColorProvider;
	private ScriptCodeScanner fCodeScanner;

	private ScriptPartitionScanner fPartitionScanner;

	/**
	 * Creates a new plug-in instance.
	 */
	public BeanshellEditorPlugin() {
		fgInstance = this;
	}

	/**
	 * Returns the default plug-in instance.
	 * 
	 * @return the default plug-in instance
	 */
	public static BeanshellEditorPlugin getDefault() {
		return fgInstance;
	}

	public ScriptPartitionScanner getScriptPartitionScanner() {
		if (fPartitionScanner == null)
			fPartitionScanner = new ScriptPartitionScanner();
		return fPartitionScanner;
	}

	public RuleBasedScanner getScriptCodeScanner() {
		if (fCodeScanner == null)
			fCodeScanner = new ScriptCodeScanner(getScriptColorProvider());
		return fCodeScanner;
	}

	/**
	 * Returns the singleton Script color provider.
	 * 
	 * @return the singleton Script color provider
	 */
	public ScriptColorProvider getScriptColorProvider() {
		if (fColorProvider == null)
			fColorProvider = new ScriptColorProvider();
		return fColorProvider;
	}

	public static ImageDescriptor getImageDescriptor(String path) {
		return AbstractUIPlugin.imageDescriptorFromPlugin("com.eco.bio7.edit", path);
	}
}
