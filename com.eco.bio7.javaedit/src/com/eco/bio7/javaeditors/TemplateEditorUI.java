/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.eco.bio7.javaeditors;


import java.io.IOException;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;

import org.eclipse.jface.text.templates.ContextTypeRegistry;
import org.eclipse.jface.text.templates.persistence.TemplateStore;

import org.eclipse.ui.editors.text.templates.ContributionTemplateStore;
import org.eclipse.ui.editors.text.templates.ContributionContextTypeRegistry;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import com.eco.bio7.javaeditor.Bio7EditorPlugin;
import com.eco.bio7.javapreferences.template.JavaContextType;

/**
 * The main plugin class to be used in the desktop.
 */
public class TemplateEditorUI  {
	/** Key to store custom templates. */
	private static final String CUSTOM_TEMPLATES_KEY= "org.eclipse.ui.examples.templateeditor.customtemplates"; //$NON-NLS-1$
	
	/** The shared instance. */
	private static TemplateEditorUI fInstance;
	
	/** The template store. */
	private TemplateStore fStore;
	/** The context type registry. */
	private ContributionContextTypeRegistry fRegistry;
	
	private TemplateEditorUI() {
	}

	/**
	 * Returns the shared instance.
	 * 
	 * @return the shared instance
	 */
	public static TemplateEditorUI getDefault() {
		if (fInstance == null)
			fInstance= new TemplateEditorUI();
		return fInstance;
	}

	/**
	 * Returns this plug-in's template store.
	 * 
	 * @return the template store of this plug-in instance
	 */
	public TemplateStore getTemplateStore() {
		if (fStore == null) {
			fStore= new ContributionTemplateStore(getContextTypeRegistry(), Bio7EditorPlugin.getDefault().getPreferenceStore(), CUSTOM_TEMPLATES_KEY);
			try {
				fStore.load();
			} catch (IOException e) {
				Bio7EditorPlugin.getDefault().getLog().log(new Status(IStatus.ERROR, "org.eclipse.ui.examples.javaeditor", IStatus.OK, "", e)); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		return fStore;
	}

	/**
	 * Returns this plug-in's context type registry.
	 * 
	 * @return the context type registry for this plug-in instance
	 */
	public ContextTypeRegistry getContextTypeRegistry() {
		if (fRegistry == null) {
			// create an configure the contexts available in the template editor
			fRegistry= new ContributionContextTypeRegistry();
			fRegistry.addContextType(JavaContextType.XML_CONTEXT_TYPE);
		}
		return fRegistry;
	}

	/* Forward plug-in methods to javaeditor example plugin default instance */
	public ImageRegistry getImageRegistry() {
		return Bio7EditorPlugin.getDefault().getImageRegistry();
	}

	public static ImageDescriptor imageDescriptorFromPlugin(String string, String default_image) {
		return AbstractUIPlugin.imageDescriptorFromPlugin(string, default_image);
	}

	public IPreferenceStore getPreferenceStore() {
		return Bio7EditorPlugin.getDefault().getPreferenceStore();
	}

	public void savePluginPreferences() {
		Bio7EditorPlugin.getDefault().savePluginPreferences();
	}

}
