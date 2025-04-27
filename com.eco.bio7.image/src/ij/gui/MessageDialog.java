/*Message Dialog copied from the Bio7 project.
 * Author: Marcel Austenfeld*/
package ij.gui;

import java.awt.Frame;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/* Changed for Bio7! A modal dialog base on SWT as a replacement for the ImageJ message dialog! */

public class MessageDialog {
	protected MultiLineLabel label;
	private boolean escapePressed;
	protected int result;

	public MessageDialog(Frame parent, String title, String message) {

		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {
			public void run() {
				MessageBox messageBox = new MessageBox(getShell(),

						SWT.ICON_WARNING);

				messageBox.setText(title);
				messageBox.setMessage(message);
				result = messageBox.open();
				if (result == SWT.OK) {
					escapePressed = true; 
				}
			}
		});

	}

	/**
	 * Returns a platform shell for dialogs, etc.
	 * 
	 * @return a shell
	 */
	public static Shell getShell() {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (window == null) {
			IWorkbenchWindow[] windows = PlatformUI.getWorkbench().getWorkbenchWindows();
			if (windows.length > 0) {
				return windows[0].getShell();
			}
		} else {
			return window.getShell();
		}
		return null;
	}

	public boolean escapePressed() {
		return escapePressed;
	}

}
