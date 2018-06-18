package com.eco.bio7.image;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class ClusterOption extends Dialog {

	private List list_1;
	private List list;
	private Text text;
	protected Object result;
	protected Shell shell;

	/**
	 * Create the dialog
	 * @param parent
	 * @param style
	 */
	public ClusterOption(Shell parent, int style) {
		super(parent, style);
	}

	/**
	 * Create the dialog
	 * @param parent
	 */
	public ClusterOption(Shell parent) {
		this(parent, SWT.NONE);
	}

	/**
	 * Open the dialog
	 * @return the result
	 */
	public Object open() {
		createContents();
		shell.open();
		shell.layout();
		Display display = getParent().getDisplay();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		return result;
	}

	/**
	 * Create contents of the dialog
	 */
	protected void createContents() {
		shell = new Shell(getParent(), SWT.APPLICATION_MODAL | SWT.TITLE | SWT.BORDER | SWT.CLOSE);
		shell.setSize(300, 245);
		shell.setText("SWT Dialog");

		text = new Text(shell, SWT.BORDER);
		text.setBounds(10, 10, 80, 25);

		final Label numberOfClustersLabel = new Label(shell, SWT.NONE);
		numberOfClustersLabel.setText("Number of clusters");
		numberOfClustersLabel.setBounds(96, 13, 149, 22);

		final Button okButton = new Button(shell, SWT.NONE);
		okButton.setText("Ok");
		okButton.setBounds(10, 194, 80, 25);

		list = new List(shell, SWT.BORDER);
		list.setBounds(184, 84, 100, 100);

		list_1 = new List(shell, SWT.BORDER);
		list_1.setBounds(10, 84, 100, 100);

		final Button button = new Button(shell, SWT.NONE);
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				
				CTabItem[] items = CanvasView.getCanvas_view().tabFolder.getItems();
				for (int i = 0; i < items.length; i++) {
					String title=items[i].getText();
					list.add(title, i);
					
					
				}
			}
		});
		button.setText(">>");
		button.setBounds(116, 83, 62, 25);

		final Button button_1 = new Button(shell, SWT.NONE);
		button_1.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				
				
				
			}
		});
		button_1.setText("<<");
		button_1.setBounds(116, 114, 62, 25);
		//
	}

}
