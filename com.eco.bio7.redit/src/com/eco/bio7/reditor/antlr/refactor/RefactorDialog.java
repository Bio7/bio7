package com.eco.bio7.reditor.antlr.refactor;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;

public class RefactorDialog extends Dialog {
	public Text text;

	private String value=null;

	private boolean global;

	private Button globalCheckbox;

	

	public boolean isGlobal() {
		return global;
	}

	public void setGlobal(boolean global) {
		this.global = global;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	/**
	 * Create the dialog.
	 * @param parentShell
	 */
	public RefactorDialog(Shell parentShell) {
		super(parentShell);
	}

	/**
	 * Create contents of the dialog.
	 * @param parent
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		GridLayout gridLayout = (GridLayout) container.getLayout();
		gridLayout.makeColumnsEqualWidth = true;
		
		Label lblInsertName = new Label(container, SWT.NONE);
		GridData gd_lblInsertName = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_lblInsertName.heightHint = 25;
		lblInsertName.setLayoutData(gd_lblInsertName);
		lblInsertName.setText("Name of Variable");
		
		text = new Text(container, SWT.BORDER);
		GridData gd_text = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_text.heightHint = 25;
		text.setLayoutData(gd_text);
		text.addVerifyListener(new VerifyListener() {  
		    @Override  
		    public void verifyText(VerifyEvent e) {
		        /* Notice how we combine the old and new below */
		        String currentText = ((Text)e.widget).getText();
		       if(currentText==null||currentText.isEmpty()){
		    	   
		       }
		    }  
		});
		
		globalCheckbox = new Button(container, SWT.CHECK);
		GridData gd_btnCheckButton = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_btnCheckButton.heightHint = 25;
		globalCheckbox.setLayoutData(gd_btnCheckButton);
		globalCheckbox.setText("Global?");

		return container;
	}

	/**
	 * Create contents of the button bar.
	 * @param parent
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		Button button = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		
		Button button_1 = createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
		
	}

	/**
	 * Return the initial size of the dialog.
	 */
	@Override
	protected Point getInitialSize() {
		return new Point(450, 300);
	}
	
	 private void saveInput() {
		    value = text.getText();
		    global=globalCheckbox.getSelection();
		   

		  }

		  @Override
		  protected void okPressed() {
		    saveInput();
		    super.okPressed();
		  }

}
