package com.eco.bio7.console;

import groovy.util.GroovyScriptEngine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IRegion;
import org.eclipse.swt.graphics.Color;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.eclipse.ui.console.TextConsolePage;
import org.eclipse.ui.console.TextConsoleViewer;

import com.eco.bio7.scriptengines.ScriptEngineConnection;

public class Console {
	private IOConsole bio7Console;

	private IOConsoleOutputStream stream;

	public Console() {

		bio7Console = new IOConsole("Bio7 Console", null);

		ConsolePlugin.getDefault().getConsoleManager().addConsoles(new IConsole[] { bio7Console });
		ConsolePlugin.getDefault().getConsoleManager().showConsoleView(bio7Console);

		stream = bio7Console.newOutputStream();


	}

	public Color getColor() {
		return stream.getColor();
	}

	public IOConsole getConsole() {
		return bio7Console;
	}

	public int hashCode() {
		return stream.hashCode();
	}

	public void print(String message) {
		try {
			if (stream.isClosed() == false) {
				if (message != null) {
					stream.write(message);
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void println() {
		try {
			if (stream.isClosed() == false) {
				stream.write("\n");
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void clear() {
		bio7Console.clearConsole();
	}

	public void println(String message) {
		try {
			if (stream.isClosed() == false) {
				if (message != null) {
					stream.write(message);
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void setColor(Color color) {
		stream.setColor(color);
	}

}