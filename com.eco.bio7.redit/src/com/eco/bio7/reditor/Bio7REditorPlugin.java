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
 *     M.Austenfeld - Changes for the Bio7 application.
 *******************************************************************************/
package com.eco.bio7.reditor;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import com.eco.bio7.reditors.RCodeScanner;
import com.eco.bio7.reditors.RColorProvider;


import com.eco.bio7.reditors.RPartitionScanner;

public class Bio7REditorPlugin extends AbstractUIPlugin {

	public final static String R_PARTITIONING = "__R_example_partitioning"; 

	private static Bio7REditorPlugin fgInstance;

	private RColorProvider fColorProvider;

	private RCodeScanner fCodeScanner;

	private RPartitionScanner fPartitionScanner;
	
	//private RPartitionStringCodeScanner rPartitionStringCodeScanner;

	public Bio7REditorPlugin() {
		fgInstance = this;
	}

	public static Bio7REditorPlugin getDefault() {
		return fgInstance;
	}

	public RPartitionScanner getRPartitionScanner() {
		if (fPartitionScanner == null)
			fPartitionScanner = new RPartitionScanner();
		return fPartitionScanner;
	}
	

	public RuleBasedScanner getRCodeScanner() {
		if (fCodeScanner == null)
			fCodeScanner = new RCodeScanner(getRColorProvider());
		return fCodeScanner;
	}

	public RColorProvider getRColorProvider() {
		if (fColorProvider == null)
			fColorProvider = new RColorProvider();
		return fColorProvider;
	}
	public static ImageDescriptor getImageDescriptor(String path) {
		return AbstractUIPlugin.imageDescriptorFromPlugin("com.eco.bio7.redit", path);
	}

}
