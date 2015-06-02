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
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.image.CanvasView;
import com.eco.bio7.rbridge.RServe;
import com.eco.bio7.rbridge.RState;

import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.GridData;

public class RSkeletonInputDialog extends Dialog {
	private Text txtMypackage;
	RPackageSkeleton pack;
	private List list;
	private List list_1;
	private Button btnUseRList;
	private Button btnUseSelectedFiles;

	/**
	 * Create the dialog.
	 * 
	 * @param parentShell
	 */
	public RSkeletonInputDialog(Shell parentShell, RPackageSkeleton packageSkeleton) {
		super(parentShell);
		setShellStyle(SWT.RESIZE | SWT.TITLE | SWT.APPLICATION_MODAL);
		pack = packageSkeleton;
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
		gridLayout.makeColumnsEqualWidth = true;
		gridLayout.makeColumnsEqualWidth = true;
		gridLayout.makeColumnsEqualWidth = true;
		gridLayout.numColumns = 3;
		container.setLayout(new GridLayout(3, false));

		btnUseSelectedFiles = new Button(container, SWT.CHECK);
		GridData gd_btnUseSelectedFiles = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
		gd_btnUseSelectedFiles.heightHint = 30;
		btnUseSelectedFiles.setLayoutData(gd_btnUseSelectedFiles);
		btnUseSelectedFiles.setSelection(true);
		btnUseSelectedFiles.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				btnUseRList.setSelection(false);
				list_1.removeAll();
			}
		});
		btnUseSelectedFiles.setText("Use Selected Files");
		new Label(container, SWT.NONE);

		btnUseRList = new Button(container, SWT.CHECK);
		GridData gd_btnUseRList = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
		gd_btnUseRList.heightHint = 30;
		btnUseRList.setLayoutData(gd_btnUseRList);
		btnUseRList.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {

				btnUseSelectedFiles.setSelection(false);
			}
		});
		btnUseRList.setText("Use Selected R Workspace Variables");
		new Label(container, SWT.NONE);

		list = new List(container, SWT.V_SCROLL | SWT.MULTI | SWT.H_SCROLL | SWT.BORDER);
		list.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 2));

		final Button button = new Button(container, SWT.NONE);
		GridData gd_button = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
		gd_button.heightHint = 35;
		button.setLayoutData(gd_button);
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				String[] sel = list.getSelection();
				for (int i = 0; i < sel.length; i++) {
					list_1.add(sel[i], i);

				}

			}
		});
		button.setText(">>");

		list_1 = new List(container, SWT.V_SCROLL | SWT.MULTI | SWT.H_SCROLL | SWT.BORDER);
		list_1.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 2));

		final Button button_1 = new Button(container, SWT.NONE);
		GridData gd_button_1 = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
		gd_button_1.heightHint = 35;
		button_1.setLayoutData(gd_button_1);
		button_1.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				int[] sel = list_1.getSelectionIndices();
				for (int i = 0; i < sel.length; i++) {
					list_1.remove(sel);
				}

			}
		});
		button_1.setText("<<");

		txtMypackage = new Text(container, SWT.BORDER);
		txtMypackage.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		txtMypackage.setText("MyPackage");

		Label lblNameOfThe = new Label(container, SWT.NONE);
		GridData gd_lblNameOfThe = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_lblNameOfThe.heightHint = 30;
		lblNameOfThe.setLayoutData(gd_lblNameOfThe);
		lblNameOfThe.setText("Enter a name for the package");
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

					RConnection c = RServe.getConnection();
					
					/*Create a custom environment for temporary files!*/
					c.eval("try(.bio7TempEnvPackage<- new.env())");
					/*Create a variable list (variables in current environment) in the custom environment!*/
					c.eval("try(assign(\"tempPackageBio7VariablesName\", ls(), env=.bio7TempEnvPackage))");

					x = c.eval("try(.bio7TempEnvPackage$tempPackageBio7VariablesName)");
					try {
						v = x.asStrings();
					} catch (REXPMismatchException e1) {

						e1.printStackTrace();
					}
					/*Remove the temp. variable from the custom environment!*/
					c.eval("try(remove(tempPackageBio7VariablesName,envir=.bio7TempEnvPackage))");
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
		return new Point(523, 333);
	}

	protected void buttonPressed(int buttonId) {
		if (buttonId == IDialogConstants.CANCEL_ID) {
			this.cancelPressed();
			pack.cancelCreation = true;
			pack.packageName = "myPackage";

			return;
		}
		if (buttonId == IDialogConstants.OK_ID) {
			pack.cancelCreation = false;
			pack.packageName = txtMypackage.getText();
			pack.includeRVariables = list_1.getItems();
			pack.builtFromVariables = btnUseRList.getSelection();

			this.okPressed();
			return;
		}
		super.buttonPressed(buttonId);
	}
}
