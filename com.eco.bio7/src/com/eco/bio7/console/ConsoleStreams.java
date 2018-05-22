package com.eco.bio7.console;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

import com.eco.bio7.rbridge.views.RShellView;
import com.eco.bio7.rcp.StartBio7Utils;
import com.eco.bio7.util.Util;

public class ConsoleStreams {

	private static ObservedByteArrayOutputStream Bio7ErrorOutput;

	private static ObservedByteArrayOutputStream Bio7ErrorOutput2;

	private static PrintStream Bio7ErrorStream;

	private static PrintStream Bio7OutputStream;

	private static PrintStream Bio7ErrorStream2;

	private static PrintStream Bio7OutputStream2;

	public static java.awt.Button clear;

	public static java.awt.Button close;

	class ObservedByteArrayOutputStream extends ByteArrayOutputStream {
		public PrintStream ConnectedPrintStream;

		public void write(byte[] b, int off, int len) {
			super.write(b, off, len);
			ConnectedPrintStream.checkError();

			String out = this.toString();
			// Print to the console !!!!
			StartBio7Utils.getConsoleInstance().cons.print(out);
			StyledText rshellViewTextConsole = RShellView.getTextConsole();
			if (rshellViewTextConsole != null) {
				Display display = Util.getDisplay();
				display.asyncExec(new Runnable() {

					public void run() {
						rshellViewTextConsole.append(out);
						rshellViewTextConsole.setTopIndex(rshellViewTextConsole.getLineCount() - 1);
					}
				});
			}

			this.reset();
		}
	}

	public ConsoleStreams() {
		System.setErr(Bio7ErrorStream);
		System.setOut(Bio7OutputStream);
		Bio7ErrorStream2 = System.err;
		Bio7OutputStream2 = System.out;

		Bio7ErrorOutput = new ObservedByteArrayOutputStream();
		Bio7ErrorStream = new PrintStream(Bio7ErrorOutput, true);
		Bio7ErrorOutput.ConnectedPrintStream = Bio7ErrorStream;
		System.setErr(Bio7ErrorStream);

		Bio7ErrorOutput2 = new ObservedByteArrayOutputStream();
		Bio7OutputStream = new PrintStream(Bio7ErrorOutput2, true);
		Bio7ErrorOutput2.ConnectedPrintStream = Bio7OutputStream;
		System.setOut(Bio7OutputStream);

	}

	public static void clear() {
		Bio7ErrorOutput2.reset();
		Bio7ErrorOutput.reset();

	}

	public static void end() {
		clear();
		System.setErr(Bio7ErrorStream2);
		System.setOut(Bio7OutputStream2);

	}

	public static PrintStream getBio7OutputStream() {
		return Bio7OutputStream;
	}

}