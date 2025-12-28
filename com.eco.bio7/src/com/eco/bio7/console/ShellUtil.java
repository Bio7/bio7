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

		// properties.put(ITerminalsConnectorConstants.PROP_PROCESS_PATH,
		// "/usr/local/bin/r");
		// properties.put(ITerminalsConnectorConstants.PROP_PROCESS_ARGS, "-e
		// 'library(Rserve);run.Rserve()'");

		// terminalService.openConsole(properties, null);

		// 4. Retrieve and Open Service
		Bundle bundle = FrameworkUtil.getBundle(this.getClass());
		ServiceTracker<ITerminalService, ITerminalService> tracker = new ServiceTracker<>(bundle.getBundleContext(),
				ITerminalService.class, null);
		tracker.open();

		ITerminalService terminalService = tracker.getService();
		if (terminalService != null) {
			// Pass 'null' for the ITerminalService.Done callback
			if (terminalService != null) {
				// 1. Force the Terminal View to become visible
				Display.getDefault().syncExec(() -> {
					// Place showView and openConsole code here

					try {
						IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
						page.showView("org.eclipse.terminal.view.ui.TerminalsView");
					} catch (Exception e) {
						e.printStackTrace(); // View ID might vary in some custom RCPs
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

		Map<String, Object> properties = new HashMap<>();

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

		ITerminalService terminalService = tracker.getService();
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
}
