package com.eco.bio7.browser.editor;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.reconciler.MonoReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;

import com.eco.bio7.util.Util;

public class XMLConfiguration extends SourceViewerConfiguration {
	private XMLDoubleClickStrategy doubleClickStrategy;
	private XMLTagScanner tagScanner;
	private XMLScanner scanner;
	private ColorManager colorManager;
	private XMLEditor xmlEditor;

	public XMLConfiguration(ColorManager colorManager, XMLEditor xmlEditor) {
		this.colorManager = colorManager;
		this.xmlEditor = xmlEditor;
	}

	public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
		return new String[] { IDocument.DEFAULT_CONTENT_TYPE, XMLPartitionScanner.XML_COMMENT,
				XMLPartitionScanner.XML_TAG };
	}

	public ITextDoubleClickStrategy getDoubleClickStrategy(ISourceViewer sourceViewer, String contentType) {
		if (doubleClickStrategy == null)
			doubleClickStrategy = new XMLDoubleClickStrategy();
		return doubleClickStrategy;
	}

	protected XMLScanner getXMLScanner() {
		if (scanner == null) {
			scanner = new XMLScanner(colorManager);
			if (Util.isThemeBlack()) {
				scanner.setDefaultReturnToken(
						new Token(new TextAttribute(colorManager.getColor(IXMLColorConstants.DEFAULT_DARK))));
			} else {
				scanner.setDefaultReturnToken(
						new Token(new TextAttribute(colorManager.getColor(IXMLColorConstants.DEFAULT))));
			}
		}
		return scanner;
	}

	protected XMLTagScanner getXMLTagScanner() {
		if (tagScanner == null) {
			tagScanner = new XMLTagScanner(colorManager);
			if (Util.isThemeBlack()) {
				tagScanner.setDefaultReturnToken(
						new Token(new TextAttribute(colorManager.getColor(IXMLColorConstants.TAG_DARK))));
			} else {
				tagScanner.setDefaultReturnToken(
						new Token(new TextAttribute(colorManager.getColor(IXMLColorConstants.TAG))));
			}
		}
		return tagScanner;
	}

	public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
		PresentationReconciler reconciler = new PresentationReconciler();

		DefaultDamagerRepairer dr = new DefaultDamagerRepairer(getXMLTagScanner());
		reconciler.setDamager(dr, XMLPartitionScanner.XML_TAG);
		reconciler.setRepairer(dr, XMLPartitionScanner.XML_TAG);

		dr = new DefaultDamagerRepairer(getXMLScanner());
		reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
		reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);

		NonRuleBasedDamagerRepairer ndr = new NonRuleBasedDamagerRepairer(
				new TextAttribute(colorManager.getColor(IXMLColorConstants.XML_COMMENT)));
		reconciler.setDamager(ndr, XMLPartitionScanner.XML_COMMENT);
		reconciler.setRepairer(ndr, XMLPartitionScanner.XML_COMMENT);

		return reconciler;
	}

	@Override
	public IReconciler getReconciler(ISourceViewer sourceViewer) {

		MonoReconciler reconciler = new MonoReconciler(new XmlReconcilingStrategy(xmlEditor), false);

		reconciler.setDelay(XmlReconcilingStrategy.DELAY);
		// reconciler.addReconcilingParticipant(sourceViewer);
		return reconciler;
	}

}