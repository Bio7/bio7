package com.eco.bio7.image;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;

public class ImageRoiSelectionInputDialog extends Dialog {
	public Spinner spinner;
	private Button btnCreateSignature;
    private boolean sel=true;
    private Label lblSignature;
	/**
	 * Create the dialog.
	 * @param parentShell
	 */
	public ImageRoiSelectionInputDialog(Shell parentShell) {
		super(parentShell);
	}

	/**
	 * Create contents of the dialog.
	 * @param parent
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		container.setLayout(null);
		{
			spinner = new Spinner(container, SWT.BORDER);
			spinner.setBounds(10, 122, 88, 32);
		}
		{
			lblSignature = new Label(container, SWT.NONE);
			lblSignature.setBounds(10, 76, 375, 40);
			lblSignature.setText("Custom Class Signature");
		}
		{
			btnCreateSignature = new Button(container, SWT.CHECK);
			btnCreateSignature.setBounds(10, 10, 375, 60);
			btnCreateSignature.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					sel=!sel;
				}
			});
			btnCreateSignature.setSelection(true);
			btnCreateSignature.setText("Create Custom Class Signature (if desel. increasing sign. is created!)");
		}

		return container;
	}

	/**
	 * Create contents of the button bar.
	 * @param parent
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}

	/**
	 * Return the initial size of the dialog.
	 */
	@Override
	protected Point getInitialSize() {
		return new Point(418, 233);
	}
	protected void buttonPressed(int buttonId) {
		if (buttonId == IDialogConstants.CANCEL_ID) {
			this.cancelPressed();
			ImageRoiSelectionTransferJob.cancelJob=true;
			
			return;
		}
		if (buttonId == IDialogConstants.OK_ID) {
			ImageRoiSelectionTransferJob.cancelJob=false;
			ImageRoiSelectionTransferJob.doSetSignature(sel);
			ImageRoiSelectionTransferJob.setSignatureNumber(spinner.getSelection());
			
			this.okPressed();
			return;
		}
		super.buttonPressed(buttonId);
	}
}
