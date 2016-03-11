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

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DefaultIndentLineAutoEditStrategy;
import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextHoverExtension2;
import org.eclipse.jface.text.ITextOperationTarget;
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
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import com.eco.bio7.rbridge.RState;
import com.eco.bio7.reditor.Bio7REditorPlugin;
import com.eco.bio7.reditor.code.RAssistProcessor;
import com.eco.bio7.rpreferences.template.RCompletionProcessor;

public class RConfiguration extends TextSourceViewerConfiguration {

	// private RDoubleClickStrategy doubleClickStrategy;

	private RColorManager colorManager;

	private RColorProvider provider;

	private REditor rEditor;

	private IPreferenceStore store;

	private RCompletionProcessor processor;

	private SingleTokenScanner single;

	private SingleTokenScanner comment;

	public static String htmlHelpText = "";
	
	

	public RConfiguration(RColorManager colorManager, REditor rEditor) {
		this.colorManager = colorManager;
		this.rEditor = rEditor;
		store = Bio7REditorPlugin.getDefault().getPreferenceStore();
	}
	
	public RCompletionProcessor getProcessor() {
		return processor;
	}

	public static class SingleTokenScanner extends BufferedRuleBasedScanner {
		public SingleTokenScanner(TextAttribute attribute) {
			setDefaultReturnToken(new Token(attribute));
		}
	}

	public ITextDoubleClickStrategy getDoubleClickStrategy(ISourceViewer sourceViewer, String contentType) {
		return new RDoubleClickSelector();

	}
    /*Method to set automatically an extra char when editing the source, e.g., closing a brace!*/
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
		FontData f2 = PreferenceConverter.getFontData(store, "colourkeyfont2");
		RGB rgbkey3 = PreferenceConverter.getColor(store, "colourkey3");
		FontData f3 = PreferenceConverter.getFontData(store, "colourkeyfont3");
		
		single=new SingleTokenScanner(new TextAttribute(new Color(Display.getDefault(), rgbkey2), null, 1, new Font(Display.getCurrent(), f2)));
		DefaultDamagerRepairer ndr = new DefaultDamagerRepairer(single);
		reconciler.setDamager(ndr, "R_MULTILINE_STRING");
		reconciler.setRepairer(ndr, "R_MULTILINE_STRING");
		/*
		 * We have to set the comments separately, too (to ignore quotes in
		 * comments!)
		 */
		comment=new SingleTokenScanner(new TextAttribute(new Color(Display.getDefault(), rgbkey3), null, 1, new Font(Display.getCurrent(), f3)));
		DefaultDamagerRepairer ndrcomment = new DefaultDamagerRepairer(comment);
		reconciler.setDamager(ndrcomment, "R_COMMENT");
		reconciler.setRepairer(ndrcomment, "R_COMMENT");

		/*
		 * DefaultDamagerRepairer ndr = new
		 * DefaultDamagerRepairer(Bio7REditorPlugin
		 * .getDefault().getRPartitionScanner()); NonRuleBasedDamagerRepairer
		 * ndr = new NonRuleBasedDamagerRepairer( new TextAttribute(new
		 * Color(Display.getDefault(),rgbkey2), null, 1, new
		 * Font(Display.getCurrent(), f2))); //NonRuleBasedDamagerRepairer ndr =
		 * new NonRuleBasedDamagerRepairer(new
		 * TextAttribute(provider.getColor(rgbkey2), null, 1, new
		 * Font(Display.getCurrent(), f2)));
		 * reconciler.setDamager(ndr,"R_MULTILINE_STRING");
		 * reconciler.setRepairer(ndr,"R_MULTILINE_STRING");
		 */
		return reconciler;
	}

	public void  resetMultilineStringToken(RGB rgbkey2, FontData f2) {
		
		single.setDefaultReturnToken(new Token(new TextAttribute(new Color(Display.getDefault(), rgbkey2), null, 1, new Font(Display.getCurrent(), f2))));
		
	}
	
	public void resetCommentToken(RGB rgbkey3, FontData f3) {
		
		comment.setDefaultReturnToken(new Token(new TextAttribute(new Color(Display.getDefault(), rgbkey3), null, 1, new Font(Display.getCurrent(), f3))));
		
	}

	public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {
		ContentAssistant assistant = new ContentAssistant();
		assistant.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));

		processor = new RCompletionProcessor(rEditor);
		assistant.setContentAssistProcessor(processor, IDocument.DEFAULT_CONTENT_TYPE);

		assistant.enableAutoActivation(true);
		assistant.setAutoActivationDelay(200);

		assistant.setContextInformationPopupOrientation(IContentAssistant.CONTEXT_INFO_ABOVE);
		assistant.setInformationControlCreator(getInformationControlCreator(sourceViewer));

		return assistant;
	}

	public IReconciler getReconciler(ISourceViewer sourceViewer) {
		RReconcilingStrategy strategy = new RReconcilingStrategy();
		strategy.setEditor(rEditor);

		MonoReconciler reconciler = new MonoReconciler(strategy, false);
		reconciler.setDelay(200);
		return reconciler;
	}

	public IQuickAssistAssistant getQuickAssistAssistant(ISourceViewer sourceViewer) {
		IQuickAssistAssistant quickAssist = new QuickAssistAssistant();
		quickAssist.setQuickAssistProcessor(new RAssistProcessor(rEditor));
		quickAssist.setInformationControlCreator(getInformationControlCreator(sourceViewer));
		return quickAssist;
	}

	@Override
	public ITextHover getTextHover(ISourceViewer sourceViewer, String contentType) {

		if (store.getBoolean("SHOW_INFOPOPUP")) {
			return new MarkdownTextHover();
		}
		return null;
	}

	public class MarkdownTextHover implements ITextHover, ITextHoverExtension2 {

		protected String help = "";

		// R Return information to be shown when the cursor is on the given
		// region
		@Override
		public Object getHoverInfo2(ITextViewer textViewer, IRegion hoverRegion) {

			int offset = hoverRegion.getOffset();
			int length = 0;
			int minusLength = 0;

			IDocument doc = textViewer.getDocument();

			while (true) {
				char c = 0;
				if (offset + length >= 0 && offset + length < doc.getLength()) {

					try {
						c = doc.getChar(offset + length);
					} catch (BadLocationException e) {

						e.printStackTrace();
					}

					if (Character.isLetter(c) == false && (c == '.') == false && Character.isDigit(c) == false)
						break;
					if (offset + length >= doc.getLength()-1) {
						
						break;
					}
					length++;
					
				} else {
					break;
				}
			}
			while (true) {
				char c = 0;
				if (offset + length >= 0 && offset + length < doc.getLength()) {

					try {
						c = doc.getChar(offset + minusLength);
					} catch (BadLocationException e) {

						e.printStackTrace();
					}

					if (Character.isLetter(c) == false && (c == '.') == false && Character.isDigit(c) == false)
						break;

					minusLength--;
					if (offset + minusLength <= 0) {
						break;
					}
				} else {
					break;
				}
			}
			final int wordOffset = offset + minusLength + 1;
			final int resultedLength = length - minusLength - 1;

			if (resultedLength > 0) {

				try {
					htmlHelpText = textViewer.getDocument().get(wordOffset, resultedLength);
				} catch (BadLocationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			RConnection c = REditor.getRserveConnection();
			if (c != null) {
				if (RState.isBusy() == false) {
					RState.setBusy(true);
					Display display = PlatformUI.getWorkbench().getDisplay();
					display.syncExec(new Runnable() {
						public void run() {

							try {

								c.eval("try(.bio7TempHtmlHelpFile <- paste(tempfile(), \".txt\", sep=\"\"),silent = T)").toString();
								c.eval("try(tools::Rd2txt(utils:::.getHelpFile(?" + htmlHelpText + "),.bio7TempHtmlHelpFile,package=\"tools\", stages=c(\"install\", \"render\"),outputEncoding = \"\"),silent = T)");
								String out = null;
								try {
									out = (String) c.eval("try(.bio7TempHtmlHelpFile)").asString();
								} catch (REXPMismatchException e) {

									e.printStackTrace();
								}

								String url = out.replace("\\", "/");

								byte[] encoded = null;
								try {
									encoded = Files.readAllBytes(Paths.get(url));
								} catch (IOException e) {

									e.printStackTrace();
								}
								help = new String(encoded, Charset.defaultCharset());
								// help = new Util().fileToString(url);
								help = help.replace("_\b", "");

							} catch (RserveException e1) {

								// e1.printStackTrace();
							}
						}
					});
					RState.setBusy(false);
				}
			}
			/*QuickFix operation!*/
			/*if (resultedLength > 0) {
				Display display2 = PlatformUI.getWorkbench().getDisplay();
				display2.syncExec(new Runnable() {

					public void run() {
						ITextOperationTarget operation = (ITextOperationTarget) rEditor.getAdapter(ITextOperationTarget.class);
				
						if (operation != null && operation.canDoOperation(ISourceViewer.QUICK_ASSIST)) {
                            //if(wordOffset+resultedLength<doc.getLength()-1){
							rEditor.selectAndReveal(wordOffset, resultedLength);

							operation.doOperation(ISourceViewer.QUICK_ASSIST);
                           // }

						}
				
					}
				});
			}*/

			return help;

		}

		String readFile(String path, Charset encoding) throws IOException {
			byte[] encoded = Files.readAllBytes(Paths.get(path));
			return new String(encoded, encoding);
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
			// if no text is selected then we return a region of the size 0 (a
			// single character)
			return new Region(offset, 0);
		}

	}

	@Override
	public IInformationControlCreator getInformationControlCreator(ISourceViewer sourceViewer) {
		return new IInformationControlCreator() {
			public IInformationControl createInformationControl(Shell parent) {

				/* SeeRHoverInfomrationControll for HTML implementation! */
				return new RDefaultInformationControl(parent, "Mouse Right-Click To Open Help Browser", null);
			}
		};
	}

}
