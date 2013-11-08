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
		container.setLayout(null);
		{
			spinner = new Spinner(container, SWT.BORDER);
			spinner.setBounds(10, 143, 88, 21);
		}
		{
			lblSignature = new Label(container, SWT.NONE);
			lblSignature.setBounds(111, 146, 152, 18);
			lblSignature.setText("Custom Class Signature");
		}
		{
			btnCreateSignature = new Button(container, SWT.CHECK);
			btnCreateSignature.setBounds(10, 121, 350, 21);
			btnCreateSignature.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					sel=!sel;
				}
			});
			btnCreateSignature.setSelection(true);
			btnCreateSignature.setText("Create Custom Class Signature (else increasing sign. is created!)");
		}
		
		spinner_1 = new Spinner(container, SWT.BORDER);
		spinner_1.setBounds(10, 48, 88, 21);
		spinner_1.setMinimum(1);
		spinner_1.setSelection(1);
		
		spinner_2 = new Spinner(container, SWT.BORDER);
		spinner_2.setBounds(111, 48, 88, 21);
		ImagePlus impd = WindowManager.getCurrentImage();
		int stackSize=impd.getStackSize();
		spinner_2.setMinimum(1);
		spinner_2.setSelection(stackSize);
		spinner_2.setMaximum(stackSize);
		
		Label lblTransferStackIntervall = new Label(container, SWT.NONE);
		lblTransferStackIntervall.setBounds(10, 10, 250, 21);
		lblTransferStackIntervall.setText("Transfer Selected Stack Slice Selections");
		
		Label lblFrom = new Label(container, SWT.NONE);
		lblFrom.setBounds(10, 29, 48, 21);
		lblFrom.setText("From");
		
		Label lblTo = new Label(container, SWT.NONE);
		lblTo.setBounds(112, 29, 224, 21);
		lblTo.setText("To");
		
		Label label = new Label(container, SWT.SEPARATOR | SWT.HORIZONTAL);
		label.setBounds(10, 88, 189, 27);

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
		return new Point(447, 260);
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
