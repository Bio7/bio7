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

import java.util.List;

import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.quickassist.IQuickAssistInvocationContext;
import org.eclipse.jface.text.quickassist.IQuickAssistProcessor;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.texteditor.MarkerAnnotation;

/** The quick fixes (Ctrl+1) of the markers of the code analysis. */
public class PythonQuickAssistProcessor implements IQuickAssistProcessor {

	@Override
	public String getErrorMessage() {
		return null;
	}

	@Override
	public boolean canFix(Annotation annotation) {
		/* Only markers with quick fixes (the light bulb in the ruler) */
		if (!(annotation instanceof MarkerAnnotation ma)) {
			return false;
		}
		try {
			return ma.getMarker().exists() && PythonFixes.MARKER.equals(ma.getMarker().getType()) && !ma.getMarker().getAttribute(PythonFixes.FIXES, "").isEmpty();
		} catch (org.eclipse.core.runtime.CoreException e) {
			return false;
		}
	}

	@Override
	public boolean canAssist(IQuickAssistInvocationContext invocationContext) {
		return false;
	}

	@Override
	public ICompletionProposal[] computeQuickAssistProposals(IQuickAssistInvocationContext context) {
		ISourceViewer viewer = context.getSourceViewer();
		PythonFixes.MarkerFixes mf = PythonFixes.markerAt(viewer, context.getOffset(), true);
		if (mf == null || mf.proposals().isEmpty()) {
			return new ICompletionProposal[0];
		}
		List<ICompletionProposal> p = mf.proposals();
		return p.toArray(new ICompletionProposal[0]);
	}
}
