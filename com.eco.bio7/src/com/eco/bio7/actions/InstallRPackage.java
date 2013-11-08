package com.eco.bio7.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.rosuda.REngine.Rserve.RConnection;

import com.eco.bio7.rbridge.PackagesList;
import com.eco.bio7.rbridge.RServe;
import com.eco.bio7.rcp.ApplicationWorkbenchWindowAdvisor;

public class InstallRPackage extends Action {

	private PackagesList shell;

	public InstallRPackage(String text, IWorkbenchWindow window) {
		super(text);

		setId("com.eco.bio7.rserve_installRPackage");
		//setActionDefinitionId("com.eco.bio7.execute_r_install_package");
	}

	public void run() {
		
		RConnection d = RServe.getConnection();
		if (d != null) {

			try {
				Display display = Display.getDefault();

				if (shell == null || shell.isDisposed()) {
					shell = null;
					if (ApplicationWorkbenchWindowAdvisor.getOS().equals("Linux")) {
						shell = new PackagesList(display, SWT.SHELL_TRIM);
					} else {
						shell = new PackagesList(display, SWT.ON_TOP | SWT.SHELL_TRIM);
					}
				}

				shell.open();
				shell.layout();

			} catch (Exception e) {
				e.printStackTrace();
			}

		} else {

			MessageBox messageBox = new MessageBox(new Shell(),

			SWT.ICON_WARNING);
			messageBox.setMessage("RServer connection failed - Server is not running !");
			messageBox.open();

		}
	}

}