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

package com.eco.bio7.jobs;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.preference.IPreferenceStore;
import com.eco.bio7.discrete.Field;
import com.eco.bio7.discrete.Quad2d;
import com.eco.bio7.javaeditor.Bio7EditorPlugin;
import com.eco.bio7.methods.CurrentStates;
import com.thoughtworks.xstream.XStream;

public class SaveWorkspaceJob extends WorkspaceJob {

	private String source;
	private String javaSource;
	private String fileName;

	public SaveWorkspaceJob(String source, String javaSource, String fileName) {
		super("Save Pattern");

		this.source = source;
		this.javaSource = javaSource;
		this.fileName = fileName;
	}

	public IStatus runInWorkspace(IProgressMonitor monitor) {
		monitor.beginTask("Save Pattern...", Field.getHeight());
		/*
		 * Storing the relevant data for the grid in XML defined in class
		 * "DataDescriptorGrids"!
		 */
		XStream xstream = new XStream();
		xstream.allowTypesByRegExp(new String[] { ".*" });
		DataDescriptorGrids grid = new DataDescriptorGrids();
		Quad2d quad = Quad2d.getQuad2dInstance();

		if (quad != null) {
			grid.setFieldSizeX(Field.getWidth());
			grid.setFieldSizeY(Field.getHeight());
			ArrayList<String> sp = CurrentStates.getStateList();
			grid.setStateNames(sp);
			grid.setColorR(CurrentStates.getR());
			grid.setColorG(CurrentStates.getG());
			grid.setColorB(CurrentStates.getB());
			grid.setDescriptions(CurrentStates.getStateDescriptions());
			/* A reference */
			grid.setStates(Field.getStateArray());
			//IPreferenceStore storeJava = Bio7EditorPlugin.getDefault().getPreferenceStore();
		}
		grid.setClassBody(false);
		/* Save java source! */

		if (javaSource != null) {
			grid.setSource(javaSource);
		} else {
			grid.setSource(null);
		}
		/* The file name is required if a regular Java class is stored! */
		if (fileName != null) {
			grid.setFileName(fileName);
		} else {
			grid.setFileName(null);
		}
		FileWriter fs = null;
		try {
			fs = new FileWriter(source);
		} catch (IOException e) {

			e.printStackTrace();
		}
		xstream.toXML(grid, fs);

		return Status.OK_STATUS;
	}

}
