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

public class ImageSelectionInputDialog extends Dialog {
	public Text txtMatrix;
	public Spinner spinner;
	private FormData formData_1;
	private Button btnCreateSignature;
    private boolean sel=true;
    private FormData formData_2;
    private FormData formData_3;
	/**
	 * Create the dialog.
	 * @param parentShell
	 */
	public ImageSelectionInputDialog(Shell parentShell) {
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
			txtMatrix = new Text(container, SWT.BORDER);
			txtMatrix.setText("matrix");
			{
				formData_1 = new FormData();
				formData_1.left = new FormAttachment(0, 10);
				formData_1.right = new FormAttachment(100, -10);
				formData_1.width = 144;
				txtMatrix.setLayoutData(formData_1);
			}
		}
		{
			spinner = new Spinner(container, SWT.BORDER);
			{
				formData_3 = new FormData();
				formData_3.left = new FormAttachment(0, 10);
				formData_3.width = 59;
				spinner.setLayoutData(formData_3);
			}
		}
		{
			Label lblMatrixName = new Label(container, SWT.NONE);
			formData_1.top = new FormAttachment(lblMatrixName, 6);
			{
				FormData formData = new FormData();
				formData.left = new FormAttachment(0, 10);
				formData.top = new FormAttachment(0, 10);
				lblMatrixName.setLayoutData(formData);
			}
			lblMatrixName.setText("Matrix Name");
		}
		{
			Label lblSignature = new Label(container, SWT.NONE);
			formData_3.top = new FormAttachment(0, 129);
			{
				formData_2 = new FormData();
				formData_2.bottom = new FormAttachment(spinner, -6);
				formData_2.left = new FormAttachment(txtMatrix, 0, SWT.LEFT);
				lblSignature.setLayoutData(formData_2);
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
				formData.top = new FormAttachment(txtMatrix, 22);
				formData.left = new FormAttachment(0, 10);
				btnCreateSignature.setLayoutData(formData);
			}
			btnCreateSignature.setSelection(true);
			btnCreateSignature.setText("Create Custom Class Signature (if desel. no sign. is created!)");
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
		return new Point(358, 240);
	}
	protected void buttonPressed(int buttonId) {
		if (buttonId == IDialogConstants.CANCEL_ID) {
			this.cancelPressed();
			
			
			return;
		}
		if (buttonId == IDialogConstants.OK_ID) {
			ImageSelectionTransferJob.setSignature(sel);
			ImageSelectionTransferJob.setInput(txtMatrix.getText());
			ImageSelectionTransferJob.setInput2(spinner.getSelection());
			
			this.okPressed();
			return;
		}
		super.buttonPressed(buttonId);
	}
}
