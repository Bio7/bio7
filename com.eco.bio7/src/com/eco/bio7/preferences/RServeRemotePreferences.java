package com.eco.bio7.preferences;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.eco.bio7.Bio7Plugin;

public class RServeRemotePreferences extends FieldEditorPreferencePage implements IWorkbenchPreferencePage, PropertyChangeListener {

	/**
	 * Create the preference page.
	 */
	public RServeRemotePreferences() {
		super(GRID);
		setPreferenceStore(Bio7Plugin.getDefault().getPreferenceStore());
	}

	/**
	 * Create contents of the preference page.
	 */
	@Override
	protected void createFieldEditors() {
		addField(new SpacerFieldEditor(getFieldEditorParent()));
		addField(new LabelFieldEditor("Rserve remote:", getFieldEditorParent()));
		addField(new BooleanFieldEditor("REMOTE", "Remote enabled", getFieldEditorParent()));
		addField(new StringFieldEditor("HOST", "Host name/IP", getFieldEditorParent()));
		addField(new IntegerFieldEditor("TCP", "TCP port", getFieldEditorParent()));
		addField(new StringFieldEditor("USERNAME", "Username", getFieldEditorParent()));
		StringFieldEditor sf = new StringFieldEditor("PASSWORD", "Password", getFieldEditorParent());
		sf.getTextControl(getFieldEditorParent()).setEchoChar('*');
		addField(sf);
	}

	/**
	 * Initialize the preference page.
	 */
	public void init(IWorkbench workbench) {
		// Initialize the preference page
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		// TODO Auto-generated method stub
		
	}

}
