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
 * Extracts the selected statements (complete lines) into a function: the
 * parameters (the variables of the scope which are read) and the results (the
 * changed variables which are used later) are detected; several results are
 * returned as a list.
 */
public class ExtractMethod extends RRefactorDelegate {

	public ExtractMethod() {
		super("com.eco.bio7.reditor.refactor.extract.method");
	}

	@Override
	protected String title() {
		return "Extract Function";
	}

	@Override
	protected RRefactor.Result compute(RRefactor refactor, String text, ITextSelection sel, Shell shell) {
		if (sel.getLength() == 0) {
			MessageDialog.openInformation(shell, title(), "Please select one or more statements.");
			return null;
		}
		String name = askName(shell, "Name of the function:", "new_function");
		return name == null ? null : refactor.extractFunction(sel.getOffset(), sel.getOffset() + sel.getLength(), name);
	}
}
