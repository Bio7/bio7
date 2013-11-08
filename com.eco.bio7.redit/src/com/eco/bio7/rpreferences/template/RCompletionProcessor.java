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
package com.eco.bio7.rpreferences.template;

import org.eclipse.swt.graphics.Image;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateCompletionProcessor;
import com.eco.bio7.reditors.TemplateEditorUI;


/**
 * A completion processor for XML templates.
 */
public class RCompletionProcessor extends TemplateCompletionProcessor {
	private static final String DEFAULT_IMAGE= "$nl$/icons/template.gif"; //$NON-NLS-1$

	/**
	 * We watch for angular brackets since those are often part of XML
	 * templates.
	 * 
	 * @param viewer the viewer
	 * @param offset the offset left of which the prefix is detected
	 * @return the detected prefix
	 */
	protected String extractPrefix(ITextViewer viewer, int offset) {
		IDocument document= viewer.getDocument();
		int i= offset;
		if (i > document.getLength())
			return ""; 
		
		try {
			while (i > 0) {
				char ch= document.getChar(i - 1);
				if (ch != '<' && !Character.isJavaIdentifierPart(ch))
					break;
				i--;
			}
			return document.get(i, offset - i);
		} catch (BadLocationException e) {
			return ""; 
		}
	}

	/**
	 * Cut out angular brackets for relevance sorting, since the template name
	 * does not contain the brackets.
	 * 
	 * @param template the template
	 * @param prefix the prefix
	 * @return the relevance of the <code>template</code> for the given <code>prefix</code>
	 */
	protected int getRelevance(Template template, String prefix) {
		if (prefix.startsWith("<")) 
			prefix= prefix.substring(1);
		if (template.getName().startsWith(prefix))
			return 90;
		return 0;
	}

	/**
	 * Simply return all templates.
	 * 
	 * @param contextTypeId the context type, ignored in this implementation
	 * @return all templates
	 */
	protected Template[] getTemplates(String contextTypeId) {
		return TemplateEditorUI.getDefault().getTemplateStore().getTemplates();
	}
	// add the chars for Completion here !!!
	/*public char[] getCompletionProposalAutoActivationCharacters() {
		return new char[] { 'f','g' };
	}*/

	/**
	 * Return the XML context type that is supported by this plug-in.
	 * 
	 * @param viewer the viewer, ignored in this implementation
	 * @param region the region, ignored in this implementation
	 * @return the supported XML context type
	 */
	protected TemplateContextType getContextType(ITextViewer viewer, IRegion region) {
		return TemplateEditorUI.getDefault().getContextTypeRegistry().getContextType(RContextType.XML_CONTEXT_TYPE);
	}

	/**
	 * Always return the default image.
	 * 
	 * @param template the template, ignored in this implementation
	 * @return the default template image
	 */
	protected Image getImage(Template template) {
		ImageRegistry registry= TemplateEditorUI.getDefault().getImageRegistry();
		Image image= registry.get(DEFAULT_IMAGE);
		if (image == null) {
			ImageDescriptor desc= TemplateEditorUI.imageDescriptorFromPlugin("com.eco.bio7.redit", DEFAULT_IMAGE); //$NON-NLS-1$
			registry.put(DEFAULT_IMAGE, desc);
			image= registry.get(DEFAULT_IMAGE);
		}
		return image;
	}

}
