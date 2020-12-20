package com.eco.bio7.image;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;

public class ImageSelectionInputDialog extends Dialog {
	public Text txtMatrix;
	public Spinner spinner;
	private Button btnCreateSignature;
	private boolean sel = true;

	/**
	 * Create the dialog.
	 * 
	 * @param parentShell
	 */
	public ImageSelectionInputDialog(Shell parentShell) {
		super(parentShell);
	}

	/**
	 * Create contents of the dialog.
	 * 
	 * @param parent
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		GridLayout gridLayout = (GridLayout) container.getLayout();
		gridLayout.numColumns = 2;
		gridLayout.makeColumnsEqualWidth = true;
		container.setLayout(new GridLayout(2, false));
		{
			Label lblMatrixName = new Label(container, SWT.NONE);
			GridData gd_lblMatrixName = new GridData(SWT.FILL, SWT.FILL, true,
					false, 1, 1);
			gd_lblMatrixName.heightHint = 30;
			lblMatrixName.setLayoutData(gd_lblMatrixName);
			lblMatrixName.setText("Matrix Name");
		}
		new Label(container, SWT.NONE);
		{
			txtMatrix = new Text(container, SWT.BORDER);
			GridData gd_txtMatrix = new GridData(SWT.FILL, SWT.FILL, true,
					false, 2, 1);
			gd_txtMatrix.heightHint = 30;
			txtMatrix.setLayoutData(gd_txtMatrix);
			txtMatrix.setText("matrix");
		}
		new Label(container, SWT.NONE);
		new Label(container, SWT.NONE);
		{
			Label lblSignature = new Label(container, SWT.NONE);
			GridData gd_lblSignature = new GridData(SWT.FILL, SWT.FILL, true,
					false, 1, 1);
			gd_lblSignature.heightHint = 30;
			lblSignature.setLayoutData(gd_lblSignature);
			lblSignature.setText("Custom Class Signature");
		}
		new Label(container, SWT.NONE);
		{
			btnCreateSignature = new Button(container, SWT.CHECK);
			GridData gd_btnCreateSignature = new GridData(SWT.FILL, SWT.FILL,
					true, false, 2, 1);
			gd_btnCreateSignature.heightHint = 30;
			btnCreateSignature.setLayoutData(gd_btnCreateSignature);
			btnCreateSignature.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					sel = !sel;
				}
			});
			btnCreateSignature.setSelection(true);
			btnCreateSignature
					.setText("Create Custom Class Signature (if desel. no sign. is created!)");
		}
		{
			spinner = new Spinner(container, SWT.BORDER);
			GridData gd_spinner = new GridData(SWT.FILL, SWT.FILL, true, false,
					1, 1);
			gd_spinner.heightHint = 30;
			spinner.setLayoutData(gd_spinner);
		}
		new Label(container, SWT.NONE);

		return container;
	}

	/**
	 * Create contents of the button bar.
	 * 
	 * @param parent
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL,
				true);
		createButton(parent, IDialogConstants.CANCEL_ID,
				IDialogConstants.CANCEL_LABEL, false);
	}

	/**
	 * Return the initial size of the dialog.
	 */
	@Override
	protected Point getInitialSize() {
		return new Point(604, 406);
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
