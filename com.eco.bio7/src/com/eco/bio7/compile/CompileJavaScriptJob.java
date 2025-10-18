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

package com.eco.bio7.compile;

import java.io.FileNotFoundException;
import java.io.FileReader;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import com.eco.bio7.actions.Bio7Action;
import com.eco.bio7.scriptengines.ScriptEngineConnection;

class CompileJavaScriptJob extends WorkspaceJob implements IJobChangeListener {

	private String aScript;// a script as a String!

	private String source;// from a source file!

	private String script;

	public static String COMPILE_IMPORTS = InterpreterMessages.getString("Import.javascript");

	public CompileJavaScriptJob(String aScript, String source) {
		super("Interpret Source");
		this.aScript = aScript;
		this.source = source;

	}

	public IStatus runInWorkspace(IProgressMonitor monitor) {
		monitor.beginTask("Interpret the source...", IProgressMonitor.UNKNOWN);
		String head = COMPILE_IMPORTS;
		ScriptEngine engineJavaScript = ScriptEngineConnection.getScriptingEngineJavaScript();
		if (source == null) {
			StringBuffer buffer = new StringBuffer();
			buffer.append(head);
			buffer.append(aScript);
			script = buffer.toString();

			ScriptEngine gs = engineJavaScript;
			try {
				gs.eval(script);
			} catch (ScriptException e) {
				
				e.printStackTrace();
			}
			buffer = null;
			script = null;
			aScript=null;
			
		} else if (aScript == null) {
			/*
			 * Interprets the JavaScript code. 
			 * 
			 * */
			ScriptEngine gs = engineJavaScript;
			
			FileReader reader = null;
			try {
				reader = new FileReader(source);
			} catch (FileNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			try {
				gs.eval(head);
				gs.eval(reader);
			} catch (ScriptException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			source =null;

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
