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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateCompletionProcessor;
import org.eclipse.jface.text.templates.TemplateContext;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.eclipse.jface.text.templates.TemplateException;
import org.eclipse.jface.text.templates.TemplateProposal;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

import com.eco.bio7.reditors.TemplateEditorUI;

/**
 * A completion processor for XML templates.
 */
public class RCompletionProcessor extends TemplateCompletionProcessor {

	private static final Comparator fgProposalComparator = new ProposalComparator();

	private static final String DEFAULT_IMAGE = "$nl$/icons/template.gif"; //$NON-NLS-1$
	
	private static final String CALCULATED_TEMPLATE_IMAGE = "$nl$/icons/methpub_obj.gif"; //$NON-NLS-1$

	public static String[] statistics;

	public static String[] statisticsContext;

	public static String[] statisticsSet;

	private boolean triggerNext;
	
	private int count = 0;//Variable to count the listed template.
	
	private int defaultTemplatesLength;//Global variable to get the current template amount.



	public RCompletionProcessor(boolean startupTemplates) {

		loadRCodePackageTemplates(startupTemplates);

	}

	public static void loadRCodePackageTemplates(boolean startupTempl) {
		IPreferenceStore s = new ScopedPreferenceStore(InstanceScope.INSTANCE, "com.eco.bio7");

		ArrayList<String> data0 = new ArrayList<String>();
		ArrayList<String> data1 = new ArrayList<String>();
		ArrayList<String> data2 = new ArrayList<String>();
		String tempPath;
		if (startupTempl == false) {
			tempPath = s.getString("pathTempR") + "rproposals.txt";
		} else {
			tempPath = s.getString("pathTempR") + "rproposalsDefault.txt";
		}
		tempPath = tempPath.replace("\\", "/");
		// System.out.println(tempPath);
		BufferedReader br = null;
		try {
			File rPropFile = new File(tempPath);
			if (rPropFile.exists()) {

				br = new BufferedReader(new FileReader(rPropFile));
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String line;
		try {
			while ((line = br.readLine()) != null) {
				/* Split the string to get the seperated values! */
				String[] theline = line.split("####");
				// System.out.println(theline.length);

				try {
					data0.add(theline[0]);
					data1.add(theline[1]);
					data2.add(theline[2]);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();

				}

			}
			br.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (data0.size() == data1.size() && data0.size() == data2.size()) {
			statistics = data0.toArray(new String[data0.size()]);
			statisticsContext = data1.toArray(new String[data1.size()]);
			statisticsSet = data2.toArray(new String[data2.size()]);
		} else {
			statistics = new String[] { "Error in Template file!" };
			statisticsContext = new String[] { "Error in Template file!" };
			statisticsSet = new String[] { "Error in Template file!" };
		}
	}

	/**
	 * We watch for angular brackets since those are often part of XML
	 * templates.
	 * 
	 * @param viewer
	 *            the viewer
	 * @param offset
	 *            the offset left of which the prefix is detected
	 * @return the detected prefix
	 */
	protected String extractPrefix(ITextViewer viewer, int offset) {
		IDocument document = viewer.getDocument();
		int i = offset;
		if (i > document.getLength())
			return "";

		try {
			while (i > 0) {
				char ch = document.getChar(i - 1);
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
	 * @param template
	 *            the template
	 * @param prefix
	 *            the prefix
	 * @return the relevance of the <code>template</code> for the given
	 *         <code>prefix</code>
	 */
	protected int getRelevance(Template template, String prefix) {
		if (prefix.startsWith("<"))
			prefix = prefix.substring(1);
		if (template.getName().startsWith(prefix))
			return 90;
		return 0;
	}

	private static final class ProposalComparator implements Comparator<Object> {
		public int compare(Object o1, Object o2) {
			return ((TemplateProposal) o2).getRelevance() - ((TemplateProposal) o1).getRelevance();
		}
	}

	@SuppressWarnings("unchecked")
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
		count=0;

		ITextSelection selection = (ITextSelection) viewer.getSelectionProvider().getSelection();

		// adjust offset to end of normalized selection
		if (selection.getOffset() == offset)
			offset = selection.getOffset() + selection.getLength();

		String prefix = extractPrefix(viewer, offset);
		Region region = new Region(offset - prefix.length(), prefix.length());
		TemplateContext context = createContext(viewer, region);
		if (context == null)
			return new ICompletionProposal[0];

		context.setVariable("selection", selection.getText()); // name of the selection variables {line, word}_selection //$NON-NLS-1$

		Template[] templates = getTemplates(context.getContextType().getId());
		defaultTemplatesLength = templates.length;

		List<ICompletionProposal> matches = new ArrayList<ICompletionProposal>();
		for (int i = 0; i < templates.length; i++) {
			Template template = templates[i];
			try {
				context.getContextType().validate(template.getPattern());
			} catch (TemplateException e) {
				continue;
			}
			if (template.matches(prefix, context.getContextType().getId())) {
				matches.add(createProposal(template, context, (IRegion) region, getRelevance(template, prefix)));
			}

		}

		/* Proposals from List! */
		// if(triggerNext){
		Template[] temp = new Template[statistics.length];

		for (int i = 0; i < temp.length; i++) {
			temp[i] = new Template(statistics[i], statisticsContext[i], context.getContextType().getId(), statisticsSet[i], true);

		}
		for (int i = 0; i < temp.length; i++) {
			Template template = temp[i];
			try {
				context.getContextType().validate(template.getPattern());
			} catch (TemplateException e) {
				continue;
			}
			if (template.matches(prefix, context.getContextType().getId()))
				matches.add(createProposal(template, context, (IRegion) region, getRelevance(template, prefix)));

			// }

			// triggerNext=false;
		}

		Collections.sort(matches, fgProposalComparator);

		// ICompletionProposal com=new CompletionProposal();

		ICompletionProposal[] pro = (ICompletionProposal[]) matches.toArray(new ICompletionProposal[matches.size()]);

		triggerNext = true;

		return pro;

	}

	/*
	 * public static ICompletionProposal[] join(ICompletionProposal [] ...
	 * parms) { // calculate size of target array int size = 0; for
	 * (ICompletionProposal[] array : parms) { size += array.length; }
	 * 
	 * ICompletionProposal[] result = new ICompletionProposal[size];
	 * 
	 * int j = 0; for (ICompletionProposal[] array : parms) { for
	 * (ICompletionProposal s : array) { result[j++] = s; } } return result; }
	 */

	/**
	 * Simply return all templates.
	 * 
	 * @param contextTypeId
	 *            the context type, ignored in this implementation
	 * @return all templates
	 */
	protected Template[] getTemplates(String contextTypeId) {
		return TemplateEditorUI.getDefault().getTemplateStore().getTemplates();
	}

	// add the chars for Completion here !!!

	/*
	 * public char[] getCompletionProposalAutoActivationCharacters() {
	 * 
	 * return "abcdefghijklmnopqrstuvwxyz".toCharArray();
	 * 
	 * }
	 */

	/**
	 * Return the R context type that is supported by this plug-in.
	 * 
	 * @param viewer
	 *            the viewer, ignored in this implementation
	 * @param region
	 *            the region, ignored in this implementation
	 * @return the supported R context type
	 */
	protected TemplateContextType getContextType(ITextViewer viewer, IRegion region) {
		return TemplateEditorUI.getDefault().getContextTypeRegistry().getContextType(RContextType.XML_CONTEXT_TYPE);
	}

	/**
	 * Always return the default image.
	 * 
	 * @param template
	 *            the template, ignored in this implementation
	 * @return the default template image
	 */
	protected Image getImage(Template template) {

		if (count < defaultTemplatesLength) {
			count++;
			ImageRegistry registry = TemplateEditorUI.getDefault()
					.getImageRegistry();
			Image image = registry.get(DEFAULT_IMAGE);
			if (image == null) {
				ImageDescriptor desc = TemplateEditorUI
						.imageDescriptorFromPlugin(
								"com.eco.bio7.redit", DEFAULT_IMAGE); //$NON-NLS-1$
				registry.put(DEFAULT_IMAGE, desc);
				image = registry.get(DEFAULT_IMAGE);
			}
			return image;
		} else {

			ImageRegistry registry = TemplateEditorUI.getDefault()
					.getImageRegistry();
			Image image = registry.get(CALCULATED_TEMPLATE_IMAGE);
			if (image == null) {
				ImageDescriptor desc = TemplateEditorUI
						.imageDescriptorFromPlugin(
								"com.eco.bio7.redit", CALCULATED_TEMPLATE_IMAGE); //$NON-NLS-1$
				registry.put(CALCULATED_TEMPLATE_IMAGE, desc);
				image = registry.get(CALCULATED_TEMPLATE_IMAGE);
			}
			return image;
		}

	}

}
