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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateCompletionProcessor;
import org.eclipse.jface.text.templates.TemplateContext;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.eclipse.jface.text.templates.TemplateException;
import org.eclipse.jface.text.templates.TemplateProposal;
import org.eclipse.jface.window.DefaultToolTip;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import com.eco.bio7.reditor.Bio7REditorPlugin;
import com.eco.bio7.reditor.antlr.Parse;
import com.eco.bio7.reditor.antlr.ref.RRefPhaseListen;
import com.eco.bio7.reditors.REditor;
import com.eco.bio7.reditors.TemplateEditorUI;
import com.eco.bio7.util.Util;

/**
 * A completion processor for R templates.
 */
public class RCompletionProcessor extends TemplateCompletionProcessor {

	private static final Comparator fgProposalComparator = new ProposalComparator();

	private static final String DEFAULT_IMAGE = "$nl$/icons/template_obj.png"; //$NON-NLS-1$

	private static final String CALCULATED_TEMPLATE_IMAGE = "$nl$/icons/methpub_obj.png"; //$NON-NLS-1$

	private static final String FIELD_IMAGE = "$nl$/icons/default_co.png"; //$NON-NLS-1$

	private static final String METHOD_IMAGE = "$nl$/icons/brkp_obj.png"; //$NON-NLS-1$

	// private boolean triggerNext;

	private int count = 0;// Variable to count the listed template.

	private int defaultTemplatesLength;// Global variable to get the current
										// template amount.

	public static String[] getStatistics() {
		return statistics;
	}

	public static void setStatistics(String[] statistics) {
		RCompletionProcessor.statistics = statistics;
	}

	public static String[] getStatisticsContext() {
		return statisticsContext;
	}

	public static void setStatisticsContext(String[] statisticsContext) {
		RCompletionProcessor.statisticsContext = statisticsContext;
	}

	public static String[] getStatisticsSet() {
		return statisticsSet;
	}

	public static void setStatisticsSet(String[] statisticsSet) {
		RCompletionProcessor.statisticsSet = statisticsSet;
	}

	private IPreferenceStore store;

	private DefaultToolTip tooltip;

	private REditor editor;

	private RRefPhaseListen ref;

	private String[] splitBuffScopedFun;

	private String[] splitVars;

	private boolean writeAllTemplateArguments = false;

	private static String[] statistics;
	private static String[] statisticsContext;
	private static String[] statisticsSet;

	public RCompletionProcessor(REditor rEditor, ContentAssistant assistant) {
		this.editor = rEditor;
		/*
		 * At startup load the default R proposals and add them to the
		 * templates!
		 */
		CalculateRProposals.loadRCodePackageTemplates();
		/* Load the created proposals! */
		statistics = CalculateRProposals.getStatistics();
		statisticsContext = CalculateRProposals.getStatisticsContext();
		statisticsSet = CalculateRProposals.getStatisticsSet();

		store = Bio7REditorPlugin.getDefault().getPreferenceStore();

	}

	public DefaultToolTip getTooltip() {
		return tooltip;
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
		/*
		 * if (prefix.startsWith("(")) prefix = prefix.substring(1);
		 */
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
		count = 0;

		ITextSelection selection = (ITextSelection) viewer.getSelectionProvider().getSelection();

		// Adjust offset to end of normalized selection
		if (selection.getOffset() == offset)
			offset = selection.getOffset() + selection.getLength();

		String prefix = extractPrefix(viewer, offset);

		int leng = prefix.length();
		Region region = null;

		RRefPhaseListen ref = new Parse(editor).parseFromOffset(offset);

		StringBuffer buffScopedFunctions = ref.getBuffScopeFunctions();
		splitBuffScopedFun = buffScopedFunctions.toString().split(",");

		StringBuffer buffScopedVars = ref.getBuffScopeVars();
		splitVars = buffScopedVars.toString().split(",");

		boolean isInVarCall = ref.isInVarCall();

		String proposalNameFound = ref.getProposalFuncFound();

		StringBuffer resultMethodCallVars = ref.getMethodCallVars();

		/*
		 * In parentheses we show an popup instead of the completion dialog! We
		 * return null to avoid the opening of the template dialog!
		 */

		int callTemplates = store.getInt("ACTIVATION_AMOUNT_CHAR_COMPLETION");

		if (isInVarCall) {
			if (leng <= 0) {
				if (proposalNameFound != null) {

					tooltipActionTemplates(viewer, offset, leng, proposalNameFound, buffScopedVars, buffScopedFunctions);
				} else {

					tooltipAction(viewer, offset, leng, ref, resultMethodCallVars, buffScopedVars, buffScopedFunctions);
				}
				/* Return null so that no information center is shown! */
				return null;
			} else if (leng >= callTemplates) {

				region = new Region(offset - prefix.length(), prefix.length());

			} else {
				return null;
			}
		} else {
			if (leng >= callTemplates) {
				region = new Region(offset - prefix.length(), prefix.length());
			} else {
				return null;
			}

		}
		TemplateContext context = createContext(viewer, region);
		if (context == null)
			return new ICompletionProposal[0];

		context.setVariable("selection", selection.getText()); // name 
																// of the
																// selection
																// variables
																// {line,
																// word}_selection

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

		/* Proposals from local defined variables! */

		if (splitVars.length > 0) {
			Template[] tempLocalVars = new Template[splitVars.length];

			for (int i = 0; i < tempLocalVars.length; i++) {
				tempLocalVars[i] = new Template(splitVars[i] + " (variable) ", splitVars[i], context.getContextType().getId(), splitVars[i], true);

				Template template = tempLocalVars[i];
				try {
					context.getContextType().validate(template.getPattern());
				} catch (TemplateException e) {
					continue;
				}
				if (template.matches(prefix, context.getContextType().getId()))
					matches.add(createProposal(template, context, (IRegion) region, getRelevance(template, prefix)));

			}
		}
		/* Proposals from local defined functions! */
		// if(triggerNext){
		if (splitBuffScopedFun.length > 0) {
			Template[] tempLocalFunctions = new Template[splitBuffScopedFun.length];

			for (int i = 0; i < tempLocalFunctions.length; i++) {
				tempLocalFunctions[i] = new Template(splitBuffScopedFun[i] + " (function) ", splitBuffScopedFun[i], context.getContextType().getId(), splitBuffScopedFun[i] + "(${cursor})", true);

				Template template = tempLocalFunctions[i];
				try {
					context.getContextType().validate(template.getPattern());
				} catch (TemplateException e) {
					continue;
				}
				if (template.matches(prefix, context.getContextType().getId()))
					matches.add(createProposal(template, context, (IRegion) region, getRelevance(template, prefix)));

			}

		}

		/* Proposals from List! */

		Template[] temp = new Template[statistics.length];

		for (int i = 0; i < temp.length; i++) {
			if (writeAllTemplateArguments) {
				temp[i] = new Template(statistics[i], statisticsContext[i], context.getContextType().getId(), statisticsSet[i], true);
			} else {
				temp[i] = new Template(statistics[i], statisticsContext[i], context.getContextType().getId(), statistics[i] + "(${cursor})", true);
			}
			Template template = temp[i];
			try {
				context.getContextType().validate(template.getPattern());
			} catch (TemplateException e) {
				continue;
			}
			if (template.matches(prefix, context.getContextType().getId()))
				matches.add(createProposal(template, context, (IRegion) region, getRelevance(template, prefix)));
		}

		Collections.sort(matches, fgProposalComparator);

		ICompletionProposal[] pro = (ICompletionProposal[]) matches.toArray(new ICompletionProposal[matches.size()]);

		// triggerNext = true;

		return pro;

	}

	/* Method to open a tooltip instead of the template suggestions! */
	private void tooltipAction(ITextViewer viewer, int offset, int leng, RRefPhaseListen ref, StringBuffer resultmethodCallVars, StringBuffer resultBuffScopeVars, StringBuffer buffScopedFunctions) {

		if (resultmethodCallVars != null && resultmethodCallVars.length() > 0) {

			/* Append the variables after the method call variables! */
			String[] scopedVars = resultBuffScopeVars.toString().split(",");
			String[] scopedFunctions = buffScopedFunctions.toString().split(",");

			String[] resultMethodCallVars = resultmethodCallVars.toString().split(",");

			creatPopupTable(viewer, offset, resultMethodCallVars, scopedVars, scopedFunctions);

		}
	}

	private void tooltipActionTemplates(ITextViewer viewer, int offset, int leng, String funcNameFromProposals, StringBuffer resultBuffScopeVars, StringBuffer buffScopedFunctions) {

		if (funcNameFromProposals != null) {

			for (int i = 0; i < statisticsSet.length; i++) {
				/* Do we have the method in the proposals? */
				if (funcNameFromProposals.equals(statistics[i])) {

					String calc = statisticsSet[i];

					/* Find the arguments in the template proposals! */
					int parOpen = calc.indexOf("(");
					int parClose = calc.lastIndexOf(")");

					/* Here we control the length. Must be greater -1! */
					if (parOpen + 1 + parClose >= 0) {
						calc = calc.substring(parOpen + 1, parClose);

						String[] proposalMethods = split(calc).toArray(new String[0]);
						String[] scopedVars = resultBuffScopeVars.toString().split(",");
						String[] scopedFunctions = buffScopedFunctions.toString().split(",");

						creatPopupTable(viewer, offset, proposalMethods, scopedVars, scopedFunctions);
					}

				}

			}
		}

	}

	private void creatPopupTable(ITextViewer viewer, int offset, String[] proposalMethod, String[] scopedVars, String[] scopedFunctions) {
		StyledText te = viewer.getTextWidget();
		Font f = te.getFont();

		int height = f.getFontData()[0].getHeight();

		StyledText sh = viewer.getTextWidget();

		Point poi = sh.getLocationAtOffset(offset);
		poi = sh.toDisplay(poi);
		int locx = poi.x;
		int locy = poi.y;
		Util.getDisplay().asyncExec(new Runnable() {
			public void run() {

				RPopupTable listPopup = new RPopupTable(viewer.getTextWidget().getShell());
				editor.setRPopupShell(listPopup.getShell());

				listPopup.setFont(f);
				listPopup.setItems(proposalMethod, "arguments");
				if (scopedVars.length == 1 && scopedVars[0].isEmpty()) {

				} else {
					listPopup.setItems(scopedVars, "variables");
				}

				if (scopedFunctions.length == 1 && scopedFunctions[0].isEmpty()) {

				} else {
					listPopup.setItems(scopedFunctions, "functions");
				}

				Rectangle rect = new Rectangle(locx, locy - height - 150, 300, 200);
				String selected = listPopup.open(rect);
				if (selected != null) {
					try {
						viewer.getDocument().replace(offset, 0, selected);
						editor.getSelectionProvider().setSelection(new TextSelection(offset + selected.length(), 0));
					} catch (BadLocationException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}
			}
		});
	}

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

	public char[] getCompletionProposalAutoActivationCharacters() {

		if (store.getBoolean("TYPED_CODE_COMPLETION")) {
			String ac = store.getString("ACTIVATION_CHARS");
			// return "abcdefghijklmnopqrstuvwxyz".toCharArray();
			if (ac == null || ac.isEmpty()) {

				return null;
			}
			return ac.toCharArray();
		}

		return null;

	}

	/* Extend prefixes for R functions with a dot, e.g. t.test() */
	protected String extractPrefix(ITextViewer viewer, int offset) {
		int i = offset;
		IDocument document = viewer.getDocument();
		if (i > document.getLength())
			return "";

		try {
			while (i > 0) {
				char ch = document.getChar(i - 1);
				if (!Character.isJavaIdentifierPart(ch) && (ch == '.') == false)
					break;
				i--;
			}

			return document.get(i, offset - i);
		} catch (BadLocationException e) {
			return "";
		}
	}

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
			ImageRegistry registry = TemplateEditorUI.getDefault().getImageRegistry();
			Image image = registry.get(DEFAULT_IMAGE);
			if (image == null) {
				ImageDescriptor desc = TemplateEditorUI.imageDescriptorFromPlugin("com.eco.bio7.redit", DEFAULT_IMAGE); //$NON-NLS-1$
				registry.put(DEFAULT_IMAGE, desc);
				image = registry.get(DEFAULT_IMAGE);
			}
			return image;

		} else if (count >= defaultTemplatesLength && count < defaultTemplatesLength + splitVars.length) {
			count++;
			ImageRegistry registry = TemplateEditorUI.getDefault().getImageRegistry();
			Image image = registry.get(METHOD_IMAGE);
			if (image == null) {
				ImageDescriptor desc = TemplateEditorUI.imageDescriptorFromPlugin("com.eco.bio7.redit", METHOD_IMAGE); //$NON-NLS-1$
				registry.put(METHOD_IMAGE, desc);
				image = registry.get(METHOD_IMAGE);
			}
			return image;
		} else if (count >= defaultTemplatesLength + splitVars.length && count < defaultTemplatesLength + splitBuffScopedFun.length + splitVars.length) {
			count++;
			ImageRegistry registry = TemplateEditorUI.getDefault().getImageRegistry();
			Image image = registry.get(FIELD_IMAGE);
			if (image == null) {
				ImageDescriptor desc = TemplateEditorUI.imageDescriptorFromPlugin("com.eco.bio7.redit", FIELD_IMAGE); //$NON-NLS-1$
				registry.put(FIELD_IMAGE, desc);
				image = registry.get(FIELD_IMAGE);
			}
			return image;
		}

		else {

			ImageRegistry registry = TemplateEditorUI.getDefault().getImageRegistry();
			Image image = registry.get(CALCULATED_TEMPLATE_IMAGE);
			if (image == null) {
				ImageDescriptor desc = TemplateEditorUI.imageDescriptorFromPlugin("com.eco.bio7.redit", CALCULATED_TEMPLATE_IMAGE); //$NON-NLS-1$
				registry.put(CALCULATED_TEMPLATE_IMAGE, desc);
				image = registry.get(CALCULATED_TEMPLATE_IMAGE);
			}
			return image;
		}

	}

	/*
	 * Answer and source from StackOverflow:
	 * http://stackoverflow.com/questions/34388828/java-splitting-a-comma-
	 * separated-string-but-ignoring-commas-in-parentheses/34389323#34389323
	 * Author: Tagir Valeev Profile:
	 * http://stackoverflow.com/users/4856258/tagir-valeev
	 * 
	 * Adaptions to ignore comma n quotes!
	 */
	public List<String> split(String input2) {
		int nParens = 0;
		int start = 0;
		/*
		 * Temporary replace comma in quotes else the argument "," will be
		 * splitted!
		 */
		String tempReplacement = "$$null$$";
		String input = input2.replace("\",\"", tempReplacement);
		List<String> result = new ArrayList<>();
		for (int i = 0; i < input.length(); i++) {
			switch (input.charAt(i)) {
			case ',':
				if (nParens == 0) {
					/* Replace the temporary comma in quote replacement! */
					result.add(input.substring(start, i).replace(tempReplacement, "\",\""));
					start = i + 1;
				}
				break;
			case '(':
				nParens++;
				break;
			case ')':
				nParens--;
				if (nParens < 0)
					throw new IllegalArgumentException("Unbalanced parenthesis at offset #" + i);
				break;
			}
		}
		if (nParens > 0)
			throw new IllegalArgumentException("Missing closing parenthesis");
		result.add(input.substring(start).replace(tempReplacement, "\",\""));
		return result;
	}

}
