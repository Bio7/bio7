package com.eco.bio7.preferences;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.eco.bio7.Bio7Plugin;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;

public class LatexPreferences extends FieldEditorPreferencePage implements IWorkbenchPreferencePage, PropertyChangeListener {

	/**
	 * Create the preference page.
	 */
	public LatexPreferences() {
		super(GRID);
		setPreferenceStore(Bio7Plugin.getDefault().getPreferenceStore());
        
	}

	/**
	 * Create contents of the preference page.
	 */
	@Override
	protected void createFieldEditors() {
		addField(new SpacerFieldEditor(getFieldEditorParent()));

		
		addField(new DirectoryFieldEditor("pdfLatex", "pdflatex/xelatex Dir", getFieldEditorParent()));

		addField(new SpacerFieldEditor(getFieldEditorParent()));
		addField(new ComboFieldEditor("LATEX_ENGINE", "TeX Engine", new String[][] { { "pdflatex", "pdflatex" }, { "xelatex", "xelatex" }}, getFieldEditorParent()));
		addField(new BooleanFieldEditor("LATEX_CLEAN_FILES", "Clean Auxiliary Files", BooleanFieldEditor.DEFAULT, getFieldEditorParent()));
		{
			StringFieldEditor stringFieldEditor = new StringFieldEditor("LATEX_FILES_EXT_DELETE", "File Extensions", -1, StringFieldEditor.VALIDATE_ON_KEY_STROKE, getFieldEditorParent());
			
			addField(stringFieldEditor);
		}
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

}
