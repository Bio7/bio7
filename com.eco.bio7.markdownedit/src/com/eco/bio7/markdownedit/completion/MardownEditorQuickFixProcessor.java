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

	/* The codes of the R code analysis with quick fixes in R chunks */
	private static final java.util.Set<String> R_FIXES = java.util.Set.of("Err26", "Err27", "Err28", "Err29", "Err30", "Warn16", "Warn24", "Warn25", "Warn26", "Warn27", "Warn28");

	@Override
	public boolean canFix(Annotation annotation) {
		 if (annotation instanceof SpellingAnnotation && !annotation.isMarkedDeleted()) {
			hasQuickFix = true;
		}
		/* An R error with a quick fix (e.g., an unclosed bracket) */
		if (annotation instanceof org.eclipse.ui.texteditor.MarkerAnnotation m && !annotation.isMarkedDeleted()
				&& R_FIXES.contains(m.getMarker().getAttribute(org.eclipse.core.resources.IMarker.TEXT, ""))) {
			return true;
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
		java.util.List<ICompletionProposal> rProposals = new ArrayList<>();
		
		Iterator<?> it = viewer.getAnnotationModel().getAnnotationIterator();
		ArrayList<SpellingProblem> annotationList = new ArrayList<>();
		while (it.hasNext()) {
			Annotation annotation = (Annotation) it.next();

			/* The quick fixes of the R chunks (the markers of the R code analysis) */
			if (annotation instanceof org.eclipse.ui.texteditor.MarkerAnnotation m) {
				org.eclipse.core.resources.IMarker marker = m.getMarker();
				int start = marker.getAttribute(org.eclipse.core.resources.IMarker.CHAR_START, -1);
				int end = marker.getAttribute(org.eclipse.core.resources.IMarker.CHAR_END, -1);
				String code = marker.getAttribute(org.eclipse.core.resources.IMarker.TEXT, "");
				if (start >= 0 && offset >= start && offset <= end && R_FIXES.contains(code)) {
					ICompletionProposal[] p = com.eco.bio7.reditor.code.RQuickFixSolutions.documentProposals(viewer,
							com.eco.bio7.markdownedit.chunks.RChunkCode.virtualCode(viewer.getDocument()), code, marker.getAttribute("TOKEN_TEXT", ""),
							marker.getAttribute("REPLACEMENT_TEXT", ""));
					if (p != null) {
						rProposals.addAll(java.util.Arrays.asList(p));
					}
				}
				continue;
			}
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
		if (!rProposals.isEmpty()) {
			if (propSpelling != null) {
				rProposals.addAll(java.util.Arrays.asList(propSpelling));
			}
			return rProposals.toArray(new ICompletionProposal[0]);
		}
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