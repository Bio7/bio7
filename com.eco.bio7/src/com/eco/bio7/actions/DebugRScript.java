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
package com.eco.bio7.actions;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;
import org.rosuda.REngine.Rserve.RConnection;

import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.compile.RInterpreterJob;
import com.eco.bio7.console.ConsolePageParticipant;
import com.eco.bio7.rbridge.RServe;
import com.eco.bio7.rbridge.RState;
import com.eco.bio7.rcp.StartBio7Utils;

public class DebugRScript implements IEditorActionDelegate {
	private IEditorPart part;
	private IMarker[] markers;
    boolean untrace=false;
	public void dispose() {

	}

	public void init(IWorkbenchWindow window) {

	}

	public void run(IAction action) {
		StartBio7Utils utils = StartBio7Utils.getConsoleInstance();
		if (utils != null) {
			utils.cons.clear();
		}
		IEditorPart editor = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		if (editor.isDirty()) {
			editor.doSave(new NullProgressMonitor());
		}

		IResource resource = (IResource) editor.getEditorInput().getAdapter(IResource.class);
		

		IDocument doc = ((ITextEditor) editor).getDocumentProvider().getDocument(editor.getEditorInput());
		RConnection d = RServe.getConnection();
		IEditorInput editorInput = editor.getEditorInput();
		IFile aFile = null;

		if (editorInput instanceof IFileEditorInput) {
			aFile = ((IFileEditorInput) editorInput).getFile();
		}
		String loc = aFile.getLocation().toString();

		boolean remote = Bio7Plugin.getDefault().getPreferenceStore().getBoolean("REMOTE");
		IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
		// boolean rPipe = store.getBoolean("r_pipe");

		if (d == null) {
			String selectionConsole = ConsolePageParticipant.getInterpreterSelection();

			if (selectionConsole.equals("R")) {

				
				
				System.out.println();
				int b = 0;
				if (resource != null) {
					IMarker[] markersfind = findMyMarkers(resource);
					for (int i = 0; i < markersfind.length; i++) {
						System.out.println("find");
						Integer a = null;
						try {
							a = (Integer) markersfind[i].getAttribute(IMarker.LINE_NUMBER);
						} catch (CoreException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						b = a.intValue();
						System.out.println(b);
						//ConsolePageParticipant.pipeInputToConsole("source('" + loc + "')");
					}
				}
				if(b>0){
				
				
				ConsolePageParticipant.pipeInputToConsole("setBreakpoint('" + loc +"#"+b+ "',clear=FALSE)");
				ConsolePageParticipant.pipeInputToConsole("source('" + loc + "')");
				System.out.print("setBreakpoint('" + loc +"#"+b+ "')");
				
				
				}
				
			}
			else{
				Bio7Dialog.message("Please start the \"Native R\" shell in the Bio7 console!");
			}

		}

	}

	public IMarker[] findMyMarkers(IResource target) {
		String type = "com.eco.bio7.redit.debugMarker";

		try {
			markers = target.findMarkers(type, false, IResource.DEPTH_ZERO);

		} catch (CoreException e) {

			e.printStackTrace();
		}
		return markers;
	}

	public void selectionChanged(IAction action, ISelection selection) {

	}

	public void setActiveEditor(IAction action, IEditorPart targetEditor) {
		part = targetEditor;
	}

}
