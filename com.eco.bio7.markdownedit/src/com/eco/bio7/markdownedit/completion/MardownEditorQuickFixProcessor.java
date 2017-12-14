package com.eco.bio7.markdownedit.completion;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.quickassist.IQuickAssistInvocationContext;
import org.eclipse.jface.text.quickassist.IQuickAssistProcessor;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.internal.texteditor.spelling.NoCompletionsProposal;
import org.eclipse.ui.texteditor.spelling.SpellingAnnotation;
import org.eclipse.ui.texteditor.spelling.SpellingProblem;

import com.eco.bio7.markdownedit.editors.MarkdownEditor;


public class MardownEditorQuickFixProcessor implements IQuickAssistProcessor {

	private MarkdownEditor rEditor;
	private boolean hasQuickFix;
	private static final ICompletionProposal[] fgNoSuggestionsProposal=  new ICompletionProposal[] { new NoCompletionsProposal() };


	public MardownEditorQuickFixProcessor(MarkdownEditor rEditor) {
		this.rEditor = rEditor;
	}

	public String getErrorMessage() {

		return null;
	}

	@Override
	public boolean canFix(Annotation annotation) {
		 if (annotation instanceof SpellingAnnotation && !annotation.isMarkedDeleted()) {
			hasQuickFix = true;
		}

		
		return hasQuickFix;
	}

	@Override
	public boolean canAssist(IQuickAssistInvocationContext invocationContext) {

		return false;
	}

	public ICompletionProposal[] computeQuickAssistProposals(IQuickAssistInvocationContext invocationContext) {
		ISourceViewer viewer = invocationContext.getSourceViewer();
		int offset = invocationContext.getOffset();
		// String text = getCompleteText(viewer.getDocument(), offset);
		
		ICompletionProposal[] propSpelling = null;
		
		Iterator<?> it = viewer.getAnnotationModel().getAnnotationIterator();
		ArrayList<SpellingProblem> annotationList = new ArrayList<>();
		while (it.hasNext()) {
			Annotation annotation = (Annotation) it.next();

			 if (annotation instanceof SpellingAnnotation) {
				IAnnotationModel model= viewer.getAnnotationModel();
				if (canFix(annotation)) {
					Position pos = model.getPosition(annotation);
					if (isAtPosition(offset, pos)) {
						collectSpellingProblems(annotation, annotationList);
					}
				}
				SpellingProblem[] spellingProblems = annotationList.toArray(new SpellingProblem[annotationList.size()]);
				for (int i = 0; i < spellingProblems.length; i++) {
					propSpelling = spellingProblems[i].getProposals(invocationContext);
				}
			}

		}
		
		/*Combine quick assist proposals and spelling proposals!*/
		return propSpelling;
	}

	private boolean isAtPosition(int offset, Position pos) {
		return (pos != null) && (offset >= pos.getOffset() && offset <= (pos.getOffset() + pos.getLength()));
	}

	private void collectSpellingProblems(Annotation annotation, List<SpellingProblem> problems) {
		if (annotation instanceof SpellingAnnotation)
			problems.add(((SpellingAnnotation) annotation).getSpellingProblem());
	}

	/*
	 * private List<ICompletionProposal>
	 * computeProposals(IQuickAssistInvocationContext context, SpellingProblem[]
	 * spellingProblems) { List<ICompletionProposal> proposals = new ArrayList<>();
	 * for (int i = 0; i < spellingProblems.length; i++)
	 * proposals.addAll(Arrays.asList(spellingProblems[i].getProposals(context)));
	 * 
	 * return proposals; }
	 */

}