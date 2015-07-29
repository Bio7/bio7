package com.eco.bio7.image;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;

public class TransferSelectionCoordsDialog extends Dialog {

	private Combo combo;
	protected boolean transferAsList=true;
	protected String geometrySelection;
	protected int geometrySelectionSelection=0;
	
	public boolean transferAsList() {
		return transferAsList;
	}

	public String getGeometrySelection() {
		return geometrySelection;
	}

	public int getGeometrySelectionSelection() {
		return geometrySelectionSelection;
	}

	

	public TransferSelectionCoordsDialog(Shell parentShell) {
		super(parentShell);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		GridLayout gridLayout = (GridLayout) container.getLayout();
		gridLayout.marginHeight = 14;
		gridLayout.marginHeight = 14;
		gridLayout.marginHeight = 30;
		gridLayout.makeColumnsEqualWidth = true;
		gridLayout.makeColumnsEqualWidth = true;
		GridLayout gl_container = new GridLayout(1, false);
		gl_container.marginHeight = 30;
		container.setLayout(gl_container);

		Button btnSelectionCoordinatesAs = new Button(container, SWT.RADIO);
		btnSelectionCoordinatesAs.addSelectionListener(new SelectionAdapter() {
			
			public void widgetSelected(SelectionEvent e) {
				combo.setEnabled(false);
				transferAsList=true;
			}
		});
		GridData gd_btnSelectionCoordinatesAs = new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1);
		gd_btnSelectionCoordinatesAs.heightHint = 30;
		btnSelectionCoordinatesAs.setLayoutData(gd_btnSelectionCoordinatesAs);
		btnSelectionCoordinatesAs.setSelection(true);
		btnSelectionCoordinatesAs.setText("Selection coordinates as a list");

		Button btnRadioButton = new Button(container, SWT.RADIO);
		btnRadioButton.addSelectionListener(new SelectionAdapter() {
			
			public void widgetSelected(SelectionEvent e) {
				combo.setEnabled(true);
				transferAsList=false;
			}
		});
		GridData gd_btnRadioButton = new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1);
		gd_btnRadioButton.heightHint = 30;
		btnRadioButton.setLayoutData(gd_btnRadioButton);
		btnRadioButton.setText("Spatial Geometries (R package 'sp' and 'maptools' required!)\r\n");

		combo = new Combo(container, SWT.NONE);
		combo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				
				geometrySelection=combo.getText();
				geometrySelectionSelection=combo.getSelectionIndex();
				
				
			}
		});
		combo.setItems(new String[] { "SpatialPolygons", "SpatialLines", "SpatialPoints" });
		GridData gd_combo = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_combo.heightHint = 30;
		combo.setLayoutData(gd_combo);
		combo.setText("Geometries");
		combo.select(0);
		combo.setEnabled(false);
		return container;
	}

	// overriding this methods allows you to set the
	// title of the custom dialog
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Transfer ROI selections");
	}

	@Override
	protected Point getInitialSize() {
		return new Point(450, 300);
	}

	@Override
	protected void okPressed() {

		super.okPressed();
	}

	protected void cancelPressed() {

		super.cancelPressed();
	}

}