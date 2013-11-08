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

public class PcaDialog extends Dialog {


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
	 * @param parentShell
	 */
	public PcaDialog(Shell parentShell) {
		super(parentShell);
	}

	/**
	 * Create contents of the dialog
	 * @param parent
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		
		Composite container = (Composite) super.createDialogArea(parent);
		container.setLayout(null);

		Label numberOfClustersLabel;
		numberOfClustersLabel = new Label(container, SWT.NONE);
		numberOfClustersLabel.setBounds(119, 14, 138, 15);
		numberOfClustersLabel.setText("Number of Components");

		spinner = new Spinner(container, SWT.BORDER);
		spinner.setSelection(2);
		spinner.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				ClusterJob.setCluster(spinner.getSelection());
			}
		});
		spinner.setBounds(10, 11, 103, 20);

		list = new List(container, SWT.V_SCROLL | SWT.MULTI | SWT.H_SCROLL | SWT.BORDER);
		list.setBounds(10, 116, 149, 123);
		CTabItem[] items = CanvasView.getCanvas_view().tabFolder.getItems();
		for (int i = 0; i < items.length; i++) {
			String title=items[i].getText();
			list.add(title, i);
			
			
		}

		list_1 = new List(container, SWT.V_SCROLL | SWT.MULTI | SWT.H_SCROLL | SWT.BORDER);
		list_1.setBounds(219, 116, 149, 123);
		CTabItem item = CanvasView.tabFolder.getSelection();
        list_1.add(item.getText());
		final Button button = new Button(container, SWT.NONE);
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				String [] sel=list.getSelection();
				for (int i = 0; i < sel.length; i++) {
					list_1.add(sel[i], i);
					
					
				}
				
			}
		});
		button.setText(">>");
		button.setBounds(165, 115, 48, 25);

		final Button button_1 = new Button(container, SWT.NONE);
		button_1.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				int []sel=list_1.getSelectionIndices();
				for (int i = 0; i < sel.length; i++) {
					list_1.remove(sel);
				}
				
			}
		});
		button_1.setText("<<");
		button_1.setBounds(165, 157, 48, 25);
		text = new Text(container, SWT.BORDER);
		text.setEnabled(false);
		text.setBounds(10, 45, 295, 25);
		final Button euclideanButton = new Button(container, SWT.CHECK);
		euclideanButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				text.setEnabled(!text.isEnabled());
			}
		});
		euclideanButton.setText("arguments");
		euclideanButton.setBounds(311, 47, 93, 16);

		onlyTransferButton = new Button(container, SWT.CHECK);
		onlyTransferButton.addSelectionListener(new SelectionAdapter() {
			

			public void widgetSelected(final SelectionEvent e) {
				if(onlyTransfer){
					onlyTransfer=false;
				}
				else{
					onlyTransfer=true;
				}
			}
		});
		onlyTransferButton.setText("Only Transfer");
		onlyTransferButton.setBounds(10, 94, 93, 16);

		
		//
		return container;
	}

	/**
	 * Create contents of the button bar
	 * @param parent
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL,
				true);
		final Button button = createButton(parent, IDialogConstants.CANCEL_ID,
				IDialogConstants.CANCEL_LABEL, false);
	}

	/**
	 * Return the initial size of the dialog
	 */
	@Override
	protected Point getInitialSize() {
		return new Point(430, 336);
	}
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Cluster Images");
	}
	protected void buttonPressed(int buttonId) {
		if (buttonId == IDialogConstants.CANCEL_ID) {
			PcaJob.doClusterImages(false);
			this.cancelPressed();
			return;
		}
		if (buttonId == IDialogConstants.OK_ID) {
			PcaJob.doClusterImages(true);
			PcaJob.setCluster(spinner.getSelection());
			PcaJob.setClusterImages(list_1.getItems());
			PcaJob.setTextOptions(text.getText());
			PcaJob.onlyImageTransfer(onlyTransferButton.getSelection());
			this.okPressed();
			return;
		}
		super.buttonPressed(buttonId);
	}

}
