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

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.swt.widgets.Shell;

import com.eco.bio7.reditor.antlr.refactor.RRefactor;

/**
 * Extracts the selected expression into a variable before its statement
 * (optionally all the same expressions after it, until one of its variables
 * changes).
 */
public class ExtractVariable extends RRefactorDelegate {

	public ExtractVariable() {
		super("com.eco.bio7.reditor.refactor.extract.variable");
	}

	@Override
	protected String title() {
		return "Extract Variable";
	}

	@Override
	protected RRefactor.Result compute(RRefactor refactor, String text, ITextSelection sel, Shell shell) {
		if (sel.getLength() == 0) {
			MessageDialog.openInformation(shell, title(), "Please select an expression.");
			return null;
		}
		int start = sel.getOffset(), end = sel.getOffset() + sel.getLength();
		int count = refactor.occurrences(start, end);
		boolean all = false;
		if (count > 1) {
			MessageDialog d = new MessageDialog(shell, title(), null, "The expression occurs " + count + " times (until one of its variables changes).",
					MessageDialog.QUESTION, 0, "Replace All (" + count + ")", "Only the Selection", "Cancel");
			int answer = d.open();
			if (answer == 2 || answer < 0) {
				return null;
			}
			all = answer == 0;
		}
		String name = askName(shell, "Name of the variable:", "value");
		return name == null ? null : refactor.extractVariable(start, end, name, all);
	}
}
