package com.eco.bio7.beanshell.editor.preferences;

import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

class LabelFieldEditor extends FieldEditor {

	private Label label;

	public LabelFieldEditor(String value, Composite parent) {
		super("label", value, parent);

	}

	protected void adjustForNumColumns(int numColumns) {
		((GridData) label.getLayoutData()).horizontalSpan = numColumns;
	}

	protected void doFillIntoGrid(Composite parent, int numColumns) {
		label = getLabelControl(parent);

		GridData gridData = new GridData();
		gridData.horizontalSpan = numColumns;
		gridData.horizontalAlignment = GridData.FILL;
		gridData.grabExcessHorizontalSpace = false;
		gridData.verticalAlignment = GridData.CENTER;
		gridData.grabExcessVerticalSpace = false;

		label.setLayoutData(gridData);
		Font font = null;
		if ( SWT.getPlatform().equals("cocoa")) {
			font = new Font(parent.getDisplay(), "Helvetica Neue", 15, SWT.BOLD);
		} else {
			font = new Font(parent.getDisplay(), "Arial", 9, SWT.BOLD);
		}
		label.setFont(font);
	}

	public int getNumberOfControls() {
		return 1;
	}

	protected void doLoad() {
	}

	protected void doLoadDefault() {
	}

	protected void doStore() {
	}
}
