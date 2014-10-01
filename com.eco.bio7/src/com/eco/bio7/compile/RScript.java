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

import java.util.ArrayList;
import java.util.Collections;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.batch.BatchModel;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.rbridge.PlotJob;
import com.eco.bio7.rbridge.RServe;
import com.eco.bio7.rbridge.RState;

public class RScript {

	private static ArrayList marked = new ArrayList();
	private static String script;
	protected static REXP rexp;

	public static IMarker[] findMyMarkers(IResource target) {
		String type = IMarker.TASK;
		IMarker[] markers = null;

		try {
			markers = target.findMarkers(type, true, IResource.DEPTH_INFINITE);
		} catch (CoreException e) {

			e.printStackTrace();
		}
		return markers;
	}

	/* Finds the markers and plots the source at the marked locations! */
	public static void getmarkers() {
		marked.clear();
		String inhalt = "";
		IRegion reg = null;
		int b = -1;
		IEditorPart editore = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		if (editore != null) {
			IResource resource = (IResource) editore.getEditorInput().getAdapter(IResource.class);

			IMarker[] markersfind = findMyMarkers(resource);
			for (int i = 0; i < markersfind.length; i++) {
				Integer at = null;
				try {
					at = (Integer) markersfind[i].getAttribute(IMarker.LINE_NUMBER);
				} catch (CoreException e) {

					e.printStackTrace();
				}
				marked.add(at.intValue());

			}

			Collections.sort(marked);
			int temp = -1000000;// An arbitrary value to remove double entries
								// in
								// the
			// list for plotting !
			for (int i = 0; i < marked.size(); i++) {

				Integer at = null;

				at = (Integer) marked.get(i);

				b = at.intValue();
				if (b != temp) {

					IEditorPart editore2 = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
					ITextEditor editor2 = (ITextEditor) editore2;
					IDocumentProvider prov = editor2.getDocumentProvider();
					IDocument doc = prov.getDocument(editor2.getEditorInput());

					try {
						reg = doc.getLineInformation(b - 1);
					} catch (BadLocationException e1) {

						e1.printStackTrace();
					}

					try {
						inhalt = inhalt + doc.get(reg.getOffset(), reg.getLength()) + ";";
					} catch (BadLocationException e) {

						e.printStackTrace();
					}
				}
				temp = b;

			}

			if (b != -1) {
				IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
				boolean customDevice = store.getBoolean("USE_CUSTOM_DEVICE");
				if (customDevice == false) {
					startPlotJob(inhalt, b);
				} else {
					Bio7Dialog.message("Can't plot marked commands if custom device is used!\nPlease deselect the \"Use Custom Device\" preferences in the Rserve preferences!");
				}

			}
		}
	}

	/* Starts the job in PlotJob! */
	private static void startPlotJob(String inhalt, int b) {

		PlotJob plot = new PlotJob(inhalt, b);
		plot.addJobChangeListener(new JobChangeAdapter() {
			public void done(IJobChangeEvent event) {
				if (event.getResult().isOK()) {

					RState.setBusy(false);
				} else {

					RState.setBusy(false);
				}
			}
		});
		plot.setUser(true);
		plot.schedule();

	}

	/**
	 * Evaluates a script for R without running in a job.
	 */
	public static void script(String a) {

		String script_draft = a;
		script = script_draft.replace('\r', ' ');// replace LineFeed!

		final RConnection cscript = RServe.getConnection();

		if (RServe.isRrunning()) {
			if (cscript != null) {

				Display display = PlatformUI.getWorkbench().getDisplay();
				display.syncExec(new Runnable() {

					public void run() {

						try {
							cscript.voidEval(script);
						} catch (RserveException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

					}
				});

			}

		}
		script_draft = null;
		script = null;

	}

	/**
	 * Evaluates a script in R running in a job.
	 * 
	 * @param result
	 * @param s
	 */
	public static void rscriptjob(String result, String loc) {
		if (RState.isBusy() == false) {
			RState.setBusy(true);
			RInterpreterJob Do = new RInterpreterJob(result, false, loc);
			Do.setUser(true);
			Do.addJobChangeListener(new JobChangeAdapter() {
				public void done(IJobChangeEvent event) {
					if (event.getResult().isOK()) {
						int countDev = RServe.getDisplayNumber();
						RState.setBusy(false);
						if (countDev > 0) {
							RServe.closeAndDisplay();
						}
						BatchModel.resumeFlow();

					} else {
						RState.setBusy(false);
					}
				}
			});

			Do.schedule();
		} else {
			System.out.println("RServer is busy. Can't execute the R script!");
		}

	}

	/**
	 * A method to get all current workspace variables name.
	 * 
	 * @return the r workspace object names as a String array.
	 */
	public static String[] getRObjects() {
		REXP x = null;
		String[] v = null;

		if (RServe.isAliveDialog()) {
			if (RState.isBusy() == false) {

				// List all variables in the R workspace!

				try {
					RServe.getConnection().eval("try(var<-ls())");
					x = RServe.getConnection().eval("try(var)");
					try {
						v = x.asStrings();
					} catch (REXPMismatchException e1) {

						e1.printStackTrace();
					}
					RServe.getConnection().eval("try(remove(var))");
				} catch (RserveException e1) {

					e1.printStackTrace();
				}

			} else {
				System.out.println("RServer is busy. Can't execute the R script!");

			}

		}
		return v;
	}

	

	/**
	 * A method to return REXP objects of Rserve running in a job. This method also checks if a R job is already running.
	 * @param eval a R command.
	 * @return a REXP object.
	 */
	public static REXP valueFromRJob(String eval) {
		
		if (RServe.isAliveDialog()) {
			if (RState.isBusy() == false) {
				RState.setBusy(true);
				Job job = new Job("Transfer from R") {
					

					@Override
					protected IStatus run(IProgressMonitor monitor) {
						monitor.beginTask("Transfer Data ...", IProgressMonitor.UNKNOWN);
						try {
							rexp=RServe.getConnection().eval( eval);
						} catch (RserveException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						monitor.done();
						return Status.OK_STATUS;
					}

				};
				job.addJobChangeListener(new JobChangeAdapter() {
					public void done(IJobChangeEvent event) {
						if (event.getResult().isOK()) {

							RState.setBusy(false);
						} else {

							RState.setBusy(false);
						}
					}
				});
				// job.setSystem(true);
				job.schedule();
			} else {
				System.out.println("RServer is busy. Can't execute the R script!");
			}
		}
		return rexp;
	}
}
