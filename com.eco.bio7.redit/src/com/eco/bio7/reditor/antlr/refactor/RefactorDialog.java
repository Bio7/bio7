package com.eco.bio7.reditor.antlr.refactor;

import java.util.ArrayList;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.custom.ScrolledComposite;

public class RefactorDialog extends Dialog {
	public Text text;

	private String value = null;

	private boolean global;

	private Button globalCheckbox;

	private String[] id;

	private String argsText;
	private Table table;

	private CheckboxTableViewer checkboxTableViewer;

	public String getArgsText() {
		return argsText;
	}

	public void setArgsText(String argsText) {
		this.argsText = argsText;
	}

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
	 * 
	 * @param parentShell
	 * @param iD
	 */
	public RefactorDialog(Shell parentShell, String[] buffIds) {
		super(parentShell);
		this.id = buffIds;
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
				String currentText = ((Text) e.widget).getText();
				if (currentText == null || currentText.isEmpty()) {

				}
			}
		});
		// args.setText(id);
		globalCheckbox = new Button(container, SWT.CHECK);
		GridData gd_btnCheckButton = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_btnCheckButton.heightHint = 25;
		globalCheckbox.setLayoutData(gd_btnCheckButton);
		globalCheckbox.setText("Global?");
		
		ScrolledComposite scrolledComposite = new ScrolledComposite(container, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		scrolledComposite.setAlwaysShowScrollBars(true);
		GridData gd_scrolledComposite = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
		gd_scrolledComposite.heightHint = 252;
		gd_scrolledComposite.widthHint = 400;
		scrolledComposite.setLayoutData(gd_scrolledComposite);
		scrolledComposite.setExpandHorizontal(true);
		scrolledComposite.setExpandVertical(true);

		checkboxTableViewer = CheckboxTableViewer.newCheckList(scrolledComposite, SWT.BORDER | SWT.FULL_SELECTION);
		table = checkboxTableViewer.getTable();
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		table.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				/*
				 * String string = event.detail == SWT.CHECK ? "Checked" :
				 * "Selected"; System.out.println(event.item + " " + string);
				 */
			}
		});

		TableColumn tblclmnVariables = new TableColumn(table, SWT.NONE);
		tblclmnVariables.setWidth(100);
		tblclmnVariables.setText("Parameters");
		scrolledComposite.setContent(table);
		scrolledComposite.setMinSize(table.computeSize(SWT.DEFAULT, SWT.DEFAULT));

		if (id.length > 0) {
			for (int i = 0; i < id.length; i++) {
				String temp = id[i];
				new TableItem(table, SWT.CHECK).setText(0, temp);
			}
		}

		return container;
	}

	/**
	 * Create contents of the button bar.
	 * 
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
		return new Point(450, 479);
	}

	private void saveInput() {
		ArrayList<Integer> arrList = new ArrayList<Integer>();
		value = text.getText();
		global = globalCheckbox.getSelection();
		int count = table.getItemCount();
		if (count > 0) {

			for (int i = 0; i < count; i++) {
				if (table.getItem(i).getChecked()) {

					arrList.add(i);
				}
			}

			StringBuffer buff = new StringBuffer();
			for (int i = 0; i < arrList.size(); i++) {
				int selNumber = arrList.get(i);

				buff.append(table.getItem(selNumber).getText());
				if (i < arrList.size() - 1) {
					buff.append(",");
				}

			}

			argsText = buff.toString();
		} else {
			argsText = "";
		}

	}

	@Override
	protected void okPressed() {
		saveInput();
		super.okPressed();
	}

}
