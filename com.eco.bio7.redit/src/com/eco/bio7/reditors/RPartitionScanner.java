/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     
 *     M.Austenfeld - Minor changes for the Bio7 application.
 *******************************************************************************/
package com.eco.bio7.reditors;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.rules.*;


public class RPartitionScanner extends RuleBasedPartitionScanner {

	public final static String R_MULTILINE_COMMENT = "__java_multiline_comment"; //$NON-NLS-1$

	public final static String R_DOC = "__java_javadoc"; //$NON-NLS-1$

	public final static String[] R_PARTITION_TYPES = new String[] { R_MULTILINE_COMMENT, R_DOC };

	/**
	 * Detector for empty comments.
	 */
	static class EmptyCommentDetector implements IWordDetector {

		/*
		 * (non-Javadoc) Method declared on IWordDetector
		 */
		public boolean isWordStart(char c) {
			return (c == '/');
		}

		/*
		 * (non-Javadoc) Method declared on IWordDetector
		 */
		public boolean isWordPart(char c) {
			return (c == '*' || c == '/');
		}
	}

	/**
	 * 
	 */
	static class WordPredicateRule extends WordRule implements IPredicateRule {

		private IToken fSuccessToken;

		public WordPredicateRule(IToken successToken) {
			super(new EmptyCommentDetector());
			fSuccessToken = successToken;
			addWord("/**/", fSuccessToken);
		}

		/*
		 * @see org.eclipse.jface.text.rules.IPredicateRule#evaluate(ICharacterScanner,
		 *      boolean)
		 */
		public IToken evaluate(ICharacterScanner scanner, boolean resume) {
			return super.evaluate(scanner);
		}

		/*
		 * @see org.eclipse.jface.text.rules.IPredicateRule#getSuccessToken()
		 */
		public IToken getSuccessToken() {
			return fSuccessToken;
		}
	}

	/**
	 * Creates the partitioner and sets up the appropriate rules.
	 */
	public RPartitionScanner() {
		super();

		IToken comment = new Token(R_MULTILINE_COMMENT);

		List rules = new ArrayList();

		// Add rule for single line comments.
		rules.add(new EndOfLineRule("#", Token.UNDEFINED)); //$NON-NLS-1$

		// Add rule for strings and character constants.
		rules.add(new SingleLineRule("\"", "\"", Token.UNDEFINED, '\\')); 
		rules.add(new SingleLineRule("'", "'", Token.UNDEFINED, '\\')); 

		// Add special case word rule.
		rules.add(new WordPredicateRule(comment));

		IPredicateRule[] result = new IPredicateRule[rules.size()];
		rules.toArray(result);
		setPredicateRules(result);
	}
}
