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
import org.eclipse.jface.preference.StringFieldEditor;

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
		selectionDevice=new RadioGroupFieldEditor("BROWSER_SELECTION", "Select browser:", 3, new String[][] { { "SWT Browser", "SWT_BROWSER" }}, getFieldEditorParent(), false);
		addField(selectionDevice);
		addField(new LabelFieldEditor("SWT Browser:", getFieldEditorParent()));
		addField(new BooleanFieldEditor("PDF_USE_BROWSER", "Embed PDF in browser view", BooleanFieldEditor.DEFAULT, getFieldEditorParent()));
		SpacerFieldEditor spacer1 = new SpacerFieldEditor(getFieldEditorParent());
		addField(spacer1);
		//addField(new LabelFieldEditor("JavaFX Browser:", getFieldEditorParent()));
		//addField(new BooleanFieldEditor("OPEN_BOWSER_IN_EXTRA_VIEW","Create a new view for each PDF file",getFieldEditorParentControl()));
		//addField(new BooleanFieldEditor("ENABLE_JAVAFXWEBKIT_SCROLLBARS","Enable JavaFX scrollbars",getFieldEditorParentControl()));
		//addField(new BooleanFieldEditor("ENABLE_BROWSER_SCROLLBARS","Enable browser scrollbars",getFieldEditorParentControl()));
		//addField(new BooleanFieldEditor("SCROLL_TO_DOCUMENT_END","Scroll to HTML document end",getFieldEditorParentControl()));
		//addField(new BooleanFieldEditor("REQUEST_EDITOR_FOCUS","Request editor focus (Markdown, HTML, Text editor)",getFieldEditorParentControl()));
		//addField(new BooleanFieldEditor("ENABLE_BROWSER_LOG","Print browser messages",getFieldEditorParentControl()));
		//addField(new BooleanFieldEditor("COPY_PDF_PATH_TO_CLIP","Copy PDF file path to clipboard",getFieldEditorParentControl()));
		SpacerFieldEditor spacer2 = new SpacerFieldEditor(getFieldEditorParent());
		addField(spacer2);
		addField(new LabelFieldEditor("R install package Help:", getFieldEditorParent()));
		addField(new StringFieldEditor("INSTALL_R_PACKAGES_DESCRPTION_URL", "URL: Install R packages help descr. ", -1, StringFieldEditor.VALIDATE_ON_KEY_STROKE, getFieldEditorParent()));
		SpacerFieldEditor spacer3 = new SpacerFieldEditor(getFieldEditorParent());
		addField(spacer3);
		addField(new LabelFieldEditor("JavaScript:", getFieldEditorParent()));
		addField(new BooleanFieldEditor("INTERPRET_JAVASCRIPT_IN_BROWSER","Execute JavaScript editor script in current browser",getFieldEditorParentControl()));
		
	}

}
