package com.eco.bio7.markdownedit.editors;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.reconciler.MonoReconciler;
import org.eclipse.jface.text.rules.BufferedRuleBasedScanner;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import com.eco.bio7.markdownedit.Activator;
import com.eco.bio7.markdownedit.completion.RMarkdownCompletionProcessor;

public class MarkdownConfiguration extends SourceViewerConfiguration {
	private MarkdownDoubleClickStrategy doubleClickStrategy;
	private MarkdownTagScanner tagScanner;
	private MarkdownScanner scanner;
	private ColorManager colorManager;
	private MarkdownEditor markdownEditor;
	private RMarkdownCompletionProcessor processorRMarkdown;
	private IPreferenceStore store;
	public SingleTokenScanner comment;
	public SingleTokenScanner yaml;

	public MarkdownConfiguration(ColorManager colorManager, MarkdownEditor markdownEditor) {
		this.colorManager = colorManager;
		this.markdownEditor = markdownEditor;
		store = Activator.getDefault().getPreferenceStore();
	}

	public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
		return new String[] { IDocument.DEFAULT_CONTENT_TYPE, MarkdownPartitionScanner.MARKDOWN_COMMENT, MarkdownPartitionScanner.MARKDOWN_TAG, MarkdownPartitionScanner.YAML_HEADER };
	}

	public ITextDoubleClickStrategy getDoubleClickStrategy(ISourceViewer sourceViewer, String contentType) {
		if (doubleClickStrategy == null)
			doubleClickStrategy = new MarkdownDoubleClickStrategy();
		return doubleClickStrategy;
	}

	protected MarkdownScanner getMarkdownScanner() {
		if (scanner == null) {
			scanner = new MarkdownScanner(colorManager);
			scanner.setDefaultReturnToken(new Token(new TextAttribute(colorManager.getColor(IMarkdownColorConstants.DEFAULT))));
		}
		return scanner;
	}

	protected MarkdownTagScanner getMarkdownTagScanner() {
		if (tagScanner == null) {
			tagScanner = new MarkdownTagScanner(colorManager);
			tagScanner.setDefaultReturnToken(new Token(new TextAttribute(colorManager.getColor(IMarkdownColorConstants.TAG))));
		}
		return tagScanner;
	}

	public IReconciler getReconciler(ISourceViewer sourceViewer) {
		RMarkdownReconcilingStrategy strategy = new RMarkdownReconcilingStrategy();

		strategy.setEditor(markdownEditor);

		MonoReconciler reconciler = new MonoReconciler(strategy, false);
		/*Set the reconcile time from the preferences!*/
		int timeReconcile = store.getInt("RECONCILE_MARKDOWN_TIME");
		reconciler.setDelay(timeReconcile);
		return reconciler;
	}

	public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {

		RGB rgbkey2 = PreferenceConverter.getColor(store, "colourkey2");
		RGB rgbkey3 = PreferenceConverter.getColor(store, "colourkey3");
		PresentationReconciler reconciler = new PresentationReconciler();

		DefaultDamagerRepairer dr = new DefaultDamagerRepairer(getMarkdownTagScanner());
		reconciler.setDamager(dr, MarkdownPartitionScanner.MARKDOWN_TAG);
		reconciler.setRepairer(dr, MarkdownPartitionScanner.MARKDOWN_TAG);

		DefaultDamagerRepairer dr2 = new DefaultDamagerRepairer(getMarkdownScanner());
		reconciler.setDamager(dr2, IDocument.DEFAULT_CONTENT_TYPE);
		reconciler.setRepairer(dr2, IDocument.DEFAULT_CONTENT_TYPE);

		/* We create the special token with a default style from the preferences! */
		comment = new SingleTokenScanner(new TextAttribute(new Color(Display.getDefault(), rgbkey2), null, isBold("BOLD_COLOURKEY2")));
		DefaultDamagerRepairer ndr = new DefaultDamagerRepairer(comment);
		reconciler.setDamager(ndr, MarkdownPartitionScanner.MARKDOWN_COMMENT);
		reconciler.setRepairer(ndr, MarkdownPartitionScanner.MARKDOWN_COMMENT);

		/*
		 * NonRuleBasedDamagerRepairer ndr = new NonRuleBasedDamagerRepairer(new TextAttribute(new Color(Display.getDefault(), rgbkey2), null, isBold("BOLD_COLOURKEY2"))); reconciler.setDamager(ndr,
		 * MarkdownPartitionScanner.MARKDOWN_COMMENT); reconciler.setRepairer(ndr, MarkdownPartitionScanner.MARKDOWN_COMMENT);
		 */

		/* We create the special token with a default style from the preferences! */
		yaml = new SingleTokenScanner(new TextAttribute(new Color(Display.getDefault(), rgbkey3), null, isBold("BOLD_COLOURKEY3")));
		DefaultDamagerRepairer ndrcomment = new DefaultDamagerRepairer(yaml);
		reconciler.setDamager(ndrcomment, MarkdownPartitionScanner.YAML_HEADER);
		reconciler.setRepairer(ndrcomment, MarkdownPartitionScanner.YAML_HEADER);

		/*
		 * NonRuleBasedDamagerRepairer ndr2 = new NonRuleBasedDamagerRepairer(new TextAttribute(new Color(Display.getDefault(), rgbkey2), null, isBold("BOLD_COLOURKEY2"))); reconciler.setDamager(ndr2,
		 * MarkdownPartitionScanner.YAML_HEADER); reconciler.setRepairer(ndr2, MarkdownPartitionScanner.YAML_HEADER);
		 */

		return reconciler;
	}

	private int isBold(String string2) {
		int style = 0;

		if (store.getBoolean(string2)) {
			style = 1;
		}

		return style;
	}

	public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {
		ContentAssistant assistant = new ContentAssistant();
		assistant.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));

		processorRMarkdown = new RMarkdownCompletionProcessor();
		assistant.setContentAssistProcessor(processorRMarkdown, IDocument.DEFAULT_CONTENT_TYPE);

		assistant.enableAutoActivation(true);
		assistant.setAutoActivationDelay(500);

		assistant.setContextInformationPopupOrientation(IContentAssistant.CONTEXT_INFO_ABOVE);
		assistant.setInformationControlCreator(getInformationControlCreator(sourceViewer));

		return assistant;
	}

	public class SingleTokenScanner extends BufferedRuleBasedScanner {
		public Token att;

		public SingleTokenScanner(TextAttribute attribute) {
			att = new Token(attribute);

			setDefaultReturnToken(att);

		}

		public Token getToken() {
			return att;
		}

	}

}