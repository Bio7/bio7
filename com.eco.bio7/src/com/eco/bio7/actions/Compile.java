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

import org.codehaus.commons.compiler.jdk.SimpleCompiler;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;
import com.eco.bio7.compile.CompileClassAndMultipleClasses;
import com.eco.bio7.compile.CompilerMessages;
import com.eco.bio7.compile.JavaCompileWorkspaceJob;
import com.eco.bio7.javaeditor.Bio7EditorPlugin;
import com.eco.bio7.rcp.StartBio7Utils;

public class Compile extends Action {
	public static String COMPILE_IMPORTS = CompilerMessages.getString("Import.bio7");
	private boolean classbody = true;
	public static IResource resource;
	public IWorkbenchPage pag;
	protected IFile ifile;

	public  Compile(String text){
		super(text);
		setId("com.eco.bio7.compilejava");
		setActionDefinitionId("com.eco.bio7.compilejavaaction");
		
	}

	public void run() {
		StartBio7Utils utils = StartBio7Utils.getConsoleInstance();
		if (utils != null) {
			utils.cons.clear();
		}
		IPreferenceStore store = Bio7EditorPlugin.getDefault().getPreferenceStore();
		classbody = store.getBoolean("classbody");
		IEditorPart editor = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		pag = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		IDocument doc = ((ITextEditor) editor).getDocumentProvider().getDocument(editor.getEditorInput());
		resource = (IResource) editor.getEditorInput().getAdapter(IResource.class);

		IEditorInput editorInput = editor.getEditorInput();

		if (editorInput instanceof IFileEditorInput) {
			ifile = ((IFileEditorInput) editorInput).getFile();
		}

		SimpleCompiler.resource = resource;
		/* Compile a class from a classbody! */
		if (classbody == true) {

			String a = doc.get();
			String script = (a);

			JavaCompileWorkspaceJob job = new JavaCompileWorkspaceJob(script);

			job.addJobChangeListener(new JobChangeAdapter() {
				public void done(IJobChangeEvent event) {
					if (event.getResult().isOK()) {

					}
				}
			});

			job.setUser(true);

			job.schedule();
			/* Compile a project with one or several Classes! */
		} else {

			NullProgressMonitor monitor = new NullProgressMonitor();
			IEditorPart[] dirtyEditors = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getDirtyEditors();
			for (IEditorPart iEditorPart : dirtyEditors) {
				iEditorPart.doSave(monitor);
			}

			CompileClassAndMultipleClasses cp = new CompileClassAndMultipleClasses();
			cp.compileClasses(resource, ifile, pag);

		}

		// Quad2d.getQuad2dInstance().repaint();
	}

	

}
