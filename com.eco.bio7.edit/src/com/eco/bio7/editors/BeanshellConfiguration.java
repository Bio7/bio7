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
package com.eco.bio7.editors;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.text.DefaultIndentLineAutoEditStrategy;
import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.rules.BufferedRuleBasedScanner;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.source.IAnnotationHover;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;

import com.eco.bio7.editor.BeanshellEditorPlugin;
import com.eco.bio7.reditor.preferences.template.BeanshellCompletionProcessor;

public class BeanshellConfiguration extends TextSourceViewerConfiguration {

	private BeanshellDoubleClickStrategy doubleClickStrategy;
	IPreferenceStore store = BeanshellEditorPlugin.getDefault().getPreferenceStore();
	private ColorManager colorManager;
	ScriptColorProvider provider;

	public BeanshellConfiguration(ColorManager colorManager) {
		this.colorManager = colorManager;

	}

	public static class SingleTokenScanner extends BufferedRuleBasedScanner {
		public SingleTokenScanner(TextAttribute attribute) {
			setDefaultReturnToken(new Token(attribute));
		}
	}

	public ITextDoubleClickStrategy getDoubleClickStrategy(
			ISourceViewer sourceViewer, String contentType) {
		return new ScriptDoubleClickSelector();
	}

	public IAutoEditStrategy[] getAutoEditStrategies(
			ISourceViewer sourceViewer, String contentType) {
		IAutoEditStrategy strategy = (IDocument.DEFAULT_CONTENT_TYPE
				.equals(contentType) ? new ScriptAutoIndentStrategy()
				: new DefaultIndentLineAutoEditStrategy());
		return new IAutoEditStrategy[] { strategy };
	}

	public String getConfiguredDocumentPartitioning(ISourceViewer sourceViewer) {
		return BeanshellEditorPlugin.SCRIPT_PARTITIONING;
	}

	public String[] getIndentPrefixes(ISourceViewer sourceViewer,
			String contentType) {
		return new String[] { "\t", "    " }; //$NON-NLS-1$ //$NON-NLS-2$
	}

	public int getTabWidth(ISourceViewer sourceViewer) {
		return 4;
	}

	public String getDefaultPrefix(ISourceViewer sourceViewer,
			String contentType) {
		return (IDocument.DEFAULT_CONTENT_TYPE.equals(contentType) ? "//" : null); //$NON-NLS-1$
	}

	public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
		return new String[] { IDocument.DEFAULT_CONTENT_TYPE,
				ScriptPartitionScanner.SCRIPT_DOC,
				ScriptPartitionScanner.SCRIPT_MULTILINE_COMMENT,
				ScriptPartitionScanner.SCRIPT_MULTILINE_STRING};
	}

	public IPresentationReconciler getPresentationReconciler(
			ISourceViewer sourceViewer) {
		ScriptColorProvider provider = BeanshellEditorPlugin.getDefault()
				.getScriptColorProvider();
		PresentationReconciler reconciler = new PresentationReconciler();
		reconciler
				.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));

		DefaultDamagerRepairer dr = new DefaultDamagerRepairer(
				BeanshellEditorPlugin.getDefault().getScriptCodeScanner());
		reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
		reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);

		dr = new DefaultDamagerRepairer(BeanshellEditorPlugin.getDefault()
				.getScriptPartitionScanner());
		reconciler.setDamager(dr, ScriptPartitionScanner.SCRIPT_MULTILINE_COMMENT);
		reconciler.setRepairer(dr, ScriptPartitionScanner.SCRIPT_MULTILINE_COMMENT);

		dr = new DefaultDamagerRepairer(new SingleTokenScanner(
				new TextAttribute(provider
						.getColor(ScriptColorProvider.MULTI_LINE_COMMENT))));

		reconciler.setDamager(dr, ScriptPartitionScanner.SCRIPT_MULTILINE_COMMENT);
		reconciler.setRepairer(dr, ScriptPartitionScanner.SCRIPT_MULTILINE_COMMENT);
		
		dr = new DefaultDamagerRepairer(BeanshellEditorPlugin.getDefault()
				.getScriptPartitionScanner());
		reconciler.setDamager(dr, ScriptPartitionScanner.SCRIPT_MULTILINE_STRING);
		reconciler.setRepairer(dr, ScriptPartitionScanner.SCRIPT_MULTILINE_STRING);

		dr = new DefaultDamagerRepairer(new SingleTokenScanner(
				new TextAttribute(provider.getColor(PreferenceConverter.getColor(store, "colourkey2")),null, isBold("BOLD_COLOURKEY2"))));

		reconciler.setDamager(dr, ScriptPartitionScanner.SCRIPT_MULTILINE_STRING);
		reconciler.setRepairer(dr, ScriptPartitionScanner.SCRIPT_MULTILINE_STRING);

		return reconciler;
	}
	private int isBold(String string2) {
		int style = 0;
		IPreferenceStore store = BeanshellEditorPlugin.getDefault().getPreferenceStore();
		if (store.getBoolean(string2)) {
			style = 1;
		}

		return style;
	}

	public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {
		ContentAssistant assistant = new ContentAssistant();
		assistant
				.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));

		IContentAssistProcessor processor = new BeanshellCompletionProcessor();
		assistant.setContentAssistProcessor(processor,
				IDocument.DEFAULT_CONTENT_TYPE);
		assistant.enableAutoActivation(true);
		assistant.setAutoActivationDelay(500);

		assistant
				.setContextInformationPopupOrientation(IContentAssistant.CONTEXT_INFO_ABOVE);
		assistant
				.setInformationControlCreator(getInformationControlCreator(sourceViewer));

		return assistant;
	}

}
