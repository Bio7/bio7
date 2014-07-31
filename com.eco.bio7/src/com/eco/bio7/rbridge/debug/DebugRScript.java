/*******************************************************************************
 * Copyright (c) 2007-2014 M. Austenfeld
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/
package com.eco.bio7.rbridge.debug;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.rosuda.REngine.Rserve.RConnection;
import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.console.ConsolePageParticipant;
import com.eco.bio7.rbridge.RServe;

public class DebugRScript extends Action {
	private IEditorPart part;
	private IMarker[] markers;
	boolean untrace = false;
	private IEditorPart editor;
	private Socket debugSocket;

	public DebugRScript() {
		super("Debug");

		setId("Debug");
		setText("Debug Action");

		ImageDescriptor desc = ImageDescriptor.createFromImage(new Image(Display.getCurrent(), getClass().getResourceAsStream("/pics/rundebug.gif")));

		this.setImageDescriptor(desc);
	}

	public void dispose() {

	}

	public void init(IWorkbenchWindow window) {

	}

	public void run() {
		
		IPreferenceStore store=Bio7Plugin.getDefault().getPreferenceStore();

		editor = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
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


		if (d == null) {

			String selectionConsole = ConsolePageParticipant.getInterpreterSelection();

			if (selectionConsole.equals("R")) {

				/* Find the line numbers of the markers! */
				int lineNum = 0;
				String expression = null;			
				

				if (resource != null) {
					Map<Integer, String> map1 = findMyMarkers(resource);
					/* Sorting the Map with a Treemap! */
					Map<Integer, String> map = new TreeMap<Integer, String>(map1);

					for (Map.Entry<Integer, String> entry : map.entrySet()) {

						lineNum = entry.getKey();
						expression = entry.getValue();

						if (lineNum > 0) {
							
							int port=store.getInt("R_DEBUG_PORT");
								if (expression == null) {
									ConsolePageParticipant con = ConsolePageParticipant.getConsolePageParticipantInstance();
                                    
									con.pipeToRConsole("source('" + loc + "')");
									/*Create a hidden environment for the temporary variable!*/
									con.pipeToRConsole(".bio7tempenv<- new.env()");
									con.pipeToRConsole("assign(\"bio7tempVar\", findLineNum('" + loc + "#" + lineNum + "'), env=.bio7tempenv)");
									con.pipeToRConsole("setBreakpoint('" + loc + "#" + lineNum + "')");
									con.pipeToRConsole("con1 <- socketConnection(port = "+port+", server = TRUE)");
									con.pipeToRConsole("writeLines(.bio7tempenv$bio7tempVar[[1]]$name, con1)");
									con.pipeToRConsole("writeLines(as.character(.bio7tempenv$bio7tempVar[[1]]$line), con1)");
									con.pipeToRConsole("close(con1)");
									readSocket(lineNum);
									
								
								}
								/*If an expression is available!*/
								else {
									ConsolePageParticipant con = ConsolePageParticipant.getConsolePageParticipantInstance();

									con.pipeToRConsole("source('" + loc + "')");
									/*Create a hidden environment for the temporary variable!*/
									con.pipeToRConsole(".bio7tempenv<- new.env()");
									con.pipeToRConsole("assign(\"bio7tempVar\", findLineNum('" + loc + "#" + lineNum + "'), env=.bio7tempenv)");
									con.pipeToRConsole("setBreakpoint('" + loc + "#" + lineNum + "',tracer=quote("+expression+"))");
									con.pipeToRConsole("con1 <- socketConnection(port = "+port+", server = TRUE)");
									con.pipeToRConsole("writeLines(.bio7tempenv$bio7tempVar[[1]]$name, con1)");
									con.pipeToRConsole("writeLines(as.character(.bio7tempenv$bio7tempVar[[1]]$line), con1)");
									con.pipeToRConsole("close(con1)");
									readSocket(lineNum);
									
								}
							}
						}
					

				}

			} else {
				Bio7Dialog.message("Please start the \"Native R\" shell in the Bio7 console!");
			}

		}

	}

	
	private void readSocket(int lineNum) {
		
		String lineNumber = "0";
		String result = null;
		IPreferenceStore store=Bio7Plugin.getDefault().getPreferenceStore();
		int port=store.getInt("R_DEBUG_PORT");
		try {
			debugSocket = new Socket("127.0.0.1", port);
			

			BufferedReader input = null;
			try {
				input = new BufferedReader(new InputStreamReader(debugSocket.getInputStream()));
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			result = input.readLine();

			lineNumber = input.readLine();
			
			debugSocket.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		for (int i = 0; i < markers.length; i++) {
			Integer line = null;
			try {
				line = (Integer) markers[i].getAttribute(IMarker.LINE_NUMBER);
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			if (line.intValue() == lineNum) {

				try {
					markers[i].setAttribute(IMarker.TEXT, result);
					// System.out.println("This is " +
					// markers[i].getAttribute(IMarker.TEXT));
				} catch (CoreException e) {// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		}
		ITextEditor edit = (ITextEditor) editor;
		IDocumentProvider dp = edit.getDocumentProvider();
		IDocument doc = dp.getDocument(editor.getEditorInput());

		IRegion reg = null;
		
		try {
			reg = doc.getLineInformation(Integer.parseInt(lineNumber) - 1);
		} catch (BadLocationException e1) {

			e1.printStackTrace();
		}

		edit.selectAndReveal(reg.getOffset() + reg.getLength(), 0);
	}

	

	public Map<Integer, String> findMyMarkers(IResource target) {
		String type = "com.eco.bio7.redit.debugMarker";

		try {
			markers = target.findMarkers(type, false, IResource.DEPTH_ZERO);

		} catch (CoreException e) {

			e.printStackTrace();
		}

		Map<Integer, String> map1 = new HashMap<Integer, String>();

		for (int i = 0; i < markers.length; ++i) {
			try {
				map1.put((Integer) markers[i].getAttribute(IMarker.LINE_NUMBER), (String) markers[i].getAttribute(IMarker.MESSAGE));
				// System.out.println( (String)
				// markers[i].getAttribute(IMarker.TEXT));
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
