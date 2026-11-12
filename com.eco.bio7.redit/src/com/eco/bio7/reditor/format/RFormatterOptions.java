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

package com.eco.bio7.reditor.format;

/**
 * The options of the built-in R formatter ({@link RFormatter}).
 */
public final class RFormatterOptions {

	/** Spaces per indentation level (if no tabs are used). */
	public int indentSize = 4;

	/** Indent with tabs (one per level) instead of spaces. */
	public boolean useTabs = false;

	/**
	 * Extra levels for a continued statement (e.g., after a trailing operator like
	 * '+' or '%>%', or the body of an 'if' without braces).
	 */
	public int continuationIndent = 1;

	/** Spaces around binary operators (not for '::', '$', '@', ':', '^'). */
	public boolean spaceAroundOperators = true;

	/** A space after a comma. */
	public boolean spaceAfterComma = true;

	/** A space between 'if', 'for', 'while' and '(' . */
	public boolean spaceBeforeKeywordParenthesis = true;

	/** Replace the assignment '=' (not an argument '=') with '<-'. */
	public boolean replaceAssignment = false;

	/** Set the '{' of a block on a new line (else: on the line of its header). */
	public boolean braceOnNewLine = false;

	/** The maximum number of consecutive blank lines (0: none). */
	public int maxBlankLines = 2;

	/** The text of one indentation level. */
	String indentUnit() {
		return useTabs ? "\t" : " ".repeat(Math.max(0, indentSize));
	}
}
