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
package com.eco.bio7.ijmacro.editors;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.text.DefaultIndentLineAutoEditStrategy;
import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.reconciler.MonoReconciler;
import org.eclipse.jface.text.rules.BufferedRuleBasedScanner;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;
import com.eco.bio7.ijmacro.editor.IJMacroEditorPlugin;
import com.eco.bio7.ijmacro.editor.hoover.IJMacroEditorTextHover;
import com.eco.bio7.ijmacro.editor.hoover.IJMacroSimpleDefaultInformationControl;
import com.eco.bio7.ijmacro.editor.preferences.template.IJMacroCompletionProcessor;

public class IJMacroConfiguration extends TextSourceViewerConfiguration {

	private IJMacroDoubleClickStrategy doubleClickStrategy;

	private ColorManager colorManager;
	private ScriptColorProvider provider;
	private IJMacroEditor editor;
	private IPreferenceStore store;

	public SingleTokenScanner multiLineComment;

	public IJMacroConfiguration(ColorManager colorManager, IJMacroEditor editor) {
		this.colorManager = colorManager;
		this.editor = editor;
		store = IJMacroEditorPlugin.getDefault().getPreferenceStore();

	}
	
	public  class SingleTokenScanner extends BufferedRuleBasedScanner {
		public Token att;

		public SingleTokenScanner(TextAttribute attribute) {
			att = new Token(attribute);

			setDefaultReturnToken(att);

		}

		public Token getToken() {
			return att;
		}

	}

	/*public static class SingleTokenScanner extends BufferedRuleBasedScanner {
		public SingleTokenScanner(TextAttribute attribute) {
			setDefaultReturnToken(new Token(attribute));
		}
	}*/

	public ITextDoubleClickStrategy getDoubleClickStrategy(ISourceViewer sourceViewer, String contentType) {
		return new ScriptDoubleClickSelector();
	}

	/*
	 * Method to set automatically an extra char when editing the source, e.g.,
	 * closing a brace!
	 */
	public IAutoEditStrategy[] getAutoEditStrategies(ISourceViewer sourceViewer, String contentType) {
		IAutoEditStrategy strategy = (IDocument.DEFAULT_CONTENT_TYPE.equals(contentType)
				? new IJMacroEditorEditStrategy()
				: new DefaultIndentLineAutoEditStrategy());
		return new IAutoEditStrategy[] { strategy };
	}

	public String getConfiguredDocumentPartitioning(ISourceViewer sourceViewer) {
		return IJMacroEditorPlugin.SCRIPT_PARTITIONING;
	}

	public String[] getIndentPrefixes(ISourceViewer sourceViewer, String contentType) {
		return new String[] { "\t", "    " }; //$NON-NLS-1$ //$NON-NLS-2$
	}

	public int getTabWidth(ISourceViewer sourceViewer) {
		return 4;
	}

	public String getDefaultPrefix(ISourceViewer sourceViewer, String contentType) {
		return (IDocument.DEFAULT_CONTENT_TYPE.equals(contentType) ? "//" : null); //$NON-NLS-1$
	}

	public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
		return new String[] { IDocument.DEFAULT_CONTENT_TYPE, ScriptPartitionScanner.SCRIPT_DOC,
				ScriptPartitionScanner.SCRIPT_MULTILINE_COMMENT };
	}

	public IReconciler getReconciler(ISourceViewer sourceViewer) {
		IJMacroReconcilingStrategy strategy = new IJMacroReconcilingStrategy();
		strategy.setEditor(editor);

		MonoReconciler reconciler = new MonoReconciler(strategy, false);
		reconciler.setDelay(200);
		return reconciler;
	}

	public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
		ScriptColorProvider provider = IJMacroEditorPlugin.getDefault().getScriptColorProvider();
		PresentationReconciler reconciler = new PresentationReconciler();
		reconciler.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));

		DefaultDamagerRepairer dr = new DefaultDamagerRepairer(IJMacroEditorPlugin.getDefault().getScriptCodeScanner());
		reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
		reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);

		dr = new DefaultDamagerRepairer(IJMacroEditorPlugin.getDefault().getScriptPartitionScanner());
		reconciler.setDamager(dr, ScriptPartitionScanner.SCRIPT_MULTILINE_COMMENT);
		reconciler.setRepairer(dr, ScriptPartitionScanner.SCRIPT_MULTILINE_COMMENT);

		/*dr = new DefaultDamagerRepairer(
				new SingleTokenScanner(new TextAttribute(provider.getColor(ScriptColorProvider.MULTI_LINE_COMMENT))));

		reconciler.setDamager(dr, ScriptPartitionScanner.SCRIPT_MULTILINE_COMMENT);
		reconciler.setRepairer(dr, ScriptPartitionScanner.SCRIPT_MULTILINE_COMMENT);*/
		RGB rgbkey8 = PreferenceConverter.getColor(store, "colourkey8");	
		//We create the special token with a default style from the preferences!
		multiLineComment = new SingleTokenScanner(new TextAttribute(new Color(Display.getDefault(), rgbkey8), null, isBold("BOLD_COLOURKEY8")));
		DefaultDamagerRepairer ndr = new DefaultDamagerRepairer(multiLineComment);
		reconciler.setDamager(ndr,  ScriptPartitionScanner.SCRIPT_MULTILINE_COMMENT);
		reconciler.setRepairer(ndr,  ScriptPartitionScanner.SCRIPT_MULTILINE_COMMENT);


		return reconciler;
	}
	
	/*RGB rgbkey8 = PreferenceConverter.getColor(store, "colourkey8");	
	We create the special token with a default style from the preferences!
	single = new SingleTokenScanner(new TextAttribute(new Color(Display.getDefault(), rgbkey2), null, isBold("BOLD_COLOURKEY8")));
	DefaultDamagerRepairer ndr = new DefaultDamagerRepairer(single);
	reconciler.setDamager(ndr,  ScriptPartitionScanner.SCRIPT_MULTILINE_COMMENT);
	reconciler.setRepairer(ndr,  ScriptPartitionScanner.SCRIPT_MULTILINE_COMMENT);

	return reconciler;
}*/
private int isBold(String string2) {
	int style = 0;
	IPreferenceStore store = IJMacroEditorPlugin.getDefault().getPreferenceStore();
	if (store.getBoolean(string2)) {
		style = 1;
	}

	return style;
}

	public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {
		ContentAssistant assistant = new ContentAssistant();
		assistant.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));

		IContentAssistProcessor processor = new IJMacroCompletionProcessor(editor);
		assistant.setContentAssistProcessor(processor, IDocument.DEFAULT_CONTENT_TYPE);
		assistant.enableAutoActivation(true);
		assistant.setAutoActivationDelay(200);

		assistant.setContextInformationPopupOrientation(IContentAssistant.CONTEXT_INFO_ABOVE);
		assistant.setInformationControlCreator(getInformationControlCreator(sourceViewer));

		return assistant;
	}
	/* Displays the code completion popup without a toolbar! */
	public IInformationControlCreator getInformationControlCreator(ISourceViewer sourceViewer) {
		return new IInformationControlCreator() {
			public IInformationControl createInformationControl(Shell parent) {

				/*
				 * SeeRHoverInformationControl for HTML implementation! ToolBar
				 * is added in the InformationControl!
				 */
				return new IJMacroSimpleDefaultInformationControl(parent);
			}
		};
	}

	@Override
	public ITextHover getTextHover(ISourceViewer sourceViewer, String contentType) {
		if (store.getBoolean("SHOW_INFOPOPUP")) {

			return new IJMacroEditorTextHover(editor);
		} else {
			return null;
		}

	}

}
