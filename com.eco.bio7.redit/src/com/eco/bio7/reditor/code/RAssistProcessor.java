package com.eco.bio7.reditor.code;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.quickassist.IQuickAssistInvocationContext;
import org.eclipse.jface.text.quickassist.IQuickAssistProcessor;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.texteditor.MarkerAnnotation;

public class RAssistProcessor implements IQuickAssistProcessor {

	// @Override
	public String getErrorMessage() {
		// TODO Auto-generated method stub
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
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		// String text=annotation.getText();
		/* Text is not NA! */
		if (text != null && text.startsWith("Err")) {
			// errorCode = text;
			return true;
		}

		else if (text != null && text.startsWith("Warn")) {
			// errorCode = text;
			return true;
		}

		else {
			return false;
		}

	}

	@Override
	public boolean canAssist(IQuickAssistInvocationContext invocationContext) {

		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public ICompletionProposal[] computeQuickAssistProposals(IQuickAssistInvocationContext invocationContext) {
		ISourceViewer viewer = invocationContext.getSourceViewer();
		int offset = invocationContext.getOffset();
		String text = getCompleteText(viewer.getDocument(), offset);
		ICompletionProposal[] prop = null;

		Iterator<?> it = viewer.getAnnotationModel().getAnnotationIterator();

		while (it.hasNext()) {
			Annotation annotation = (Annotation) it.next();

			if (annotation instanceof MarkerAnnotation) {
				MarkerAnnotation markAnn = (MarkerAnnotation) annotation;

				final IMarker marker = markAnn.getMarker();

				Integer startChar = marker.getAttribute(IMarker.CHAR_START, -1);
				Integer endChar = marker.getAttribute(IMarker.CHAR_END, -1);

				if (startChar > 0 && endChar > 0 && offset <= endChar && offset >= startChar) {
					try {
						String errorProposal = (String) (marker.getAttribute(IMarker.TEXT));
						prop = theProposals(errorProposal, offset, text, prop);
					} catch (CoreException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

			}
		}

		return prop;

	}

	private ICompletionProposal[] theProposals(String errorCode, int offset, String text, ICompletionProposal[] prop) {
		if (errorCode != null) {
			switch (errorCode) {
			case "Err1":
				prop = new ICompletionProposal[] {

						new QuickFixCompletionProposal("Remove ')'", offset, text.length(), "", 1) };
				break;
			case "Err2":

				break;
			case "Err3":
				prop = new ICompletionProposal[] {

						new QuickFixCompletionProposal("Remove ')'", offset, text.length(), "", 1) };
				break;
			case "Err4":

				break;
			case "Err5":
				prop = new ICompletionProposal[] {

						new QuickFixCompletionProposal("Remove ')'", offset, text.length(), "", 1) };
				break;
			case "Err6":

				break;
			case "Err7":
				prop = new ICompletionProposal[] {

						new QuickFixCompletionProposal("Remove ')'", offset, text.length(), "", 1) };
				break;
			case "Err8":
				prop = new ICompletionProposal[] {

						new QuickFixCompletionProposal("Remove ']'", offset, text.length(), "", 1) };

				break;
			case "Err9":
				prop = new ICompletionProposal[] {

						new QuickFixCompletionProposal("Remove ']'", offset, text.length(), "", 1) };
				break;
			case "Err10":

				break;
			case "Err11":
				prop = new ICompletionProposal[] {

						new QuickFixCompletionProposal("Remove '}'", offset, text.length(), "", 1) };
				break;
			case "Warn12":
				prop = new ICompletionProposal[] {

						new QuickFixCompletionProposal("Replace 'true' with 'TRUE'", offset, text.length(), "TRUE", 4) };

				break;
			case "Warn13":
				prop = new ICompletionProposal[] {

						new QuickFixCompletionProposal("Replace 'false' with 'FALSE'", offset, text.length(), "FALSE", 5) };

				break;
			case "Warn14":
				prop = new ICompletionProposal[] {

						new QuickFixCompletionProposal("Replace 'null' with 'NULL'", offset, text.length(), "NULL", 4) };

				break;
			case "Warn15":
				prop = new ICompletionProposal[] {

						new QuickFixCompletionProposal("Replace 'na' with 'NA'", offset, text.length(), "NA", 2) };

				break;
			case "Err16":
				prop = new ICompletionProposal[] {

						new QuickFixCompletionProposal("Remove ')'", offset, text.length(), "", 1) };

				break;
			case "Err17":

				break;
			case "Err18":
				prop = new ICompletionProposal[] {

						new QuickFixCompletionProposal("Remove ')'", offset, text.length(), "", 1) };
				break;
			case "Err19":

				break;
			case "Err20":
				prop = new ICompletionProposal[] {

						new QuickFixCompletionProposal("Replace '=>' with '>='", offset, text.length(), ">=", 2) };
				break;
			case "Err21":
				prop = new ICompletionProposal[] {

						new QuickFixCompletionProposal("Replace '=<' with '<='", offset, text.length(), "<=", 2) };
				break;

			default:
				break;
			}
			/*
			 * if (errorCode.equals("Err7")) { prop = new ICompletionProposal[]
			 * {
			 * 
			 * new QuickFixCompletionProposal("Remove", offset, text.length(),
			 * ""),
			 * 
			 * // new // QuickFixCompletionProposal(
			 * "This is the second proposal!",offset,text.length(),"")
			 * 
			 * }; }
			 */
		}
		return prop;
	}

	private String getCompleteText(IDocument document, int currentOffset) {

		int count = currentOffset;

		String tobeCompleteText = "";

		try {

			while (--count >= 0) {

				char c = ' ';

				c = document.getChar(count);

				if (isWhiteSpace(c)) {

					break;

				}

			}

			tobeCompleteText = document.get(count + 1, currentOffset - (count + 1));

		} catch (Exception e) {

			e.printStackTrace();

		}

		return tobeCompleteText;

	}

	private boolean isWhiteSpace(char c) {

		if (c == '\n' || c == '\t' || c == ' ')

			return true;

		return false;

	}

}