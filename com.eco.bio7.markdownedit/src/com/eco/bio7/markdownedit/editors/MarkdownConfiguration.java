package com.eco.bio7.markdownedit.editors;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
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
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;
import org.eclipse.jface.text.reconciler.MonoReconciler;
import org.eclipse.jface.text.reconciler.Reconciler;
import org.eclipse.jface.text.rules.BufferedRuleBasedScanner;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.source.IAnnotationHover;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;
import org.eclipse.ui.internal.editors.text.EditorsPlugin;
import org.eclipse.ui.texteditor.spelling.SpellingService;
import com.eco.bio7.markdownedit.Activator;
import com.eco.bio7.markdownedit.completion.MardownEditorQuickFixProcessor;
import com.eco.bio7.markdownedit.completion.RMarkdownCompletionProcessor;
import com.eco.bio7.markdownedit.hoover.RMarkdownEditorTextHover;

public class MarkdownConfiguration extends TextSourceViewerConfiguration {
	private MarkdownDoubleClickStrategy doubleClickStrategy;
	private MarkdownTagScanner tagScanner;
	private MarkdownScanner scanner;
	private ColorManager colorManager;
	private MarkdownEditor markdownEditor;
	private RMarkdownCompletionProcessor processorRMarkdown;
	private IPreferenceStore store;
	public SingleTokenScanner comment;
	public SingleTokenScanner yaml;
	private MardownEditorQuickFixProcessor assist;

	public MarkdownConfiguration(ColorManager colorManager, MarkdownEditor markdownEditor, IPreferenceStore preferenceStore) {
		super(preferenceStore);
		this.colorManager = colorManager;
		this.markdownEditor = markdownEditor;
		store = Activator.getDefault().getPreferenceStore();
	}

	public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
		return new String[] { IDocument.DEFAULT_CONTENT_TYPE, MarkdownPartitionScanner.MARKDOWN_R_CHUNK, MarkdownPartitionScanner.MARKDOWN_TAG };
	}

	public ITextDoubleClickStrategy getDoubleClickStrategy(ISourceViewer sourceViewer, String contentType) {
		if (doubleClickStrategy == null)
			doubleClickStrategy = new MarkdownDoubleClickStrategy();
		return doubleClickStrategy;
	}

	public MarkdownScanner getMarkdownScanner() {
		if (scanner == null) {
			scanner = new MarkdownScanner(colorManager);
			// scanner.setDefaultReturnToken(new Token(new
			// TextAttribute(colorManager.getColor(IMarkdownColorConstants.DEFAULT))));
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

	public ITextHover getTextHover(ISourceViewer sourceViewer, String contentType) {
		return new RMarkdownEditorTextHover(markdownEditor,assist);
	}

	/*private final class TextHover extends DefaultTextHover implements ITextHoverExtension {
		public TextHover(ISourceViewer sourceViewer) {
			super(sourceViewer);
		}

		
		 * @see org.eclipse.jface.text.ITextHoverExtension#getHoverControlCreator()
		 
		public IInformationControlCreator getHoverControlCreator() {
			return new IInformationControlCreator() {
				public IInformationControl createInformationControl(Shell parent) {
					return new DefaultInformationControl(parent, EditorsUI.getTooltipAffordanceString());
				}
			};
		}
	}*/

	public IReconciler getReconciler(ISourceViewer sourceViewer) {

		/*
		 * if (store == null || !store.getBoolean(SpellingService.PREFERENCE_SPELLING_ENABLED)) return null;
		 */

		SpellingService spellingService = EditorsUI.getSpellingService();
		if (spellingService.getActiveSpellingEngineDescriptor(store) == null)
			return null;

		// RMarkdownReconcilingStrategy strategy2 = new RMarkdownReconcilingStrategy();
		IReconcilingStrategy strategy = new RMarkdownSpellingReconcileStrategy(sourceViewer, spellingService);
		((RMarkdownSpellingReconcileStrategy) strategy).setEditor(markdownEditor);
		MonoReconciler reconciler = new MonoReconciler(strategy, false);
		// reconciler.setDelay(500);
		// return reconciler;

		// MarkdownSpellingReconcileStrategy strategySpelling= new
		// MarkdownSpellingReconcileStrategy(sourceViewer,EditorsUI.getSpellingService());
		// strategy.setEditor(markdownEditor);

		// MonoReconciler reconciler = new MonoReconciler(strategy, false);
		Reconciler reconciler2 = new Reconciler();
		reconciler2.setDocumentPartitioning(IDocumentExtension3.DEFAULT_PARTITIONING);
		// reconciler2.setReconcilingStrategy(strategy,IDocument.DEFAULT_CONTENT_TYPE);
		// reconciler2.setReconcilingStrategy(strategy,MarkdownPartitionScanner.MARKDOWN_TAG);
		// reconciler2.setReconcilingStrategy(strategySpelling,MarkdownPartitionScanner.MARKDOWN_TAG);
		// reconciler2.setIsAllowedToModifyDocument(true);
		// reconciler2.setIsIncrementalReconciler(true);
		/* Set the reconcile time from the preferences! */
		int timeReconcile = 500;
		try {
			timeReconcile = Integer.parseInt(store.getString("RECONCILE_MARKDOWN_TIME"));
		} catch (NumberFormatException e) {
			System.out.println("Please add a valid number to the Compile Markdown interval preference\nDefault value is used (500ms)!");
			//e.printStackTrace();
		}
		reconciler.setDelay(timeReconcile);
		// strategy.initialReconcile();
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

		/*
		 * We create the special token with a default style from the preferences!
		 */
		comment = new SingleTokenScanner(new TextAttribute(new Color(Display.getDefault(), rgbkey2), null, isBold("BOLD_COLOURKEY2")));
		DefaultDamagerRepairer ndr = new DefaultDamagerRepairer(comment);
		reconciler.setDamager(ndr, MarkdownPartitionScanner.MARKDOWN_R_CHUNK);
		reconciler.setRepairer(ndr, MarkdownPartitionScanner.MARKDOWN_R_CHUNK);

		/*
		 * NonRuleBasedDamagerRepairer ndr = new NonRuleBasedDamagerRepairer(new TextAttribute(new Color(Display.getDefault(), rgbkey2), null, isBold("BOLD_COLOURKEY2"))); reconciler.setDamager(ndr,
		 * MarkdownPartitionScanner.MARKDOWN_COMMENT); reconciler.setRepairer(ndr, MarkdownPartitionScanner.MARKDOWN_COMMENT);
		 */

		/*
		 * We create the special token with a default style from the preferences!
		 */
		yaml = new SingleTokenScanner(new TextAttribute(new Color(Display.getDefault(), rgbkey3), null, isBold("BOLD_COLOURKEY3")));
		DefaultDamagerRepairer ndrcomment = new DefaultDamagerRepairer(yaml);
		// reconciler.setDamager(ndrcomment, MarkdownPartitionScanner.YAML_HEADER);
		// reconciler.setRepairer(ndrcomment, MarkdownPartitionScanner.YAML_HEADER);

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
	
	@Override
	public IAnnotationHover getAnnotationHover(ISourceViewer sourceViewer) {
		// TODO Auto-generated method stub
		return super.getAnnotationHover(sourceViewer);
		// return new AnnotationHover();
	}
	

	public IQuickAssistAssistant getQuickAssistAssistant(ISourceViewer sourceViewer) {
		/*
		 * if (store == null || !store.getBoolean(SpellingService.PREFERENCE_SPELLING_ENABLED)) return null;
		 */

		QuickAssistAssistant assistant = new QuickAssistAssistant();
		assist=new MardownEditorQuickFixProcessor(markdownEditor);
		assistant.setQuickAssistProcessor(assist);
		assistant.setRestoreCompletionProposalSize(EditorsPlugin.getDefault().getDialogSettingsSection("quick_assist_proposal_size")); //$NON-NLS-1$
		assistant.setInformationControlCreator(getQuickAssistAssistantInformationControlCreator());

		return assistant;
	}

	private IInformationControlCreator getQuickAssistAssistantInformationControlCreator() {
		return new IInformationControlCreator() {
			public IInformationControl createInformationControl(Shell parent) {
				
				//return new RMarkdownSimpleDefaultInformationControl(parent);
				return new DefaultInformationControl(parent, EditorsPlugin.getAdditionalInfoAffordanceString());
			}

		};
	}

}