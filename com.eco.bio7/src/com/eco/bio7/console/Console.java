package com.eco.bio7.console;

import java.io.IOException;

import org.eclipse.swt.graphics.Color;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.console.IOConsoleOutputStream;

public class Console extends IOConsole{
	
	public final static String ID = "com.eco.bio7.IOConsole";
	private IOConsoleOutputStream stream;

	public Console(String title) {
		super(title, null);

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

	/*public int hashCode() {
		return stream.hashCode();
	}*/

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