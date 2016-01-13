package com.eco.bio7.reditor.code;

import java.util.Iterator;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.quickassist.IQuickAssistInvocationContext;
import org.eclipse.jface.text.quickassist.IQuickAssistProcessor;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.texteditor.MarkerAnnotation;

public class RAssistProcessor implements IQuickAssistProcessor {

	public String getErrorMessage() {

		return null;
	}

	@Override
	public boolean canFix(Annotation annotation) {
		String text = null;
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
		}

		/* Text is not NA! */
		if (text != null && text.startsWith("Err")) {
			// errorCode = text;
			//System.out.println("true to fix");
			return true;
		}
		/* Text is not NA! */
		else if (text != null && text.startsWith("Warn")) {

			return true;
		}

		else {
			return false;
		}

	}

	@Override
	public boolean canAssist(IQuickAssistInvocationContext invocationContext) {

		return false;
	}

	@Override
	public ICompletionProposal[] computeQuickAssistProposals(IQuickAssistInvocationContext invocationContext) {
		ISourceViewer viewer = invocationContext.getSourceViewer();
		int offset = invocationContext.getOffset();
		// String text = getCompleteText(viewer.getDocument(), offset);
		ICompletionProposal[] prop = null;
		RQuickFixSolutions solutions = new RQuickFixSolutions();
		Iterator<?> it = viewer.getAnnotationModel().getAnnotationIterator();

		while (it.hasNext()) {
			Annotation annotation = (Annotation) it.next();

			if (annotation instanceof MarkerAnnotation) {
				MarkerAnnotation markAnn = (MarkerAnnotation) annotation;

				final IMarker marker = markAnn.getMarker();

				Integer startChar = marker.getAttribute(IMarker.CHAR_START, -1);
				Integer endChar = marker.getAttribute(IMarker.CHAR_END, -1);
                String tokenText=marker.getAttribute("TOKEN_TEXT","");
				if (startChar > 0 && endChar > 0 && offset <= endChar && offset >= startChar) {
					try {
						String errorProposal = (String) (marker.getAttribute(IMarker.TEXT));
						prop = solutions.getProposals(errorProposal, offset, endChar, prop,tokenText);
					} catch (CoreException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

			}
		}

		return prop;

	}

	/*
	 * private String getCompleteText(IDocument document, int currentOffset) {
	 * 
	 * int count = currentOffset;
	 * 
	 * String tobeCompleteText = "";
	 * 
	 * try {
	 * 
	 * while (--count >= 0) {
	 * 
	 * char c = ' ';
	 * 
	 * c = document.getChar(count);
	 * 
	 * if (isWhiteSpace(c)) {
	 * 
	 * break;
	 * 
	 * }
	 * 
	 * }
	 * 
	 * tobeCompleteText = document.get(count + 1, currentOffset - (count + 1));
	 * 
	 * } catch (Exception e) {
	 * 
	 * e.printStackTrace();
	 * 
	 * }
	 * 
	 * return tobeCompleteText;
	 * 
	 * }
	 * 
	 * private boolean isWhiteSpace(char c) {
	 * 
	 * if (c == '\n' || c == '\t' || c == ' ')
	 * 
	 * return true;
	 * 
	 * return false;
	 * 
	 * }
	 */

}