package com.eco.bio7.rcp;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsoleManager;
import org.rosuda.REngine.Rserve.RConnection;
import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.rbridge.RConnectionJob;
import com.eco.bio7.rbridge.RServe;
import com.eco.bio7.rbridge.TerminateRserve;
import com.eco.bio7.worldwind.WorldWindView;

public class Application implements IApplication {

	/*
	 * public Object run(Object args) throws Exception { Display display =
	 * PlatformUI.createDisplay();
	 * 
	 * try { int returnCode = PlatformUI.createAndRunWorkbench(display, new
	 * ApplicationWorkbenchAdvisor()); if (returnCode == PlatformUI.RETURN_RESTART)
	 * { return IPlatformRunnable.EXIT_RESTART; } return IPlatformRunnable.EXIT_OK;
	 * } finally {
	 * 
	 * if (RConnectionJob.getProc() != null) { RConnectionJob.getProc().destroy();//
	 * Kills the Rserve // application!" }
	 * 
	 * terminate_rserve(); display.dispose();
	 * 
	 * Save the ImageJ preferences! try { ij.Prefs.savePreferences(); } catch
	 * (RuntimeException e) {
	 * 
	 * e.printStackTrace(); } Finally save the workspace! saveWorkspace();
	 * 
	 * }
	 * 
	 * }
	 */

	@Override
	public Object start(IApplicationContext context) throws Exception {

		Display display = PlatformUI.createDisplay();
		try {
			int returnCode = PlatformUI.createAndRunWorkbench(display, new ApplicationWorkbenchAdvisor());
			if (returnCode == PlatformUI.RETURN_RESTART) {
				return IApplication.EXIT_RESTART;
			}
			return IApplication.EXIT_OK;
		} finally {

			// IConsoleManager manager = ConsolePlugin.getDefault().getConsoleManager();

			/*
			 * IConsole[] consoles = manager.getConsoles(); for (int i = 0; i <
			 * consoles.length; i++) { if(consoles[i] instanceof IOConsole){ IOConsole io =
			 * (IOConsole) consoles[i]; io.partitionerFinished(); io.destroy(); } }
			 */

			/*
			 * Close display to free all SWT resource. Important for MacOSX (former version
			 * did not close display for JOGL which has now been ported to NewtSWTCanvas).
			 * If not closed Java crashes at exit!
			 */
			display.dispose();

			
		}
	}

	@Override
	public void stop() {
		if (!PlatformUI.isWorkbenchRunning())
			return;
		final IWorkbench workbench = PlatformUI.getWorkbench();
		final Display display = workbench.getDisplay();
		display.syncExec(() -> {
			if (!display.isDisposed())
				workbench.close();
		});
	}

	/*
	 * @Override public void stop() { final IWorkbench workbench =
	 * PlatformUI.getWorkbench(); if (workbench == null) return; final Display
	 * display = workbench.getDisplay(); display.syncExec(new Runnable() { public
	 * void run() { if (!display.isDisposed()) workbench.close(); } });
	 * 
	 * }
	 */

}
