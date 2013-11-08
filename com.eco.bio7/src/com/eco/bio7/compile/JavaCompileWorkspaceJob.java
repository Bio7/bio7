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

package com.eco.bio7.compile;

import java.io.IOException;
import java.io.StringReader;
import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.compiler.CompilerFactoryFactory;
import org.codehaus.commons.compiler.IClassBodyEvaluator;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;

import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.actions.Bio7Action;
import com.eco.bio7.methods.Compiled;
import com.eco.bio7.rcp.StartBio7Utils;
import com.eco.bio7.worldwind.DynamicLayer;

public class JavaCompileWorkspaceJob extends WorkspaceJob implements IJobChangeListener {

	private String source;

	public JavaCompileWorkspaceJob(String source) {
		super("Compile Java");

		this.source = source;
       
	}

	public IStatus runInWorkspace(IProgressMonitor monitor) {
		
		monitor.beginTask("Compile the java source...", IProgressMonitor.UNKNOWN);
		
		StringBuffer buffer = new StringBuffer();
		
		buffer.append(source);
		
		String script = buffer.toString();
		
		Model model = null;
		
		
		
		IClassBodyEvaluator cbe = null;
		try {
			cbe = CompilerFactoryFactory.getDefaultCompilerFactory().newClassBodyEvaluator();
			//cbe.setDebuggingInformation(true, true, true);
			cbe.setParentClassLoader(Bio7Plugin.class.getClassLoader());
			cbe.setClassName("Model");//which is extended of Model.class..see below!
		} catch (Exception e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		} 
		 cbe.setExtendedClass(Model.class); 
		
		
		 try {
			model = (Model) cbe.createInstance(new StringReader(script));
		} catch (CompileException e1) {
			
			System.out.println(e1.getMessage());
		} catch (IOException e1) {
			
			e1.printStackTrace();
		}
	
		/* For Java WorldWind! */
		Compiled.setModel(model);
		DynamicLayer.setEcoclass(model);

		if (monitor.isCanceled()) {
			try {
				throw new InterruptedException();
			} catch (InterruptedException e) {
				Bio7Action.stopFlow();
				System.out.println("Flow canceled!");

			}
		}
		buffer = null;
		script = null;

		return Status.OK_STATUS;

	}

	public void aboutToRun(IJobChangeEvent event) {

	}

	public void awake(IJobChangeEvent event) {

	}

	public void done(IJobChangeEvent event) {

	}

	public void running(IJobChangeEvent event) {

	}

	public void scheduled(IJobChangeEvent event) {

	}

	public void sleeping(IJobChangeEvent event) {

	}

}
