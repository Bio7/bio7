package com.eco.bio7.image;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.GridData;

public class ClusterDialog extends Dialog {

	private Text text;
	private List list_1;
	private List list;
	private Spinner spinner;
	protected boolean onlyTransfer;
	private Button onlyTransferButton;

	public Spinner getSpinner() {
		return spinner;
	}

	public void setSpinner(Spinner spinner) {
		this.spinner = spinner;
	}

	/**
	 * Create the dialog
	 * 
	 * @param parentShell
	 */
	public ClusterDialog(Shell parentShell) {
		super(parentShell);
	}

	/**
	 * Create contents of the dialog
	 * 
	 * @param parent
	 */
	@Override
	protected Control createDialogArea(Composite parent) {

		Composite container = (Composite) super.createDialogArea(parent);

		Label numberOfClustersLabel;
		container.setLayout(new GridLayout(5, true));

		spinner = new Spinner(container, SWT.BORDER);
		GridData gd_spinner = new GridData(SWT.FILL, SWT.FILL, true, false, 1,
				1);
		gd_spinner.heightHint = 30;
		spinner.setLayoutData(gd_spinner);
		spinner.setSelection(2);
		spinner.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				ClusterJob.setCluster(spinner.getSelection());
			}
		});
		numberOfClustersLabel = new Label(container, SWT.NONE);
		GridData gd_numberOfClustersLabel = new GridData(SWT.FILL, SWT.FILL,
				true, false, 3, 1);
		gd_numberOfClustersLabel.heightHint = 30;
		numberOfClustersLabel.setLayoutData(gd_numberOfClustersLabel);
		numberOfClustersLabel.setText("Number of Clusters");
		new Label(container, SWT.NONE);
		text = new Text(container, SWT.BORDER);
		GridData gd_text = new GridData(SWT.FILL, SWT.FILL, true, false, 4, 1);
		gd_text.heightHint = 30;
		text.setLayoutData(gd_text);
		text.setEnabled(false);

		final Button euclideanButton = new Button(container, SWT.CHECK);
		GridData gd_euclideanButton = new GridData(SWT.FILL, SWT.FILL, true,
				false, 1, 1);
		gd_euclideanButton.heightHint = 30;
		euclideanButton.setLayoutData(gd_euclideanButton);
		euclideanButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {

				text.setEnabled(!text.isEnabled());

			}
		});
		euclideanButton.setText("arguments");

		onlyTransferButton = new Button(container, SWT.CHECK);
		GridData gd_onlyTransferButton = new GridData(SWT.FILL, SWT.FILL, true,
				false, 2, 1);
		gd_onlyTransferButton.heightHint = 30;
		onlyTransferButton.setLayoutData(gd_onlyTransferButton);
		onlyTransferButton.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(final SelectionEvent e) {
				if (onlyTransfer) {
					onlyTransfer = false;
				} else {
					onlyTransfer = true;
				}
			}
		});
		onlyTransferButton.setText("Only Transfer");
		new Label(container, SWT.NONE);
		new Label(container, SWT.NONE);
		new Label(container, SWT.NONE);

		list = new List(container, SWT.V_SCROLL | SWT.MULTI | SWT.H_SCROLL
				| SWT.BORDER);
		list.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 2));
		final Button button = new Button(container, SWT.NONE);
		GridData gd_button = new GridData(SWT.FILL, SWT.BOTTOM, true, true, 1, 1);
		gd_button.heightHint = 40;
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

		list_1 = new List(container, SWT.V_SCROLL | SWT.MULTI | SWT.H_SCROLL
				| SWT.BORDER);
		list_1.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 2));
		CTabItem item = CanvasView.tabFolder.getSelection();
		list_1.add(item.getText());
		CTabItem[] items = CanvasView.getCanvas_view().tabFolder.getItems();
		for (int i = 0; i < items.length; i++) {
			String title = items[i].getText();
			list.add(title, i);

		}

		final Button button_1 = new Button(container, SWT.NONE);
		GridData gd_button_1 = new GridData(SWT.FILL, SWT.TOP, true, true, 1,
				1);
		gd_button_1.heightHint = 40;
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

		//
		return container;
	}

	/**
	 * Create contents of the button bar
	 * 
	 * @param parent
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		Button button_1 = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL,
				true);
		final Button button = createButton(parent, IDialogConstants.CANCEL_ID,
				IDialogConstants.CANCEL_LABEL, false);
	}

	/**
	 * Return the initial size of the dialog
	 */
	@Override
	protected Point getInitialSize() {
		return new Point(601, 418);
	}

	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Cluster Images");
	}

	protected void buttonPressed(int buttonId) {
		if (buttonId == IDialogConstants.CANCEL_ID) {
			this.cancelPressed();
			ClusterJob.doClusterImages(false);

			return;
		}
		if (buttonId == IDialogConstants.OK_ID) {
			ClusterJob.doClusterImages(true);
			ClusterJob.setCluster(spinner.getSelection());
			ClusterJob.setClusterImages(list_1.getItems());
			ClusterJob.setTextOptions(text.getText());
			ClusterJob.onlyImageTransfer(onlyTransferButton.getSelection());
			this.okPressed();
			return;
		}
		super.buttonPressed(buttonId);
	}

}
