package com.eco.bio7.console;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.widgets.Display;
import org.eclipse.terminal.view.core.ITerminalService;
import org.eclipse.terminal.view.core.ITerminalsConnectorConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.util.tracker.ServiceTracker;

/**
 * A shell utility class which opens a terminal and starts a process with
 * arguments.
 */
public class ShellUtil {

	Map<String, Object> properties;
	private ITerminalService terminalService;

	/**
	 * A method to execute shell commands
	 * 
	 * @param properties a HashMap with terminal properties.
	 */
	public void execShellCommand(HashMap<String, Object> properties) {

		properties.put(ITerminalsConnectorConstants.PROP_TERMINAL_CONNECTOR_ID,
				"org.eclipse.terminal.connector.local.LocalConnector");

		properties.put(ITerminalsConnectorConstants.PROP_DELEGATE_ID,
				"org.eclipse.terminal.connector.local.launcher.local");

		Bundle bundle = FrameworkUtil.getBundle(this.getClass());
		ServiceTracker<ITerminalService, ITerminalService> tracker = new ServiceTracker<>(bundle.getBundleContext(),
				ITerminalService.class, null);
		tracker.open();

		terminalService = tracker.getService();
		if (terminalService != null) {

			if (terminalService != null) {

				Display.getDefault().syncExec(() -> {

					try {
						IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
						page.showView("org.eclipse.terminal.view.ui.TerminalsView");
					} catch (Exception e) {
						e.printStackTrace();
					}

					terminalService.openConsole(properties);
				});
			}

		}
	}

	/**
	 * A method to execute shell commands.
	 * 
	 * @param title       the title (id) of the tab
	 * @param processPath the path to the process
	 * @param processArgs the arguments for the process
	 */
	public void execShellCommand(String title, String processPath, String processArgs) {

		properties = new HashMap<>();

		properties.put(ITerminalsConnectorConstants.PROP_TITLE, title);
		properties.put(ITerminalsConnectorConstants.PROP_PROCESS_PATH, processPath);

		properties.put(ITerminalsConnectorConstants.PROP_PROCESS_ARGS, processArgs);

		properties.put(ITerminalsConnectorConstants.PROP_TERMINAL_CONNECTOR_ID,
				"org.eclipse.terminal.connector.local.LocalConnector");

		properties.put(ITerminalsConnectorConstants.PROP_DELEGATE_ID,
				"org.eclipse.terminal.connector.local.launcher.local");

		Bundle bundle = FrameworkUtil.getBundle(this.getClass());
		ServiceTracker<ITerminalService, ITerminalService> tracker = new ServiceTracker<>(bundle.getBundleContext(),
				ITerminalService.class, null);
		tracker.open();

		terminalService = tracker.getService();
		if (terminalService != null) {

			if (terminalService != null) {
				Display.getDefault().syncExec(() -> {

					try {
						IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
						page.showView("org.eclipse.terminal.view.ui.TerminalsView");
					} catch (Exception e) {
						e.printStackTrace();
					}

					terminalService.openConsole(properties);

				});
			}

		}
	}

	/**
	 * A method to terminate the current process.
	 */
	public void terminateProcess() {
		terminalService.terminateConsole(properties);
	}

	/**
	 * A method to terminate the current process.
	 * 
	 * @param properties the properties the process started with.
	 */
	public void terminateProcess(HashMap<String, Object> properties) {
		this.properties = properties;
		terminalService.terminateConsole(properties);
	}

	/**
	 * A method to close the current process.
	 */
	public void closeProcess() {
		terminalService.closeConsole(properties);
	}

	/**
	 * A method to close the current process.
	 * 
	 * @param properties the properties the process started with.
	 */
	public void closeProcess(HashMap<String, Object> properties) {
		this.properties = properties;
		terminalService.closeConsole(properties);
	}

}
