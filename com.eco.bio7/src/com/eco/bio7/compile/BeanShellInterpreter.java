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
 * This class provides some static methods for the interpretation of BeanShell source or String expressions.
 * 
 * @author Bio7
 * 
 */
public class BeanShellInterpreter {

	private static CompileBeanShellJob ab;

	private static String COMPILE_IMPORTS =

	InterpreterMessages.getString("Import.bio7");

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

					if (source == null) {
						StringBuffer buffer = new StringBuffer();
						buffer.append(COMPILE_IMPORTS);
						buffer.append(aScript);
						String script = buffer.toString();

						try {
							ScriptEngineConnection.getScriptingEngineBeanShell().eval(script);
						} catch (ScriptException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						buffer = null;
						script = null;
					} else if (aScript == null) {

						try {
							ScriptEngineConnection.getScriptingEngineBeanShell().eval(COMPILE_IMPORTS);
						} catch (ScriptException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

						try {
							ScriptEngineConnection.getScriptingEngineBeanShell().eval(source);
						} catch (ScriptException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

					}

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

		ScriptEngine engineBeanShell = ScriptEngineConnection.getScriptingEngineBeanShell();
		if (source == null) {
			StringBuffer buffer = new StringBuffer();
			buffer.append(COMPILE_IMPORTS);
			buffer.append(aScript);
			String script = buffer.toString();

			try {
				engineBeanShell.eval(script);

			} catch (ScriptException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			buffer = null;
			script = null;
		} else if (aScript == null) {

			try {
				engineBeanShell.eval(COMPILE_IMPORTS);
			} catch (ScriptException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			FileReader reader = null;
			try {
				reader = new FileReader(source);
			} catch (FileNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			try {

				engineBeanShell.eval(reader);

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

		ab = new CompileBeanShellJob(aScript, source);
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

	/**
	 * Returns an object from the BeanShell interpreter.
	 * 
	 * @param name
	 *            a string representation of the name of the object which must be available in the namespace of BeanShell.
	 * @return an object with the given name if available.
	 */
	public static Object getObject(String name) {
		Object o = null;

		try {
			o = ScriptEngineConnection.getScriptingEngineBeanShell().get(name);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return o;
	}

}
