/*******************************************************************************
 * Copyright (c) 2007-2026 M. Austenfeld
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/
package com.eco.bio7.rcp;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IWorkbenchWindow;

import com.eco.bio7.Bio7Plugin;

/**
 * Restarts Bio7 once with a debug port (JDWP), so that a debugger (e.g. Eclipse:
 * Debug Configurations > Remote Java Application) can attach and debug the
 * dynamically compiled Java code inside Bio7. The Bio7.ini file is not changed:
 * a helper process waits until Bio7 has exited and starts the launcher again
 * with the additional VM argument (Bio7.ini contains --launcher.appendVmargs).
 * In the debug mode the same menu action restarts Bio7 without the debug port.
 */
public final class DebugRestart {

	private static final String PORT_PREF = "DEBUG_RESTART_PORT";
	private static final int DEFAULT_PORT = 8000;
	private static final Pattern ADDRESS = Pattern.compile("address=([^,]+)");

	private DebugRestart() {
	}

	/** True if Bio7 runs with a debug port (JDWP agent). */
	public static boolean isDebugMode() {
		return jdwpArgument() != null;
	}

	/** The address of the debug port (e.g. "127.0.0.1:8000"), null if not in the debug mode. */
	public static String debugAddress() {
		String arg = jdwpArgument();
		if (arg == null) {
			return null;
		}
		Matcher m = ADDRESS.matcher(arg);
		return m.find() ? m.group(1) : "";
	}

	private static String jdwpArgument() {
		for (String arg : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
			if (arg.startsWith("-agentlib:jdwp") || arg.startsWith("-Xrunjdwp")) {
				return arg;
			}
		}
		return null;
	}

	/** The File menu action (the text depends on the current mode). */
	public static Action createAction(IWorkbenchWindow window) {
		final boolean debug = isDebugMode();
		Action action = new Action(debug ? "Restart Without Debug Port..." : "Restart in Debug Mode...") {
			@Override
			public void run() {
				if (debug) {
					restartNormally(window);
				} else {
					restartInDebugMode(window);
				}
			}
		};
		action.setId("com.eco.bio7.restartDebugMode");
		action.setToolTipText(debug ? "Restart Bio7 without the debug port" : "Restart Bio7 with a port for a Java debugger");
		return action;
	}

	private static void restartInDebugMode(IWorkbenchWindow window) {
		IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
		int port = store.getInt(PORT_PREF);
		if (port <= 0) {
			port = DEFAULT_PORT;
		}
		InputDialog dialog = new InputDialog(window.getShell(), "Restart in Debug Mode",
				"Bio7 restarts once with a debug port, Bio7.ini is not changed.\n\n"
						+ "Then attach a Java debugger to this port, e.g. in Eclipse:\n"
						+ "Debug Configurations > Remote Java Application (host: localhost).\n\n"
						+ "The debugging information of the dynamic Java compiler is switched on,\n"
						+ "so that breakpoints in dynamically compiled classes work.\n\nPort (only reachable from this computer):",
				Integer.toString(port), text -> {
					try {
						int p = Integer.parseInt(text.trim());
						return p >= 1024 && p <= 65535 ? null : "Use a port between 1024 and 65535.";
					} catch (NumberFormatException e) {
						return "Use a port between 1024 and 65535.";
					}
				});
		if (dialog.open() != Window.OK) {
			return;
		}
		port = Integer.parseInt(dialog.getValue().trim());
		store.setValue(PORT_PREF, port);
		/* Breakpoints and variables need the debugging information (javac -g)! */
		IEclipsePreferences javaPrefs = InstanceScope.INSTANCE.getNode("com.eco.bio7.javaedit");
		javaPrefs.putBoolean("compiler_debug", true);
		try {
			javaPrefs.flush();
		} catch (Exception e) {
			e.printStackTrace();
		}
		restart(window, "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=127.0.0.1:" + port);
	}

	private static void restartNormally(IWorkbenchWindow window) {
		boolean ok = MessageDialog.open(MessageDialog.QUESTION, window.getShell(), "Restart Without Debug Port",
				"Bio7 runs in the debug mode (port " + debugAddress() + ").\n\nRestart Bio7 without the debug port?", 0,
				"Restart", IDialogConstants.CANCEL_LABEL) == 0;
		if (ok) {
			restart(window, null);
		}
	}

	/* Starts a helper which launches Bio7 again after this instance has exited, then closes Bio7! */
	private static void restart(IWorkbenchWindow window, String vmArgument) {
		String launcher = System.getProperty("eclipse.launcher");
		if (launcher == null || !new File(launcher).canExecute()) {
			MessageDialog.openError(window.getShell(), "Restart", "The Bio7 launcher was not found (eclipse.launcher: " + launcher
					+ ").\nPlease restart Bio7 manually.");
			return;
		}
		if (!window.getWorkbench().saveAllEditors(true)) {
			return;
		}
		try {
			startHelper(ProcessHandle.current().pid(), relaunchCommand(launcher, vmArgument));
		} catch (IOException e) {
			MessageDialog.openError(window.getShell(), "Restart", "Bio7 cannot be restarted: " + e.getMessage());
			return;
		}
		window.getWorkbench().close();
	}

	/**
	 * Starts Bio7 again after this instance has exited (for a restart requested by
	 * the workbench, e.g. after an update). The debug port is kept in the debug
	 * mode. Called when the workbench has already been closed.
	 */
	public static void relaunchAfterExit() {
		/*
		 * Started from the Eclipse IDE (development mode): the launcher of the IDE does
		 * not know the launch configuration, so the same command is started again!
		 */
		if (System.getProperty("osgi.dev") != null) {
			List<String> command = currentCommand();
			if (command != null) {
				try {
					startHelper(ProcessHandle.current().pid(), command);
					return;
				} catch (IOException e) {
					System.err.println("[Bio7] Restart not possible: " + e.getMessage());
				}
			}
		}
		String launcher = System.getProperty("eclipse.launcher");
		if (launcher == null || !new File(launcher).canExecute()) {
			System.err.println("[Bio7] Restart not possible, the launcher was not found: " + launcher);
			return;
		}
		try {
			startHelper(ProcessHandle.current().pid(), relaunchCommand(launcher, jdwpArgument()));
		} catch (IOException e) {
			System.err.println("[Bio7] Restart not possible: " + e.getMessage());
		}
	}

	/* The command line of this process (Java, arguments of the JVM and of the application) or null! */
	private static List<String> currentCommand() {
		ProcessHandle.Info info = ProcessHandle.current().info();
		if (info.command().isEmpty() || info.arguments().isEmpty()) {
			return null;
		}
		List<String> command = new ArrayList<String>();
		command.add(info.command().get());
		for (String argument : info.arguments().get()) {
			command.add(argument);
		}
		return command;
	}

	private static List<String> relaunchCommand(String launcher, String vmArgument) {
		List<String> command = new ArrayList<String>();
		command.add(launcher);
		if (Platform.getInstanceLocation() != null && Platform.getInstanceLocation().getURL() != null) {
			command.add("-data");
			command.add(new File(Platform.getInstanceLocation().getURL().getFile()).getAbsolutePath());
		}
		if (vmArgument != null) {
			command.add("-vmargs");
			command.add(vmArgument);
		}
		return command;
	}

	/* The helper waits until the process has exited (the workspace is unlocked) and starts the command! */
	private static void startHelper(long pid, List<String> command) throws IOException {
		List<String> helper = new ArrayList<String>();
		if (File.separatorChar == '\\') {
			StringBuilder args = new StringBuilder();
			for (int i = 1; i < command.size(); i++) {
				args.append(i > 1 ? "," : "").append(psQuote("\"" + command.get(i) + "\""));
			}
			String script = "Wait-Process -Id " + pid + " -ErrorAction SilentlyContinue; Start-Process -FilePath " + psQuote(command.get(0))
					+ (args.length() > 0 ? " -ArgumentList " + args : "");
			helper.add("powershell.exe");
			helper.add("-NoProfile");
			helper.add("-WindowStyle");
			helper.add("Hidden");
			helper.add("-Command");
			helper.add(script);
		} else {
			helper.add("/bin/sh");
			helper.add("-c");
			helper.add("while kill -0 " + pid + " 2>/dev/null; do sleep 0.5; done; exec \"$@\"");
			helper.add("bio7-restart");
			helper.addAll(command);
		}
		ProcessBuilder builder = new ProcessBuilder(helper);
		builder.redirectOutput(ProcessBuilder.Redirect.DISCARD);
		builder.redirectError(ProcessBuilder.Redirect.DISCARD);
		builder.start();
	}

	private static String psQuote(String s) {
		return "'" + s.replace("'", "''") + "'";
	}
}
