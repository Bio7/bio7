/*******************************************************************************
 * Copyright (c) 2007-2026 M. Austenfeld
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/
package com.eco.bio7.pythonedit.analysis;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * The code completion of the Python editor: the names of the code analysis
 * (jedi or the standard library), the attributes of the variables of the running
 * Python and the templates.
 */
public class PythonCodeCompletion implements IContentAssistProcessor {

	private final ITextEditor editor;
	private final IContentAssistProcessor templates;
	private String error;

	public PythonCodeCompletion(ITextEditor editor, IContentAssistProcessor templates) {
		this.editor = editor;
		this.templates = templates;
	}

	@Override
	@SuppressWarnings("unchecked")
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
		List<ICompletionProposal> result = new ArrayList<>();
		IDocument doc = viewer.getDocument();
		error = null;
		try {
			int line = doc.getLineOfOffset(offset);
			int lineStart = doc.getLineOffset(line);
			String before = doc.get(lineStart, offset - lineStart);
			String expr = expression(before);
			String prefix = expr.contains(".") ? expr.substring(expr.lastIndexOf('.') + 1) : expr;
			int start = offset - prefix.length();
			Set<String> seen = new LinkedHashSet<>();
			/* The attributes of a variable of the running Python (e.g., the columns of a DataFrame) */
			Function<String, List<String[]>> live = PythonEditorSupport.liveCompletion;
			if (live != null) {
				String base = expr.contains(".") ? expr.substring(0, expr.lastIndexOf('.')) : "";
				List<String[]> names = live.apply(base);
				if (names != null) {
					for (String[] n : names) {
						if (n[0].startsWith(prefix) && seen.add(n[0]) && (!n[0].startsWith("_") || prefix.startsWith("_"))) {
							result.add(proposal(n[0], start, offset - start, n[0] + "  -  " + n[1] + " (running Python)", n.length > 2 ? n[2] : null, n[1]));
						}
					}
				}
			}
			/* The code analysis */
			Map<String, Object> args = new LinkedHashMap<>();
			args.put("source", doc.get());
			args.put("path", editor.getEditorInput() instanceof IFileEditorInput fi && fi.getFile().getLocation() != null ? fi.getFile().getLocation().toOSString() : null);
			args.put("line", line + 1);
			args.put("col", offset - lineStart);
			Map<String, Object> r = AnalysisClient.get().request("complete", args, 6000);
			if (r != null && r.get("items") instanceof List<?> items) {
				for (Object o : items) {
					Map<String, Object> it = (Map<String, Object>) o;
					String name = String.valueOf(it.get("name"));
					if (!seen.add(name)) {
						continue;
					}
					String kind = String.valueOf(it.getOrDefault("kind", ""));
					String signature = it.get("signature") instanceof String s && !s.isEmpty() ? s : null;
					String display = (signature != null ? shorten(signature) : name) + "  -  " + kind;
					String doc2 = it.get("doc") instanceof String d && !d.isEmpty() ? d : null;
					String info = signature != null ? signature + (doc2 != null ? "\n\n" + doc2 : "") : doc2;
					result.add(proposal(name, start, offset - start, display, info, kind));
				}
			}
		} catch (BadLocationException e) {
			/* No proposals of the analysis */
		}
		/* The templates (not after a dot) */
		if (templates != null) {
			try {
				String before = doc.get(Math.max(0, offset - 1), Math.min(1, offset));
				if (!before.equals(".")) {
					ICompletionProposal[] t = templates.computeCompletionProposals(viewer, offset);
					if (t != null) {
						result.addAll(List.of(t));
					}
				}
			} catch (BadLocationException e) {
				/* No templates */
			}
		}
		return result.toArray(new ICompletionProposal[0]);
	}

	private static String shorten(String signature) {
		return signature.length() > 80 ? signature.substring(0, 77) + "..." : signature;
	}

	private static ICompletionProposal proposal(String name, int start, int length, String display, String info, String kind) {
		return new CompletionProposal(name, start, length, name.length(), image(kind), display, null, info);
	}

	private static Image image(String kind) {
		String key = switch (kind) {
		case "module" -> ISharedImages.IMG_OBJ_FOLDER;
		case "class" -> ISharedImages.IMG_OBJ_ELEMENT;
		case "function" -> ISharedImages.IMG_OBJ_FILE;
		default -> null;
		};
		return key == null ? null : PlatformUI.getWorkbench().getSharedImages().getImage(key);
	}

	/* The dotted expression before the cursor (e.g., "np.li") */
	static String expression(String before) {
		int i = before.length();
		while (i > 0) {
			char c = before.charAt(i - 1);
			if (Character.isLetterOrDigit(c) || c == '_' || c == '.') {
				i--;
			} else {
				break;
			}
		}
		String e = before.substring(i);
		return e.startsWith(".") ? e.substring(1) : e;
	}

	@Override
	public IContextInformation[] computeContextInformation(ITextViewer viewer, int offset) {
		return null;
	}

	@Override
	public char[] getCompletionProposalAutoActivationCharacters() {
		return new char[] { '.' };
	}

	@Override
	public char[] getContextInformationAutoActivationCharacters() {
		return null;
	}

	@Override
	public String getErrorMessage() {
		return error;
	}

	@Override
	public IContextInformationValidator getContextInformationValidator() {
		return null;
	}
}
