package com.eco.bio7.rbridge.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.rosuda.REngine.Rserve.RConnection;

import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.rbridge.RLibraryList;
import com.eco.bio7.rbridge.RServe;
import com.eco.bio7.rbridge.RState;
import com.eco.bio7.rcp.ApplicationWorkbenchWindowAdvisor;

public class LoadRLibrary extends Action {

	private RLibraryList shell;

	public LoadRLibrary(String text, IWorkbenchWindow window) {
		super(text);

		setId("com.eco.bio7.rserve_loadRLibrary");
		//setActionDefinitionId("com.eco.bio7.execute_r_load_RLibrary");
	}

	public void run() {

		RConnection d = RServe.getConnection();
		if (d != null) {
			if (RState.isBusy() == false) {

				try {
					final Display display = Display.getDefault();

					if (shell == null || shell.isDisposed()) {
						shell = null;
						if (ApplicationWorkbenchWindowAdvisor.getOS().equals("Linux")) {
							shell = new RLibraryList(display, SWT.SHELL_TRIM);
						} else {

							display.syncExec(new Runnable() {

								public void run() {

									shell = new RLibraryList(display, SWT.ON_TOP | SWT.SHELL_TRIM);
								}
							});
						}
					}

					shell.open();
					shell.layout();

				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {

				Bio7Dialog.message("RServer is busy!");
			}
		} else {

			MessageBox messageBox = new MessageBox(new Shell(),

			SWT.ICON_WARNING);
			messageBox.setMessage("RServer connection failed - Server is not running !");
			messageBox.open();

		}
	}

}