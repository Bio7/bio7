package com.eco.bio7.javapreferences;

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
import com.eco.bio7.javaeditor.Bio7EditorPlugin;
import org.eclipse.jface.preference.ComboFieldEditor;




public class WorkbenchPreferenceJava extends FieldEditorPreferencePage implements IWorkbenchPreferencePage, PropertyChangeListener {

	public WorkbenchPreferenceJava() {
		super(GRID);

		setPreferenceStore(Bio7EditorPlugin.getDefault().getPreferenceStore());
		setDescription("Bio7 Java Options");
	}

	public void createFieldEditors() {
		addField(new SpacerFieldEditor(getFieldEditorParent()));
		addField(new LabelFieldEditor("Java Compiler:", getFieldEditorParent()));
		addField(new BooleanFieldEditor("classbody", "Default parse and compile a Java file with a classbody", getFieldEditorParent()));
		addField(new ComboFieldEditor("compiler_version", "Java Version", new String[][]{{"1.8", "1.8"},{"1.7", "1.7"},{"1.6", "1.6"}, {"1.5", "1.5"}, {"1.4", "1.4"}}, getFieldEditorParent()));
		addField(new BooleanFieldEditor("compiler_debug", "Generate Debugging Information", BooleanFieldEditor.DEFAULT, getFieldEditorParent()));
		addField(new BooleanFieldEditor("compiler_verbose", "Verbose", BooleanFieldEditor.DEFAULT, getFieldEditorParent()));
		addField(new BooleanFieldEditor("compiler_warnings", "Generate Warnings", BooleanFieldEditor.DEFAULT, getFieldEditorParent()));
		addField(new SpacerFieldEditor(getFieldEditorParent()));
		{
			LabelFieldEditor labelFieldEditor = new LabelFieldEditor("Editor:", getFieldEditorParent());
			labelFieldEditor.setLabelText("Editor: Colours And Fonts");
			addField(labelFieldEditor);
		}
		addField(new ColorFieldEditor("colourkey", "Colour Keywords:", getFieldEditorParent()));
		addField(new FontFieldEditor("colourkeyfont", "Font:", getFieldEditorParent()));
		addField(new ColorFieldEditor("colourkey1", "Colour Type:", getFieldEditorParent()));
		addField(new FontFieldEditor("colourkeyfont1", "Font:", getFieldEditorParent()));
		addField(new ColorFieldEditor("colourkey2", "Colour String:", getFieldEditorParent()));
		addField(new FontFieldEditor("colourkeyfont2", "Font:", getFieldEditorParent()));
		addField(new ColorFieldEditor("colourkey3", "Colour Single Comment:", getFieldEditorParent()));
		addField(new FontFieldEditor("colourkeyfont3", "Font:", getFieldEditorParent()));
		//addField(new ColorFieldEditor("colourkey8", "Colour Multi-Line Comment:", getFieldEditorParent()));
		//addField(new FontFieldEditor("colourkeyfont8", "Font:", getFieldEditorParent()));
		addField(new ColorFieldEditor("colourkey4", "Colour Default:", getFieldEditorParent()));
		addField(new FontFieldEditor("colourkeyfont4", "Font:", getFieldEditorParent()));
		addField(new ColorFieldEditor("colourkey5", "Operators:", getFieldEditorParent()));
		addField(new FontFieldEditor("colourkeyfont5", "Font:", getFieldEditorParent()));
		addField(new ColorFieldEditor("colourkey6", "Braces:", getFieldEditorParent()));
		addField(new FontFieldEditor("colourkeyfont6", "Font:", getFieldEditorParent()));
		addField(new ColorFieldEditor("colourkey7", "Numbers:", getFieldEditorParent()));
		addField(new FontFieldEditor("colourkeyfont7", "Font:", getFieldEditorParent()));		
		
		

	}

	public void init(IWorkbench workbench) {

		IPreferenceStore store = Bio7EditorPlugin.getDefault().getPreferenceStore();
		store.setDefault("classbody",false);
		PreferenceConverter.setDefault(store, "colourkey", new RGB(127, 0, 85));
		PreferenceConverter.setDefault(store, "colourkey1", new RGB(127, 0, 85));
		PreferenceConverter.setDefault(store, "colourkey2", new RGB(42, 0, 255));
		PreferenceConverter.setDefault(store, "colourkey3", new RGB(63, 127, 95));
		PreferenceConverter.setDefault(store, "colourkey4", new RGB(0, 0, 0));
		PreferenceConverter.setDefault(store, "colourkey5", new RGB(0, 0, 0));
		PreferenceConverter.setDefault(store, "colourkey6", new RGB(0, 0, 0));
		PreferenceConverter.setDefault(store, "colourkey7", new RGB(0, 0, 0));
		

	}

	public boolean performOk() {

		return super.performOk();
	}

	public void propertyChange(PropertyChangeEvent evt) {

	}

}
