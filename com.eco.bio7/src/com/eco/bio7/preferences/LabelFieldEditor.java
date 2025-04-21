package com.eco.bio7.preferences;

import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.eco.bio7.image.Util;

class LabelFieldEditor extends FieldEditor {

	private Label label;
	Font font;

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

		if (font!=null&&font.isDisposed() == false) {
			font.dispose();

		}
		
		label.setLayoutData(gridData);
		if (Util.getOS().equals("Mac")) {

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
