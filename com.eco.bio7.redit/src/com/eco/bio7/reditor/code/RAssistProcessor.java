package com.eco.bio7.reditor.code;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.quickassist.IQuickAssistInvocationContext;
import org.eclipse.jface.text.quickassist.IQuickAssistProcessor;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.internal.texteditor.spelling.NoCompletionsProposal;
import org.eclipse.ui.texteditor.MarkerAnnotation;
import org.eclipse.ui.texteditor.spelling.SpellingAnnotation;
import org.eclipse.ui.texteditor.spelling.SpellingProblem;

import com.eco.bio7.reditors.REditor;

public class RAssistProcessor implements IQuickAssistProcessor {

	private REditor rEditor;
	private static final ICompletionProposal[] fgNoSuggestionsProposal=  new ICompletionProposal[] { new NoCompletionsProposal() };


	public RAssistProcessor(REditor rEditor) {
		this.rEditor = rEditor;
	}

	public String getErrorMessage() {

		return null;
	}

	@Override
	public boolean canFix(Annotation annotation) {
		String text = null;
		boolean hasQuickFix = false;
		if (annotation instanceof MarkerAnnotation) {
			IMarker marker = ((MarkerAnnotation) annotation).getMarker();
			try {
				if (marker.exists()) {
					if (marker.getAttribute(IMarker.TEXT) != null) {
						/* Get String error code or text is 'NA'! */
						text = (String) marker.getAttribute(IMarker.TEXT);

					}
				}
			} catch (CoreException e) {

				e.printStackTrace();
			}
			if (text != null) {

				/*
				 * If text is 'NA' the marker comes from an Error without an QuickFix. Warn and
				 * error 17,19 are reserved as pure info messages. Implementation maybe later!
				 */
				if (text.equals("NA") || text.startsWith("Err12") || text.startsWith("Err17")
						|| text.startsWith("Err19") || text.startsWith("Warn17") || text.startsWith("Warn19")) {
					hasQuickFix = false;
				}

				else {
					/*
					 * here we have a QuickFix solution. Marked with the QuickFix icon instead of
					 * the triangle!
					 */
					hasQuickFix = true;
				}

			}

		} else if (annotation instanceof SpellingAnnotation && !annotation.isMarkedDeleted()) {
			hasQuickFix = true;
		}

		/* Text is not NA! */
		/*
		 * if (text != null && text.startsWith("Err")) { // errorCode = text; //
		 * System.out.println("true to fix"); return true; } Text is not NA! else if
		 * (text != null && text.startsWith("Warn")) {
		 * 
		 * return true; }
		 * 
		 * else { return false; }
		 */
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
		ICompletionProposal[] prop = null;
		ICompletionProposal[] propSpelling = null;
		RQuickFixSolutions solutions = new RQuickFixSolutions(viewer, rEditor);
		Iterator<?> it = viewer.getAnnotationModel().getAnnotationIterator();
		ArrayList<SpellingProblem> annotationList = new ArrayList<>();
		while (it.hasNext()) {
			Annotation annotation = (Annotation) it.next();

			if (annotation instanceof MarkerAnnotation) {
				MarkerAnnotation markAnn = (MarkerAnnotation) annotation;

				final IMarker marker = markAnn.getMarker();

				Integer startChar = marker.getAttribute(IMarker.CHAR_START, -1);
				Integer endChar = marker.getAttribute(IMarker.CHAR_END, -1);
				String tokenText = marker.getAttribute("TOKEN_TEXT", "");
				String replacementText = marker.getAttribute("REPLACEMENT_TEXT", "");

				if (startChar >=0 && endChar >= 0 && offset <= endChar && offset >= startChar) {
					try {
						String errorProposal = (String) (marker.getAttribute(IMarker.TEXT));
						prop = solutions.getProposals(errorProposal, offset, endChar, prop, tokenText, replacementText);
					} catch (CoreException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

			} else if (annotation instanceof SpellingAnnotation) {
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
		return ArrayUtils.addAll(prop,propSpelling);
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