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
    private boolean sel=true;
    private Label lblSignature;
	private Spinner spinner_1;
	public Spinner spinner_2;
	/**
	 * Create the dialog.
	 * @param parentShell
	 */
	public ImageRoiStackSelectionInputDialog(Shell parentShell) {
		super(parentShell);
	}

	/**
	 * Create contents of the dialog.
	 * @param parent
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		container.setLayout(new GridLayout(2, true));
		ImagePlus impd = WindowManager.getCurrentImage();
		int stackSize=impd.getStackSize();
		Label lblTransferStackIntervall = new Label(container, SWT.NONE);
		GridData gd_lblTransferStackIntervall = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
		gd_lblTransferStackIntervall.heightHint = 30;
		lblTransferStackIntervall.setLayoutData(gd_lblTransferStackIntervall);
		lblTransferStackIntervall.setText("Transfer Selected Stack Slice Selections");
		
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
		{
			btnCreateSignature = new Button(container, SWT.CHECK);
			GridData gd_btnCreateSignature = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
			gd_btnCreateSignature.heightHint = 30;
			btnCreateSignature.setLayoutData(gd_btnCreateSignature);
			btnCreateSignature.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					sel=!sel;
				}
			});
			btnCreateSignature.setSelection(true);
			btnCreateSignature.setText("Create Custom Class Signature (else increasing signature is created!)");
		}
		{
			spinner = new Spinner(container, SWT.BORDER);
			GridData gd_spinner = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
			gd_spinner.heightHint = 30;
			spinner.setLayoutData(gd_spinner);
		}
		{
			lblSignature = new Label(container, SWT.NONE);
			GridData gd_lblSignature = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
			gd_lblSignature.heightHint = 30;
			lblSignature.setLayoutData(gd_lblSignature);
			lblSignature.setText("Custom Class Signature");
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
		return new Point(620, 441);
	}
	protected void buttonPressed(int buttonId) {
		if (buttonId == IDialogConstants.CANCEL_ID) {
			this.cancelPressed();
			
			ImageStackRoiSelectionTransferJob.cancelJob=true;
			return;
		}
		if (buttonId == IDialogConstants.OK_ID) {
			ImageStackRoiSelectionTransferJob.cancelJob=false;
			ImageStackRoiSelectionTransferJob.doSetSignature(sel);
			ImageStackRoiSelectionTransferJob.setSignatureNumber(spinner.getSelection());
			ImageStackRoiSelectionTransferJob.setMinStackSlice(spinner_1.getSelection());
			ImageStackRoiSelectionTransferJob.setMaxStackSlice(spinner_2.getSelection());
			
			this.okPressed();
			return;
		}
		super.buttonPressed(buttonId);
	}
}
