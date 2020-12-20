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
 * This class provides some static methods for the interpretation of Groovy source or String expressions.
 * 
 * @author Bio7
 * 
 */
public class GroovyInterpreter {

	private static CompileGroovyJob ab;

	private static String COMPILE_IMPORTS = InterpreterMessages.getString("Import.groovy");

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
					buffer.append(COMPILE_IMPORTS);
					buffer.append(aScript);
					String script = buffer.toString();

					try {
						groovyInterpret(script, source);
					} catch (Exception e1) {
						// TODO Auto-generated catch block
						System.out.println(e1.getMessage());
					}
					buffer = null;
					script = null;

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
	 * @param inter
	 *            a string to interpret.
	 * @param source
	 *            a string representation of a source to interpret.
	 */
	public static void doInterpret(final String inter, final String source) {
		StringBuffer buffer = new StringBuffer();
		buffer.append(COMPILE_IMPORTS);
		buffer.append(inter);
		String script = buffer.toString();

		try {
			groovyInterpret(script, source);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			System.out.println(e.getMessage());
		}

		buffer = null;
		script = null;
	}

	/*
	 * Interprets the groovy code. File content is converted to a string (Groovy needs the default imports! and doesn't stores the workspace after each evaluation (different in BeanShell!).
	 */
	private static void groovyInterpret(final String aScript, final String source) {
		if (source == null) {

			ScriptEngine gs = ScriptEngineConnection.getScriptingEngineGroovy();
			try {
				gs.eval(aScript);
			} catch (ScriptException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} else if (aScript == null) {
			ScriptEngine gs = ScriptEngineConnection.getScriptingEngineGroovy();
			String result = BatchModel.fileToString(source);

			try {
				gs.eval(COMPILE_IMPORTS + result);
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

		ab = new CompileGroovyJob(aScript, source);
		ab.setUser(true);
		ab.setName("Goovy_Interpret_Job");
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
