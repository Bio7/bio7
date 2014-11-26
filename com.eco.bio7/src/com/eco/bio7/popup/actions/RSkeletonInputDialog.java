package com.eco.bio7.popup.actions;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Button;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RserveException;

import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.image.CanvasView;
import com.eco.bio7.rbridge.RServe;
import com.eco.bio7.rbridge.RState;

public class RSkeletonInputDialog extends Dialog {
	private Text txtMypackage;
	RPackageSkeleton pack;
	private List list;
	private List list_1;
	private Button btnUseRList;
	private Button btnUseSelectedFiles;
	/**
	 * Create the dialog.
	 * @param parentShell
	 */
	public RSkeletonInputDialog(Shell parentShell, RPackageSkeleton packageSkeleton) {
		super(parentShell);
		pack=packageSkeleton;
	}

	/**
	 * Create contents of the dialog.
	 * @param parent
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		container.setLayout(null);
		
		txtMypackage = new Text(container, SWT.BORDER);
		txtMypackage.setText("MyPackage");
		txtMypackage.setBounds(10, 217, 209, 26);
		
		Label lblNameOfThe = new Label(container, SWT.NONE);
		lblNameOfThe.setBounds(249, 220, 169, 26);
		lblNameOfThe.setText("Enter a name for the package");
		
		list = new List(container, SWT.V_SCROLL | SWT.MULTI | SWT.H_SCROLL | SWT.BORDER);
		list.setBounds(10, 65, 149, 123);
		
		
		

		list_1 = new List(container, SWT.V_SCROLL | SWT.MULTI | SWT.H_SCROLL | SWT.BORDER);
		list_1.setBounds(219, 65, 149, 123);
		
		final Button button = new Button(container, SWT.NONE);
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				String [] sel=list.getSelection();
				for (int i = 0; i < sel.length; i++) {
					list_1.add(sel[i], i);
					
					
				}
				
			}
		});
		button.setText(">>");
		button.setBounds(165, 64, 48, 25);

		final Button button_1 = new Button(container, SWT.NONE);
		button_1.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				int []sel=list_1.getSelectionIndices();
				for (int i = 0; i < sel.length; i++) {
					list_1.remove(sel);
				}
				
			}
		});
		button_1.setText("<<");
		button_1.setBounds(165, 106, 48, 25);
		
		btnUseRList = new Button(container, SWT.CHECK);
		btnUseRList.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				
				btnUseSelectedFiles.setSelection(false);
			}
		});
		btnUseRList.setBounds(10, 42, 408, 16);
		btnUseRList.setText("Use Selected R Workspace Variables");
		
		btnUseSelectedFiles = new Button(container, SWT.CHECK);
		btnUseSelectedFiles.setSelection(true);
		btnUseSelectedFiles.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				btnUseRList.setSelection(false);
				list_1.removeAll();
			}
		});
		btnUseSelectedFiles.setBounds(10, 10, 203, 16);
		btnUseSelectedFiles.setText("Use Selected Files");
		displayRObjects();
		return container;
	}
	private void displayRObjects() {
		REXP x = null;
		list.removeAll();

		if (RServe.isAliveDialog()) {
			if (RState.isBusy() == false) {
				String[] v = null;
				// List all variables in the R workspace!

				try {
					RServe.getConnection().eval("try(XXXXXvarXXXXX<-ls())");
					x = RServe.getConnection().eval("try(XXXXXvarXXXXX)");
					try {
						v = x.asStrings();
					} catch (REXPMismatchException e1) {

						e1.printStackTrace();
					}
					RServe.getConnection().eval("try(remove(XXXXXvarXXXXX))");
				} catch (RserveException e1) {

					e1.printStackTrace();
				}

				for (int i = 0; i < v.length; i++) {

					list.add(v[i]);

				}
			} else {
				Bio7Dialog.message("Rserve is busy!");

			}

		}
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
		return new Point(434, 329);
	}
	protected void buttonPressed(int buttonId) {
		if (buttonId == IDialogConstants.CANCEL_ID) {
			this.cancelPressed();
			pack.cancelCreation=true;
			pack.packageName="myPackage";
		
			return;
		}
		if (buttonId == IDialogConstants.OK_ID) {
			pack.cancelCreation=false;
			pack.packageName=txtMypackage.getText();
			pack.includeRVariables=list_1.getItems();
			pack.builtFromVariables=btnUseRList.getSelection();
			
			this.okPressed();
			return;
		}
		super.buttonPressed(buttonId);
	}
}
