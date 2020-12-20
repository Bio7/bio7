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
import org.eclipse.jface.preference.IntegerFieldEditor;

public class RPrefDebug extends FieldEditorPreferencePage implements IWorkbenchPreferencePage, PropertyChangeListener {

	
	
	
	private static RPrefDebug instance;

	public static RPrefDebug getInstance() {
		return instance;
	}
	
	public Composite getFieldEditorParentControl(){
		return getFieldEditorParent();
	}

	public RPrefDebug() {
		super(GRID);
		setPreferenceStore(Bio7Plugin.getDefault().getPreferenceStore());
        instance=this;
        
	}

	public void createFieldEditors() {
		
		addField(new SpacerFieldEditor(getFieldEditorParent()));
		addField(new LabelFieldEditor("Debug:", getFieldEditorParent()));		
		
		addField(new IntegerFieldEditor("R_DEBUG_PORT", "Port", getFieldEditorParent()));
	}

	public void init(IWorkbench workbench) {

	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		
		

	}


}