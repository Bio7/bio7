package com.eco.bio7.markdownedit.editors;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.reconciler.MonoReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import com.eco.bio7.markdownedit.completion.RMarkdownCompletionProcessor;



public class MarkdownConfiguration extends SourceViewerConfiguration {
	private MarkdownDoubleClickStrategy doubleClickStrategy;
	private MarkdownTagScanner tagScanner;
	private MarkdownScanner scanner;
	private ColorManager colorManager;
	private MarkdownEditor markdownEditor;
	private RMarkdownCompletionProcessor processorRMarkdown;

	public MarkdownConfiguration(ColorManager colorManager, MarkdownEditor markdownEditor) {
		this.colorManager = colorManager;
		this.markdownEditor=markdownEditor;
	}
	public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
		return new String[] {
			IDocument.DEFAULT_CONTENT_TYPE,
			MarkdownPartitionScanner.MARKDOWN_COMMENT,
			MarkdownPartitionScanner.MARKDOWN_TAG };
	}
	public ITextDoubleClickStrategy getDoubleClickStrategy(
		ISourceViewer sourceViewer,
		String contentType) {
		if (doubleClickStrategy == null)
			doubleClickStrategy = new MarkdownDoubleClickStrategy();
		return doubleClickStrategy;
	}

	protected MarkdownScanner getMarkdownScanner() {
		if (scanner == null) {
			scanner = new MarkdownScanner(colorManager);
			scanner.setDefaultReturnToken(
				new Token(
					new TextAttribute(
						colorManager.getColor(IMarkdownColorConstants.DEFAULT))));
		}
		return scanner;
	}
	protected MarkdownTagScanner getMarkdownTagScanner() {
		if (tagScanner == null) {
			tagScanner = new MarkdownTagScanner(colorManager);
			tagScanner.setDefaultReturnToken(
				new Token(
					new TextAttribute(
						colorManager.getColor(IMarkdownColorConstants.TAG))));
		}
		return tagScanner;
	}
	public IReconciler getReconciler(ISourceViewer sourceViewer) {
		RMarkdownReconcilingStrategy strategy = new RMarkdownReconcilingStrategy();
		
		strategy.setEditor(markdownEditor);
       
		MonoReconciler reconciler = new MonoReconciler(strategy, false);
		reconciler.setDelay(200);
		return reconciler;
	}

	public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
		PresentationReconciler reconciler = new PresentationReconciler();

		DefaultDamagerRepairer dr =
			new DefaultDamagerRepairer(getMarkdownTagScanner());
		reconciler.setDamager(dr, MarkdownPartitionScanner.MARKDOWN_TAG);
		reconciler.setRepairer(dr, MarkdownPartitionScanner.MARKDOWN_TAG);

		dr = new DefaultDamagerRepairer(getMarkdownScanner());
		reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
		reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);

		NonRuleBasedDamagerRepairer ndr =
			new NonRuleBasedDamagerRepairer(
				new TextAttribute(
					colorManager.getColor(IMarkdownColorConstants.MARKDOWN_COMMENT)));
		reconciler.setDamager(ndr, MarkdownPartitionScanner.MARKDOWN_COMMENT);
		reconciler.setRepairer(ndr, MarkdownPartitionScanner.MARKDOWN_COMMENT);

		return reconciler;
	}
	public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {
		ContentAssistant assistant = new ContentAssistant();
		assistant.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));

		
		
        processorRMarkdown=new RMarkdownCompletionProcessor();
        assistant.setContentAssistProcessor(processorRMarkdown,IDocument.DEFAULT_CONTENT_TYPE );
        
		assistant.enableAutoActivation(true);
		assistant.setAutoActivationDelay(500);

		assistant.setContextInformationPopupOrientation(IContentAssistant.CONTEXT_INFO_ABOVE);
		assistant.setInformationControlCreator(getInformationControlCreator(sourceViewer));

		return assistant;
	}

}