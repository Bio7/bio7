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
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;
import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.compile.PythonInterpreter;
import com.eco.bio7.console.ConsolePageParticipant;
import com.eco.bio7.rcp.StartBio7Utils;

public class InterpretPython implements IEditorActionDelegate {

	private IFile file;
	private static boolean isBlender = false;

	@Override
	public void run(IAction action) {
		StartBio7Utils utils = StartBio7Utils.getConsoleInstance();
		if (utils != null) {
			utils.cons.clear();
		}

		IEditorPart editor = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		IDocument doc = ((ITextEditor) editor).getDocumentProvider().getDocument(editor.getEditorInput());
		if (editor.getEditorInput() instanceof IFileEditorInput) {

			file = ((IFileEditorInput) editor.getEditorInput()).getFile();
		}

		if (file.getFileExtension().equals("py")) {

			String a = doc.get();
			IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
			boolean cPython = store.getBoolean("python_pipe");
			String sel = store.getString("python_select");
			String blenderSel = store.getString("blender_options");

			if (cPython == true) {
				String selectionConsole = ConsolePageParticipant.getInterpreterSelection();

				if (editor.isDirty()) {
					editor.doSave(new NullProgressMonitor());
				}
				IEditorInput editorInput = editor.getEditorInput();
				IFile aFile = null;

				if (editorInput instanceof IFileEditorInput) {
					aFile = ((IFileEditorInput) editorInput).getFile();
				}
				String loc = aFile.getLocation().toString();

				if (sel.equals("Python")) {
					if (selectionConsole.equals("Python")) {
						ConsolePageParticipant.pipeInputToConsole("execfile('" + loc + "')",true,true);
					} else {
						Bio7Dialog.message("Please start the \"Native Python\" Shell in the Bio7 console!");
					}
				}

				else if (sel.equals("Blender")) {
					if (selectionConsole.equals("shell")) {
						String path = store.getString("path_blender");
						String blenderArgs = store.getString("blender_args");
						if (blenderSel.equals("pscript")) {

							ConsolePageParticipant.pipeInputToConsole("\"" + path + "/blender\"" + " " + blenderArgs + " -P " + loc,true,true);
						} else if (blenderSel.equals("interactive")) {
							if (isBlender == false) {
								ConsolePageParticipant.pipeInputToConsole("\"" + path + "/blender\"" + " " + blenderArgs + " --python-console",true,true);
								isBlender = true;
							}
							ConsolePageParticipant.pipeInputToConsole(store.getString("before_script_blender"),true,true);
							ConsolePageParticipant.pipeInputToConsole("exec(compile(open('" + loc + "').read(),'" + loc + "', 'exec'))",true,true);
							ConsolePageParticipant.pipeInputToConsole(store.getString("after_script_blender"),true,true);
							System.out.println("Please restart the Bio7 native console for a new interactive session if you have closed Blender!");
						} else {
							ConsolePageParticipant.pipeInputToConsole("\"" + path + "/blender\"" + " " + blenderArgs,true,true);
						}
					} else {
						Bio7Dialog.message("Please start the Shell in the Bio7 Console\n" + "to interpret the Python script in Blender!");
					}
				}

			}

			else {

				PythonInterpreter.interpretJob(a, null);
			}
		}
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setActiveEditor(IAction action, IEditorPart targetEditor) {
		// TODO Auto-generated method stub

	}

	public static void setBlenderInteractive(boolean isBlender) {
		InterpretPython.isBlender = isBlender;
	}

}
