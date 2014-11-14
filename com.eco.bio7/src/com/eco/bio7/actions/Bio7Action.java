package com.eco.bio7.actions;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

import com.eco.bio7.rcp.ApplicationActionBarAdvisor;
import com.eco.bio7.rcp.StartBio7Utils;

/**
 * This class provides some static methods to access some internal Bio7 methods.
 * 
 * @author Bio7
 * 
 */
public class Bio7Action {

	/**
	 * Starts the calculation thread of the Bio7 application.
	 */
	public static void startCalculation() {
		ApplicationActionBarAdvisor.getStart().run();

	}

	/**
	 * Stops the calculation thread of the Bio7 application.
	 */
	public static void stopCalculation() {
		ApplicationActionBarAdvisor.getStart().run();

	}

	/**
	 * Resets the counter of the Bio7 application.
	 */
	public static void reset() {
		ApplicationActionBarAdvisor.getCounterreset().run();

	}

	/**
	 * Starts or stops the Rserve application.
	 */
	public static void callRserve() {
		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {

			public void run() {
				ApplicationActionBarAdvisor.getStartrserve().run();
			}
		});

	}

	/**
	 * Opens the connection to OpenOffice.
	 */
	public static void callOpenOffice() {
		ApplicationActionBarAdvisor.getOpenofficeconnection().run();

	}

	/**
	 * Stops a running flow in the Bio7 application.
	 */
	public static void stopFlow() {
		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {

			public void run() {
				ApplicationActionBarAdvisor.getFlowstop().run();
			}
		});
	}

	/**
	 * Starts a flow in the Bio7 application.
	 */
	public static void flowStart() {
		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {

			public void run() {
				ApplicationActionBarAdvisor.getFlowexternalstartaction().run();

			}
		});
	}

	/**
	 * A method to call the default imports for the BeanShell interpreter.
	 */
	public static void defaultImports() {
		ApplicationActionBarAdvisor.getBshimportaction().run();
	}

	/**
	 * Returns the platform display of the Bio7 application.
	 * 
	 * 
	 */
	public static Display getBio7Display() {
		return PlatformUI.getWorkbench().getDisplay();
	}

	/**
	 * Clears the console of the Bio7 application.
	 */
	public static void clearConsole() {
		StartBio7Utils.getConsoleInstance().cons.getConsole().clearConsole();

	}

}
