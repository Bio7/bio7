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
package com.eco.bio7.reditors;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DefaultIndentLineAutoEditStrategy;
import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextHoverExtension2;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.quickassist.IQuickAssistAssistant;
import org.eclipse.jface.text.quickassist.QuickAssistAssistant;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.reconciler.MonoReconciler;
import org.eclipse.jface.text.rules.BufferedRuleBasedScanner;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;

import com.eco.bio7.reditor.Bio7REditorPlugin;
import com.eco.bio7.reditor.code.RAssistProcessor;
import com.eco.bio7.rpreferences.template.RCompletionProcessor;

public class RConfiguration extends TextSourceViewerConfiguration {

	private RDoubleClickStrategy doubleClickStrategy;

	private RColorManager colorManager;

	RColorProvider provider;

	private REditor rEditor;

	public RConfiguration(RColorManager colorManager, REditor rEditor) {
		this.colorManager = colorManager;
        this.rEditor=rEditor;
	}

	public static class SingleTokenScanner extends BufferedRuleBasedScanner {
		public SingleTokenScanner(TextAttribute attribute) {
			setDefaultReturnToken(new Token(attribute));
		}
	}

	public ITextDoubleClickStrategy getDoubleClickStrategy(ISourceViewer sourceViewer, String contentType) {
		return new RDoubleClickSelector();
		
	}

	public IAutoEditStrategy[] getAutoEditStrategies(ISourceViewer sourceViewer, String contentType) {
		IAutoEditStrategy strategy = (IDocument.DEFAULT_CONTENT_TYPE.equals(contentType) ? new RAutoIndentStrategy() : new DefaultIndentLineAutoEditStrategy());
		return new IAutoEditStrategy[] { strategy };
	}

	public String getConfiguredDocumentPartitioning(ISourceViewer sourceViewer) {
		return Bio7REditorPlugin.R_PARTITIONING;
	}

	public String[] getIndentPrefixes(ISourceViewer sourceViewer, String contentType) {
		return new String[] { "\t", "    " }; 
	}

	public int getTabWidth(ISourceViewer sourceViewer) {
		return 4;
	}

	public String getDefaultPrefix(ISourceViewer sourceViewer, String contentType) {
		return (IDocument.DEFAULT_CONTENT_TYPE.equals(contentType) ? "//" : null); 
	}

	public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
		return new String[] { IDocument.DEFAULT_CONTENT_TYPE, RPartitionScanner.R_DOC, RPartitionScanner.R_MULTILINE_COMMENT };
	}

	public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
		RColorProvider provider = Bio7REditorPlugin.getDefault().getRColorProvider();
		PresentationReconciler reconciler = new PresentationReconciler();
		reconciler.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));

		DefaultDamagerRepairer dr = new DefaultDamagerRepairer(Bio7REditorPlugin.getDefault().getRCodeScanner());
		reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
		reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);

		dr = new DefaultDamagerRepairer(Bio7REditorPlugin.getDefault().getRPartitionScanner());
		reconciler.setDamager(dr, RPartitionScanner.R_MULTILINE_COMMENT);
		reconciler.setRepairer(dr, RPartitionScanner.R_MULTILINE_COMMENT);

		dr = new DefaultDamagerRepairer(new SingleTokenScanner(new TextAttribute(provider.getColor(RColorProvider.MULTI_LINE_COMMENT))));
		reconciler.setDamager(dr, RPartitionScanner.R_MULTILINE_COMMENT);
		reconciler.setRepairer(dr, RPartitionScanner.R_MULTILINE_COMMENT);

		return reconciler;
	}

	public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {
		ContentAssistant assistant = new ContentAssistant();
		assistant.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));

		IContentAssistProcessor processor = new RCompletionProcessor();
		assistant.setContentAssistProcessor(processor, IDocument.DEFAULT_CONTENT_TYPE);
		
		
		//assistant.setContentAssistProcessor(new JavaCompletionProcessor(), IDocument.DEFAULT_CATEGORY);
		
		assistant.enableAutoActivation(true);
		assistant.setAutoActivationDelay(500);

		assistant.setContextInformationPopupOrientation(IContentAssistant.CONTEXT_INFO_ABOVE);
		assistant.setInformationControlCreator(getInformationControlCreator(sourceViewer));

		return assistant;
	}
	
	public IReconciler getReconciler(ISourceViewer sourceViewer)
    {
        RReconcilingStrategy strategy = new RReconcilingStrategy();
        strategy.setEditor(rEditor);
        
        MonoReconciler reconciler = new MonoReconciler(strategy,false);
        
        return reconciler;
    }
	
	public IQuickAssistAssistant getQuickAssistAssistant(ISourceViewer sourceViewer) {
		  IQuickAssistAssistant quickAssist = new QuickAssistAssistant();
		  quickAssist.setQuickAssistProcessor(new RAssistProcessor());
		  quickAssist.setInformationControlCreator(getInformationControlCreator(sourceViewer));
		  return quickAssist; 
		}
	
	@Override
	public ITextHover getTextHover(ISourceViewer sourceViewer, String contentType) {
	        return new MarkdownTextHover();
	}

	
	public class MarkdownTextHover implements ITextHover, ITextHoverExtension2 {
	        // return information to be shown when the cursor is on the given region
	        @Override
	        public Object getHoverInfo2(ITextViewer textViewer, IRegion hoverRegion) {
	                try {
	                        return textViewer.getDocument().getPartition(hoverRegion.getOffset()).toString()+" "+textViewer.getDocument().getPartition(hoverRegion.getLength()).toString();
	                } 
	                catch (BadLocationException e) {
	                        return "No info because of " + e.getMessage();                  
	                }
	        }

	        // just an old version of the API method that returns only strings
	        @Override
	        public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {               
	                return getHoverInfo2(textViewer, hoverRegion).toString();
	        }

	        // returns the region object for a given position in the text editor
	        @Override
	        public IRegion getHoverRegion(ITextViewer textViewer, int offset) {
	                Point selection = textViewer.getSelectedRange();
	                if (selection.x <= offset && offset < selection.x + selection.y) {
	                        return new Region(selection.x, selection.y);
	                }
	                // if no text is selected then we return a region of the size 0 (a single character)
	                return new Region(offset, 0);
	        }

			

			

			
	}
	

}
