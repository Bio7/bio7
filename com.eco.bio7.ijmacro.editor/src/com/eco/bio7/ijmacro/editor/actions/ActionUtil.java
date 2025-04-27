package com.eco.bio7.ijmacro.editor.actions;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

public class ActionUtil {
	public static Display openBrowser() {
		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {
			public void run() {
				try {
					IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
					page.showView("com.eco.bio7.browser.Browser");
				} catch (PartInitException e) {
					// TODO Auto-generated catch
					// block
					e.printStackTrace();
				}

			}
		});
		return display;
	}
}
