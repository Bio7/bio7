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
package com.eco.bio7.rbridge.debug;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorActionDelegate;
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
import com.eco.bio7.rcp.StartBio7Utils;

public class DebugRScript extends Action {
	private IEditorPart part;
	private IMarker[] markers;
	boolean untrace = false;
	private boolean BROWSER = false;
	private String tempFileName = "";
	private IEditorPart editor;

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

		boolean remote = Bio7Plugin.getDefault().getPreferenceStore().getBoolean("REMOTE");
		IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
		// boolean rPipe = store.getBoolean("r_pipe");

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
					/* Sorting the Map with a Treemap! */
					Map<Integer, String> map = new TreeMap<Integer, String>(map1);

					for (Map.Entry<Integer, String> entry : map.entrySet()) {

						lineNum = entry.getKey();
						expression = entry.getValue();

						if (lineNum > 0) {

							if (BROWSER) {
								/* Insert the debug command at line start! */
								IRegion reg = null;
								try {
									reg = doc.getLineInformation(lineNum);
								} catch (BadLocationException e1) {

									e1.printStackTrace();
								}

								if (expression == null) {
									String command = ";browser();";
									int length = command.length();
									buf.insert(reg.getOffset() - correctInsert, command);
									correctInsert = correctInsert - length;
								} else {
									String command = ";browser(expr=isTRUE(" + expression + "))";
									int length = command.length();

									buf.insert(reg.getOffset() - correctInsert, command);
									correctInsert = correctInsert - length;
								}

								content = buf.toString();
								/* Here we write the commands to the console (no file!) */
								ConsolePageParticipant.pipeInputToConsole(content, true, false);
								// System.out.println(content);
								// System.out.println(loc);
							}
							// Using trace instead of the browser call!
							else {

								File fi = new File(tempFileName);
								/*
								 * if(fi!=null&&fi.exists()){ fi.delete(); }
								 */
								/* We store the timestamp which we need for a synchronized read of the stored variables in the temp file! */
								long last = fi.lastModified();
								UUID ui = UUID.randomUUID();
								String fileUid = ui.toString();

								String fileName = "tempRVariables.txt";
								if (expression != null) {
									ConsolePageParticipant con=ConsolePageParticipant.getConsolePageParticipantInstance();
									
									con.pipeToRConsole("source('" + loc + "')");
									con.pipeToRConsole("XXX<-findLineNum('" + loc + "#" + lineNum + "')");
									con.pipeToRConsole("setBreakpoint('" + loc + "#" + lineNum + "')");
									// ConsolePageParticipant.pipeInputToConsole("writeClipboard(XXX[[1]]$name, format = 1)", true, false);
									writeTempRData(con,"XXX[[1]]$name", "XXX[[1]]$line", fileName);
									con.pipeToRConsole("print(\"Debug Info Set\")");

									/*ConsolePageParticipant.pipeInputToConsole("source('" + loc + "')", true, false);
									ConsolePageParticipant.pipeInputToConsole("XXX<-findLineNum('" + loc + "#" + lineNum + "')", true, false);
									ConsolePageParticipant.pipeInputToConsole("setBreakpoint('" + loc + "#" + lineNum + "')", true, false);
									// ConsolePageParticipant.pipeInputToConsole("writeClipboard(XXX[[1]]$name, format = 1)", true, false);
									writeTempRData("XXX[[1]]$name", "XXX[[1]]$line", fileName);
									ConsolePageParticipant.pipeInputToConsole("print(\"Debug Info Set\")");*/
								}

								// readClipboardJava(lineNum);
								System.out.println("Finished Writing!");
								readTempFileJava(lineNum, fileName, last);
								System.out.println("Finished Reading!");

							}
						}
					}

				}

			} else {
				Bio7Dialog.message("Please start the \"Native R\" shell in the Bio7 console!");
			}

		}

	}

	private void writeTempRData(ConsolePageParticipant con,String name, String line, String fileName) {
		con.pipeToRConsole("fileConn<-file(\"" + fileName + "\")");
		con.pipeToRConsole("writeLines(c(" + name + "," + line + "), fileConn)");
		con.pipeToRConsole("close(fileConn)");
		
	}

	private void readTempFileJava(int lineNum, String fileName, long oldModified) {
		tempFileName = fileName;
		File fi = new File(fileName);
		FileReader fr = null;
		String result;
		String lineNumber = "0";
		/* We need a synchronus write and read from the R process and Java. If R has finished the writing the file has been modified for Java! */

		try {
			Thread.sleep(200);
		} catch (InterruptedException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}

		try {
			fr = new FileReader(fileName);
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		try (BufferedReader br = new BufferedReader(fr)) {

			result = br.readLine();

			lineNumber = br.readLine();

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
						// System.out.println("This is " + markers[i].getAttribute(IMarker.TEXT));
					} catch (CoreException e) {// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

			}

			fr.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		ITextEditor edit = (ITextEditor) editor;
		IDocumentProvider dp = edit.getDocumentProvider();
		IDocument doc = dp.getDocument(editor.getEditorInput());

		IRegion reg = null;
		System.out.println(lineNumber);
		try {
			reg = doc.getLineInformation(Integer.parseInt(lineNumber) - 1);
		} catch (BadLocationException e1) {

			e1.printStackTrace();
		}

		edit.selectAndReveal(reg.getOffset() + reg.getLength(), 0);

	}

	private void readClipboardJava(int lineNum) {

		Clipboard clipboard = new Clipboard(Display.getCurrent());

		String result = (String) clipboard.getContents(TextTransfer.getInstance());

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
				map1.put((Integer) markers[i].getAttribute(IMarker.LINE_NUMBER), (String) markers[i].getAttribute(IMarker.TEXT));
				// System.out.println( (String) markers[i].getAttribute(IMarker.TEXT));
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
