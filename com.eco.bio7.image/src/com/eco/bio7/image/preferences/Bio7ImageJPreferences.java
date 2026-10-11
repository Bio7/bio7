package com.eco.bio7.image.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.eco.bio7.image.ui.Activator;
import com.eco.bio7.image.ui.Util;

import ij.Menus;

/**
 * This class represents a preference page that is contributed to the
 * Preferences dialog. By subclassing <samp>FieldEditorPreferencePage</samp>, we
 * can use the field support built into JFace that allows us to create a page
 * that is small and knows how to save, restore and apply itself.
 * <p>
 * This page is used to modify preferences only. They are stored in the
 * preference store that belongs to the main plug-in class. That way,
 * preferences can be accessed directly via the preference store.
 */

public class Bio7ImageJPreferences extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public Bio7ImageJPreferences() {
		super(GRID);
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
		setDescription("Preferences ImageJ");
	}

	/**
	 * Creates the field editors. Field editors are abstractions of the common GUI
	 * blocks needed to manipulate various types of preferences. Each field editor
	 * knows how to save and restore itself.
	 */
	public void createFieldEditors() {

		addField(new DirectoryFieldEditor("PLUGINS_PATH", "&Plugins Path", getFieldEditorParent()));

		/* SWTImageJ loads the macros from the folder 'macros' next to the plugins folder! */
		addField(new LabelFieldEditor("Macros are loaded from the 'macros' folder next to the plugins folder (restart necessary).", getFieldEditorParent()));

		addField(new SpacerFieldEditor(getFieldEditorParent()));
		addField(new LabelFieldEditor("Image Methods Dialog Size:", getFieldEditorParent()));
		addField(new IntegerFieldEditor("IMAGE_METHODS_SIZE_X", "x", getFieldEditorParent()));
		addField(new IntegerFieldEditor("IMAGE_METHODS_SIZE_Y", "y", getFieldEditorParent()));
		addField(new SpacerFieldEditor(getFieldEditorParent()));
		addField(new LabelFieldEditor("ImageJ Toolbar Dialog Size:", getFieldEditorParent()));
		addField(new IntegerFieldEditor("IMAGEJ_TOOLBAR_SIZE_X", "x", getFieldEditorParent()));
		addField(new IntegerFieldEditor("IMAGEJ_TOOLBAR_SIZE_Y", "y", getFieldEditorParent()));
		addField(new SpacerFieldEditor(getFieldEditorParent()));
		addField(new LabelFieldEditor("Detached ImageJ Views:", getFieldEditorParent()));
		addField(new BooleanFieldEditor("ENABLE_DETACHED_VIEW_WINDOWS", "&Open detached views in a window", getFieldEditorParent()));
		addField(new IntegerFieldEditor("DETACHED_IMAGE_POSITION_X", "x", getFieldEditorParent()));
		addField(new IntegerFieldEditor("DETACHED_IMAGE_POSITION_Y", "y", getFieldEditorParent()));
		addField(new IntegerFieldEditor("DETACHED_IMAGE_WIDTH", "Width", getFieldEditorParent()));
		addField(new IntegerFieldEditor("DETACHED_IMAGE_HEIGHT", "Height", getFieldEditorParent()));
		addField(new IntegerFieldEditor("DETACHED_IMAGE_SPACING_X", "Spacing x-direction", getFieldEditorParent()));
		addField(new IntegerFieldEditor("DETACHED_IMAGE_SPACING_Y", "Spacing y-direction", getFieldEditorParent()));
		addField(new IntegerFieldEditor("DETACHED_IMAGE_SPACING_X_MAX", "Max x-direction", getFieldEditorParent()));
		addField(new IntegerFieldEditor("DETACHED_IMAGE_SPACING_Y_MAX", "Max y-direction", getFieldEditorParent()));
		

	}

	@Override
	public boolean performOk() {

		Display dis = Util.getDisplay();
		dis.asyncExec(new Runnable() {

			public void run() {
				Menus.updateImageJMenus();
			}
		});
		return super.performOk();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) {
	}

}