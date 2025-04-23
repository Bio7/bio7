package com.eco.bio7.rpreferences.template;

import java.util.ArrayList;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContentAssistProcessorExtension;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import com.eco.bio7.reditor.Bio7REditorPlugin;

public class RoxygenCompletionProcessor implements IContentAssistProcessor, IContentAssistProcessorExtension {

	private IPreferenceStore store;

	public RoxygenCompletionProcessor() {
		store = Bio7REditorPlugin.getDefault().getPreferenceStore();
	}

	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
		String prefix = extractPrefix(viewer, offset);
		// IDocument document = viewer.getDocument();

		String[] roxygenTags = { "@aliases", "@author", "@concepts", "@describeIn", "@description", "@details",
				"@docType", "@evalRd", "@example", "@examples", "@export", "@exportClass", "@exportMethod", "@family",
				"@field", "@format", "@import", "@importClassesFrom", "@importFrom", "@importMethodsFrom", "@include",
				"@inherit", "@inheritDotParams", "@inheritParams", "@inheritSection", "@keywords", "@md", "@method",
				"@name", "@note", "@noMd", "@noRd", "@param", "@rawRd", "@rawNamespace", "@rdname", "@references",
				"@return", "@section", "@seealso", "@slot", "@source", "@template", "@templateVar", "@title", "@usage",
				"@useDynLib" };

		ArrayList<CompletionProposal> list = new ArrayList<CompletionProposal>();
		int length = prefix.length();
		if (length >= 0) {
			for (int i = 0; i < roxygenTags.length; i++) {

				/*
				 * Here we filter out the templates by comparing the typed letters with the
				 * available templates!
				 */

				if (roxygenTags[i].length() >= length && roxygenTags[i].substring(0, length).equalsIgnoreCase(prefix)) {

					list.add(new CompletionProposal(roxygenTags[i], offset - length, length, roxygenTags[i].length(),
							null, roxygenTags[i], null, null));
				}
			}

		}

		CompletionProposal[] propo = list.toArray(new CompletionProposal[list.size()]);

		return propo;

	}

	public IContextInformation[] computeContextInformation(ITextViewer viewer, int offset) {
		/*
		 * IContextInformation[] NO_CONTEXTS = new IContextInformation[1]; String text =
		 * "A title for the roxygen documentation"; NO_CONTEXTS[0] = new
		 * ContextInformation("'@Title", text);
		 */
		return null;
	}

	// add the chars for Completion here !!!

	public char[] getCompletionProposalAutoActivationCharacters() {

		if (store.getBoolean("TYPED_CODE_COMPLETION")) {
			String ac = store.getString("ACTIVATION_CHARS");
			// return "abcdefghijklmnopqrstuvwxyz".toCharArray();
			if (ac == null || ac.isEmpty()) {

				return null;
			}
			return ac.toCharArray();
		}

		return null;

	}

	// ... remaining methods are optional ...
	@Override
	public char[] getContextInformationAutoActivationCharacters() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getErrorMessage() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IContextInformationValidator getContextInformationValidator() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isCompletionProposalAutoActivation(char c, ITextViewer viewer, int offset) {
		if (store.getBoolean("TYPED_CODE_COMPLETION")) {
			/*
			 * Check that we don't trigger the action with special characters, e.g., with a
			 * ',' in a function call!
			 */
			if (!Character.isJavaIdentifierPart(c) && (c == '.') == false && (c == '@') == false
					&& (c == ':') == false) {
				return false;
			}
			/*
			 * Prefix here is one character less than in the computeCompletionProposals
			 * calculation! Maybe triggered before the character is set?
			 */
			String prefix = extractPrefix(viewer, offset);
			if (prefix.startsWith("@")) {
				int activateKeyNrTyped = store.getInt("ACTIVATION_AMOUNT_CHAR_COMPLETION");
				if (prefix.length() + 1 == activateKeyNrTyped) {
					return true;
				}
			}

		}
		return false;
	}

	@Override
	public boolean isContextInformationAutoActivation(char c, ITextViewer viewer, int offset) {
		// TODO Auto-generated method stub
		return false;
	}

	/* Extend prefixes for R functions with a dot, e.g. t.test() */
	protected String extractPrefix(ITextViewer viewer, int offset) {
		int i = offset;
		IDocument document = viewer.getDocument();
		if (i > document.getLength())
			return "";

		try {
			while (i > 0) {
				char ch = document.getChar(i - 1);
				/*
				 * We need to extra include the '@' character for S4 class vars!
				 */
				if (!Character.isJavaIdentifierPart(ch) && (ch == '.') == false && (ch == '@') == false
						&& (ch == ':') == false)
					break;
				i--;
			}

			return document.get(i, offset - i);
		} catch (BadLocationException e) {
			return "";
		}
	}
}