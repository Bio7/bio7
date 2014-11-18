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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.nebula.widgets.grid.Grid;
import org.eclipse.nebula.widgets.grid.GridItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.console.ConsolePageParticipant;

public class DebugProgress {

	private int line = 1;
	private Socket debugSocket;
	protected Grid debugGrid;
	protected StyledText styledTxT;
	private String outputLine;

	public void progress(String command) {
		

		String selectionConsole = ConsolePageParticipant.getInterpreterSelection();
		IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
		int port = store.getInt("R_DEBUG_PORT");
		if (selectionConsole.equals("R")) {
			ConsolePageParticipant con = ConsolePageParticipant.getConsolePageParticipantInstance();
			/* Create socket connection in global environment! */
			con.pipeToRConsole(".GlobalEnv$.socketCon1 <- socketConnection(port = " + port + ", server = TRUE,timeout=10);" + "sink(.GlobalEnv$.socketCon1)");
			/* Pipe the debug command ('n','s','f','c') to R */
			con.pipeToRConsole(command);
			/*
			 * Pipe several commands and capture the output! 
			 * 1. Write a line in the console to seperate any output from the captured variables.
			 * 2. Close the capturing. 
			 * 3. Close the socket connection. 
			 * 4. Write a line!
			 */
			con.pipeToRConsole("" 
					+ "writeLines(\"Bio7...Debug...Start...\");" 
					+ "print(ls.str(), max.level = 0);" 
					+ "sink();" // Close
					/*capturing output.*/
					+ "close(.GlobalEnv$.socketCon1);" 
					+ "writeLines(\"\")");

			/* Clear the debug spreadsheet! */
			deleteSpreadSheet();

			/* Read the R socket data with Java! */
			String data = javaReadFromRSocket(port, command);

			IEditorPart edit = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();

			ITextEditor editor = (ITextEditor) edit;
			IDocumentProvider dp = editor.getDocumentProvider();
			IDocument doc = dp.getDocument(editor.getEditorInput());

			if (data != null && data.length() > 0) {

				/* Extract the line number between .R# and : */
				Pattern p = Pattern.compile(".R#(.*?):");

				Matcher m = p.matcher(data);
				if (m.find()) {
					int temp = 0;
					try {
						temp = Integer.parseInt(m.group(1));
					} catch (NumberFormatException e1) {

						// e1.printStackTrace();
						System.out.println("Could not parse line number!");
					}
					int lines = doc.getNumberOfLines();
					if (temp > 0 && temp <= lines) {
						line = temp;
						IRegion reg = null;
						try {
							reg = doc.getLineInformation(line - 1);
						} catch (BadLocationException e) {

							e.printStackTrace();
						}

						editor.selectAndReveal(reg.getOffset() + reg.getLength(), 0);

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

				}

				else {

				}

			}

		} else {
			Bio7Dialog.message("Please start the \"Native R\" shell in the Bio7 console!");
		}
	}

	private String javaReadFromRSocket(int port, String command) {
		String data = null;
		try {

			debugSocket = new Socket("127.0.0.1", port);
			debugSocket.setTcpNoDelay(true);
			debugSocket.setSoTimeout(3000);
			BufferedReader input = null;
			try {
				input = new BufferedReader(new InputStreamReader(debugSocket.getInputStream()));
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
            /*Read the first line!*/
			data = input.readLine();
			// variable=input.readLine();
			String line;
			int count = 0;
			boolean capture = false;
			boolean printConsole = false;
			deleteDebugTextView();
			try {
				
				while ((line = input.readLine()) != null) {

					// Omit the first line if a debug message!
					// System.out.println(line);
					
					System.out.println("debug line: "+line);

					if (line.startsWith("Bio7...Debug...Start...")) {
						capture = true;
						printConsole = false;
						/* Clear the debug spreadsheet! */
						deleteSpreadSheet();
					}

					if (line.startsWith("debug:")) {
						//System.out.println();
						printConsole = true;
						

					}
					
					// debugging in:

					if (capture) {
						/* find ':' at first occurrence! */
						String sp[] = line.split(":", 2);

						// occurence.
						if (sp.length == 2) {
							/* Find whitespace at 4th position! */
							String sp2[] = sp[1].split("\\s", 4);
							if (debugGrid != null) {
								GridItem it = new GridItem(debugGrid, SWT.NONE, count);

								it.setText(0, sp[0]);
								if (sp2.length > 3) {
									it.setText(1, sp2[2]);
									it.setText(2, sp2[3]);
								}
							}

						}
					}
					if (printConsole) {
						
						printToDebugTextView(line);
						
					}

					count++;

				}
			} catch (IOException e) {
				System.err.println("Error: " + e);
			}

			input.close();
			debugSocket.close();

		} catch (IOException e) {

			e.printStackTrace();
		}
		return data;
	}

	private void printToDebugTextView(String line) {
		outputLine = line;
		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {
			public void run() {
				styledTxT = DebugTextView.getStyledText();

				if (styledTxT!=null&&styledTxT.isDisposed() == false) {

					styledTxT.append(outputLine);
					styledTxT.append("\n");
				}

			}

		});
	}

	private void deleteDebugTextView() {
		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {
			public void run() {
				styledTxT = DebugTextView.getStyledText();

				if (styledTxT != null && styledTxT.isDisposed() == false) {
					styledTxT.setText("");
				}
			}
		});
	}

	private void deleteSpreadSheet() {
		/* Delete all items in the debug spreadsheet! */
		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {
			public void run() {
				debugGrid = DebugVariablesView.getDebugVariablesGrid();

				if (debugGrid!=null&&debugGrid.isDisposed() == false) {
					int itemCount = debugGrid.getItemCount();
					if (itemCount > 0) {

						debugGrid.remove(0, itemCount - 1);

					}
				}

			}

		});
	}

}
