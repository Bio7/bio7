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
package com.eco.bio7.rbridge.actions;

import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
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

import java.util.*;

import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.compile.RInterpreterJob;
import com.eco.bio7.console.ConsolePageParticipant;
import com.eco.bio7.rbridge.RServe;
import com.eco.bio7.rbridge.RState;
import com.eco.bio7.rcp.StartBio7Utils;

public class DebugTraceRScript implements IEditorActionDelegate {
	private IEditorPart part;
	private IMarker[] markers;
	boolean untrace = false;

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
		ConsolePageParticipant.pipeInputToConsole("source('"+loc+"')", true, true);
		if (d == null) {
			String selectionConsole = ConsolePageParticipant.getInterpreterSelection();

			if (selectionConsole.equals("R")) {

				/* Find the line numbers of the markers! */
				int lineNum = 0;
				String expression = null;
				String content;
				int correctInsert = 0;
				StringBuffer buf = new StringBuffer();
				buf.append(doc.get());

				if (resource != null) {
					Map<Integer, String> map1 = findMyMarkers(resource);
					Map<Integer, String> map = new TreeMap<Integer, String>(map1);

				       
			        for(Map.Entry<Integer, String> entry : map.entrySet()) {
			           
			   

						//Integer lineNumber = null;
						
			        	lineNum = entry.getKey();
						expression = entry.getValue();
						
						//lineNum = lineNumber.intValue();

						/*if (lineNum > 0) {
							 Insert the debug command at line start! 
							IRegion reg = null;
							try {
								reg = doc.getLineInformation(lineNum);
							} catch (BadLocationException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
							//System.out.println(reg.getOffset());
							// lineNum=lineNum-correctInsert;
							if (expression==null) {
								String command = " browser();";
								int length = command.length();
								buf.insert(reg.getOffset() - correctInsert, command);
								correctInsert = correctInsert - length;
							} else {
								String command = " browser(expr=isTRUE(" + expression + "));";
								int length = command.length();

								buf.insert(reg.getOffset() - correctInsert, command);
								correctInsert = correctInsert - length;
							}
						}*/
						System.out.println(lineNum);
						ConsolePageParticipant.pipeInputToConsole("findLineNum('" + loc +"#"+lineNum+ "')");

						ConsolePageParticipant.pipeInputToConsole("setBreakpoint('" + loc +"#"+lineNum+ "')");
					}

					// buf.insert(reg.getOffset(), ";browser(expr=isTRUE(x==9));");

					//content = buf.toString();
					/* Here we write the commands to the console (no file!) */
					
					

					

					// System.out.print("setBreakpoint('" + loc +"#"+b+ "')");

				}

			} else {
				Bio7Dialog.message("Please start the \"Native R\" shell in the Bio7 console!");
			}

		}

	}

	public Map<Integer, String> findMyMarkers(IResource target) {
		String type = "com.eco.bio7.redit.debugMarker";

		try {
			markers = target.findMarkers(type, false, IResource.DEPTH_ZERO);

		} catch (CoreException e) {

			e.printStackTrace();
		}
		
		
		 Map<Integer, String> map1 = new HashMap<Integer, String>();
		
        for(int i = 0 ; i < markers.length ; ++i) {
        	try {
				map1.put((Integer)markers[i].getAttribute(IMarker.LINE_NUMBER),(String)markers[i].getAttribute(IMarker.MESSAGE));
				
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
       
       
        
		
		
		
		return map1;
	}

	public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
		List<Map.Entry<K, V>> list = new LinkedList<Map.Entry<K, V>>(map.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<K, V>>() {
			public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
				return (o1.getValue()).compareTo(o2.getValue());
			}
		});

		Map<K, V> result = new LinkedHashMap<K, V>();
		for (Map.Entry<K, V> entry : list) {
			result.put(entry.getKey(), entry.getValue());
		}
		return result;
	}

	public void selectionChanged(IAction action, ISelection selection) {

	}

	public void setActiveEditor(IAction action, IEditorPart targetEditor) {
		part = targetEditor;
	}

}
