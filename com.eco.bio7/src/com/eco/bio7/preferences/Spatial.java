package com.eco.bio7.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.ScaleFieldEditor;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.eco.bio7.Bio7Plugin;
import com.eco.bio7.spatial.SpatialStructure;

public class Spatial extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
	IPreferenceStore store;

	/**
	 * Create the preference page
	 */
	public Spatial() {
		super(FLAT);
		setPreferenceStore(Bio7Plugin.getDefault().getPreferenceStore());
		store = Bio7Plugin.getDefault().getPreferenceStore();
	}

	/**
	 * Create contents of the preference page
	 */
	@Override
	protected void createFieldEditors() {
		{
			addField(new FileFieldEditor("spatialImage", "Default Image", getFieldEditorParent()));
		}

		{
			addField(new FileFieldEditor("heightmap", "Default Height Map", getFieldEditorParent()));
		}
		addField(new SpacerFieldEditor(getFieldEditorParent()));

		{
			addField(new BooleanFieldEditor("tile", "Image as Tile", getFieldEditorParent()));
		}

		{
			addField(new BooleanFieldEditor("transform", "Store view", getFieldEditorParent()));
		}

		{
			addField(new BooleanFieldEditor("cartesian", "Cartesian as default", getFieldEditorParent()));
		}

		{
			final ScaleFieldEditor scaleFieldEditor = new ScaleFieldEditor("spatialX", "Size X", getFieldEditorParent());
			final Scale scale = scaleFieldEditor.getScaleControl();
			scale.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(final SelectionEvent e) {
					SpatialStructure sg = SpatialStructure.getSpatialStructureInstance();
					if (sg != null) {
						sg.setSizeX(scale.getSelection());
					}

					store.setValue("spatialX", scale.getSelection());

				}
			});
			scale.setMinimum(1);
			scale.setSelection(1000);
			scale.setPageIncrement(1);
			scale.setMaximum(10000);
			addField(scaleFieldEditor);
		}

		{
			final ScaleFieldEditor scaleFieldEditor = new ScaleFieldEditor("spatialY", "Size Y", getFieldEditorParent());
			final Scale scale = scaleFieldEditor.getScaleControl();
			scale.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(final SelectionEvent e) {
					SpatialStructure sg = SpatialStructure.getSpatialStructureInstance();
					if (sg != null) {
						sg.setSizeY(scale.getSelection());
					}

					store.setValue("spatialY", scale.getSelection());
				}
			});
			scale.setSelection(1000);
			scale.setPageIncrement(1);
			scale.setMaximum(10000);
			addField(scaleFieldEditor);
		}

		{
			final ScaleFieldEditor scaleFieldEditor = new ScaleFieldEditor("spatialZ", "Size Z", getFieldEditorParent());
			final Scale scale = scaleFieldEditor.getScaleControl();
			scale.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(final SelectionEvent e) {
					SpatialStructure sg = SpatialStructure.getSpatialStructureInstance();
					if (sg != null) {
						sg.setSizeZ(scale.getSelection());
					}

					store.setValue("spatialZ", scale.getSelection());
				}
			});
			scale.setSelection(1000);
			scale.setPageIncrement(1);
			scale.setMaximum(10000);
			addField(scaleFieldEditor);
		}
		// Create the field editors
	}

	/**
	 * Initialize the preference page
	 */
	public void init(IWorkbench workbench) {
		// Initialize the preference page
	}

	public boolean performOk() {
		IPreferenceStore store = Bio7Plugin.getDefault().getPreferenceStore();
		int sizeX = store.getInt("spatialX");
		int sizeY = store.getInt("spatialY");
		int sizeZ = store.getInt("spatialZ");
		boolean storeTransfom=store.getBoolean("transform");

		SpatialStructure sg = SpatialStructure.getSpatialStructureInstance();
		if (sg != null) {
			sg.setSizeX(sizeX);
			sg.setSizeY(sizeY);
			sg.setSizeZ(sizeZ);
			
			/*If the values should be stored!*/
			if(storeTransfom){
				
				store.setValue("transformZ", sg.getTransz());
				store.setValue("rotatx", sg.getRotatx());
				store.setValue("rotaty", sg.getRotaty());
				store.setValue("rotatz", sg.getRotatz());
				
			}
			
		}

		return super.performOk();
	}

}
