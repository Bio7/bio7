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

	private static final String DEFAULT_IMAGE = "$nl$/icons/template.gif"; //$NON-NLS-1$

	private static final String CALCULATED_TEMPLATE_IMAGE = "$nl$/icons/methpub_obj.gif"; //$NON-NLS-1$

	private boolean triggerNext;

	private int count = 0;// Variable to count the listed template.

	private int defaultTemplatesLength;// Global variable to get the current
										// template amount.

	private IPreferenceStore store;

	private DefaultToolTip tooltip;

	private REditor editor;

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
	protected String extractPrefix(ITextViewer viewer, int offset) {
		IDocument document = viewer.getDocument();
		int i = offset;
		if (i > document.getLength())
			return "";

		try {
			int countBrace = 0;
			while (i > 0) {
				char ch = document.getChar(i - 1);
				/*
				 * We add the detection of functions and function calls with
				 * '.'!
				 */
				/* Detect nested braces! */
				if (ch == '(') {
					countBrace++;
					if (countBrace == 2) {
						break;
					}
				}
				if (ch != '(' && ch != '.' && !Character.isJavaIdentifierPart(ch))
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
		/*
		 * In parentheses we show an popup instead of the completion dialog! We
		 * return null to avoid the opening of the template dialog!
		 */
		
		if (prefix.endsWith("(")) {
			prefix = tooltipAction(viewer, offset, prefix, leng);

			/* Return null so that no information center is shown! */
			return null;
		} else {
			// System.out.println("Prefix is: "+ prefix);
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

		/* Proposals from List! */
		// if(triggerNext){
		Template[] temp = new Template[CalculateRProposals.statistics.length];

		for (int i = 0; i < temp.length; i++) {
			temp[i] = new Template(CalculateRProposals.statistics[i], CalculateRProposals.statisticsContext[i], context.getContextType().getId(), CalculateRProposals.statisticsSet[i], true);

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

	/* Method to open a tooltip instead of the template suggestions! */
	private String tooltipAction(ITextViewer viewer, int offset, String prefix, int leng) {

		IDocument doc = ((ITextEditor) editor).getDocumentProvider().getDocument(editor.getEditorInput());

		ANTLRInputStream input = new ANTLRInputStream(doc.get());
		RLexer lexer = new RLexer(input);

		CommonTokenStream tokens = new CommonTokenStream(lexer);

		UnderlineListener li = new UnderlineListener(editor);
		RFilter filter = new RFilter(tokens);
		/*
		 * We have to remove the filter, too! Else we get error messages on the
		 * console!
		 */
		filter.removeErrorListeners();
		// filter.addErrorListener(li);

		filter.stream(); // call start rule: stream
		tokens.reset();

		RParser parser = new RParser(tokens);
		parser.removeErrorListeners();
		/*
		 * Add some modified error messages by implementing a custom error
		 * strategy!
		 */
		parser.setErrorHandler(new RErrorStrategy());
		parser.setBuildParseTree(true);

		lexer.removeErrorListeners();
		// lexer.addErrorListener(li);
		parser.removeErrorListeners();
		// parser.getInterpreter().setPredictionMode(PredictionMode.LL_EXACT_AMBIG_DETECTION);
		parser.addErrorListener(li);

		ParseTreeWalker walker = new ParseTreeWalker();

		RuleContext tree = parser.prog();
		/* Create the listener to create the outline, etc. */
		RBaseListen list = new RBaseListen(tokens, editor, parser);

		list.startStop.clear();
		walker.walk(list, tree);

		RRefPhaseListen ref = new RRefPhaseListen(tokens, list, parser,offset);
		walker.walk(ref, tree);
		StringBuffer resultmethodVars=ref.getMethodCallVars();
		
		if(resultmethodVars!=null&&resultmethodVars.length()>0){
			
			
			
			 String[] splitMethod =resultmethodVars.toString().split(",");
			 
			 creatPopupList(viewer, offset, splitMethod);
		       
			
			
		}
        
		else{
		
		/*trim the method name!*/
		prefix = prefix.substring(0, leng - 1);
		prefix = prefix.trim();

		for (int i = 0; i < CalculateRProposals.statisticsSet.length; i++) {
            /*Do we have the method in the proposals?*/
			if (prefix.equals(CalculateRProposals.statistics[i])) {
				
				 String calc=CalculateRProposals.statisticsSet[i];
				 int parOpen = calc.indexOf("(");
				 int parClose = calc.indexOf(")");
				 calc = calc.substring(parOpen+1, parClose);
				 String[] splitMethod =calc.split(",");
				
			
					creatPopupList(viewer, offset, splitMethod);
			       
			}
			}
		}
		
		return prefix;
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
		} else {

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
