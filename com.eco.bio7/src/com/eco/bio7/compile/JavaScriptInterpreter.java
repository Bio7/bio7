/*******************************************************************************
 * Copyright (c) 2007-2017 M. Austenfeld
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/

package com.eco.bio7.compile;

import java.lang.reflect.InvocationTargetException;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;
import com.eco.bio7.actions.Bio7Action;
import com.eco.bio7.batch.BatchModel;
import com.eco.bio7.scriptengines.ScriptEngineConnection;

/**
 * This class provides some static methods for the interpretation of Jython source or String expressions.
 * 
 * @author Bio7
 * 
 */
public class JavaScriptInterpreter {

	private static CompileJavaScriptJob ab;

	//private static String COMPILE_IMPORTS = InterpreterMessages.getString("Import.jython");

	/**
	 * Interprets the source or a String expression with a progress monitor. One argument has to be chosen (null reference for the other argument !).
	 * 
	 * @param aScript
	 *            a string to interpret.
	 * @param source
	 *            a string representation of a source to interpret.
	 */
	public static void interpret(final String aScript, final String source) {

		IWorkbench wb = PlatformUI.getWorkbench();
		IProgressService ps = wb.getProgressService();

		try {
			ps.busyCursorWhile(new IRunnableWithProgress() {

				public void run(IProgressMonitor pm) {

					pm.beginTask("Interpret the source...", 2);
					pm.worked(1);

					StringBuffer buffer = new StringBuffer();
					//buffer.append(COMPILE_IMPORTS);
					buffer.append(aScript);
					String script = buffer.toString();
					javaScriptInterpret(script, source);
					script = null;
					buffer = null;

					if (pm.isCanceled()) {
						try {
							throw new InterruptedException();
						} catch (InterruptedException e) {
							Bio7Action.stopFlow();
							System.out.println("Flow canceled!");

						}
					}
					pm.worked(2);

				}
			});
		} catch (InvocationTargetException e) {
			System.out.println(e.getMessage());
			System.out.println("");
		} catch (InterruptedException e) {
			System.out.println(e.getMessage());
			System.out.println("");
		}

	}

	/**
	 * Interprets the source or a String expression. This method is the one to be called inside another java or script source. One argument has to be chosen (null reference for the other argument !).
	 * 
	 * @param aScript
	 *            a string to interpret.
	 * @param source
	 *            a string representation of a source to interpret.
	 */
	public static void doInterpret(final String aScript, final String source) {
		StringBuffer buffer = new StringBuffer();
		//buffer.append(COMPILE_IMPORTS);
		buffer.append(aScript);

		try {
			String script = buffer.toString();
			javaScriptInterpret(script, source);
			script = null;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			System.out.println(e.getMessage());
		}

		buffer = null;

	}

	/*
	 * Interprets the Jython code. File content is converted to a string
	 */
	private static void javaScriptInterpret(final String aScript, final String source) {
		if (source == null) {

			ScriptEngine gs = ScriptEngineConnection.getScriptingEngineJavaScript();
			try {
				gs.eval(aScript);
			} catch (ScriptException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} else if (aScript == null) {
			ScriptEngine gs = ScriptEngineConnection.getScriptingEngineJavaScript();
			String result = BatchModel.fileToString(source);

			try {
				gs.eval(result);
			} catch (ScriptException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}

	/**
	 * Interprets the source or a String expression in a job. One argument has to be chosen (null reference for the other argument !).
	 * 
	 * @param aScript
	 *            a string to interpret.
	 * @param source
	 *            a string representation of a source to interpret.
	 */
	public static void interpretJob(final String aScript, final String source) {
		BatchModel.pause();
		ab = new CompileJavaScriptJob(aScript, source);
		ab.setUser(true);

		ab.addJobChangeListener(new JobChangeAdapter() {
			public void done(IJobChangeEvent event) {
				if (event.getResult().isOK()) {

					BatchModel.resumeFlow();

				}
			}
		});
		ab.schedule();

	}

}
