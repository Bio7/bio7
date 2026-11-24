/*******************************************************************************
 * Copyright (c) 2007-2026 M. Austenfeld
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * Contributors:
 * Marcel Austenfeld - initial API and implementation
 *******************************************************************************/
package com.eco.bio7.ijmacro.editor.toolbar.debug;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * The condition of a breakpoint of the ImageJ macro editor: the debugger stops
 * in the line only if "variable operator value" is true (a numeric variable of
 * the macro, a number). Without a variable, the breakpoint stops always.
 */
public class SetDebugBreakpointMacroValueDialog extends Dialog {

	private static final String[] OPERATORS = { "==", "!=", ">", ">=", "<", "<=" };

	private final String title;
	private final String info;
	private String name = "", operator = "==", value = "";
	private Text txtName, txtValue;
	private Combo combo;
	private Label error;

	/**
	 * @param parentShell the parent
	 * @param title       the title of the dialog
	 * @param expression  the condition "variable operator value" (null: none)
	 */
	public SetDebugBreakpointMacroValueDialog(Shell parentShell, String title, String expression) {
		this(parentShell, title, expression, null);
	}

	/** As above with a line of information (e.g., the line and its code). */
	public SetDebugBreakpointMacroValueDialog(Shell parentShell, String title, String expression, String info) {
		super(parentShell);
		this.title = title;
		this.info = info;
		setShellStyle(getShellStyle() | SWT.RESIZE);
		/* The condition of the breakpoint: three parts separated by spaces */
		if (expression != null) {
			String[] p = expression.trim().split("\\s+");
			if (p.length == 3) {
				name = p[0];
				operator = p[1];
				value = p[2];
			}
		}
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText(title);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		GridLayout layout = new GridLayout(2, false);
		layout.marginLeft = 10;
		layout.marginRight = 10;
		container.setLayout(layout);
		if (info != null) {
			Label l = new Label(container, SWT.WRAP);
			l.setText(info);
			l.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		}
		Label hint = new Label(container, SWT.WRAP);
		hint.setText("Stop only if the condition is true (e.g., i >= 10). Without a variable the breakpoint always stops.");
		GridData hd = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
		hd.widthHint = 420;
		hint.setLayoutData(hd);
		new Label(container, SWT.NONE).setText("Variable:");
		txtName = new Text(container, SWT.BORDER);
		txtName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		txtName.setText(name);
		new Label(container, SWT.NONE).setText("Operator:");
		combo = new Combo(container, SWT.READ_ONLY);
		combo.setItems(OPERATORS);
		combo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		int k = java.util.Arrays.asList(OPERATORS).indexOf(operator);
		combo.select(Math.max(0, k));
		new Label(container, SWT.NONE).setText("Value:");
		txtValue = new Text(container, SWT.BORDER);
		txtValue.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		txtValue.setText(value);
		error = new Label(container, SWT.WRAP);
		error.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		error.setForeground(container.getDisplay().getSystemColor(SWT.COLOR_RED));
		/* A changed value: the message of a wrong value is removed */
		txtName.addModifyListener(e -> error.setText(""));
		txtValue.addModifyListener(e -> error.setText(""));
		return container;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, "Set", true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}

	@Override
	protected Point getInitialSize() {
		Point p = super.getInitialSize();
		return new Point(Math.max(480, p.x), p.y);
	}

	@Override
	protected void okPressed() {
		String n = txtName.getText().trim(), v = txtValue.getText().trim();
		if (!n.isEmpty()) {
			if (!n.matches("[A-Za-z_][A-Za-z0-9_]*")) {
				error.setText("The variable must be a name of the macro (e.g., i).");
				return;
			}
			try {
				Double.parseDouble(v);
			} catch (NumberFormatException e) {
				error.setText("The value must be a number (e.g., 10 or 0.5).");
				return;
			}
		}
		name = n;
		value = v;
		operator = combo.getText();
		super.okPressed();
	}

	/** The variable ("" for a breakpoint without a condition). */
	public String getName() {
		return name;
	}

	public String getOperator() {
		return operator;
	}

	public String getValue() {
		return value;
	}

	/** The condition "variable operator value" (null without a variable). */
	public String getExpression() {
		return name.isEmpty() ? null : name + " " + operator + " " + value;
	}
}
