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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.console.ConsolePageParticipant;

public class DebugContinueAction extends Action {
	private int line = 1;
	private Socket debugSocket;

	public DebugContinueAction() {
		super("Continue");

		setId("Continue");
		setText("Continue");

		ImageDescriptor desc = ImageDescriptor.createFromImage(new Image(Display.getCurrent(), getClass().getResourceAsStream("/pics/stepreturn_co.gif")));

		this.setImageDescriptor(desc);
	}

	public void run() {

		String selectionConsole = ConsolePageParticipant.getInterpreterSelection();

		if (selectionConsole.equals("R")) {

			//System.out.println("c");

			IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
			int port = store.getInt("R_DEBUG_PORT");

			ConsolePageParticipant con = ConsolePageParticipant.getConsolePageParticipantInstance();

			// con.pipeToRConsole("con1 <- socketConnection(port = "+port+", server = TRUE);sink(con1);c;sink();close(con1)");
			con.pipeToRConsole("con1 <- socketConnection(port = " + port + ", server = TRUE,timeout=10)");
			con.pipeToRConsole("sink(con1)");
			con.pipeToRConsole("c");
			con.pipeToRConsole("sink()");
			con.pipeToRConsole("close(con1)");
			con.pipeToRConsole("writeLines(\"\")");
			//System.out.println("c");

			String data = null;

			try {

				debugSocket = new Socket("127.0.0.1", port);
				debugSocket.setSoTimeout(10000);
				BufferedReader input = null;
				try {
					input = new BufferedReader(new InputStreamReader(debugSocket.getInputStream()));
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

				data = input.readLine();
				/*
				 * I simply add a ':' char at the end for the default pattern
				 * matcher!
				 */
				data = data + ":";
				input.close();
				debugSocket.close();

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			// System.out.println(data);

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

					}

				}

				else {
					// System.out.println("nothing to paste");
				}

			}

		} else {
			Bio7Dialog.message("Please start the \"Native R\" shell in the Bio7 console!");
		}

	}

}