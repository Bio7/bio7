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

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.internal.text.html.HTMLTextPresenter;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DefaultIndentLineAutoEditStrategy;
import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
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
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import com.eco.bio7.browser.BrowserView;
import com.eco.bio7.rbridge.RState;
import com.eco.bio7.reditor.Bio7REditorPlugin;
import com.eco.bio7.reditor.code.RAssistProcessor;
import com.eco.bio7.rpreferences.template.RCompletionProcessor;
import com.eco.bio7.rpreferences.template.CompletionProcessor;
import com.eco.bio7.util.Util;

public class RConfiguration extends TextSourceViewerConfiguration {

	private RDoubleClickStrategy doubleClickStrategy;

	private RColorManager colorManager;

	RColorProvider provider;

	private REditor rEditor;

	public static String htmlHelpText = "";

	public RConfiguration(RColorManager colorManager, REditor rEditor) {
		this.colorManager = colorManager;
		this.rEditor = rEditor;
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
		// return new String[] { IDocument.DEFAULT_CONTENT_TYPE,
		// RPartitionScanner.R_DOC, RPartitionScanner.R_MULTILINE_COMMENT };
		return new String[] { IDocument.DEFAULT_CONTENT_TYPE };
	}

	public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
		RColorProvider provider = Bio7REditorPlugin.getDefault().getRColorProvider();
		PresentationReconciler reconciler = new PresentationReconciler();
		reconciler.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));

		DefaultDamagerRepairer dr = new DefaultDamagerRepairer(Bio7REditorPlugin.getDefault().getRCodeScanner());
		reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
		reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);

		/*
		 * dr = new DefaultDamagerRepairer(Bio7REditorPlugin.getDefault().
		 * getRPartitionScanner()); reconciler.setDamager(dr,
		 * RPartitionScanner.R_MULTILINE_COMMENT); reconciler.setRepairer(dr,
		 * RPartitionScanner.R_MULTILINE_COMMENT);
		 * 
		 * dr = new DefaultDamagerRepairer(new SingleTokenScanner(new
		 * TextAttribute
		 * (provider.getColor(RColorProvider.MULTI_LINE_COMMENT))));
		 * reconciler.setDamager(dr, RPartitionScanner.R_MULTILINE_COMMENT);
		 * reconciler.setRepairer(dr, RPartitionScanner.R_MULTILINE_COMMENT);
		 */

		return reconciler;
	}

	public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {
		ContentAssistant assistant = new ContentAssistant();
		assistant.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));

		IContentAssistProcessor processor = new RCompletionProcessor(true);
		assistant.setContentAssistProcessor(processor, IDocument.DEFAULT_CONTENT_TYPE);

		/*
		 * IContentAssistProcessor cap =new CompletionProcessor();
		 * assistant.setContentAssistProcessor(cap,IDocument.DEFAULT_CATEGORY);
		 */

		// assistant.setContentAssistProcessor(new JavaCompletionProcessor(),
		// IDocument.DEFAULT_CATEGORY);

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
		quickAssist.setQuickAssistProcessor(new RAssistProcessor());
		quickAssist.setInformationControlCreator(getInformationControlCreator(sourceViewer));
		return quickAssist;
	}

	@Override
	public ITextHover getTextHover(ISourceViewer sourceViewer, String contentType) {

		return new MarkdownTextHover();
	}

	public class MarkdownTextHover implements ITextHover, ITextHoverExtension2 {

		protected String help = "";

		// return information to be shown when the cursor is on the given region
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

					length++;
					if (offset + length >= doc.getLength()) {
						break;
					}
				}
				else{
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
				}
				else{
					break;
				}
			}
			final int wordOffset = offset + minusLength + 1;
			final int resultedLength = length - minusLength - 1;

			if (resultedLength > 0) {

				/*
				 * Display display = PlatformUI.getWorkbench().getDisplay();
				 * display.syncExec(new Runnable() {
				 * 
				 * public void run() { textViewer.setSelectedRange(wordOffset,
				 * resultedLength); } });
				 */

				try {
					htmlHelpText = textViewer.getDocument().get(wordOffset, resultedLength);
				} catch (BadLocationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			/*
			 * Display display = PlatformUI.getWorkbench().getDisplay();
			 * display.syncExec(new Runnable() { public void run() { try {
			 * IWorkbenchPage page = PlatformUI.getWorkbench()
			 * .getActiveWorkbenchWindow().getActivePage();
			 * page.showView("com.eco.bio7.browser.Browser"); } catch
			 * (PartInitException e) { // TODO Auto-generated catch block
			 * e.printStackTrace(); }
			 * 
			 * } });
			 */

			RConnection c = REditor.getRserveConnection();
			if (c != null) {
				if (RState.isBusy() == false) {
					RState.setBusy(true);
					Display display = PlatformUI.getWorkbench().getDisplay();
					display.syncExec(new Runnable() {
						public void run() {

							try {

								c.eval("try(outfile <- paste(tempfile(), \".txt\", sep=\"\"),silent = T)").toString();
								c.eval("try(tools::Rd2txt(utils:::.getHelpFile(?" + htmlHelpText + "),outfile,package=\"tools\", stages=c(\"install\", \"render\"),outputEncoding = \"\"),silent = T)");
								String out = null;
								try {
									out = (String) c.eval("try(outfile)").asString();
								} catch (REXPMismatchException e) {

									e.printStackTrace();
								}

								String url = out.replace("\\", "/");

								// StandardCharsets.UTF_8
								/*
								 * try { List<String> lines =
								 * Files.readAllLines(Paths.get(url),
								 * Charset.defaultCharset()); } catch
								 * (IOException e) { // TODO Auto-generated
								 * catch block e.printStackTrace(); }
								 */

								byte[] encoded = null;
								try {
									encoded = Files.readAllBytes(Paths.get(url));
								} catch (IOException e) {
									// TODO Auto-generated catch block
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

			/*
			 * Job job = new Job("Html help") {
			 * 
			 * 
			 * @Override protected IStatus run(IProgressMonitor monitor) {
			 * monitor.beginTask("Help ...", IProgressMonitor.UNKNOWN);
			 * 
			 * RConnection c = REditor.getRserveConnection(); if (c != null) {
			 * if (RState.isBusy() == false) { RState.setBusy(true);
			 * 
			 * try {
			 * 
			 * c.eval(
			 * "try(outfile <- paste(tempfile(), \".txt\", sep=\"\"),silent = T)"
			 * ).toString(); c.eval("try(tools::Rd2txt(utils:::.getHelpFile(?" +
			 * htmlHelpText +
			 * "),outfile,package=\"tools\", stages=c(\"install\", \"render\"),outputEncoding = \"\"),silent = T)"
			 * ); String out = null; try { out = (String)
			 * c.eval("try(outfile)").asString(); } catch (REXPMismatchException
			 * e) {
			 * 
			 * e.printStackTrace(); }
			 * 
			 * // String pattern = "file:///" + out; String url =
			 * out.replace("\\", "/");
			 * 
			 * // StandardCharsets.UTF_8
			 * 
			 * try { List<String> lines = Files.readAllLines(Paths.get(url),
			 * Charset.defaultCharset()); } catch (IOException e) { // TODO
			 * Auto-generated catch block e.printStackTrace(); }
			 * 
			 * 
			 * byte[] encoded = null; try { encoded =
			 * Files.readAllBytes(Paths.get(url)); } catch (IOException e) { //
			 * TODO Auto-generated catch block e.printStackTrace(); } help = new
			 * String(encoded, Charset.defaultCharset()); // help = new
			 * Util().fileToString(url); help = help.replace("_\b", "");
			 * 
			 * } catch (RserveException e1) {
			 * 
			 * // e1.printStackTrace(); }
			 * 
			 * } }
			 * 
			 * monitor.done(); return Status.OK_STATUS; }
			 * 
			 * }; job.addJobChangeListener(new JobChangeAdapter() { public void
			 * done(IJobChangeEvent event) { if (event.getResult().isOK()) {
			 * 
			 * RState.setBusy(false); } else {
			 * 
			 * } } }); // job.setSystem(true); job.schedule();
			 */
			/*
			 * Document document = Jsoup.parse(help, ""); Element body =
			 * document.body();
			 * 
			 * help=buildStringFromNode(body).toString();
			 */

			// apiText = "R Code!:" + hoverRegion.getOffset() + " length:" +
			// hoverRegion.getLength();//
			// textViewer.getDocument().getPartition(hoverRegion.getOffset()).toString()+" "+textViewer.getDocument().getPartition(hoverRegion.getLength()).toString();;

			return help;

		}

		String readFile(String path, Charset encoding) throws IOException {
			byte[] encoded = Files.readAllBytes(Paths.get(path));
			return new String(encoded, encoding);
		}

		/*
		 * private String getTextContent(Element elem) { String text =
		 * elem.getContent().toString();
		 * 
		 * final List<Element> children = elem.getChildElements(); for (Element
		 * child : children) { text = text.replace(child.toString(), ""); }
		 * return text; }
		 */

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
