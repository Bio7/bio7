package com.eco.bio7.reditor.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.dialogs.PreferencesUtil;

import com.eco.bio7.reditor.Bio7REditorPlugin;

public class OpenTemplatePreferences extends Action {

	public OpenTemplatePreferences() {
		super("Preferences Template");
		setId("com.eco.bio7.r_editor_open_template_preferences");
		ImageDescriptor desc = Bio7REditorPlugin.getImageDescriptor("/icons/template_obj.png");
		this.setImageDescriptor(desc);

		setText("Preferences Template");

	}

	public void run() {
		PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(null, "rpreferencepage", null, null);
		dialog.open();

	}

}