/*******************************************************************************
 * Copyright (c) 2007-2026 M. Austenfeld
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * Contributors:
 * Marcel Austenfeld - initial API and implementation
 *******************************************************************************/
package com.eco.bio7.reditor.code;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;

import com.eco.bio7.reditor.antlr.CodeFix;

/**
 * A quick fix of the code analysis which applies one or more edits (see
 * CodeFix), e.g., return x -> return(x).
 */
public class RQuickFixEditsProposal implements ICompletionProposal {

	private final CodeFix.Proposal proposal;

	public RQuickFixEditsProposal(CodeFix.Proposal proposal) {
		this.proposal = proposal;
	}

	/** The quick fixes of the replacement text of a marker (null if none). */
	public static ICompletionProposal[] of(String replacement) {
		List<CodeFix.Proposal> list = CodeFix.decode(replacement);
		if (list.isEmpty()) {
			return null;
		}
		List<ICompletionProposal> props = new ArrayList<>();
		for (CodeFix.Proposal p : list) {
			props.add(new RQuickFixEditsProposal(p));
		}
		return props.toArray(new ICompletionProposal[0]);
	}

	@Override
	public void apply(IDocument document) {
		/* From the end: the offsets of the edits before stay valid */
		List<CodeFix.Edit> edits = new ArrayList<>(proposal.edits());
		edits.sort(Comparator.comparingInt(CodeFix.Edit::offset).reversed());
		for (CodeFix.Edit e : edits) {
			try {
				if (e.offset() >= 0 && e.offset() + e.length() <= document.getLength()) {
					document.replace(e.offset(), e.length(), e.text());
				}
			} catch (BadLocationException ex) {
				/* The document changed */
			}
		}
	}

	@Override
	public Point getSelection(IDocument document) {
		return null;
	}

	@Override
	public String getAdditionalProposalInfo() {
		StringBuilder sb = new StringBuilder();
		for (CodeFix.Edit e : proposal.edits()) {
			if (!e.text().isEmpty()) {
				sb.append(sb.length() > 0 ? "\n" : "").append(e.length() > 0 ? "Replace with: " : "Insert: ").append(e.text().trim());
			} else {
				sb.append(sb.length() > 0 ? "\n" : "").append("Remove ").append(e.length()).append(" characters");
			}
		}
		return sb.toString();
	}

	@Override
	public String getDisplayString() {
		return proposal.label();
	}

	@Override
	public Image getImage() {
		return null;
	}

	@Override
	public IContextInformation getContextInformation() {
		return null;
	}
}
