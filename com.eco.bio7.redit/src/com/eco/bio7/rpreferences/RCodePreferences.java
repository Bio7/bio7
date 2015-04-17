package com.eco.bio7.rpreferences;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ColorFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FontFieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import com.eco.bio7.reditor.Bio7REditorPlugin;

public class RCodePreferences extends FieldEditorPreferencePage implements IWorkbenchPreferencePage, PropertyChangeListener {

	public RCodePreferences() {
		super(GRID);

		setPreferenceStore(Bio7REditorPlugin.getDefault().getPreferenceStore());
		setDescription("R Code Preferences");
	}

	public void createFieldEditors() {

		
		addField(new LabelFieldEditor("Code Folding:", getFieldEditorParent()));
		
		addField(new BooleanFieldEditor("FUNCTIONS_FOLDING", "Functions", getFieldEditorParent()));
		addField(new BooleanFieldEditor("FOR_LOOP_FOLDING", "For loop", getFieldEditorParent()));
		addField(new BooleanFieldEditor("WHILE_LOOP_FOLDING", "While loop", getFieldEditorParent()));
		addField(new BooleanFieldEditor("REPEAT_LOOP_FOLDING", "Repeat loop", getFieldEditorParent()));		
		addField(new BooleanFieldEditor("IF_CONDITION_FOLDING", "If condition", getFieldEditorParent()));
		
		addField(new LabelFieldEditor("Code context:", getFieldEditorParent()));
		addField(new BooleanFieldEditor("SHOW_INFOPOPUP", "Show Infopopup", getFieldEditorParent()));
		addField(new BooleanFieldEditor("MARK_WORDS", "Mark selected words", getFieldEditorParent()));
	}

	public void init(IWorkbench workbench) {

		IPreferenceStore store = Bio7REditorPlugin.getDefault().getPreferenceStore();
		store.setDefault("FUNCTIONS_FOLDING", true);
		store.setDefault("FOR_LOOP_FOLDING", true);
		store.setDefault("WHILE_LOOP_FOLDING", true);
		store.setDefault("REPEAT_LOOP_FOLDING", true);
		store.setDefault("IF_CONDITION_FOLDING", true);
		
		store.setDefault("SHOW_INFOPOPUP", true);
		store.setDefault("MARK_WORDS", true);

	}

	public boolean performOk() {

		return super.performOk();
	}

	public void propertyChange(PropertyChangeEvent evt) {

	}

}
