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
		container.setLayout(new FormLayout());
		{
			spinner = new Spinner(container, SWT.BORDER);
			{
				FormData formData = new FormData();
				formData.width = 59;
				formData.left = new FormAttachment(0, 10);
				formData.bottom = new FormAttachment(100, -10);
				spinner.setLayoutData(formData);
			}
		}
		{
			lblSignature = new Label(container, SWT.NONE);
			{
				FormData fd_lblSignature = new FormData();
				fd_lblSignature.left = new FormAttachment(0, 10);
				fd_lblSignature.bottom = new FormAttachment(spinner, -6);
				lblSignature.setLayoutData(fd_lblSignature);
			}
			lblSignature.setText("Custom Class Signature");
		}
		{
			btnCreateSignature = new Button(container, SWT.CHECK);
			btnCreateSignature.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					sel=!sel;
				}
			});
			{
				FormData formData = new FormData();
				formData.bottom = new FormAttachment(lblSignature, -18);
				formData.left = new FormAttachment(0, 10);
				btnCreateSignature.setLayoutData(formData);
			}
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
		return new Point(418, 184);
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
