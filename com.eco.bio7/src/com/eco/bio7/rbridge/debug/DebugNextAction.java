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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.nebula.widgets.grid.Grid;
import org.eclipse.nebula.widgets.grid.GridItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.collection.Work;
import com.eco.bio7.console.ConsolePageParticipant;
import com.eco.bio7.rbridge.Bio7Grid;

public class DebugNextAction extends Action {

	private int line = 1;
	private Socket debugSocket;
	private String variable;
	protected Grid debugGrid;

	public DebugNextAction() {
		super("Next");

		setId("Next");
		setText("Next");

		ImageDescriptor desc = ImageDescriptor.createFromImage(new Image(Display.getCurrent(), getClass().getResourceAsStream("/pics/stepover_co.gif")));

		this.setImageDescriptor(desc);
	}

	public void run() {
		ArrayList<String> buf = new ArrayList<String>();
		String selectionConsole = ConsolePageParticipant.getInterpreterSelection();
		IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
		int port = store.getInt("R_DEBUG_PORT");
		if (selectionConsole.equals("R")) {
			ConsolePageParticipant con = ConsolePageParticipant.getConsolePageParticipantInstance();

			con.pipeToRConsole(".socketCon1 <- socketConnection(port = " + port + ", server = TRUE,timeout=10);" 
			+ "sink(.socketCon1)");

			con.pipeToRConsole("n");
			//System.out.println();
			con.pipeToRConsole(""
			//+ "sink(file=paste(.bio7tempenvpath$pathTemp,'debug.txt',sep=''));" 
			+ "ls.str();" 
			+ "sink();" 
			+ "close(.socketCon1);" 
			+ "writeLines(\"\")");

			/*
			 * con.pipeToRConsole("sink(file='clipboard')");
			 * con.pipeToRConsole("ls.str()"); // con.pipeToRConsole("where");
			 * con.pipeToRConsole("sink()");
			 * con.pipeToRConsole("close(.socketCon1)");
			 * con.pipeToRConsole("writeLines(\"\")");
			 */
			Display display = PlatformUI.getWorkbench().getDisplay();
			display.syncExec(new Runnable() {
				public void run() {
					Work.openView("com.eco.bio7.rbridge.debug.DebugVariablesView");
					debugGrid = DebugVariablesView.getDebugVariablesGrid();

					if (debugGrid != null) {
						int itemCount = debugGrid.getItemCount();
						if (itemCount > 0) {

							debugGrid.remove(0, itemCount - 1);

						}
					}
				}

			});

			final ConsolePageParticipant inst = ConsolePageParticipant.getConsolePageParticipantInstance();
			// inst.getIoc().clearConsole();
			String data = null;

			try {

				debugSocket = new Socket("127.0.0.1", port);
				debugSocket.setTcpNoDelay(true);
				debugSocket.setSoTimeout(5000);
				BufferedReader input = null;
				try {
					input = new BufferedReader(new InputStreamReader(debugSocket.getInputStream()));
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

				data = input.readLine();
				// variable=input.readLine();
				String line;
				int count = 0;
                
				while ((line = input.readLine()) != null) {
					System.out.print("xxxx");
					if(line.startsWith("debug")==false){
					
					/* Split the string to get the seperated values! */

					// String[] res = line.split("\\r?\\n");// find
					// linebreaks!.
					// for (int i = 0; i < res.length; i++) {

					String sp[] = line.split(":", 2);// find ':' at first
														// occurence!

					// occurence.
					if (sp.length == 2) {
						String sp2[] = sp[1].split("\\s+");// find
															// Whitespaces!

						GridItem it = new GridItem(debugGrid, SWT.NONE, count);
						it.setText(0, sp[0]);
						it.setText(1, sp2[1]);
						it.setText(2, sp2[2]);

					}

					// new GridItem(debugGrid, SWT.NONE, i).setText(1,
					// sp[1]);

					// }
					}

					count++;

				}
						
				
				input.close();
				debugSocket.close();

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			// System.out.println(data);
			// System.out.println(variable);
			IEditorPart edit = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();

			ITextEditor editor = (ITextEditor) edit;
			IDocumentProvider dp = editor.getDocumentProvider();
			IDocument doc = dp.getDocument(editor.getEditorInput());

			if (data != null && data.length() > 0) {
				/* Extract the number between .R# and : */
				Pattern p = Pattern.compile(".R#(.*?):");
				Matcher m = p.matcher(data);
				if (m.find()) {
					int temp = 0;
					try {
						temp = Integer.parseInt(m.group(1));
					} catch (NumberFormatException e1) {
						// TODO Auto-generated catch block
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
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						// System.out.println("Line is: " + line);
						editor.selectAndReveal(reg.getOffset() + reg.getLength(), 0);

						IResource resource = (IResource) editor.getEditorInput().getAdapter(IResource.class);
						try {
							resource.deleteMarkers("com.eco.bio7.reditor.debugrulermark", false, IResource.DEPTH_ZERO);
						} catch (CoreException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}

						IMarker marker;

						try {

							marker = resource.createMarker("com.eco.bio7.reditor.debugrulermark");
							marker.setAttribute(IMarker.CHAR_START, reg.getOffset());
							marker.setAttribute(IMarker.CHAR_END, reg.getOffset() + reg.getLength());
						} catch (CoreException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

					}

				}

				else {
					// System.out.println("nothing to paste");
				}
				
				
				IPreferenceStore s = new ScopedPreferenceStore(InstanceScope.INSTANCE, "com.eco.bio7");

				
				String tempPath;
				
					tempPath = s.getString("pathTempR") + "debug.txt";
				
				tempPath = tempPath.replace("\\", "/");
				// System.out.println(tempPath);
				BufferedReader br = null;
				try {
					File rPropFile = new File(tempPath);
					if (rPropFile.exists()) {

						br = new BufferedReader(new FileReader(rPropFile));
					}
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				/*Display display = PlatformUI.getWorkbench().getDisplay();
				display.syncExec(new Runnable() {
					public void run() {
						Work.openView("com.eco.bio7.rbridge.debug.DebugVariablesView");
						debugGrid = DebugVariablesView.getDebugVariablesGrid();

						if (debugGrid != null) {
							int itemCount = debugGrid.getItemCount();
							if (itemCount > 0) {

								debugGrid.remove(0, itemCount - 1);

							}
						}
					}

				});
				String line;
				int count = 0;
				try {
					while ((line = br.readLine()) != null) {
						System.out.println(line);
						 Split the string to get the seperated values! 

						// String[] res = line.split("\\r?\\n");// find
						// linebreaks!.
						// for (int i = 0; i < res.length; i++) {

						String sp[] = line.split(":", 0);// find ':' at first
															// occurence!

						// occurence.
						if (sp.length == 2) {
							String sp2[] = sp[1].split("\\s+");// find
																// Whitespaces!

							GridItem it = new GridItem(debugGrid, SWT.NONE, count);
							it.setText(0, sp[0]);
							it.setText(1, sp[1]);
							//it.setText(2, sp2[2]);

						}

						// new GridItem(debugGrid, SWT.NONE, i).setText(1,
						// sp[1]);

						// }

						count++;
						

					}
					br.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}*/
			
				/*Clipboard clipboard = new Clipboard(Display.getDefault());
				String dataClip = (String) clipboard.getContents(TextTransfer.getInstance());
				Work.openView("com.eco.bio7.rbridge.debug.DebugVariablesView");
				Grid debugGrid = DebugVariablesView.getDebugVariablesGrid();
				if(debugGrid!=null){
					int itemCount = debugGrid.getItemCount();
					if (itemCount > 0) {

						debugGrid.remove(0, itemCount - 1);

					}
				
				if (dataClip != null && dataClip.length() > 0) {
					String cl = dataClip.toString();

					String[] res = cl.split("\\r?\\n");// find linebreaks!.
					for (int i = 0; i < res.length; i++) {

						String sp[] = res[i].split(":", 0);// find ':' at first occurence!
						
						// occurence.
						if (sp.length == 2) {
							String sp2[] = sp[1].split("\\s+");// find Whitespaces!
							
							if(sp2.length==3){
								
								GridItem it = new GridItem(debugGrid, SWT.NONE, i);
								it.setText(0, sp[0]);
								it.setText(1, sp2[1]);
								it.setText(2, sp2[2]);
							}
							
							GridItem it = new GridItem(debugGrid, SWT.NONE, i);
							it.setText(0, sp[0]);
							
						}

						// new GridItem(debugGrid, SWT.NONE, i).setText(1,
						// sp[1]);

					}

				}*/
				/*
				 * for (int i = 0; i < buf.size(); i++) { Bio7Grid.setValue(i,
				 * 0, buf.get(i)); } buf.clear();
				 */

			}
			

		} else {
			Bio7Dialog.message("Please start the \"Native R\" shell in the Bio7 console!");
		}

	}
}