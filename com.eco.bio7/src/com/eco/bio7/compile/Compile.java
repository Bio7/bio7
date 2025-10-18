/*******************************************************************************
 * Copyright (c) 2007-2025 M. Austenfeld
 * 
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * Contributors:
 * Marcel Austenfeld - initial API and implementation
 *******************************************************************************//*/

package com.eco.bio7.compile;

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.compiler.CompilerFactoryFactory;
import org.codehaus.commons.compiler.IClassBodyEvaluator;
import org.codehaus.commons.compiler.ISimpleCompiler;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;

import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.actions.Bio7Action;
import com.eco.bio7.batch.BatchModel;
import com.eco.bio7.methods.Compiled;
import com.eco.bio7.worldwind.DynamicLayer;

public class Compile {

	//private static String script;

	//private static String COMPILE_IMPORTS = CompilerMessages.getString("Import.bio7");

	public static void compile(String cu) throws InstantiationException {

		StringBuffer buffer = new StringBuffer();
		buffer.append(COMPILE_IMPORTS);
		buffer.append(cu);
		script = buffer.toString();

		try {
			IWorkbench wb = PlatformUI.getWorkbench();
			IProgressService ps = wb.getProgressService();
			try {
				ps.busyCursorWhile(new IRunnableWithProgress() {
					public void run(IProgressMonitor pm) {
						pm.beginTask("Compile the source...", 2);
						pm.worked(1);

						Model eco = null;

						IClassBodyEvaluator cbe = null;
						try {
							cbe = CompilerFactoryFactory.getDefaultCompilerFactory().newClassBodyEvaluator();
							cbe.setParentClassLoader(Bio7Plugin.class.getClassLoader());
							cbe.setClassName("Model");// which extends
														// Model.class..see
														// below!
						} catch (Exception e2) {
							// TODO Auto-generated catch block
							e2.printStackTrace();
						}
						cbe.setExtendedClass(Model.class);
						try {
							eco = (Model) cbe.createInstance(new StringReader(script));
						} catch (CompileException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}

						Compiled.setModel(eco);
						 For Java WorldWind! 
						DynamicLayer.setEcoclass(eco);

						if (pm.isCanceled()) {
							try {
								throw new InterruptedException();
							} catch (InterruptedException e) {
								Bio7Action.stopFlow();
								System.out.println("Canceled!");

							}
						}

						pm.worked(2);
					}
				});
			} catch (InvocationTargetException e) {

				System.out.println(e.getMessage());
				System.out.println("________________");
			} catch (InterruptedException e) {

				System.out.println(e.getMessage());
				System.out.println("________________");
			}

		} catch (SecurityException e) {

			System.out.println(e.getMessage());
			System.out.println("________________");
		} catch (IllegalArgumentException e) {

			System.out.println(e.getMessage());
			System.out.println("________________");
		}
		buffer = null;
		script = null;
	}

	public static void failure(final String text) {
		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {

			public void run() {
				MessageBox messageBox = new MessageBox(new Shell(),

				SWT.ICON_INFORMATION);
				messageBox.setMessage(text + "\n" + "Please check the Syntax !");
				messageBox.open();
			}
		});
	}

	public static void compileJob(String source) {

		JavaCompileWorkspaceJob job = new JavaCompileWorkspaceJob(source);

		job.addJobChangeListener(new JobChangeAdapter() {
			public void done(IJobChangeEvent event) {
				if (event.getResult().isOK()) {
					BatchModel.resume();

				}
			}
		});

		job.schedule();

	}

	 A helper method to compile classbody for the drag and drop functionality! 
	public static void compileClassbodyWithoutJob(String source) {
		StringBuffer buffer = new StringBuffer();
		buffer.append(COMPILE_IMPORTS);
		buffer.append(source);
		String script = buffer.toString();

		Model eco = null;

		IClassBodyEvaluator cbe = null;
		try {
			cbe = CompilerFactoryFactory.getDefaultCompilerFactory().newClassBodyEvaluator();
			cbe.setParentClassLoader(Bio7Plugin.class.getClassLoader());
			cbe.setClassName("Model");// which extends Model.class..see below!
		} catch (Exception e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		cbe.setExtendedClass(Model.class);
		try {
			eco = (Model) cbe.createInstance(new StringReader(script));
		} catch (CompileException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		Compiled.setModel(eco);
		 For Java WorldWind! 
		DynamicLayer.setEcoclass(eco);
	}

	
	 * A helper method to compile a full class for the drag and drop
	 * functionality!
	 
	public static void compileClassWithoutJob(String source, String fileName) {

		Model eco = null;

		ISimpleCompiler cbe = null;
		try {
			cbe = CompilerFactoryFactory.getDefaultCompilerFactory().newSimpleCompiler();
			cbe.setParentClassLoader(Bio7Plugin.class.getClassLoader());
			// cbe.setClassName("Model");//which extends Model.class..see below!
		} catch (Exception e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}

		try {
			cbe.cook(fileName, new StringReader(source));

			Class<?> c = null;
			try {
				c = cbe.getClassLoader().loadClass(fileName);
			} catch (ClassNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			try {
				eco = (Model) c.newInstance();
			} catch (InstantiationException | IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (CompileException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		Compiled.setModel(eco);
		 For Java WorldWind! 
		DynamicLayer.setEcoclass(eco);
	}

}
*/