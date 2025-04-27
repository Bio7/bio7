package com.eco.bio7.ijmacro.editor.toolbar.debug;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.eco.bio7.image.Util;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;

/*Class from: http://www.vogella.com/tutorials/EclipseDialogs/article.html*/

public class SetDebugBreakpointMacroValueDialog extends Dialog {
	private Text txtName;
	private Text txtValue;
	private String name = "";

	private String value = "";
	private String buttonText;
	private String operator = "==";
	private Combo combo;
	private String expression;

	public void setName(String name) {
		this.name = name;
		txtName.setText(name);
	}

	public void setValue(String value) {
		this.value = value;
		txtValue.setText(value);
	}

	public void setOperator(String operator) {
		this.operator = operator;
		int count = combo.getItemCount();
		for (int i = 0; i < count; i++) {
			if (combo.getItem(i).equals(operator)) {
				combo.select(i);
			}
		}
	}

	public String getOperator() {
		return operator;
	}
	

	public SetDebugBreakpointMacroValueDialog(Shell parentShell, String buttonText, String expression) {
		super(parentShell);
		parentShell.setText(buttonText);
		/* Center the dialog! */
		/*Monitor primary = Util.getDisplay().getPrimaryMonitor();
		Rectangle bounds = primary.getBounds();
		Rectangle rect = parentShell.getBounds();
		
		int x = bounds.x + (bounds.width - rect.width) / 2;
		int y = bounds.y + (bounds.height - rect.height) / 2;
		
		parentShell.setLocation(x, y);*/
		this.buttonText = buttonText;
		this.expression = expression;
		setShellStyle(SWT.RESIZE | SWT.TITLE);

	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		GridLayout layout = new GridLayout(2, false);
		layout.marginRight = 5;
		layout.marginLeft = 10;
		container.setLayout(layout);

		Label lblUser = new Label(container, SWT.NONE);
		lblUser.setText("Variable:");

		txtName = new Text(container, SWT.BORDER);
		txtName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		txtName.setText(name);
		txtName.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(ModifyEvent e) {
				Text textWidget = (Text) e.getSource();
				String userText = textWidget.getText();
				name = userText;
			}
		});

		Label lblOperator = new Label(container, SWT.NONE);
		lblOperator.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblOperator.setText("Operator:");

		combo = new Combo(container, SWT.NONE);
		combo.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {

				operator = combo.getText();
			}
		});
		combo.setItems(new String[] { "==", "!=", ">", ">=", "<", "<=" });
		combo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		combo.select(0);

		Label lblPassword = new Label(container, SWT.NONE);
		GridData gd_lblNewLabel = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_lblNewLabel.horizontalIndent = 1;
		lblPassword.setLayoutData(gd_lblNewLabel);
		lblPassword.setText("Value:");

		txtValue = new Text(container, SWT.BORDER);
		txtValue.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		txtValue.setText(value);
		txtValue.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(ModifyEvent e) {
				Text textWidget = (Text) e.getSource();
				String passwordText = textWidget.getText();
				value = passwordText;
			}
		});
		/*Set the default values!*/
		if (expression != null) {
			String[] expr = expression.split(" ");
			setName(expr[0]);
			setOperator(expr[1]);
			setValue(expr[2]);
		}

		return container;
	}

	// override method to use "Login" as label for the OK button
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, buttonText, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}

	@Override
	protected Point getInitialSize() {
		return new Point(564, 300);
	}

	@Override
	protected void okPressed() {
		name = txtName.getText();
		value = txtValue.getText();
		super.okPressed();
	}

	public String getName() {
		return name;
	}

	public String getValue() {
		return value;
	}

}