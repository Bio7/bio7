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
 *******************************************************************************/
/*******************************************************************************
 * Copyright (c) 2007-2014 M. Austenfeld
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/
package com.eco.bio7.reditors;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.jface.text.DefaultIndentLineAutoEditStrategy;
import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.contentassist.ContentAssistant;
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
import org.eclipse.jface.text.source.IAnnotationHover;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;
import org.eclipse.ui.texteditor.spelling.SpellingService;

import com.eco.bio7.reditor.Bio7REditorPlugin;
import com.eco.bio7.reditor.code.RAssistProcessor;
import com.eco.bio7.reditor.control.RSimpleDefaultInformationControl;
import com.eco.bio7.rpreferences.template.RoxygenCompletionProcessor;
import com.eco.bio7.rpreferences.template.RCompletionProcessor;

public class RConfiguration extends TextSourceViewerConfiguration {

	// private RDoubleClickStrategy doubleClickStrategy;

	private RColorManager colorManager;

	private RColorProvider provider;

	private REditor rEditor;

	private IPreferenceStore store;

	private RCompletionProcessor processor;

	public SingleTokenScanner single;

	public SingleTokenScanner comment;

	public IMarker selectedMarker;

	private RAssistProcessor rAssist;

	//private RTextAttribute tex;

	//private RTextAttribute tex1;

	private RoxygenCompletionProcessor processorRoxygen;

	public RConfiguration(RColorManager colorManager, REditor rEditor) {
		this.colorManager = colorManager;
		this.rEditor = rEditor;
		store = Bio7REditorPlugin.getDefault().getPreferenceStore();
	}

	public RCompletionProcessor getProcessor() {
		return processor;
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

	public ITextDoubleClickStrategy getDoubleClickStrategy(ISourceViewer sourceViewer, String contentType) {
		return new RDoubleClickSelector();

	}

	/*
	 * Method to set automatically an extra char when editing the source, e.g.,
	 * closing a brace!
	 */
	public IAutoEditStrategy[] getAutoEditStrategies(ISourceViewer sourceViewer, String contentType) {
		IAutoEditStrategy strategy = (IDocument.DEFAULT_CONTENT_TYPE.equals(contentType) ? new REditorEditStrategy() : new DefaultIndentLineAutoEditStrategy());
		return new IAutoEditStrategy[] { strategy };
	}

	/*
	 * public IAutoEditStrategy[] getAutoEditStrategies(ISourceViewer
	 * sourceViewer, String contentType) { IAutoEditStrategy strategy =
	 * (IDocument.DEFAULT_CONTENT_TYPE.equals(contentType) ? new
	 * RAutoIndentStrategy() : new DefaultIndentLineAutoEditStrategy()); return
	 * new IAutoEditStrategy[] { strategy }; }
	 */

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

		return new String[] { IDocument.DEFAULT_CONTENT_TYPE, "R_MULTILINE_STRING", "R_COMMENT" };
	}

	public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
		// RColorProvider provider =
		// Bio7REditorPlugin.getDefault().getRColorProvider();
		PresentationReconciler reconciler = new PresentationReconciler();
		reconciler.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));

		DefaultDamagerRepairer dr = new DefaultDamagerRepairer(Bio7REditorPlugin.getDefault().getRCodeScanner());
		reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
		reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);

		// IPreferenceStore store =
		// Bio7REditorPlugin.getDefault().getPreferenceStore();
		RGB rgbkey2 = PreferenceConverter.getColor(store, "colourkey2");	
		RGB rgbkey3 = PreferenceConverter.getColor(store, "colourkey3");
		
		/*We create the special token with a default style from the preferences!*/
		single = new SingleTokenScanner(new TextAttribute(new Color(Display.getDefault(), rgbkey2), null, isBold("BOLD_COLOURKEY2")));
		DefaultDamagerRepairer ndr = new DefaultDamagerRepairer(single);
		reconciler.setDamager(ndr, "R_MULTILINE_STRING");
		reconciler.setRepairer(ndr, "R_MULTILINE_STRING");
		/*
		 * We have to set the comments separately, too (to ignore quotes in
		 * comments!)
		 */
		/*We create the special token with a default style from the preferences!*/
		comment = new SingleTokenScanner(new TextAttribute(new Color(Display.getDefault(), rgbkey3), null, isBold("BOLD_COLOURKEY3")));
		DefaultDamagerRepairer ndrcomment = new DefaultDamagerRepairer(comment);
		reconciler.setDamager(ndrcomment, "R_COMMENT");
		reconciler.setRepairer(ndrcomment, "R_COMMENT");

		return reconciler;
	}
	
	private int isBold(String string2) {
		int style = 0;
		IPreferenceStore store = Bio7REditorPlugin.getDefault().getPreferenceStore();
		if (store.getBoolean(string2)) {
			style = 1;
		}

		return style;
	}

	/*public void resetMultilineStringToken(RGB rgbkey2, FontData[] f2) {
		
		RTextAttribute texTemp2 = (RTextAttribute)single.att.getData();
		texTemp2.putFontRegistry("colourkeyfont2",  f2);
		Color col=new Color(Display.getDefault(),rgbkey2);
		texTemp2.setForeground(col);
		//single.att.setData(texTemp2);
		
		
		
	}*/

	/*public void resetCommentToken(RGB rgbkey3,  FontData[] f3) {
		
		RTextAttribute texTemp3 = (RTextAttribute)comment.att.getData();
		texTemp3.putFontRegistry("colourkeyfont3",  f3);
		Color col=new Color(Display.getDefault(),rgbkey3);
		texTemp3.setForeground(col);
		//comment.att.setData(texTemp3);
		
		
	}*/

	public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {
		ContentAssistant assistant = new ContentAssistant();
		assistant.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));

		processor = new RCompletionProcessor(rEditor, assistant);
		assistant.setContentAssistProcessor(processor, IDocument.DEFAULT_CONTENT_TYPE);
		
        processorRoxygen=new RoxygenCompletionProcessor();
        assistant.setContentAssistProcessor(processorRoxygen,"R_COMMENT" );
        
		assistant.enableAutoActivation(true);
		assistant.setAutoActivationDelay(100);
        assistant.enablePrefixCompletion(true);
		assistant.setContextInformationPopupOrientation(IContentAssistant.CONTEXT_INFO_ABOVE);
		assistant.setInformationControlCreator(getInformationControlCreator(sourceViewer));

		return assistant;
	}

	public IReconciler getReconciler(ISourceViewer sourceViewer) {
		SpellingService spellingService = EditorsUI.getSpellingService();
		if (spellingService.getActiveSpellingEngineDescriptor(store) == null)
			return null;
		RReconcilingStrategy strategy = new RReconcilingStrategy(sourceViewer,spellingService);
		strategy.setEditor(rEditor);

		MonoReconciler reconciler = new MonoReconciler(strategy, false);
		reconciler.setDelay(200);
		return reconciler;
	}

	public IQuickAssistAssistant getQuickAssistAssistant(ISourceViewer sourceViewer) {
		IQuickAssistAssistant quickAssist = new QuickAssistAssistant();
		rAssist = new RAssistProcessor(rEditor);
		quickAssist.setQuickAssistProcessor(rAssist);
		quickAssist.setInformationControlCreator(getInformationControlCreator(sourceViewer));
		return quickAssist;
	}

	@Override
	public ITextHover getTextHover(ISourceViewer sourceViewer, String contentType) {

		if (store.getBoolean("SHOW_INFOPOPUP")) {
			return new REditorTextHover(rEditor, rAssist);
		}
		return null;
	}

	@Override
	public IAnnotationHover getAnnotationHover(ISourceViewer sourceViewer) {
		// TODO Auto-generated method stub
		return super.getAnnotationHover(sourceViewer);
		// return new AnnotationHover();
	}

	/* Displays the code completion popup without a toolbar! */
	public IInformationControlCreator getInformationControlCreator(ISourceViewer sourceViewer) {
		return new IInformationControlCreator() {
			public IInformationControl createInformationControl(Shell parent) {

				/*
				 * SeeRHoverInformationControl for HTML implementation! ToolBar
				 * is added in the InformationControl!
				 */
				return new RSimpleDefaultInformationControl(parent);
			}
		};
	}

	public IMarker[] findMyMarkers(IResource target) {
		String type = "org.eclipse.core.resources.problemmarker";

		IMarker[] markers = null;
		try {
			markers = target.findMarkers(type, true, IResource.DEPTH_INFINITE);
		} catch (CoreException e) {

			e.printStackTrace();
		}
		return markers;
	}

}
