package com.eco.bio7.preferences;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.eco.bio7.Bio7Plugin;

public class BrowserPreferences extends FieldEditorPreferencePage implements IWorkbenchPreferencePage, PropertyChangeListener {

	private RadioGroupFieldEditor selectionDevice;

	/**
	 * Create the preference page.
	 */
	public BrowserPreferences() {
		super(GRID);
		setPreferenceStore(Bio7Plugin.getDefault().getPreferenceStore());
	}
	public Composite getFieldEditorParentControl(){
		return getFieldEditorParent();
	}
	
	/**
	 * Initialize the preference page.
	 */
	public void init(IWorkbench workbench) {
		// Initialize the preference page
	}

	@Override
	public void propertyChange(PropertyChangeEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void createFieldEditors() {
		selectionDevice=new RadioGroupFieldEditor("BROWSER_SELECTION", "Select Browser:", 3, new String[][] { { "SWT Browser", "SWT_BROWSER" },{ "JavaFX Browser", "JAVAFX_BROWSER" }}, getFieldEditorParent(), false);
		addField(selectionDevice);
		addField(new LabelFieldEditor("SWT Browser:", getFieldEditorParent()));
		addField(new BooleanFieldEditor("PDF_USE_BROWSER", "Embed PDF in Browser View", BooleanFieldEditor.DEFAULT, getFieldEditorParent()));
		SpacerFieldEditor spacer1 = new SpacerFieldEditor(getFieldEditorParent());
		addField(spacer1);
		addField(new LabelFieldEditor("JavaFX Browser:", getFieldEditorParent()));
		addField(new BooleanFieldEditor("OPEN_BOWSER_IN_EXTRA_VIEW","Create a new View for each PDF file",getFieldEditorParentControl()));
		
	}

}
