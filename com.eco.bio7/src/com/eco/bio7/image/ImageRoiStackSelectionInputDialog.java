package com.eco.bio7.image;

import ij.ImagePlus;
import ij.WindowManager;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
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
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.wb.swt.SWTResourceManager;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.GridData;

public class ImageRoiStackSelectionInputDialog extends Dialog {
	public Spinner spinner;
	private Button btnCreateSignature;
	private boolean sel;
	private Label lblSignature;
	private Spinner spinner_1;
	public Spinner spinner_2;
	private Button btnCreateIncreasingSignature;
	private Button btnCreateSignatureFrom;
	private boolean selIncresing;
	private boolean selFromName;
	private Button btnCreateSignatureFromGroupNumber;
	private boolean selFromGroup;

	/**
	 * Create the dialog.
	 * 
	 * @param parentShell
	 */
	public ImageRoiStackSelectionInputDialog(Shell parentShell) {
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
		container.setLayout(new GridLayout(2, true));
		ImagePlus impd = WindowManager.getCurrentImage();
		int stackSize = impd.getStackSize();
		Label lblTransferStackIntervall = new Label(container, SWT.NONE);
		GridData gd_lblTransferStackIntervall = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
		gd_lblTransferStackIntervall.heightHint = 30;
		lblTransferStackIntervall.setLayoutData(gd_lblTransferStackIntervall);
		lblTransferStackIntervall.setText("Transfer selected stack slice selections");

		Label lblFrom = new Label(container, SWT.NONE);
		GridData gd_lblFrom = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_lblFrom.heightHint = 30;
		lblFrom.setLayoutData(gd_lblFrom);
		lblFrom.setText("From");

		Label lblTo = new Label(container, SWT.NONE);
		GridData gd_lblTo = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_lblTo.heightHint = 30;
		lblTo.setLayoutData(gd_lblTo);
		lblTo.setText("To");

		spinner_1 = new Spinner(container, SWT.BORDER);
		GridData gd_spinner_1 = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_spinner_1.heightHint = 30;
		spinner_1.setLayoutData(gd_spinner_1);
		spinner_1.setMinimum(1);
		spinner_1.setSelection(1);

		spinner_2 = new Spinner(container, SWT.BORDER);
		GridData gd_spinner_2 = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_spinner_2.heightHint = 30;
		spinner_2.setLayoutData(gd_spinner_2);
		spinner_2.setMinimum(1);
		spinner_2.setMaximum(10000000);
		spinner_2.setSelection(stackSize);

		Label label = new Label(container, SWT.SEPARATOR | SWT.HORIZONTAL);
		GridData gd_label = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
		gd_label.heightHint = 30;
		label.setLayoutData(gd_label);

		btnCreateSignatureFrom = new Button(container, SWT.CHECK);
		btnCreateSignatureFrom.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				if (btnCreateSignatureFrom.getSelection()) {
					btnCreateSignature.setSelection(false);
					btnCreateIncreasingSignature.setSelection(false);
					btnCreateSignatureFromGroupNumber.setSelection(false);
					sel = false;
					selIncresing = false;
					selFromGroup=false;
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
		GridData gd_btnCreateSignatureFrom = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
		gd_btnCreateSignatureFrom.heightHint = 30;
		btnCreateSignatureFrom.setLayoutData(gd_btnCreateSignatureFrom);
		btnCreateSignatureFrom.setText("Create signature from ROI Manager name (seperator = last occurence of '_')");
		
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
					selFromGroup=true;
				} else {
					if (btnCreateIncreasingSignature.getSelection() == false) {
						//btnCreateSignatureFrom.setSelection(true);
						//btnCreateSignature.setSelection(true);
						selFromGroup=false;
					}

				}
			}
		});
		GridData gd_btnCreateSignatureFromGroupNumber = new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1);
		gd_btnCreateSignatureFromGroupNumber.heightHint = 30;
		btnCreateSignatureFromGroupNumber.setLayoutData(gd_btnCreateSignatureFromGroupNumber);
		btnCreateSignatureFromGroupNumber.setText("Create signature from group number");

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
					selFromGroup=false;
				} else {
					if (btnCreateIncreasingSignature.getSelection() == false) {
						//btnCreateSignatureFrom.setSelection(true);
						//btnCreateSignature.setSelection(true);
						selIncresing = false;
					}

				}

			}
		});
		GridData gd_btnCreateIncreasingSignature = new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1);
		gd_btnCreateIncreasingSignature.heightHint = 30;
		btnCreateIncreasingSignature.setLayoutData(gd_btnCreateIncreasingSignature);
		btnCreateIncreasingSignature.setText("Create increasing signature");
		new Label(container, SWT.NONE);
		{
			btnCreateSignature = new Button(container, SWT.CHECK);
			GridData gd_btnCreateSignature = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
			gd_btnCreateSignature.heightHint = 30;
			btnCreateSignature.setLayoutData(gd_btnCreateSignature);
			btnCreateSignature.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					if (btnCreateSignature.getSelection()) {
						btnCreateSignatureFrom.setSelection(false);
						btnCreateIncreasingSignature.setSelection(false);
						sel = true;
						selIncresing = false;
						selFromName = false;
						selFromGroup=false;
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
		new Label(container, SWT.NONE);
		new Label(container, SWT.NONE);
		{
			lblSignature = new Label(container, SWT.NONE);
			GridData gd_lblSignature = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
			gd_lblSignature.heightHint = 30;
			lblSignature.setLayoutData(gd_lblSignature);
			lblSignature.setText("Custom Class Signature:");
		}
		new Label(container, SWT.NONE);
		{
			spinner = new Spinner(container, SWT.BORDER);
			spinner.setMaximum(100000);
			GridData gd_spinner = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
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
		return new Point(545, 481);
	}

	protected void buttonPressed(int buttonId) {
		if (buttonId == IDialogConstants.CANCEL_ID) {
			this.cancelPressed();

			ImageStackRoiSelectionTransferJob.cancelJob = true;
			return;
		}
		if (buttonId == IDialogConstants.OK_ID) {
			ImageStackRoiSelectionTransferJob.cancelJob = false;
			ImageStackRoiSelectionTransferJob.doSetSignature(sel);
			ImageStackRoiSelectionTransferJob.doSetIncreasingSignature(selIncresing);
			ImageStackRoiSelectionTransferJob.doSetSignatureFromName(selFromName);
			ImageStackRoiSelectionTransferJob.doSetSignatureFromGroup(selFromGroup);
			ImageStackRoiSelectionTransferJob.setSignatureNumber(spinner.getSelection());
			ImageStackRoiSelectionTransferJob.setMinStackSlice(spinner_1.getSelection());
			ImageStackRoiSelectionTransferJob.setMaxStackSlice(spinner_2.getSelection());

			this.okPressed();
			return;
		}
		super.buttonPressed(buttonId);
	}
}
