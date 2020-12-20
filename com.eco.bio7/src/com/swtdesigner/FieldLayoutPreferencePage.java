package com.swtdesigner;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * A special abstract preference page to host field editors and eny other controls
 * with flexible layout.
 * <p>
 * Subclasses must implement the <code>createPageContents</code>.
 * </p>
 * 
 * This class may be freely distributed as part of any application or plugin.
 * <p>
 * Copyright (c) 2003, Instantiations, Inc. <br>All Rights Reserved
 * </p>
 * 
 * @version $Revision$
 * @author scheglov_ke
 */
public abstract class FieldLayoutPreferencePage extends PreferencePage implements IPropertyChangeListener {
	/**
	 * The field editors.
	 */
	private List fields = new ArrayList();
	/** 
	 * The first invalid field editor, or <code>null</code>
	 * if all field editors are valid.
	 */
	private FieldEditor invalidFieldEditor = null;
	/**
	 * Creates a new field editor preference page with an empty title, and no image.
	 */
	protected FieldLayoutPreferencePage() {
		// Create a new field editor preference page with an empty title, and no image
	}
	/**
	 * Creates a new field editor preference page with the given title, but no image.
	 *
	 * @param title the title of this preference page
	 */
	protected FieldLayoutPreferencePage(String title) {
		super(title);
	}
	/**
	 * Creates a new field editor preference page with the given image, and style.
	 *
	 * @param title the title of this preference page
	 * @param image the image for this preference page, or <code>null</code> if none
	 */
	protected FieldLayoutPreferencePage(String title, ImageDescriptor image) {
		super(title, image);
	}
	/**
	 * Adds the given field editor to this page.
	 *
	 * @param editor the field editor
	 */
	@SuppressWarnings("unchecked") //$NON-NLS-1$
	protected void addField(FieldEditor editor) {
		if (this.fields == null)
			this.fields = new ArrayList();
		this.fields.add(editor);
	}
	/**
	 * Recomputes the page's error state by calling <code>isValid</code> for
	 * every field editor.
	 */
	protected void checkState() {
		boolean valid = true;
		this.invalidFieldEditor = null;
		// The state can only be set to true if all
		// field editors contain a valid value. So we must check them all
		if (this.fields != null) {
			int size = this.fields.size();
			for (int i = 0; i < size; i++) {
				FieldEditor editor = (FieldEditor) this.fields.get(i);
				valid = valid && editor.isValid();
				if (!valid) {
					this.invalidFieldEditor = editor;
					break;
				}
			}
		}
		setValid(valid);
	}
	/* (non-Javadoc)
	 * Method declared on PreferencePage.
	 */
	@Override
	protected Control createContents(Composite parent) {
		Control contens = createPageContents(parent);
		initialize();
		checkState();
		return contens;
	}
	/**
	 * Creates and returns the SWT control for the customized body 
	 * of this preference page under the given parent composite.
	 * <p>
	 * This framework method must be implemented by concrete subclasses. Any
	 * subclass returning a <code>Composite</code> object whose <code>Layout</code>
	 * has default margins (for example, a <code>GridLayout</code>) are expected to
	 * set the margins of this <code>Layout</code> to 0 pixels. 
	 * </p>
	 *
	 * @param parent the parent composite
	 * @return the new control
	 */
	protected abstract Control createPageContents(Composite parent);
	/**	
	 * The field editor preference page implementation of an <code>IDialogPage</code>
	 * method disposes of this page's controls and images.
	 * Subclasses may override to release their own allocated SWT
	 * resources, but must call <code>super.dispose</code>.
	 */
	@Override
	public void dispose() {
		super.dispose();
		if (this.fields != null) {
			Iterator e = this.fields.iterator();
			while (e.hasNext()) {
				FieldEditor pe = (FieldEditor) e.next();
				/* $codepro.preprocessor.if version >= 3.1 $ */
				pe.setPage(null);
				/* $codepro.preprocessor.elseif version < 3.0 $
				pe.setPreferencePage(null);
				$codepro.preprocessor.endif $ */
				pe.setPropertyChangeListener(null);
				pe.setPreferenceStore(null);
			}
		}
	}
	/**
	 * Initializes all field editors.
	 */
	protected void initialize() {
		if (this.fields != null) {
			Iterator e = this.fields.iterator();
			while (e.hasNext()) {
				FieldEditor pe = (FieldEditor) e.next();
				/* $codepro.preprocessor.if version >= 3.1 $ */
				pe.setPage(null);
				/* $codepro.preprocessor.elseif version < 3.0 $
				pe.setPreferencePage(null);
				$codepro.preprocessor.endif $ */
				pe.setPropertyChangeListener(this);
				pe.setPreferenceStore(getPreferenceStore());
				pe.load();
			}
		}
	}
	/**	
	 * The field editor preference page implementation of a <code>PreferencePage</code>
	 * method loads all the field editors with their default values.
	 */
	@Override
	protected void performDefaults() {
		if (this.fields != null) {
			Iterator e = this.fields.iterator();
			while (e.hasNext()) {
				FieldEditor pe = (FieldEditor) e.next();
				pe.loadDefault();
			}
		}
		// Force a recalculation of my error state.
		checkState();
		super.performDefaults();
	}
	/** 
	 * The field editor preference page implementation of this 
	 * <code>PreferencePage</code> method saves all field editors by
	 * calling <code>FieldEditor.store</code>. Note that this method
	 * does not save the preference store itself; it just stores the
	 * values back into the preference store.
	 *
	 * @see FieldEditor#store()
	 */
	@Override
	public boolean performOk() {
		if (this.fields != null) {
			Iterator e = this.fields.iterator();
			while (e.hasNext()) {
				FieldEditor pe = (FieldEditor) e.next();
				pe.store();
			}
		}
		return true;
	}
	/**
	 * The field editor preference page implementation of this <code>IPreferencePage</code>
	 * (and <code>IPropertyChangeListener</code>) method intercepts <code>IS_VALID</code> 
	 * events but passes other events on to its superclass.
	 */
	public void propertyChange(PropertyChangeEvent event) {
		if (event.getProperty().equals(FieldEditor.IS_VALID)) {
			boolean newValue = ((Boolean) event.getNewValue()).booleanValue();
			// If the new value is true then we must check all field editors.
			// If it is false, then the page is invalid in any case.
			if (newValue) {
				checkState();
			} else {
				this.invalidFieldEditor = (FieldEditor) event.getSource();
				setValid(newValue);
			}
		}
	}
	/* (non-Javadoc)
	 * Method declared on IDialog.
	 */
	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible && this.invalidFieldEditor != null) {
			this.invalidFieldEditor.setFocus();
		}
	}
}