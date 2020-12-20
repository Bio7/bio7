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

public class ImageRoiSelectionInputDialog extends Dialog {
	public Spinner spinner;
	private Button btnCreateSignature;
	private boolean sel;
	private Label lblSignature;
	private Button btnCreateSignatureFrom;
	private Button btnCreateIncreasingSignature;
	private boolean selIncresing;
	private boolean selFromName;
	private Button btnCreateSignatureFromGroupNumber;
	private boolean selFromGroup;

	/**
	 * Create the dialog.
	 * 
	 * @param parentShell
	 */
	public ImageRoiSelectionInputDialog(Shell parentShell) {
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
		gridLayout.marginHeight = 30;
		gridLayout.numColumns = 2;
		gridLayout.makeColumnsEqualWidth = true;
		container.setLayout(new GridLayout(1, false));
		{
			btnCreateSignatureFrom = new Button(container, SWT.CHECK);
			btnCreateSignatureFrom.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					if (btnCreateSignatureFrom.getSelection()) {
						btnCreateSignature.setSelection(false);
						btnCreateIncreasingSignature.setSelection(false);
						btnCreateSignatureFromGroupNumber.setSelection(false);
						sel = false;
						selIncresing = false;
						selFromGroup = false;
						selFromName = true;
					} else {
						if (btnCreateSignatureFrom.getSelection() == false) {
							//btnCreateSignature.setSelection(true);
							//btnCreateIncreasingSignature.setSelection(true);
							selFromName = false;

						}

					}

				}
			});
			GridData gd_btnCreateSignatureFrom = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
			gd_btnCreateSignatureFrom.heightHint = 30;
			gd_btnCreateSignatureFrom.widthHint = 264;
			btnCreateSignatureFrom.setLayoutData(gd_btnCreateSignatureFrom);
			btnCreateSignatureFrom.setText("Create signature from ROI Manager name (seperator = last occurence of '_')");
		}
		{
			btnCreateSignatureFromGroupNumber = new Button(container, SWT.CHECK);
			btnCreateSignatureFromGroupNumber.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					if (btnCreateSignatureFromGroupNumber.getSelection()) {
						btnCreateSignatureFrom.setSelection(false);
						btnCreateSignature.setSelection(false);
						btnCreateIncreasingSignature.setSelection(false);
						selIncresing = false;
						sel = false;
						selFromName = false;
						selFromGroup = true;
					} else {
						if (btnCreateIncreasingSignature.getSelection() == false) {
							//btnCreateSignatureFrom.setSelection(true);
							//btnCreateSignature.setSelection(true);
							selIncresing = false;
						}

					}
				}
			});
			GridData gd_btnCreateSignatureFromGroupNumber = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
			gd_btnCreateSignatureFromGroupNumber.heightHint = 30;
			btnCreateSignatureFromGroupNumber.setLayoutData(gd_btnCreateSignatureFromGroupNumber);
			btnCreateSignatureFromGroupNumber.setText("Create signature from group number");
		}
		{
			btnCreateIncreasingSignature = new Button(container, SWT.CHECK);
			btnCreateIncreasingSignature.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					if (btnCreateIncreasingSignature.getSelection()) {
						btnCreateSignatureFrom.setSelection(false);
						btnCreateSignature.setSelection(false);
						btnCreateSignatureFromGroupNumber.setSelection(false);
						selIncresing = true;
						sel = false;
						selFromName = false;
						selFromGroup = false;
					} else {
						if (btnCreateIncreasingSignature.getSelection() == false) {
							//btnCreateSignatureFrom.setSelection(true);
							//btnCreateSignature.setSelection(true);
							selIncresing = false;
						}

					}
				}
			});
			GridData gd_btnCreateIncreasingSignature = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
			gd_btnCreateIncreasingSignature.heightHint = 30;
			gd_btnCreateIncreasingSignature.widthHint = 266;
			btnCreateIncreasingSignature.setLayoutData(gd_btnCreateIncreasingSignature);
			btnCreateIncreasingSignature.setText("Create increasing signature");
		}
		new Label(container, SWT.NONE);
		{
			btnCreateSignature = new Button(container, SWT.CHECK);
			GridData gd_btnCreateSignature = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
			gd_btnCreateSignature.heightHint = 30;
			btnCreateSignature.setLayoutData(gd_btnCreateSignature);
			btnCreateSignature.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					if (btnCreateSignature.getSelection()) {
						btnCreateSignatureFrom.setSelection(false);
						btnCreateIncreasingSignature.setSelection(false);
						btnCreateSignatureFromGroupNumber.setSelection(false);
						sel = true;
						selIncresing = false;
						selFromName = false;
						selFromGroup = false;
					} else {
						if (btnCreateSignature.getSelection() == false) {
							//btnCreateSignatureFrom.setSelection(true);
							//btnCreateIncreasingSignature.setSelection(true);
							sel = false;
						}

					}

				}
			});

			btnCreateSignature.setText("Create custom class signature");
		}
		new Label(container, SWT.NONE);
		{
			lblSignature = new Label(container, SWT.NONE);
			GridData gd_lblSignature = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
			gd_lblSignature.heightHint = 30;
			lblSignature.setLayoutData(gd_lblSignature);
			lblSignature.setText("Custom Class Signature");
		}
		new Label(container, SWT.NONE);
		{
			spinner = new Spinner(container, SWT.BORDER);
			spinner.setMaximum(100000);
			GridData gd_spinner = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
			gd_spinner.widthHint = 279;
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
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}

	/**
	 * Return the initial size of the dialog.
	 */
	@Override
	protected Point getInitialSize() {
		return new Point(573, 400);
	}

	protected void buttonPressed(int buttonId) {
		if (buttonId == IDialogConstants.CANCEL_ID) {
			this.cancelPressed();
			ImageRoiSelectionTransferJob.cancelJob = true;

			return;
		}
		if (buttonId == IDialogConstants.OK_ID) {
			ImageRoiSelectionTransferJob.cancelJob = false;
			ImageRoiSelectionTransferJob.doSetSignature(sel);
			ImageRoiSelectionTransferJob.doSetIncreasingSignature(selIncresing);
			ImageRoiSelectionTransferJob.doSetSignatureFromGroup(selFromGroup);
			ImageRoiSelectionTransferJob.doSetSignatureFromName(selFromName);
			ImageRoiSelectionTransferJob.setSignatureNumber(spinner.getSelection());

			this.okPressed();
			return;
		}
		super.buttonPressed(buttonId);
	}
}
