/*Source from: https://www.programcreek.com/2010/11/add-a-file-chooserselector-for-eclipse-rcp-development/*/
package com.eco.bio7.reditor.database.view;

import java.io.File;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Text;

public class DirChooser extends Composite {

	Text mText;
	Button mButton;
	String title = null;

	public DirChooser(Composite parent) {
		super(parent, SWT.NULL);
		createContent();
	}

	public void createContent() {
		GridLayout layout = new GridLayout(2, false);
		setLayout(layout);

		mText = new Text(this, SWT.SINGLE | SWT.BORDER);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.grabExcessHorizontalSpace = true;
		gd.horizontalAlignment = GridData.FILL;
		mText.setLayoutData(gd);

		mButton = new Button(this, SWT.NONE);
		mButton.setText("...");
		mButton.addSelectionListener(new SelectionListener() {

			public void widgetDefaultSelected(SelectionEvent e) {
			}

			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog dlg = new DirectoryDialog(mButton.getShell(), SWT.OPEN);
				dlg.setText("Open");
				String dir = dlg.open();
				if (dir == null)
					return;
				dir = dir.replaceAll("\\\\", "/");

				mText.setText(dir + "/");
			}
		});
	}

	public String getText() {
		return mText.getText();

	}

	public void setText(String text) {
		mText.setText(text);

	}

	public Text getTextControl() {
		return mText;
	}

	public File getDir() {
		String text = mText.getText();
		if (text.length() == 0)
			return null;
		return new File(text);
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}
}