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
package com.eco.bio7.reditor.actions;

import org.eclipse.jface.text.ITextSelection;
import org.eclipse.swt.widgets.Shell;

import com.eco.bio7.reditor.antlr.refactor.RRefactor;

/** Replaces the variable at the cursor with its value and removes the assignment. */
public class InlineVariable extends RRefactorDelegate {

	public InlineVariable() {
		super("com.eco.bio7.reditor.refactor.inline");
	}

	@Override
	protected String title() {
		return "Inline Variable";
	}

	@Override
	protected RRefactor.Result compute(RRefactor refactor, String text, ITextSelection sel, Shell shell) {
		return refactor.inline(sel.getOffset());
	}
}
