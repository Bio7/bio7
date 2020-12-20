package com.eco.bio7.rcp;

import com.eco.bio7.console.Console;

public class StartBio7Utils {

	public Console cons;
	public com.eco.bio7.console.ConsoleStreams eco;// The stream for the console!!
	public static StartBio7Utils consoleInstance;

	public StartBio7Utils() {
		consoleInstance = this;
	}

	public void startutils() {
		try {
			cons = new Console("Bio7 Console");

		} catch (RuntimeException e) {

			e.printStackTrace();
		}
		try {
			/*Start the streams for the console!*/
			eco = new com.eco.bio7.console.ConsoleStreams();
		} catch (RuntimeException e) {

			e.printStackTrace();
		}

	}

	public static StartBio7Utils getConsoleInstance() {
		return consoleInstance;
	}

}
