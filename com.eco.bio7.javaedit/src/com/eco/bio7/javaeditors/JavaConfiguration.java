/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     
 *     M.Austenfeld - Minor changes for the Bio7 application.
 *******************************************************************************/
package com.eco.bio7.javaeditors;

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
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.reconciler.MonoReconciler;
import org.eclipse.jface.text.rules.BufferedRuleBasedScanner;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;
import com.eco.bio7.javaeditor.Bio7EditorPlugin;
import com.eco.bio7.javapreferences.template.JavaCompletionProcessor;

public class JavaConfiguration extends TextSourceViewerConfiguration {

	private JavaDoubleClickStrategy doubleClickStrategy;

	private ColorManager colorManager;
	JavaColorProvider provider;

	private JavaEditor javaEditor;

	public JavaConfiguration(ColorManager colorManager, JavaEditor editor) {
		this.colorManager = colorManager;
        this.javaEditor=editor;
	}

	public static class SingleTokenScanner extends BufferedRuleBasedScanner {
		public SingleTokenScanner(TextAttribute attribute) {
			setDefaultReturnToken(new Token(attribute));
		}
	}

	public ITextDoubleClickStrategy getDoubleClickStrategy(ISourceViewer sourceViewer, String contentType) {
		return new JavaDoubleClickSelector();
	}

	public IAutoEditStrategy[] getAutoEditStrategies(ISourceViewer sourceViewer, String contentType) {
		IAutoEditStrategy strategy = (IDocument.DEFAULT_CONTENT_TYPE.equals(contentType) ? new JavaAutoIndentStrategy() : new DefaultIndentLineAutoEditStrategy());
		return new IAutoEditStrategy[] { strategy };
	}

	public String getConfiguredDocumentPartitioning(ISourceViewer sourceViewer) {
		return Bio7EditorPlugin.JAVA_PARTITIONING;
	}

	public String[] getIndentPrefixes(ISourceViewer sourceViewer, String contentType) {
		return new String[] { "\t", "    " };
	}

	public int getTabWidth(ISourceViewer sourceViewer) {
		return 4;
	}

	public String getDefaultPrefix(ISourceViewer sourceViewer, String contentType) {
		return (IDocument.DEFAULT_CONTENT_TYPE.equals(contentType) ? "//" : null); //$NON-NLS-1$
	}

	public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
		return new String[] { IDocument.DEFAULT_CONTENT_TYPE, JavaPartitionScanner.JAVA_DOC, JavaPartitionScanner.JAVA_MULTILINE_COMMENT };
	}

	public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
		JavaColorProvider provider = Bio7EditorPlugin.getDefault().getJavaColorProvider();
		PresentationReconciler reconciler = new PresentationReconciler();
		reconciler.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));

		DefaultDamagerRepairer dr = new DefaultDamagerRepairer(Bio7EditorPlugin.getDefault().getJavaCodeScanner());
		reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
		reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);

		dr = new DefaultDamagerRepairer(Bio7EditorPlugin.getDefault().getJavaPartitionScanner());
		reconciler.setDamager(dr, JavaPartitionScanner.JAVA_MULTILINE_COMMENT);
		reconciler.setRepairer(dr, JavaPartitionScanner.JAVA_MULTILINE_COMMENT);

		dr = new DefaultDamagerRepairer(new SingleTokenScanner(new TextAttribute(provider.getColor(JavaColorProvider.MULTI_LINE_COMMENT))));
		reconciler.setDamager(dr, JavaPartitionScanner.JAVA_MULTILINE_COMMENT);
		reconciler.setRepairer(dr, JavaPartitionScanner.JAVA_MULTILINE_COMMENT);
		
		return reconciler;
	}
	
	@Override
	public IReconciler getReconciler(ISourceViewer sourceViewer) {
		
		MonoReconciler reconciler= new MonoReconciler(new JavaReconcilingStrategy(javaEditor), false);
		
	     reconciler.setDelay(JavaReconcilingStrategy.DELAY);
	     //reconciler.addReconcilingParticipant(sourceViewer);
		return reconciler;
	}
	
	
  
	public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {
		ContentAssistant assistant = new ContentAssistant();
		assistant.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));

		IContentAssistProcessor processor = new JavaCompletionProcessor();
		assistant.setContentAssistProcessor(processor, IDocument.DEFAULT_CONTENT_TYPE);
		assistant.enableAutoActivation(true);
		assistant.setAutoActivationDelay(500);

		assistant.setContextInformationPopupOrientation(IContentAssistant.CONTEXT_INFO_ABOVE);
		assistant.setInformationControlCreator(getInformationControlCreator(sourceViewer));

		return assistant;
	}

}
