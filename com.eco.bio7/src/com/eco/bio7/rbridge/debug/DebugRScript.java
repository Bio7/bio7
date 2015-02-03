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
import java.net.ConnectException;
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
import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.rosuda.REngine.Rserve.RConnection;

import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.actions.Bio7Action;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.collection.Work;
import com.eco.bio7.console.ConsolePageParticipant;
import com.eco.bio7.rbridge.RServe;

public class DebugRScript extends Action {
	private IEditorPart part;
	private IMarker[] markers;
	boolean untrace = false;
	private IEditorPart editor;
	private Socket debugSocket;
	private boolean errorFunction = false;

	public DebugRScript() {
		super("Debug");

		setId("Debug");
		setText("Debug Trace Action - Insert debugging code at chosen places in any function.");

		ImageDescriptor desc = ImageDescriptor.createFromImage(new Image(Display.getCurrent(), getClass().getResourceAsStream("/pics/rundebug.gif")));

		this.setImageDescriptor(desc);
	}

	public void dispose() {

	}

	public void init(IWorkbenchWindow window) {

	}

	public void run() {
		errorFunction = false;
		IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();

		/* Automatically disconnect from Rserve for debugging! */
		if (RServe.isAlive()) {
			Bio7Action.callRserve();
			store.setValue("RSERVE_ALIVE_DEBUG", true);// store the state to
														// resume Rserve after
														// debugging stop!
		} else {
			store.setValue("RSERVE_ALIVE_DEBUG", false);
		}

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
				// Work.openView("com.eco.bio7.rbridge.debug.DebugVariablesView");
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

							int port = store.getInt("R_DEBUG_PORT");
							if (expression == null) {
								ConsolePageParticipant con = ConsolePageParticipant.getConsolePageParticipantInstance();
								con.pipeToRConsole("options(prompt=\" \")");
								con.pipeToRConsole("source('" + loc + "')");
								/*
								 * Create a hidden environment for the temporary
								 * variable!
								 */
								con.pipeToRConsole(".bio7tempenv<- new.env();num<-NULL");
								con.pipeToRConsole("assign(\"bio7tempVar\", findLineNum('" + loc + "#" + lineNum + "'), env=.bio7tempenv)");
								con.pipeToRConsole("setBreakpoint('" + loc + "#" + lineNum + "')");
								con.pipeToRConsole("print(.bio7tempenv$bio7tempVar[[1]]$name)");
								con.pipeToRConsole(".bio7DebugScriptSocketConnection <- socketConnection(port = " + port + ",  server = TRUE,timeout=10)");
								con.pipeToRConsole("tryCatch(writeLines(.bio7tempenv$bio7tempVar[[1]]$name, .bio7DebugScriptSocketConnection),error = function(e) print('error'))");
								con.pipeToRConsole("tryCatch(writeLines(as.character(.bio7tempenv$bio7tempVar[[1]]$line), .bio7DebugScriptSocketConnection),error = function(e) print('error'))");
								con.pipeToRConsole("tryCatch(close(.bio7DebugScriptSocketConnection),error = function(e) print('error'))");
								con.pipeToRConsole("options(prompt=\"> \")");
								con.pipeToRConsole("writeLines(\"\")");
								try {
									Thread.sleep(100);
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								/* Add a linebreak in R */
								con.pipeToRConsole("cat(\"\r\")");
								readSocket(lineNum);

							}
							/* If an expression is available! */
							else {
								ConsolePageParticipant con = ConsolePageParticipant.getConsolePageParticipantInstance();
								con.pipeToRConsole("options(prompt=\" \")");
								con.pipeToRConsole("source('" + loc + "')");
								/*
								 * Create a hidden environment for the temporary
								 * variable!
								 */
								con.pipeToRConsole(".bio7tempenv<- new.env()");
								con.pipeToRConsole("try(assign(\"bio7tempVar\", findLineNum('" + loc + "#" + lineNum + "'), env=.bio7tempenv))");
								con.pipeToRConsole("try(setBreakpoint('" + loc + "#" + lineNum + "',tracer=quote(" + expression + ")))");
								con.pipeToRConsole("try(.bio7DebugScriptSocketConnection <- socketConnection(port = " + port + ", server = TRUE,timeout=10))");
								con.pipeToRConsole("tryCatch(writeLines(.bio7tempenv$bio7tempVar[[1]]$name, .bio7DebugScriptSocketConnection),error = function(e) print('error'))");
								con.pipeToRConsole("tryCatch(writeLines(as.character(.bio7tempenv$bio7tempVar[[1]]$line), .bio7DebugScriptSocketConnection),error = function(e) print('error'))");
								con.pipeToRConsole("tryCatch(close(.bio7DebugScriptSocketConnection),error = function(e) print('error'))");
								con.pipeToRConsole("options(prompt=\"> \")");
								con.pipeToRConsole("writeLines(\"\")");
								try {
									Thread.sleep(100);
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								/* Add a linebreak in R */
								con.pipeToRConsole("cat(\"\r\")");

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
		IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
		int port = store.getInt("R_DEBUG_PORT");
		BufferedReader input = null;
		try {
			try {
				debugSocket = new Socket("127.0.0.1", port);
				// debugSocket.setTcpNoDelay(true);
				debugSocket.setSoTimeout(10000);

				try {
					input = new BufferedReader(new InputStreamReader(debugSocket.getInputStream()));
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

				result = input.readLine();

			} catch (ConnectException e) {
				// TODO Auto-generated catch block
				// e.printStackTrace();
				result = "SOCKET_CON_ERROR";
			}

			if (result.equals("ERROR") || result.equals("SOCKET_CON_ERROR")) {

				errorFunction = true;
				Bio7Dialog.message("The breakpoint seems to be outside a function\ndefinition and cannot be traced!\n\n"
						+ "A breakpoint has to be set inside a function!"
						+ "\n\nPress STRG+C if the console is blocking!");
				System.out.println("Press STRG+C (evtl. several times) if the console is blocking!");
				/*We also send a SIGINT to reactivate the console if a plot was called!*/
				//UnixProcessManager.sendSigIntToProcessTree(ConsolePageParticipant.getConsolePageParticipantInstance().getRProcess());

			}

			else {
				lineNumber = input.readLine();
			}
			if (debugSocket != null)
				debugSocket.close();

			IOConsole ioc = ConsolePageParticipant.getConsolePageParticipantInstance().getIoc();
			ioc.clearConsole();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (errorFunction == false) {
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

					} catch (CoreException e) {

						e.printStackTrace();
					}
				}

			}
			ITextEditor edit = (ITextEditor) editor;
			IDocumentProvider dp = edit.getDocumentProvider();
			IDocument doc = dp.getDocument(editor.getEditorInput());

			IRegion reg = null;

			int lineNumBreakpoint = -1;
			try {
				lineNumBreakpoint = Integer.parseInt(lineNumber);
			} catch (NumberFormatException e) {
				// TODO Auto-generated catch block
				lineNumBreakpoint = 1;
				errorFunction = true;
				System.out.println("Can't parse a line number! Generate default=1!");
				// Bio7Dialog.message("The breakpoint seems to be outside a function\ndefinition and cannot be traced!\n\nA breakpoint has to be set inside a function!");

			}
			if (lineNumBreakpoint > -1) {
				try {
					reg = doc.getLineInformation(lineNumBreakpoint - 1);
				} catch (BadLocationException e1) {

					e1.printStackTrace();
				}

				edit.selectAndReveal(reg.getOffset() + reg.getLength(), 0);
				IResource resource = (IResource) editor.getEditorInput().getAdapter(IResource.class);
				try {
					resource.deleteMarkers("com.eco.bio7.reditor.debugrulermark", false, IResource.DEPTH_ZERO);
				} catch (CoreException e1) {

					e1.printStackTrace();
				}

				IMarker marker;

				try {

					marker = resource.createMarker("com.eco.bio7.reditor.debugrulermark");
					marker.setAttribute(IMarker.CHAR_START, reg.getOffset());
					marker.setAttribute(IMarker.CHAR_END, reg.getOffset() + reg.getLength());
				} catch (CoreException e) {

					e.printStackTrace();
				}
			}
			if (errorFunction == false) {

				Bio7Dialog.message("To start the debugging process call\n" + " the traced function from within the console!");
				System.out.println("To start the debugging process call" + " the traced function!");
				IOConsole ioc = ConsolePageParticipant.getConsolePageParticipantInstance().getIoc();

				ioc.getInputStream().appendData(System.getProperty("line.separator"));
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

		for (int i = 0; i < markers.length; ++i) {
			try {
				map1.put((Integer) markers[i].getAttribute(IMarker.LINE_NUMBER), (String) markers[i].getAttribute(IMarker.MESSAGE));

			} catch (CoreException e) {

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
