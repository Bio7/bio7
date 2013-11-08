package com.eco.bio7.reditor.preferences;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import org.eclipse.jface.preference.ColorFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FontFieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;


import com.eco.bio7.pythonedit.PythonEditorPlugin;
import org.eclipse.jface.preference.BooleanFieldEditor;

public class WorkbenchPreferencePython extends FieldEditorPreferencePage implements IWorkbenchPreferencePage, PropertyChangeListener {

	public WorkbenchPreferencePython() {
		super(GRID);

		setPreferenceStore(PythonEditorPlugin.getDefault().getPreferenceStore());
		setDescription("Bio7 Jython Editor");
	}

	public void createFieldEditors() {
		

		addField(new ColorFieldEditor("colourkey", "Colour Keywords:", getFieldEditorParent()));
		addField(new FontFieldEditor("colourkeyfont", "Font:", getFieldEditorParent()));
		addField(new ColorFieldEditor("colourkey1", "Colour Type:", getFieldEditorParent()));
		addField(new FontFieldEditor("colourkeyfont1", "Font:", getFieldEditorParent()));
		addField(new ColorFieldEditor("colourkey2", "Colour String:", getFieldEditorParent()));
		addField(new FontFieldEditor("colourkeyfont2", "Font:", getFieldEditorParent()));
		addField(new ColorFieldEditor("colourkey3", "Colour Single Comment:", getFieldEditorParent()));
		addField(new FontFieldEditor("colourkeyfont3", "Font:", getFieldEditorParent()));
		addField(new ColorFieldEditor("colourkey4", "Colour Default:", getFieldEditorParent()));
		addField(new FontFieldEditor("colourkeyfont4", "Font:", getFieldEditorParent()));
		addField(new ColorFieldEditor("colourkey5", "Operators:", getFieldEditorParent()));
		addField(new FontFieldEditor("colourkeyfont5", "Font:", getFieldEditorParent()));
		//addField(new ColorFieldEditor("colourkey6", "Braces:", getFieldEditorParent()));
		//addField(new FontFieldEditor("colourkeyfont6", "Font:", getFieldEditorParent()));
		addField(new ColorFieldEditor("colourkey7", "Numbers:", getFieldEditorParent()));
		addField(new FontFieldEditor("colourkeyfont7", "Font:", getFieldEditorParent()));
		addField(new ColorFieldEditor("colourkey8", "Decorators:", getFieldEditorParent()));
		addField(new FontFieldEditor("colourkeyfont8", "Font:", getFieldEditorParent()));
		addField(new ColorFieldEditor("colourkey9", "Class Name:", getFieldEditorParent()));
		addField(new FontFieldEditor("colourkeyfont9", "Font:", getFieldEditorParent()));
		addField(new ColorFieldEditor("colourkey10", "Function Name:", getFieldEditorParent()));
		addField(new FontFieldEditor("colourkeyfont10", "Font:", getFieldEditorParent()));
		//addField(new ColorFieldEditor("colourkey11", "Self:", getFieldEditorParent()));
		//addField(new FontFieldEditor("colourkeyfont11", "Font:", getFieldEditorParent()));
		addField(new ColorFieldEditor("colourkey12", "Parenthesis:", getFieldEditorParent()));
		addField(new FontFieldEditor("colourkeyfont12", "Font:", getFieldEditorParent()));

	}

	public void init(IWorkbench workbench) {

		IPreferenceStore store = PythonEditorPlugin.getDefault().getPreferenceStore();
		PreferenceConverter.setDefault(store, "colourkey", new RGB(127, 0, 85));
		PreferenceConverter.setDefault(store, "colourkey1", new RGB(127, 0, 85));
		PreferenceConverter.setDefault(store, "colourkey2", new RGB(42, 0, 255));
		PreferenceConverter.setDefault(store, "colourkey3", new RGB(63, 127, 95));
		PreferenceConverter.setDefault(store, "colourkey4", new RGB(0, 0, 0));
		PreferenceConverter.setDefault(store, "colourkey5", new RGB(0, 0, 0));
		//PreferenceConverter.setDefault(store, "colourkey6", new RGB(0, 0, 0));
		PreferenceConverter.setDefault(store, "colourkey7", new RGB(0, 0, 0));
		PreferenceConverter.setDefault(store, "colourkey8", new RGB(0, 0, 0));
		PreferenceConverter.setDefault(store, "colourkey9", new RGB(0, 0, 0));
		PreferenceConverter.setDefault(store, "colourkey10", new RGB(0, 0, 0));
		//PreferenceConverter.setDefault(store, "colourkey11", new RGB(0, 0, 0));
		PreferenceConverter.setDefault(store, "colourkey12", new RGB(0, 0, 0));

	}

	public boolean performOk() {

		return super.performOk();
	}

	public void propertyChange(PropertyChangeEvent evt) {

	}

}
