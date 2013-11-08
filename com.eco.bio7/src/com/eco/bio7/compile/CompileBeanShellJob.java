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

class CompileBeanShellJob extends WorkspaceJob implements IJobChangeListener {

	private String aScript;

	private String source;

	private String script;

	public static String COMPILE_IMPORTS = InterpreterMessages.getString("Import.bio7");

	public CompileBeanShellJob(String aScript, String source) {
		super("Interpret Source");
		this.aScript = aScript;
		this.source = source;

	}

	public IStatus runInWorkspace(IProgressMonitor monitor) {
		monitor.beginTask("Interpret the source...", IProgressMonitor.UNKNOWN);
		String head = COMPILE_IMPORTS;

		ScriptEngine engineBeanShell = ScriptEngineConnection.getScriptingEngineBeanShell();
		if (source == null) {
			StringBuffer buffer = new StringBuffer();
			buffer.append(head);
			buffer.append(aScript);
			script = buffer.toString();

			try {
				engineBeanShell.eval(script);
			} catch (ScriptException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			buffer = null;
			script = null;
		} else if (aScript == null) {

			FileReader reader = null;
			try {
				reader = new FileReader(source);
			} catch (FileNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			try {
				engineBeanShell.eval(head);
				engineBeanShell.eval(reader);

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
