/*******************************************************************************
 * Copyright (c) 2007-2025 M. Austenfeld
 * 
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * Contributors:
 * Marcel Austenfeld - initial API and implementation
 *******************************************************************************/


package com.eco.bio7.preferences;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.dialogs.PreferencesUtil;

import com.eco.bio7.Bio7Plugin;

public class RServePlotPrefs extends FieldEditorPreferencePage implements IWorkbenchPreferencePage, PropertyChangeListener {

	
	
	public MultiLineTextFieldEditor mult;
	private MultiLineTextFieldEditor mult_1;
	public StringFieldEditor deviceFilename;
	public RadioGroupFieldEditor selectionDevice;
	public RadioGroupFieldEditor selectLinuxShell;
	private RadioGroupFieldEditor selectPDFReader;
	private static RServePlotPrefs instance;

	public static RServePlotPrefs getInstance() {
		return instance;
	}
	
	public Composite getFieldEditorParentControl(){
		return getFieldEditorParent();
	}

	public RServePlotPrefs() {
		super(GRID);
		setPreferenceStore(Bio7Plugin.getDefault().getPreferenceStore());
        instance=this;
        
	}

	public void createFieldEditors() {
		
		addField(new SpacerFieldEditor(getFieldEditorParent()));
		addField(new LabelFieldEditor("Plot:", getFieldEditorParent()));
		addField(new BooleanFieldEditor("USE_CUSTOM_DEVICE", "Use Custom Device", BooleanFieldEditor.DEFAULT, getFieldEditorParent()));
		selectionDevice=new RadioGroupFieldEditor("PLOT_DEVICE_SELECTION", "Select Device:\nPlease use action \"Apply \" to see changes and after using action \"Restore Defaults\".\nValues in the Device Definition can be changed and will be stored!", 3, new String[][] { { "Image Default", "PLOT_IMAGE" },{ "Image Cairo", "PLOT_CAIRO" },{ "Image Print", "PLOT_PRINT" }, { "PDF", "PLOT_PDF" },{ "SVG", "PLOT_SVG" },{ "PostScript", "PLOT_POSTSCRIPT" },{ "ImageJ View Display Size", "PLOT_IMAGEJ_DISPLAYSIZE" },{ "ImageJ View Display Size Cairo", "PLOT_IMAGEJ_DISPLAYSIZE_CAIRO" },{ "ImageJ Image", "PLOT_IMAGEJ_IMAGESIZE" },{"ImageJ Image Cairo", "PLOT_IMAGEJ_IMAGESIZE_CAIRO"}   }, getFieldEditorParent(), false);
		addField(selectionDevice);
				
		//addField(new StringFieldEditor("DEVICE_DEFINITION", "Device Definiton", -1, StringFieldEditor.VALIDATE_ON_KEY_STROKE, getFieldEditorParent()));
		mult=new MultiLineTextFieldEditor("DEVICE_DEFINITION", "Device Definiton", -1, StringFieldEditor.VALIDATE_ON_KEY_STROKE, getFieldEditorParent());
		addField(mult);
		deviceFilename=new StringFieldEditor("DEVICE_FILENAME", "Filename", -1, StringFieldEditor.VALIDATE_ON_KEY_STROKE, getFieldEditorParent());
		addField(deviceFilename);
		deviceFilename.setEnabled(false, getFieldEditorParent());
		selectPDFReader=new RadioGroupFieldEditor("PDF_READER", "PDF Reader (Linux):", 5, new String[][] { { "Acrobat", "ACROBAT" },{ "Evince", "EVINCE" },{ "Kpdf", "KPDF" },{ "Xpdf", "XPDF" },{ "Okular", "OKULAR" } }, getFieldEditorParent(), false);
		addField(selectPDFReader);
		SpacerFieldEditor spacer1 = new SpacerFieldEditor(getFieldEditorParent());
		addField(spacer1);
		final Link link = new Link(getFieldEditorParent(), SWT.NONE);
		link.setText("See <a href=\"com.eco.bio7.browser.preferences\">'Browser Preferences'</a> to select or configure the browser for the *.pdf files.");
		link.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false, 3, 1));

		link.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				//create an instance of the custom MyPreference class
				PreferencesUtil.createPreferenceDialogOn(new Shell(Display.getDefault()),"com.eco.bio7.browser.preferences", null, null);

				
			}
		});
		addField(new SpacerFieldEditor(getFieldEditorParent()));
		addField(new LabelFieldEditor("Special image plot options:", getFieldEditorParent()));
		addField(new BooleanFieldEditor("IMPORT_R_PLOT_VIRTUAL", "Show image plots as ImageJ virtual (disk resident) stack", BooleanFieldEditor.DEFAULT, getFieldEditorParent()));
		addField(new BooleanFieldEditor("IMAGEJ_CREATE_SINGLE_PLOTS", "Open plots in individual tab", BooleanFieldEditor.DEFAULT, getFieldEditorParent()));
		addField(new BooleanFieldEditor("IJMACRO_EXECUTE_AFTER_PLOT_ENABLE", "Execute ImageJ macro after creation", BooleanFieldEditor.DEFAULT, getFieldEditorParent()));
		mult_1=new MultiLineTextFieldEditor("IJMACRO_EXECUTE_AFTER_PLOT", "Execute ImageJ macro after plot creation", -1, StringFieldEditor.VALIDATE_ON_KEY_STROKE, getFieldEditorParent());
		mult_1.setLabelText("ImageJ Macro");
		addField(mult_1);
		
		
		
	}

	public void init(IWorkbench workbench) {

	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		
		

	}


}