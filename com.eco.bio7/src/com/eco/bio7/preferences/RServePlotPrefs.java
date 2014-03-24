/*******************************************************************************
 * Copyright (c) 2007-2012 M. Austenfeld
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/


package com.eco.bio7.preferences;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import com.eco.bio7.Bio7Plugin;

public class RServePlotPrefs extends FieldEditorPreferencePage implements IWorkbenchPreferencePage, PropertyChangeListener {

	
	
	public MultiLineTextFieldEditor mult;
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
		selectionDevice=new RadioGroupFieldEditor("PLOT_DEVICE_SELECTION", "Select Device:\nPlease use action \"Apply \" to see changes and after using action \"Restore Defaults\".\nValues in the Device Definition can be changed and will be stored!", 3, new String[][] { { "Image Default", "PLOT_IMAGE" },{ "Image Cairo", "PLOT_CAIRO" },{ "Image Print", "PLOT_PRINT" }, { "PDF", "PLOT_PDF" },{ "SVG", "PLOT_SVG" },{ "PostScript", "PLOT_POSTSCRIPT" }   }, getFieldEditorParent(), false);
		addField(selectionDevice);
		
		//addField(new StringFieldEditor("DEVICE_DEFINITION", "Device Definiton", -1, StringFieldEditor.VALIDATE_ON_KEY_STROKE, getFieldEditorParent()));
		mult=new MultiLineTextFieldEditor("DEVICE_DEFINITION", "Device Definiton", -1, StringFieldEditor.VALIDATE_ON_KEY_STROKE, getFieldEditorParent());
		addField(mult);
		deviceFilename=new StringFieldEditor("DEVICE_FILENAME", "Filename", -1, StringFieldEditor.VALIDATE_ON_KEY_STROKE, getFieldEditorParent());
		addField(deviceFilename);
		deviceFilename.setEnabled(false, getFieldEditorParent());
		selectPDFReader=new RadioGroupFieldEditor("PDF_READER", "PDF Reader (Linux):", 4, new String[][] { { "Acrobat", "ACROBAT" },{ "Evince", "EVINCE" },{ "Kpdf", "KPDF" },{ "Xpdf", "XPDF" } }, getFieldEditorParent(), false);
		addField(selectPDFReader);
		
		
	}

	public void init(IWorkbench workbench) {

	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		
		

	}


}