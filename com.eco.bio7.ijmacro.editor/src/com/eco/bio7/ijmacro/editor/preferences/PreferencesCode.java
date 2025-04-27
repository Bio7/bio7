package com.eco.bio7.ijmacro.editor.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import com.eco.bio7.ijmacro.editor.IJMacroEditorPlugin;
import org.eclipse.jface.preference.FileFieldEditor;

public class PreferencesCode extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
	private IPreferenceStore store;
	private StringFieldEditor mult_1;

	public PreferencesCode() {
		super(GRID);
		store = IJMacroEditorPlugin.getDefault().getPreferenceStore();
		setPreferenceStore(IJMacroEditorPlugin.getDefault().getPreferenceStore());
		setDescription("ImageJ Macro Editor");
	}

	public void createFieldEditors() {
		addField(new BooleanFieldEditor("EVALUATE_EXTERNAL", "Evaluate external (ImageJ launcher - separate Java process)", BooleanFieldEditor.DEFAULT, getFieldEditorParent()));
		addField(new SpacerFieldEditor(getFieldEditorParent()));
		addField(new FileFieldEditor("LOCATION_EXTERNAL", "Location ImageJ launcher", getFieldEditorParent()));
		addField(new StringFieldEditor("OPTIONS_EXTERNAL", "ImageJ launcher options", -1, StringFieldEditor.VALIDATE_ON_KEY_STROKE, getFieldEditorParent()));
		addField(new LabelFieldEditor("Code context:", getFieldEditorParent()));
		
		addField(
				new BooleanFieldEditor("SHOW_INFOPOPUP", "Show info popup (restart to apply!)", getFieldEditorParent()));
		addField(new SpacerFieldEditor(getFieldEditorParent()));
		addField(new LabelFieldEditor("Code completion/templates:", getFieldEditorParent()));
		addField(new BooleanFieldEditor("IJ_COMPLETION_CONTAINS", "Open code completion templates which contain typed prefix",
				getFieldEditorParent()));
		addField(new BooleanFieldEditor("TYPED_CODE_COMPLETION", "Open code completion/templates when typing",
				getFieldEditorParent()));
		addField(new StringFieldEditor("ACTIVATION_CHARS", "Activation chars", -1,
				StringFieldEditor.VALIDATE_ON_KEY_STROKE, getFieldEditorParent()));
		addField(new SpacerFieldEditor(getFieldEditorParent()));
		addField(new LabelFieldEditor("Source:", getFieldEditorParent()));
		addField(new BooleanFieldEditor("MARK_WORDS", "Mark selected words", getFieldEditorParent()));
		addField(new SpacerFieldEditor(getFieldEditorParent()));
		addField(new LabelFieldEditor("Automatic close:", getFieldEditorParent()));
		addField(new BooleanFieldEditor("CLOSE_BRACES", "{Braces}", getFieldEditorParent()));
		addField(new BooleanFieldEditor("CLOSE_PARENTHESES", "(Parentheses)", getFieldEditorParent()));
		addField(new BooleanFieldEditor("CLOSE_BRACKETS", "[Brackets]", getFieldEditorParent()));
		addField(new BooleanFieldEditor("CLOSE_DOUBLE_QUOTE", "\"Strings\"", getFieldEditorParent()));
		addField(new BooleanFieldEditor("CLOSE_SINGLEQUOTE", "'Strings'", getFieldEditorParent()));
		addField(new SpacerFieldEditor(getFieldEditorParent()));
		addField(new LabelFieldEditor("Automatic indent:", getFieldEditorParent()));
		addField(new BooleanFieldEditor("IJMACRO_EDITOR_EDIT_INDENT", "Automatically indent", getFieldEditorParent()));
		addField(new BooleanFieldEditor("CLOSE_BRACES_LINEBREAK", "{Braces - with linebreak}", getFieldEditorParent()));
		addField(new SpacerFieldEditor(getFieldEditorParent()));
		addField(new LabelFieldEditor("Formatter:", getFieldEditorParent()));
		mult_1 = new StringFieldEditor("IJMACRO_EDITOR_FORMAT_OPTIONS", "ImageJ Macro editor formatter options", -1,
				StringFieldEditor.VALIDATE_ON_KEY_STROKE, getFieldEditorParent());
		mult_1.setLabelText("Options");
		addField(mult_1);
	}

	/**
	 * Initialize the preference page.
	 */
	public void init(IWorkbench workbench) {
		// Initialize the preference page
	}

}
