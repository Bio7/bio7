package com.eco.bio7.collection;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;

/**
 * A small modal dialog letting the user pick one of the currently
 * connected monitors.
 */
public class MonitorChooser extends Dialog {

	private List list;
	private Monitor[] monitors;
	private Monitor selected;

	public MonitorChooser(Shell parentShell) {
		super(parentShell);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = (Composite) super.createDialogArea(parent);
		composite.setLayout(new GridLayout(1, false));

		Label label = new Label(composite, SWT.NONE);
		label.setText("Select the monitor for the detached panel:");

		list = new List(composite, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.heightHint = 120;
		gd.widthHint = 320;
		list.setLayoutData(gd);

		monitors = Display.getDefault().getMonitors();
		Monitor primary = Display.getDefault().getPrimaryMonitor();
		for (int i = 0; i < monitors.length; i++) {
			org.eclipse.swt.graphics.Rectangle b = monitors[i].getBounds();
			String label2 = "Monitor " + (i + 1) + "  (" + b.width + "x" + b.height + " at " + b.x + "," + b.y + ")"
					+ (monitors[i].equals(primary) ? "  [Primary]" : "");
			list.add(label2);
		}
		if (monitors.length > 0) {
			list.select(0);
		}

		return composite;
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Choose Monitor");
	}

	@Override
	protected void okPressed() {
		int idx = list.getSelectionIndex();
		if (idx >= 0 && idx < monitors.length) {
			selected = monitors[idx];
		} else if (monitors.length > 0) {
			selected = monitors[0];
		}
		super.okPressed();
	}

	/**
	 * Opens the dialog and returns the chosen monitor, or {@code null} if
	 * the user canceled.
	 */
	public Monitor chooseMonitor() {
		if (open() == IDialogConstants.OK_ID) {
			return selected;
		}
		return null;
	}
}