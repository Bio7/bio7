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

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateCompletionProcessor;
import org.eclipse.jface.text.templates.TemplateContext;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.eclipse.jface.text.templates.TemplateException;
import org.eclipse.jface.text.templates.TemplateProposal;
import org.eclipse.jface.window.DefaultToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.PopupList;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;
import org.rosuda.REngine.Rserve.RConnection;
import com.eco.bio7.reditor.Bio7REditorPlugin;
import com.eco.bio7.reditor.antlr.Parse;
import com.eco.bio7.reditor.antlr.RBaseListen;
import com.eco.bio7.reditor.antlr.RErrorStrategy;
import com.eco.bio7.reditor.antlr.RFilter;
import com.eco.bio7.reditor.antlr.RLexer;
import com.eco.bio7.reditor.antlr.RParser;
import com.eco.bio7.reditor.antlr.UnderlineListener;
import com.eco.bio7.reditor.antlr.ref.RRefPhaseListen;
import com.eco.bio7.reditors.REditor;
import com.eco.bio7.reditors.TemplateEditorUI;

import jdk.nashorn.tools.Shell;

/**
 * A completion processor for R templates.
 */
public class RCompletionProcessor extends TemplateCompletionProcessor {

	private static final Comparator fgProposalComparator = new ProposalComparator();

	private static final String DEFAULT_IMAGE = "$nl$/icons/template_obj.png"; //$NON-NLS-1$

	private static final String CALCULATED_TEMPLATE_IMAGE = "$nl$/icons/methpub_obj.png"; //$NON-NLS-1$

	private static final String FIELD_IMAGE = "$nl$/icons/default_co.png"; //$NON-NLS-1$

	private static final String METHOD_IMAGE = "$nl$/icons/brkp_obj.png"; //$NON-NLS-1$

	private boolean triggerNext;

	private int count = 0;// Variable to count the listed template.

	private int defaultTemplatesLength;// Global variable to get the current
										// template amount.

	private IPreferenceStore store;

	private DefaultToolTip tooltip;

	private REditor editor;

	private RRefPhaseListen ref;

	private String[] splitBuffScopedFun;

	private String[] splitVars;

	public RCompletionProcessor(REditor rEditor) {
		this.editor = rEditor;
		/*
		 * At startup load the default R proposals and add them to the
		 * templates!
		 */
		CalculateRProposals.loadRCodePackageTemplates();

		store = Bio7REditorPlugin.getDefault().getPreferenceStore();

	}

	public DefaultToolTip getTooltip() {
		return tooltip;
	}

	/**
	 * We watch for brackets since those are often part of a R function
	 * templates.
	 * 
	 * @param viewer
	 *            the viewer
	 * @param offset
	 *            the offset left of which the prefix is detected
	 * @return the detected prefix
	 */
	/*
	 * protected String extractPrefix(ITextViewer viewer, int offset) {
	 * IDocument document = viewer.getDocument(); int i = offset; if (i >
	 * document.getLength()) return "";
	 * 
	 * try { int countBrace = 0; while (i > 0) { char ch = document.getChar(i -
	 * 1);
	 * 
	 * We add the detection of functions and function calls with '.'!
	 * 
	 * Detect nested braces! if (ch == '(') { countBrace++; if (countBrace == 2)
	 * { break; } } if (ch != '(' && ch != '.' &&
	 * !Character.isJavaIdentifierPart(ch)) break; i--; } return document.get(i,
	 * offset - i); } catch (BadLocationException e) { return ""; } }
	 */
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

		// adjust offset to end of normalized selection
		if (selection.getOffset() == offset)
			offset = selection.getOffset() + selection.getLength();

		String prefix = extractPrefix(viewer, offset);

		int leng = prefix.length();
		Region region;

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

		if (isInVarCall) {

			if (proposalNameFound != null) {

				tooltipActionTemplates(viewer, offset, leng, ref, proposalNameFound, buffScopedVars, buffScopedFunctions);
			} else {

				tooltipAction(viewer, offset, leng, ref, resultMethodCallVars, buffScopedVars, buffScopedFunctions);
			}

			/* Return null so that no information center is shown! */
			return null;
		} else {

			region = new Region(offset - prefix.length(), prefix.length());
		}
		TemplateContext context = createContext(viewer, region);
		if (context == null)
			return new ICompletionProposal[0];

		context.setVariable("selection", selection.getText()); // name //$NON-NLS-1$
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
				tempLocalFunctions[i] = new Template(splitBuffScopedFun[i] + " (function) ", splitBuffScopedFun[i], context.getContextType().getId(), splitBuffScopedFun[i] + "()", true);

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

		Template[] temp = new Template[CalculateRProposals.statistics.length];

		for (int i = 0; i < temp.length; i++) {
			temp[i] = new Template(CalculateRProposals.statistics[i], CalculateRProposals.statisticsContext[i], context.getContextType().getId(), CalculateRProposals.statisticsSet[i], true);
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

		triggerNext = true;

		return pro;

	}

	/* Method to open a tooltip instead of the template suggestions! */
	private void tooltipAction(ITextViewer viewer, int offset, int leng, RRefPhaseListen ref, StringBuffer resultmethodCallVars, StringBuffer resultBuffScopeVars, StringBuffer buffScopedFunctions) {
		// System.out.println(funcNameFromProposals);

		if (resultmethodCallVars != null && resultmethodCallVars.length() > 0) {
			resultmethodCallVars.append(",");

			/* Append the variables after the method call variables! */
			String[] scopedVars = resultBuffScopeVars.toString().split(",");
			String[] scopedFunctions = buffScopedFunctions.toString().split(",");

			String[] resultMethodCallVars = resultmethodCallVars.toString().split(",");

			String[] funcArgAndScopedVars = ArrayUtils.addAll(ArrayUtils.addAll(resultMethodCallVars, scopedVars), scopedFunctions);
			creatPopupList(viewer, offset, funcArgAndScopedVars);

		}
	}

	private void tooltipActionTemplates(ITextViewer viewer, int offset, int leng, RRefPhaseListen ref, String funcNameFromProposals, StringBuffer resultBuffScopeVars, StringBuffer buffScopedFunctions) {

		/* trim the method name! */
		/*
		 * prefix = prefix.substring(0, leng - 1); prefix = prefix.trim();
		 */
		if (funcNameFromProposals != null) {
			// System.out.println(funcNameFromProposals);
			for (int i = 0; i < CalculateRProposals.statisticsSet.length; i++) {
				/* Do we have the method in the proposals? */
				if (funcNameFromProposals.equals(CalculateRProposals.statistics[i])) {

					String calc = CalculateRProposals.statisticsSet[i];
					/* Find the arguments in the template proposals! */
					int parOpen = calc.indexOf("(");
					int parClose = calc.indexOf(")");
					calc = calc.substring(parOpen + 1, parClose);
					calc = calc.replace(",", " = ,");
					calc = calc.concat(" = ");
					String[] proposalMethods = calc.split(",");

					String[] scopedVars = resultBuffScopeVars.toString().split(",");
					String[] scopedFunctions = buffScopedFunctions.toString().split(",");

					String[] funcNameFromProposalsFinal = ArrayUtils.addAll(ArrayUtils.addAll(proposalMethods, scopedVars), scopedFunctions);
					creatPopupList(viewer, offset, funcNameFromProposalsFinal);

				}

			}
		}

	}

	private void creatPopupList(ITextViewer viewer, int offset, String[] splitMethod) {
		StyledText te = viewer.getTextWidget();
		Font f = te.getFont();

		int height = f.getFontData()[0].getHeight();

		StyledText sh = viewer.getTextWidget();

		Point poi = sh.getLocationAtOffset(offset);
		poi = sh.toDisplay(poi);
		int locx = poi.x;
		int locy = poi.y;
		PopupList listPopup = new PopupList(viewer.getTextWidget().getShell());

		// String[] OPTIONS = {
		// CalculateRProposals.statisticsSet[i], "B", "C"};
		listPopup.setFont(f);
		listPopup.setItems(splitMethod);

		Rectangle rect = new Rectangle(locx, locy - height - 150, 300, 200);
		String selected = listPopup.open(rect);
		try {
			viewer.getDocument().replace(offset, 0, selected);
		} catch (BadLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
	/*Extend prefixes for R functions with a dot, e.g. t.test()*/
	protected String extractPrefix(ITextViewer viewer, int offset) {
		int i= offset;
		IDocument document= viewer.getDocument();
		if (i > document.getLength())
			return ""; //$NON-NLS-1$

		try {
			while (i > 0) {
				char ch= document.getChar(i - 1);
				if (!Character.isJavaIdentifierPart(ch)&&(ch=='.')==false)
					break;
				i--;
			}

			return document.get(i, offset - i);
		} catch (BadLocationException e) {
			return ""; //$NON-NLS-1$
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

}
