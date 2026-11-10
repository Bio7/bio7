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

package com.eco.bio7.preferences;

import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

/** A label and a button which runs an action (no preference value). */
class ActionButtonFieldEditor extends FieldEditor {

	private final String buttonText;
	private final Runnable action;
	private Button button;

	ActionButtonFieldEditor(String labelText, String buttonText, Runnable action, Composite parent) {
		this.buttonText = buttonText;
		this.action = action;
		init("action_button", labelText);
		createControl(parent);
	}

	@Override
	protected void adjustForNumColumns(int numColumns) {
		((GridData) button.getLayoutData()).horizontalSpan = numColumns - 1;
	}

	@Override
	protected void doFillIntoGrid(Composite parent, int numColumns) {
		Label label = getLabelControl(parent);
		label.setLayoutData(new GridData());
		button = new Button(parent, SWT.PUSH);
		button.setText(buttonText);
		button.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, numColumns - 1, 1));
		button.addListener(SWT.Selection, _ -> action.run());
	}

	@Override
	public int getNumberOfControls() {
		return 2;
	}

	@Override
	protected void doLoad() {
	}

	@Override
	protected void doLoadDefault() {
	}

	@Override
	protected void doStore() {
	}
}
