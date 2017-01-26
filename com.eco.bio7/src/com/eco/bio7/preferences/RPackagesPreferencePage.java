package com.eco.bio7.preferences;


import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.dialogs.PreferencesUtil;

import com.eco.bio7.Bio7Plugin;

public class RPackagesPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	/**
	 * Create the preference page
	 */
	public RPackagesPreferencePage() {
		super(GRID);
		setPreferenceStore(Bio7Plugin.getDefault().getPreferenceStore());
		setDescription("R Build Tools");
	}

	/**
	 * Create contents of the preference page
	 */
	@Override
	protected void createFieldEditors() {
		
		addField(new LabelFieldEditor("Built Package Options:", getFieldEditorParent()));
		addField(new StringFieldEditor("rcmdcheck", "R CMD check", -1, StringFieldEditor.VALIDATE_ON_KEY_STROKE, getFieldEditorParent()));
		addField(new StringFieldEditor("rcmdinstall", "R CMD INSTALL", -1, StringFieldEditor.VALIDATE_ON_KEY_STROKE, getFieldEditorParent()));
		addField(new StringFieldEditor("rcmdbuild", "R CMD build", -1, StringFieldEditor.VALIDATE_ON_KEY_STROKE, getFieldEditorParent()));
		addField(new SpacerFieldEditor(getFieldEditorParent()));
		addField(new LabelFieldEditor("Knitr Options HTML:", getFieldEditorParent()));
		addField(new StringFieldEditor("knitroptions", "Knitr Options", -1, StringFieldEditor.VALIDATE_ON_KEY_STROKE, getFieldEditorParent()));
		addField(new SpacerFieldEditor(getFieldEditorParent()));
		final Link link = new Link(getFieldEditorParent(), SWT.NONE);
		link.setText("See <a href=\"com.eco.bio7.browser.preferences\">'Browser Preferences'</a> to select or configure the Browser.");
		link.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false, 3, 1));

		link.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				//create an instance of the custom MyPreference class
				PreferencesUtil.createPreferenceDialogOn(new Shell(Display.getDefault()),"com.eco.bio7.browser.preferences", null, null);

				
			}
		});
		
	}

	/**
	 * Initialize the preference page
	 */
	public void init(IWorkbench workbench) {
		// Initialize the preference page
	}

}
