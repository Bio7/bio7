/*******************************************************************************
 * Copyright (c) 2007-2013 M. Austenfeld
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/

package com.eco.bio7.compile;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import groovy.lang.GroovyShell;
import org.codehaus.groovy.control.CompilationFailedException;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import com.eco.bio7.actions.Bio7Action;
import com.eco.bio7.batch.BatchModel;
import com.eco.bio7.scriptengines.ScriptEngineConnection;

class CompileGroovyJob extends WorkspaceJob implements IJobChangeListener {

	private String aScript;// a script as a String!

	private String source;// from a source file!

	private String script;

	public static String COMPILE_IMPORTS = InterpreterMessages.getString("Import.groovy");

	public CompileGroovyJob(String aScript, String source) {
		super("Interpret Source");
		this.aScript = aScript;
		this.source = source;

	}

	public IStatus runInWorkspace(IProgressMonitor monitor) {
		monitor.beginTask("Interpret the source...", IProgressMonitor.UNKNOWN);
		String head = COMPILE_IMPORTS;

		if (source == null) {
			
			StringBuffer buffer = new StringBuffer();
			buffer.append(head);
			buffer.append(aScript);
			script = buffer.toString();

			ScriptEngine gs = ScriptEngineConnection.getScriptingEngineGroovy();
			try {
				gs.eval(script);
			} catch (ScriptException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			buffer = null;
			script = null;
			
		} else if (aScript == null) {
			/*
			 * Interprets the groovy code. File content is converted to a string 
			 * (Groovy needs the default imports! and doesn't stores the workspace
			 * after each evaluation(different in BeanShell!).
			 * */
			ScriptEngine gs = ScriptEngineConnection.getScriptingEngineGroovy();
			String result = BatchModel.fileToString(source);

			try {
				gs.eval(head + result);
			} catch (ScriptException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		if (monitor.isCanceled()) {
			try {
				throw new InterruptedException();
			} catch (InterruptedException e) {
				Bio7Action.stopFlow();
				System.out.println("Flow canceled by user!");

			}
		}
		
		

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
