package com.eco.bio7.image;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
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
import org.eclipse.swt.widgets.Text;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RserveException;

import com.eco.bio7.batch.Bio7Dialog;
import com.eco.bio7.rbridge.RServe;
import com.eco.bio7.rbridge.RState;

import org.eclipse.swt.widgets.Label;

public class TransferSelectionCoordsDialog extends Dialog {

	private Combo combo;
	protected boolean transferAsList = true;
	protected String geometrySelection;
	protected int geometrySelectionSelection = 0;
	private Combo combo_1;
	private Button btnAddDataframe;
	protected String selDataframe;
	protected String crsText;
	protected boolean doSetCrs=false;
	protected boolean doSetDataframe=false;

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
				combo_1.setEnabled(false);
				btnAddDataframe.setEnabled(false);
				/*text.setEnabled(false);
				btnAddCrs.setEnabled(false);
				btnLoadFromFile.setEnabled(false);*/
				transferAsList = true;
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
				combo_1.setEnabled(true);
				btnAddDataframe.setEnabled(true);
				/*text.setEnabled(true);
				btnAddCrs.setEnabled(true);
				btnLoadFromFile.setEnabled(true);*/
				transferAsList = false;
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

				geometrySelection = combo.getText();
				geometrySelectionSelection = combo.getSelectionIndex();

			}
		});
		combo.setItems(new String[] { "SpatialPolygons", "SpatialLines", "SpatialPoints" });
		GridData gd_combo = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		gd_combo.heightHint = 30;
		combo.setLayoutData(gd_combo);
		combo.setText("Geometries");
		combo.select(0);
		combo.setEnabled(false);

		btnAddDataframe = new Button(container, SWT.CHECK);
		btnAddDataframe.setEnabled(false);
		btnAddDataframe.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (doSetDataframe)
					doSetDataframe = false;
				else {
					doSetDataframe = true;
				}

				REXP x = null;
				combo_1.removeAll();
				combo_1.add("-Select-", 0);
				combo_1.select(0);
				if (RServe.isAliveDialog()) {
					if (RState.isBusy() == false) {
						String[] v = null;
						// List all variables in the R workspace!

						try {
							RServe.getConnection().eval("varWorkspaceType<-NULL;for(i in 1:length(ls())){if(is.data.frame(get(ls()[i]))==TRUE){varWorkspaceType<-append(varWorkspaceType,ls()[i])}}");
							x = RServe.getConnection().eval("varWorkspaceType");
							if (x.isNull() == false) {
								try {
									v = x.asStrings();
								} catch (REXPMismatchException e1) {

									e1.printStackTrace();
								}
							}
							RServe.getConnection().eval("remove(varWorkspaceType)");
						} catch (RserveException e1) {

							e1.printStackTrace();
						}
						if (v != null) {
							for (int i = 0; i < v.length; i++) {

								combo_1.add(v[i], i + 1);
								combo_1.update();
							}
						}
					} else {
						Bio7Dialog.message("Rserve is busy!");

					}

				}
				combo_1.select(0);
			}
		});
		btnAddDataframe.setText("Add selected dataframe");

		combo_1 = new Combo(container, SWT.NONE);
		combo_1.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {

				selDataframe = combo_1.getText();
			}
		});
		combo_1.setItems(new String[] { "-Select-" });
		combo_1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		combo_1.select(0);
		combo_1.setEnabled(false);
		return container;
	}

	public String getSelDataframe() {
		return selDataframe;
	}

	public void setSelDataframe(String selDataframe) {
		this.selDataframe = selDataframe;
	}

	public String getCrsText() {
		return crsText;
	}

	public void setCrsText(String crsText) {
		this.crsText = crsText;
	}

	public boolean isDoSetCrs() {
		return doSetCrs;
	}

	public void setDoSetCrs(boolean doSetCrs) {
		this.doSetCrs = doSetCrs;
	}

	public boolean isDoSetDataframe() {
		return doSetDataframe;
	}

	public void setDoSetDataframe(boolean doSetDataframe) {
		this.doSetDataframe = doSetDataframe;
	}


	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Transfer ROI selections");
	}

	@Override
	protected Point getInitialSize() {
		return new Point(450, 391);
	}

	@Override
	protected void okPressed() {

		super.okPressed();
	}

	protected void cancelPressed() {

		super.cancelPressed();
	}

}