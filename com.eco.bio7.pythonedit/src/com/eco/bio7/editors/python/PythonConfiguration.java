package com.eco.bio7.editors.python;

import org.eclipse.swt.graphics.RGB;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.ITokenScanner;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;

import com.eco.bio7.pythonedit.PythonEditorPlugin;
import com.eco.bio7.reditor.preferences.template.PythonCompletionProcessor;




public class PythonConfiguration extends SourceViewerConfiguration {
	private ColorManager colorManager;
	/* The editor (the code analysis, completion, hover and quick fixes); null without */
	private org.eclipse.ui.texteditor.ITextEditor editor;

	public PythonConfiguration(ColorManager colorManager, org.eclipse.ui.texteditor.ITextEditor editor) {
		this(colorManager);
		this.editor = editor;
	}

	public PythonConfiguration(ColorManager colorManager) {
		this.colorManager = colorManager;
	}
	
	/*
	 * @see org.eclipse.jface.text.source.SourceViewerConfiguration#getConfiguredContentTypes(org.eclipse.jface.text.source.ISourceViewer)
	 * @since 3.2
	 */
	@Override
    public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
		/* The code (the default type) and the partitions: else the hovers, the double click and the indention only work in comments and strings */
		String[] partitions = PyPartitionScanner.getTypes();
		String[] types = new String[partitions.length + 1];
		types[0] = org.eclipse.jface.text.IDocument.DEFAULT_CONTENT_TYPE;
		System.arraycopy(partitions, 0, types, 1, partitions.length);
		return types;
	}
	
	/*
	 * @see org.eclipse.jface.text.source.SourceViewerConfiguration#getConfiguredDocumentPartitioning(org.eclipse.jface.text.source.ISourceViewer)
	 * @since 3.2
	 */
	@Override
	public String getConfiguredDocumentPartitioning(ISourceViewer sourceViewer) {
		return PyPartitionScanner.PYTHON_PARTITION_TYPE;
	}
	
	/*
	 * The scanner of the strings in triple quotes: the string token of the code
	 * scanner (its color is set by the preferences and the theme)
	 */
	private ITokenScanner stringScanner() {
		RuleBasedScanner scanner = new RuleBasedScanner();
		RuleBasedScanner code = com.eco.bio7.pythonedit.PythonEditorPlugin.getDefault().getScriptCodeScanner();
		if (code instanceof com.eco.bio7.pythoneditors.ScriptCodeScanner s && s.getString() != null) {
			scanner.setDefaultReturnToken(s.getString());
		} else {
			scanner.setDefaultReturnToken(new Token(new TextAttribute(colorManager.getColor(IPythonColorConstants.MULTILINE_STRING))));
		}
		return scanner;
	}

	private ITokenScanner createSimpleTokenScanner(RGB color) {
		RuleBasedScanner scanner = new RuleBasedScanner();
		scanner.setDefaultReturnToken(
			new Token(
				new TextAttribute(
					colorManager.getColor(color))));
		
		return scanner;
	}
	
	/*
	 * @see org.eclipse.jface.text.source.SourceViewerConfiguration#getPresentationReconciler(org.eclipse.jface.text.source.ISourceViewer)
	 * @since 3.2
	 */
	@Override
    public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
		PresentationReconciler reconciler = new PresentationReconciler();
		reconciler.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));
		
		
        
		/* Strings in triple quotes: the string color of the preferences (follows the theme and the color scheme) */
		DefaultDamagerRepairer dr = new DefaultDamagerRepairer(stringScanner());
		reconciler.setDamager(dr, PyPartitionScanner.PY_MULTILINE_STRING1);
		reconciler.setRepairer(dr, PyPartitionScanner.PY_MULTILINE_STRING1);
		reconciler.setDamager(dr, PyPartitionScanner.PY_MULTILINE_STRING2);
		reconciler.setRepairer(dr, PyPartitionScanner.PY_MULTILINE_STRING2);

		/*dr= new DefaultDamagerRepairer(createSimpleTokenScanner(IPythonColorConstants.DEFAULT));
		reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
		reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);*/

		/*dr= new DefaultDamagerRepairer(createSimpleTokenScanner(IPythonColorConstants.SINGLELINE_STRING));
		reconciler.setDamager(dr, PyPartitionScanner.PY_SINGLELINE_STRING);
		reconciler.setRepairer(dr, PyPartitionScanner.PY_SINGLELINE_STRING);

		dr= new DefaultDamagerRepairer(createSimpleTokenScanner(IPythonColorConstants.COMMENT));
		reconciler.setDamager(dr, PyPartitionScanner.PY_COMMENT);
		reconciler.setRepairer(dr, PyPartitionScanner.PY_COMMENT);
		
		dr= new DefaultDamagerRepairer(createSimpleTokenScanner(IPythonColorConstants.BACKQUOTE));
		reconciler.setDamager(dr, PyPartitionScanner.PY_BACKQUOTES);
		reconciler.setRepairer(dr, PyPartitionScanner.PY_BACKQUOTES);*/
		
		dr = new DefaultDamagerRepairer(
				PythonEditorPlugin.getDefault().getScriptCodeScanner());
		reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
		reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);
		
		
		return reconciler;
	}
	public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {
		ContentAssistant assistant = new ContentAssistant();
		assistant
				.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));

		/* The code completion (analysis, the running Python) and the templates */
		IContentAssistProcessor processor = editor == null ? new PythonCompletionProcessor()
				: new com.eco.bio7.pythonedit.analysis.PythonCodeCompletion(editor, new PythonCompletionProcessor());
		assistant.setContentAssistProcessor(processor,
				IDocument.DEFAULT_CONTENT_TYPE);
		org.eclipse.jface.preference.IPreferenceStore prefs = com.eco.bio7.pythonedit.analysis.PythonPreferences.store();
		assistant.enableAutoActivation(prefs.getBoolean(com.eco.bio7.pythonedit.analysis.PythonPreferences.TYPED_COMPLETION));
		assistant.setAutoActivationDelay(prefs.getInt(com.eco.bio7.pythonedit.analysis.PythonPreferences.ACTIVATION_DELAY));
		assistant.setShowEmptyList(false);

		assistant
				.setContextInformationPopupOrientation(IContentAssistant.CONTEXT_INFO_ABOVE);
		assistant
				.setInformationControlCreator(getInformationControlCreator(sourceViewer));

		return assistant;
	}

	/* The markers of the code analysis (a moment after a change) */
	@Override
	public org.eclipse.jface.text.reconciler.IReconciler getReconciler(ISourceViewer sourceViewer) {
		if (editor == null) {
			return super.getReconciler(sourceViewer);
		}
		org.eclipse.jface.text.reconciler.MonoReconciler reconciler = new org.eclipse.jface.text.reconciler.MonoReconciler(
				new com.eco.bio7.pythonedit.analysis.PythonReconcilingStrategy(editor), false);
		reconciler.setDelay(600);
		return reconciler;
	}

	/* The quick fixes (Ctrl+1) */
	@Override
	public org.eclipse.jface.text.quickassist.IQuickAssistAssistant getQuickAssistAssistant(ISourceViewer sourceViewer) {
		org.eclipse.jface.text.quickassist.QuickAssistAssistant assistant = new org.eclipse.jface.text.quickassist.QuickAssistAssistant();
		assistant.setQuickAssistProcessor(new com.eco.bio7.pythonedit.analysis.PythonQuickAssistProcessor());
		assistant.setInformationControlCreator(getInformationControlCreator(sourceViewer));
		return assistant;
	}

	/* The hover: markers with their quick fixes, the help and the values of the running Python */
	@Override
	public org.eclipse.jface.text.ITextHover getTextHover(ISourceViewer sourceViewer, String contentType) {
		return editor == null ? super.getTextHover(sourceViewer, contentType) : new com.eco.bio7.pythonedit.analysis.PythonTextHover(editor);
	}
	/* The message of a marker in the ruler (the icon left of the code) */
	@Override
	public org.eclipse.jface.text.source.IAnnotationHover getAnnotationHover(ISourceViewer sourceViewer) {
		return new org.eclipse.jface.text.source.DefaultAnnotationHover();
	}

	/* The message of a marker in the overview ruler (right of the code) */
	@Override
	public org.eclipse.jface.text.source.IAnnotationHover getOverviewRulerAnnotationHover(ISourceViewer sourceViewer) {
		return new org.eclipse.jface.text.source.DefaultAnnotationHover();
	}
}