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
package com.eco.bio7.ijmacro.editor.preferences.template;

import org.eclipse.swt.graphics.Image;

import com.eco.bio7.ijmacro.editor.IJMacroEditorPlugin;
import com.eco.bio7.ijmacro.editor.antlr.ImageJMacroBaseListen;
import com.eco.bio7.ijmacro.editor.antlr.VariableScope;
import com.eco.bio7.ijmacro.editors.IJMacroEditor;
import com.eco.bio7.ijmacro.editors.TemplateEditorUI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.eclipse.jface.text.templates.TemplateException;
import org.eclipse.jface.text.templates.TemplateProposal;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateCompletionProcessor;
import org.eclipse.jface.text.templates.TemplateContext;

/**
 * A completion processor for Java and BeanShell templates.
 */
public class IJMacroCompletionProcessor extends TemplateCompletionProcessor {
	private static final String DEFAULT_IMAGE = "$nl$/icons/template_obj.png"; //$NON-NLS-1$
	private static final String METHOD_IMAGE = "$nl$/icons/methpub_obj.png"; //$NON-NLS-1$
	private static final String VAR_EDITOR__IMAGE = "$nl$/icons/field_public_obj.png"; //$NON-NLS-1$
	private static final String GLOBAL_VAR_EDITOR_IMAGE = "$nl$/icons/field_private_obj.png"; //$NON-NLS-1$
	private static final String FUNCTION_EDITOR__IMAGE = "$nl$/icons/brkp_obj.png";
	private static final Comparator fgProposalComparator = new ProposalComparator();
	private int count = 0;// Variable to count the listed template.
	private int defaultTemplatesLength;
	private IPreferenceStore store;
	private IJMacroEditor editor;
	private int editorVarSize;
	private int editorFunctionSize;
	private int editorGlobalVarSize;
	public static HashMap<String, String> mapFunctionAndContext;

	public IJMacroCompletionProcessor(IJMacroEditor editor) {
		store = IJMacroEditorPlugin.getDefault().getPreferenceStore();
		this.editor = editor;

	}

	/**
	 * We watch for angular brackets since those are often part of XML templates.
	 * 
	 * @param viewer the viewer
	 * @param offset the offset left of which the prefix is detected
	 * @return the detected prefix
	 */
	private static final class ProposalComparator implements Comparator<Object> {
		public int compare(Object o1, Object o2) {
			return ((TemplateProposal) o2).getRelevance() - ((TemplateProposal) o1).getRelevance();
		}
	}

	/* Extend prefixes for macro functions with a dot, e.g. t.test() */
	protected String extractPrefix(ITextViewer viewer, int offset) {
		int i = offset;
		IDocument document = viewer.getDocument();
		if (i > document.getLength())
			return "";

		try {
			while (i > 0) {
				char ch = document.getChar(i - 1);
				/*
				 * We need to extra include the '.' character!
				 */
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
	 * Cut out angular brackets for relevance sorting, since the template name does
	 * not contain the brackets.
	 * 
	 * @param template the template
	 * @param prefix   the prefix
	 * @return the relevance of the <code>template</code> for the given
	 *         <code>prefix</code>
	 */
	protected int getRelevance(Template template, String prefix) {
		// if (template.getName().toLowerCase().replace(".", "").startsWith(prefix))
		if (store.getBoolean("IJ_COMPLETION_CONTAINS")) {
			if (template.getName().contains(prefix) || template.getName().toLowerCase().contains(prefix))
				return 90;

		} else {
			if (template.getName().startsWith(prefix) || template.getName().toLowerCase().startsWith(prefix))

				return 90;
		}
		return 0;
	}

	/**
	 * Simply return all templates.
	 * 
	 * @param contextTypeId the context type, ignored in this implementation
	 * @return all templates
	 */
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
		count = 0;
		String prefix = extractPrefix(viewer, offset);
		mapFunctionAndContext = new HashMap<String, String>();
		Region region = null;
		region = new Region(offset - prefix.length(), prefix.length());
		TemplateContext context = createContext(viewer, region);

		List<ICompletionProposal> matches = new ArrayList<ICompletionProposal>();

		/* The user defined templates from the Template UI! */
		Template[] templates = getTemplates(context.getContextType().getId());
		defaultTemplatesLength = templates.length;

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

		com.eco.bio7.ijmacro.editor.antlr.Parse parse = new com.eco.bio7.ijmacro.editor.antlr.Parse(editor);
		ImageJMacroBaseListen ref = parse.parseFromOffset(offset);
		HashMap<Integer, String> map = parse.getMap();
		/* Editor Variables! */
		VariableScope varScope = ref.getTempCodeComplScope();

		ArrayList<String> buffVars = varScope.getAllVariables(varScope);
		editorVarSize = buffVars.size();
		Template[] tempLocalFunctionsEditorVars = new Template[editorVarSize];

		for (int i = 0; i < editorVarSize; i++) {
			// System.out.println(buffVars.get(i));
			String str = buffVars.get(i);
			tempLocalFunctionsEditorVars[i] = new Template(str, "Editor defined variable",
					context.getContextType().getId(), str + "${cursor}", true);

			Template template = tempLocalFunctionsEditorVars[i];
			try {
				context.getContextType().validate(template.getPattern());
			} catch (TemplateException e) {
				continue;
			}
			if (template.matches(prefix, context.getContextType().getId()))
				matches.add(createProposal(template, context, (IRegion) region, getRelevance(template, prefix)));

		}
		/* Editor Global Variables! */
		//VariableScope globalVarvScope = ref.getTempCodeComplScope();

		ArrayList<String> buffGlobalVars = ref.getGlobalVariables();
		editorGlobalVarSize = buffGlobalVars.size();
		Template[] tempLocalsEditorGlobalVars = new Template[editorGlobalVarSize];

		for (int i = 0; i < editorGlobalVarSize; i++) {
			// System.out.println(buffVars.get(i));
			String str = buffGlobalVars.get(i);
			tempLocalsEditorGlobalVars[i] = new Template(str, "Editor defined global variable",
					context.getContextType().getId(), str + "${cursor}", true);

			Template template = tempLocalsEditorGlobalVars[i];
			try {
				context.getContextType().validate(template.getPattern());
			} catch (TemplateException e) {
				continue;
			}
			if (template.matches(prefix, context.getContextType().getId()))
				matches.add(createProposal(template, context, (IRegion) region, getRelevance(template, prefix)));

		}
		/* Editor Functions! */
		ArrayList<String> functions = ref.getFunctions();
		editorFunctionSize = functions.size();
		Template[] tempLocalFunctions = new Template[editorFunctionSize];
		for (int i = 0; i < editorFunctionSize; i++) {

			String str = functions.get(i);
			String[] splitNumber = str.split("####");
			String methodLineNumber = splitNumber[0];

			String description = "Editor function";

			str = splitNumber[1];
			int firstBracket = str.indexOf('(');
			if (firstBracket > -1) {
				String contentOfBrackets = str.substring(firstBracket + 1, str.indexOf(')'));
				String contentBegin = str.substring(0, str.indexOf('('));
                /*Here we get the map from the Parse class and we get the comment from the line number (hashmap key)*/
				String editorMethodDoc = map.get(new Integer(methodLineNumber));
				if (editorMethodDoc != null) {
					//System.out.println("str is: "+str);
					//System.out.println("editor doc is: "+editorMethodDoc);
					/*For the InformationControl context (our comments) we create a new map here which will be accessed from the control!*/
					mapFunctionAndContext.put(str+";", editorMethodDoc);
				}

				if (contentOfBrackets.isEmpty() == false) {
					StringBuffer buf = new StringBuffer();
					String[] args = contentOfBrackets.split(",");
					for (int j = 0; j < args.length; j++) {
						/*
						 * Arguments with quotes are Constants and will not work with placeholder. We
						 * handle them separately!
						 */
						if (args[j].contains("\"")) {
							buf.append(args[j]);
						}
						/*
						 * The ${} placeholder will be removed in the
						 * IJMacroSimpleDefaultInformationControl to get the context information!
						 */
						else {
							buf.append("${" + args[j] + "}");
						}
						if (j < args.length - 1) {
							buf.append(",");
						}
					}

					String argFun = buf.toString();
					tempLocalFunctions[i] = new Template(str, description, context.getContextType().getId(),
							contentBegin + "(" + argFun + ");" + "${cursor}", true);
				} else {
					tempLocalFunctions[i] = new Template(str, description, context.getContextType().getId(),
							str + ";" + "${cursor}", true);
				}
			} else {
				tempLocalFunctions[i] = new Template(str, description, context.getContextType().getId(),
						str + ";" + "${cursor}", true);
			}

			Template template = tempLocalFunctions[i];
			try {
				context.getContextType().validate(template.getPattern());
			} catch (TemplateException e) {
				continue;
			}
			if (template.matches(prefix, context.getContextType().getId()))
				matches.add(createProposal(template, context, (IRegion) region, getRelevance(template, prefix)));

		}

		/* The ImageJ macro API functions! */
		String[] splitted = IJMacroFunctions.functions.split(System.lineSeparator());
		Template[] tempLocalFunctions2 = new Template[splitted.length];
		for (int i = 0; i < splitted.length; i++) {
			String[] funArray = splitted[i].split("####");

			String str = funArray[0];
			int firstBracket = str.indexOf('(');
			if (firstBracket > -1) {
				String contentOfBrackets = str.substring(firstBracket + 1, str.indexOf(')'));
				String contentBegin = str.substring(0, str.indexOf('('));
				if (contentOfBrackets.isEmpty() == false) {
					StringBuffer buf = new StringBuffer();
					String[] args = contentOfBrackets.split(",");
					for (int j = 0; j < args.length; j++) {
						/*
						 * Arguments with quotes are Constants and will not work with placeholder. We
						 * handle them separately!
						 */
						if (args[j].contains("\"")) {
							buf.append(args[j]);
						}
						/*
						 * The ${} placeholder will be removed in the
						 * IJMacroSimpleDefaultInformationControl to get the context information!
						 */
						else {
							buf.append("${" + args[j] + "}");
						}
						if (j < args.length - 1) {
							buf.append(",");
						}
					}
					String argFun = buf.toString();
					tempLocalFunctions2[i] = new Template(funArray[0], funArray[1], context.getContextType().getId(),
							contentBegin + "(" + argFun + ");" + "${cursor}", true);
				} else {
					tempLocalFunctions2[i] = new Template(funArray[0], funArray[1], context.getContextType().getId(),
							funArray[0] + ";" + "${cursor}", true);
				}
			} else {
				tempLocalFunctions2[i] = new Template(funArray[0], funArray[1], context.getContextType().getId(),
						funArray[0] + ";" + "${cursor}", true);
			}

			Template template = tempLocalFunctions2[i];
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

	protected Template[] getTemplates(String contextTypeId) {
		return TemplateEditorUI.getDefault().getTemplateStore().getTemplates();
	}

	// Add the chars for Completion here !!!
	public char[] getCompletionProposalAutoActivationCharacters() {

		if (store.getBoolean("TYPED_CODE_COMPLETION")) {
			String ac = store.getString("ACTIVATION_CHARS");
			if (ac == null || ac.isEmpty()) {

				return null;
			}
			return ac.toCharArray();
		}

		return null;

	}

	/**
	 * Return the XML context type that is supported by this plug-in.
	 * 
	 * @param viewer the viewer, ignored in this implementation
	 * @param region the region, ignored in this implementation
	 * @return the supported XML context type
	 */
	protected TemplateContextType getContextType(ITextViewer viewer, IRegion region) {
		return TemplateEditorUI.getDefault().getContextTypeRegistry()
				.getContextType(IJMacroContextType.XML_CONTEXT_TYPE);
	}

	/**
	 * Always return the default image.
	 * 
	 * @param template the template, ignored in this implementation
	 * @return the default template image
	 */
	protected Image getImage(Template template) {

		if (count < defaultTemplatesLength) {
			count++;
			ImageRegistry registry = TemplateEditorUI.getDefault().getImageRegistry();
			Image image = registry.get(DEFAULT_IMAGE);
			if (image == null) {
				ImageDescriptor desc = TemplateEditorUI.imageDescriptorFromPlugin("com.eco.bio7.ijmacro.editor", //$NON-NLS-1$
						DEFAULT_IMAGE);
				registry.put(DEFAULT_IMAGE, desc);
				image = registry.get(DEFAULT_IMAGE);
			}
			return image;

		}

		else if (count < defaultTemplatesLength + editorVarSize) {
			count++;
			ImageRegistry registry = TemplateEditorUI.getDefault().getImageRegistry();
			Image image = registry.get(VAR_EDITOR__IMAGE);
			if (image == null) {
				ImageDescriptor desc = TemplateEditorUI.imageDescriptorFromPlugin("com.eco.bio7.ijmacro.editor", //$NON-NLS-1$
						VAR_EDITOR__IMAGE);
				registry.put(VAR_EDITOR__IMAGE, desc);
				image = registry.get(VAR_EDITOR__IMAGE);
			}
			return image;

		}

		else if (count < defaultTemplatesLength + editorVarSize + editorGlobalVarSize) {
			count++;
			ImageRegistry registry = TemplateEditorUI.getDefault().getImageRegistry();
			Image image = registry.get(GLOBAL_VAR_EDITOR_IMAGE);
			if (image == null) {
				ImageDescriptor desc = TemplateEditorUI.imageDescriptorFromPlugin("com.eco.bio7.ijmacro.editor", //$NON-NLS-1$
						GLOBAL_VAR_EDITOR_IMAGE);
				registry.put(GLOBAL_VAR_EDITOR_IMAGE, desc);
				image = registry.get(GLOBAL_VAR_EDITOR_IMAGE);
			}
			return image;

		}

		else if (count < defaultTemplatesLength + editorVarSize + +editorGlobalVarSize + editorFunctionSize) {
			count++;
			ImageRegistry registry = TemplateEditorUI.getDefault().getImageRegistry();
			Image image = registry.get(METHOD_IMAGE);
			if (image == null) {
				ImageDescriptor desc = TemplateEditorUI.imageDescriptorFromPlugin("com.eco.bio7.ijmacro.editor", //$NON-NLS-1$
						METHOD_IMAGE);
				registry.put(METHOD_IMAGE, desc);
				image = registry.get(METHOD_IMAGE);
			}
			return image;

		}

		else {

			ImageRegistry registry = TemplateEditorUI.getDefault().getImageRegistry();
			Image image = registry.get(FUNCTION_EDITOR__IMAGE);
			if (image == null) {
				ImageDescriptor desc = TemplateEditorUI.imageDescriptorFromPlugin("com.eco.bio7.ijmacro.editor", //$NON-NLS-1$
						FUNCTION_EDITOR__IMAGE);
				registry.put(FUNCTION_EDITOR__IMAGE, desc);
				image = registry.get(FUNCTION_EDITOR__IMAGE);
			}
			return image;

		}
	}

}
