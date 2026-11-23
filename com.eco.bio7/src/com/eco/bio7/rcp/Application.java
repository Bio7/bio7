package com.eco.bio7.rcp;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

public class Application implements IApplication {

	@Override
	public Object start(IApplicationContext context) throws Exception {

		Display display = PlatformUI.createDisplay();
		try {
			/* One Bio7 per workspace: a workspace used by another Bio7 is not opened twice */
			if (!lockWorkspace(display)) {
				return IApplication.EXIT_OK;
			}
			WorkspaceSwitcher.remember(WorkspaceSwitcher.current());
			/* Once: the stale syntax fonts of the editors (before the editors read them) */
			EditorFontMigration.run();
			int returnCode = PlatformUI.createAndRunWorkbench(display, new ApplicationWorkbenchAdvisor());
			if (returnCode == PlatformUI.RETURN_RESTART) {
				/*
				 * The JVM runs in the launcher process (-vm libjli), so System.exit below also
				 * ends the launcher and it cannot restart Bio7. A helper process starts Bio7
				 * again (e.g., after an update or a theme change)!
				 */
				DebugRestart.relaunchAfterExit();
				return IApplication.EXIT_RESTART;
			}
			return IApplication.EXIT_OK;
		} finally {
			/*
			 * Close display to free all SWT resource. Important for MacOSX (former version
			 * did not close display for JOGL which has now been ported to NewtSWTCanvas).
			 * If not closed Java crashes at exit!
			 * We now use System.exit to avoid a deadlock on MacOSX with SWT_AWT ImageJ!
			 */
			//display.dispose();
			/*
			 * The exit must not hang (e.g., shutdown hooks or native threads of the embedded
			 * R/Python, AWT): the workbench is closed and saved already, after 10 s the
			 * process is ended (else a restart waits for it).
			 */
			Thread watchdog = new Thread(() -> {
				try {
					Thread.sleep(10_000);
				} catch (InterruptedException e) {
					return;
				}
				Runtime.getRuntime().halt(0);
			}, "Bio7 exit watchdog");
			watchdog.setDaemon(true);
			watchdog.start();
			System.exit(0); // for an error analysis comment out this command.

			
		}
	}

	/*
	 * Locks the workspace (as Eclipse). If another Bio7 uses it: another workspace
	 * can be chosen (Bio7 starts again in it) or Bio7 exits. Returns true if the
	 * workbench can start.
	 */
	private static boolean lockWorkspace(Display display) {
		Location instance = Platform.getInstanceLocation();
		if (instance == null || instance.isReadOnly() || instance.getURL() == null) {
			return true;
		}
		try {
			if (instance.lock()) {
				return true;
			}
		} catch (IOException | IllegalStateException e) {
			/* A file system without locks (e.g. some network drives): as before, without a lock */
			return true;
		}
		File workspace = WorkspaceSwitcher.current();
		Shell shell = new Shell(display);
		try {
			int choice = MessageDialog.open(MessageDialog.QUESTION, shell, "Workspace in Use",
					"The workspace\n" + workspace + "\nis used by another Bio7.\n\nChoose another workspace, or close the other Bio7 and start Bio7 again.",
					0, "Choose Another Workspace...", "Exit");
			if (choice == 0) {
				File other = WorkspaceSwitcher.choose(shell, workspace);
				if (other != null) {
					WorkspaceSwitcher.remember(other);
					if (!DebugRestart.startInWorkspaceAfterExit(other)) {
						MessageDialog.openError(shell, "Workspace in Use", "Bio7 can't be started again (the launcher was not found).\nStart Bio7 with: -data " + other);
					}
				}
			}
		} finally {
			shell.dispose();
		}
		return false;
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

}
