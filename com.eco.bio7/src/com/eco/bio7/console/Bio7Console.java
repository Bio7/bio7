/*******************************************************************************
 * Copyright (c) 2005-2017 M. Austenfeld
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/

package com.eco.bio7.console;

import java.io.OutputStream;
import java.io.PrintWriter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Display;
import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.os.pid.UnixProcessManager;
import com.eco.bio7.util.Util;

/**
 * @author Bio7 A class to write commands to the Bio7 console!
 */
public class Bio7Console {

	private static final char IAC = (char) 5;
	private static final char BRK = (char) 3;

	/**
	 * A method to write to the console.
	 * 
	 * @param command
	 *            a command for the console
	 * @param lineSeperator
	 *            a boolean if a line seperator should be written to the console.
	 * @param addToHistory
	 *            if the command should be added to the command history.
	 */
	public static void write(String command, boolean lineSeperator, boolean addToHistory) {

		ConsolePageParticipant.pipeInputToConsole(command, lineSeperator, addToHistory);

	}

	/**
	 * A method to set the encoding.
	 * 
	 * @param encoding
	 *            the encoding which will be stored in the preferences!
	 */
	public static void setEncoding(String encoding) {
		IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
		store.setValue("Console_Encoding", encoding);

	}

	/**
	 * A method which returns the selected Console.
	 * 
	 * @return a String representation of the selected console.
	 */
	public static String getConsoleSelection() {
		String selectionConsole = ConsolePageParticipant.getInterpreterSelection();
		return selectionConsole;
	}

	/**
	 * A method to open or start the shell, R or Python in the Bio7 console.
	 * 
	 * @param selection
	 *            the selected shell or native interpreter.
	 */
	public static void setConsoleSelection(String selection) {
		Display dis = Util.getDisplay();
		dis.syncExec(new Runnable() {

			public void run() {
				ConsolePageParticipant.setNativeInterpreterSelection(selection);

			}

		});

	}

	/**
	 * A method to send a CTRL+C event to the Windows OS shell.
	 */
	public static void sendWinCtrlC() {
		ConsolePageParticipant console = ConsolePageParticipant.getConsolePageParticipantInstance();
		console.sendWindowBreakHandler(false);
	}

	/**
	 * A method to send a CTRL+BREAK event to the Windows OS shell.
	 */
	public static void sendWinCtrlBreak() {
		ConsolePageParticipant console = ConsolePageParticipant.getConsolePageParticipantInstance();
		console.sendWindowBreakHandler(true);
	}

	/**
	 * A method to send a CTRL+BREAK event to the Linux OS shell.
	 */
	public static void sendLinCtrlC() {
		ConsolePageParticipant console = ConsolePageParticipant.getConsolePageParticipantInstance();
		UnixProcessManager.sendSigIntToProcessTree(console.getNativeShellProcess());
	}

	/**
	 * Sends sequence of two chars(codes 5 and 3) to a process output stream Source
	 * from: https://github.com/joewalnes/idea-community/blob/master/platform
	 * /platform-impl/src/com/intellij/execution/process/RunnerMediator.java
	 * 
	 * Copyright 2000-2010 JetBrains s.r.o.
	 *
	 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
	 * use this file except in compliance with the License. You may obtain a copy of
	 * the License at
	 *
	 * http://www.apache.org/licenses/LICENSE-2.0
	 *
	 * Unless required by applicable law or agreed to in writing, software
	 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
	 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
	 * License for the specific language governing permissions and limitations under
	 * the License.
	 */

	public static void sendCtrlBreakThroughStream() {
		ConsolePageParticipant console = ConsolePageParticipant.getConsolePageParticipantInstance();
		Process process = console.getNativeShellProcess();
		if (process != null) {
			OutputStream os = process.getOutputStream();
			PrintWriter pw = new PrintWriter(os);
			try {
				// pw.print(IAC);
				pw.print(BRK);
				pw.flush();
			} finally {
				pw.close();
			}
		}
	}

}
