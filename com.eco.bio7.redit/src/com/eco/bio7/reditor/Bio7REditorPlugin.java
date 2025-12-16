/*********************************************************************
* Copyright (c) 2007-2026 Marcel Austenfeld
*
* This program and the accompanying materials are made
* available under the terms of the Eclipse Public License 2.0
* which is available at https://www.eclipse.org/legal/epl-2.0/
*
* SPDX-License-Identifier: EPL-2.0
**********************************************************************/
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
