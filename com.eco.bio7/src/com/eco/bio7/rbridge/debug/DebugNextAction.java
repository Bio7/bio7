/*******************************************************************************
 * Copyright (c) 2007-2012 M. Austenfeld
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/

package com.eco.bio7.rbridge.debug;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.console.ConsolePageParticipant;

public class DebugNextAction extends Action {

	private Clipboard clipboard;
	private int line = 1;

	public DebugNextAction() {
		super("Next");

		setId("Next");
		setText("Next");

		ImageDescriptor desc = ImageDescriptor.createFromImage(new Image(Display.getCurrent(), getClass().getResourceAsStream("/pics/stepover_co.gif")));

		this.setImageDescriptor(desc);
	}

	public void run() {

		String selectionConsole = ConsolePageParticipant.getInterpreterSelection();

		if (selectionConsole.equals("R")) {
			ConsolePageParticipant con = ConsolePageParticipant.getConsolePageParticipantInstance();

			con.pipeToRConsole("sink(file=\"clipboard\")");
			con.pipeToRConsole("n");
			con.pipeToRConsole("sink()");
			// If necessary: bw.write("\r\n");

			// ConsolePageParticipant.pipeInputToConsole("sink(file=\"clipboard\")",
			// true, false);
			// ConsolePageParticipant.pipeInputToConsole("n", true, false);
			// System.out.println("n");

			// ConsolePageParticipant.pipeInputToConsole("sink()", true, false);
			final ConsolePageParticipant inst = ConsolePageParticipant.getConsolePageParticipantInstance();

			clipboard = new Clipboard(Display.getCurrent());
			String data = (String) clipboard.getContents(TextTransfer.getInstance());
			System.out.println(data);

			IEditorPart edit = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();

			ITextEditor editor = (ITextEditor) edit;
			IDocumentProvider dp = editor.getDocumentProvider();
			IDocument doc = dp.getDocument(editor.getEditorInput());

			if (data != null && data.length() > 0) {
				/* Extract the number between .R# and : */
				Pattern p = Pattern.compile(".R#(.*?):");
				Matcher m = p.matcher(data);
				if (m.find()) {
					int temp = Integer.parseInt(m.group(1));
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
						System.out.println("Line is: " + line);
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