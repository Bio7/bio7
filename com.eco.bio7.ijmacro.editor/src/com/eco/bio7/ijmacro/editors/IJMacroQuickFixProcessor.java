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
package com.eco.bio7.ijmacro.editors;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.quickassist.IQuickAssistInvocationContext;
import org.eclipse.jface.text.quickassist.IQuickAssistProcessor;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.texteditor.MarkerAnnotation;

import com.eco.bio7.ijmacro.editor.antlr.CodeFix;

/**
 * The quick fixes of the ImageJ macro editor (Ctrl+1): the fixes of the markers
 * of the code analysis (brackets, strings, commas, semicolons, see
 * IJCodeChecks).
 */
public class IJMacroQuickFixProcessor implements IQuickAssistProcessor {

	private static final Set<String> CODES = Set.of("Err26", "Err27", "Err28", "Err30", "Err31");

	@Override
	public String getErrorMessage() {
		return null;
	}

	@Override
	public boolean canFix(Annotation annotation) {
		return annotation instanceof MarkerAnnotation m && !annotation.isMarkedDeleted() && CODES.contains(m.getMarker().getAttribute(IMarker.TEXT, ""));
	}

	@Override
	public boolean canAssist(IQuickAssistInvocationContext invocationContext) {
		return false;
	}

	@Override
	public ICompletionProposal[] computeQuickAssistProposals(IQuickAssistInvocationContext context) {
		ISourceViewer viewer = context.getSourceViewer();
		int offset = context.getOffset();
		List<ICompletionProposal> proposals = new ArrayList<>();
		if (viewer.getAnnotationModel() == null) {
			return null;
		}
		Iterator<Annotation> it = viewer.getAnnotationModel().getAnnotationIterator();
		while (it.hasNext()) {
			if (!(it.next() instanceof MarkerAnnotation m) || !canFix(m)) {
				continue;
			}
			IMarker marker = m.getMarker();
			/* The position of the annotation follows the edits (the marker attributes not) */
			org.eclipse.jface.text.Position pos = viewer.getAnnotationModel().getPosition(m);
			int start = pos != null ? pos.getOffset() : marker.getAttribute(IMarker.CHAR_START, -1);
			int end = pos != null ? pos.getOffset() + pos.getLength() : marker.getAttribute(IMarker.CHAR_END, -1);
			if (start < 0 || offset < start || offset > end) {
				continue;
			}
			for (CodeFix.Proposal p : CodeFix.decode(marker.getAttribute("REPLACEMENT_TEXT", ""))) {
				proposals.add(new EditsProposal(p));
			}
		}
		return proposals.isEmpty() ? null : proposals.toArray(new ICompletionProposal[0]);
	}

	/** A marker at an offset: its message and quick fixes (for the hover). */
	public record MarkerFixes(String message, int severity, ICompletionProposal[] proposals) {
	}

	/** The message and the quick fixes of the problem marker at the offset (null if none). */
	public static MarkerFixes markerAt(ISourceViewer viewer, int offset) {
		if (viewer.getAnnotationModel() == null) {
			return null;
		}
		Iterator<Annotation> it = viewer.getAnnotationModel().getAnnotationIterator();
		while (it.hasNext()) {
			if (!(it.next() instanceof MarkerAnnotation m) || m.isMarkedDeleted()) {
				continue;
			}
			IMarker marker = m.getMarker();
			if (!marker.exists()) {
				continue;
			}
			int start = marker.getAttribute(IMarker.CHAR_START, -1);
			int end = marker.getAttribute(IMarker.CHAR_END, -1);
			String message = marker.getAttribute(IMarker.MESSAGE, null);
			if (start < 0 || offset < start || offset > end || message == null) {
				continue;
			}
			List<ICompletionProposal> proposals = new ArrayList<>();
			if (CODES.contains(marker.getAttribute(IMarker.TEXT, ""))) {
				for (CodeFix.Proposal p : CodeFix.decode(marker.getAttribute("REPLACEMENT_TEXT", ""))) {
					proposals.add(new EditsProposal(p));
				}
			}
			return new MarkerFixes(message, marker.getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR), proposals.toArray(new ICompletionProposal[0]));
		}
		return null;
	}

	/* A quick fix: its edits (from the end, the offsets before stay valid) */
	private static final class EditsProposal implements ICompletionProposal {
		private final CodeFix.Proposal proposal;

		EditsProposal(CodeFix.Proposal proposal) {
			this.proposal = proposal;
		}

		@Override
		public void apply(IDocument document) {
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
				sb.append(sb.length() > 0 ? "\n" : "").append(e.text().isEmpty() ? "Remove " + e.length() + " character(s)" : "Insert: " + e.text().trim());
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
}
