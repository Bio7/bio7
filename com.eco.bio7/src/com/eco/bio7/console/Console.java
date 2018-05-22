package com.eco.bio7.console;

import java.io.IOException;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.console.IOConsoleOutputStream;

import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.rbridge.views.RShellView;
import com.eco.bio7.util.Util;

public class Console extends IOConsole {

	public final static String ID = "com.eco.bio7.IOConsole";
	private IOConsoleOutputStream stream;
	private IPreferenceStore store;

	public Console(String title) {
		super(title, null);
		store = Bio7Plugin.getDefault().getPreferenceStore();

		ConsolePlugin.getDefault().getConsoleManager().addConsoles(new IConsole[] { this });
		ConsolePlugin.getDefault().getConsoleManager().showConsoleView(this);

		stream = this.newOutputStream();

	}

	public Color getColor() {
		return stream.getColor();
	}

	public IOConsole getConsole() {
		return this;
	}

	/*
	 * public int hashCode() { return stream.hashCode(); }
	 */

	public void print(String message) {
		try {
			if (stream.isClosed() == false) {
				if (message != null) {
					stream.write(message);
                   /*If enabled we write to the RShell text panel!*/
					writeToRShellText(message);
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * @param message
	 */
	private void writeToRShellText(String message) {
		if (store.getBoolean("STREAM_TO_RSHELL")) {
			StyledText rshellViewTextConsole = RShellView.getTextConsole();
			if (rshellViewTextConsole != null) {
				Display display = Util.getDisplay();
				display.asyncExec(new Runnable() {

					public void run() {
						if (Util.getOS().equals("Windows")) {
							rshellViewTextConsole.append(message.replace("\r", ""));
						} else {
							rshellViewTextConsole.append(message);
						}
						rshellViewTextConsole.setTopIndex(rshellViewTextConsole.getLineCount() - 1);
					}
				});
			}
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
		this.clearConsole();
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