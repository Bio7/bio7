/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.eco.bio7.editors;


import java.util.ArrayList;
import java.util.List;
import org.eclipse.jface.text.rules.*;

import com.eco.bio7.editor.BeanshellEditorPlugin;

/**
 * This scanner recognizes the JavaDoc comments and Java multi line comments.
 */
public class ScriptPartitionScanner extends RuleBasedPartitionScanner {

	public final static String SCRIPT_MULTILINE_COMMENT= "__java_multiline_comment"; //$NON-NLS-1$
	public final static String SCRIPT_DOC= "__java_javadoc"; 
	public final static String[] SCRIPT_PARTITION_TYPES= new String[] { SCRIPT_MULTILINE_COMMENT, SCRIPT_DOC };

	/**
	 * Detector for empty comments.
	 */
	static class EmptyCommentDetector implements IWordDetector {

		
		public boolean isWordStart(char c) {
			return (c == '/');
		}

		
		public boolean isWordPart(char c) {
			return (c == '*' || c == '/');
		}
	}
	
	
	static class WordPredicateRule extends WordRule implements IPredicateRule {
		
		private IToken fSuccessToken;
		
		public WordPredicateRule(IToken successToken) {
			super(new EmptyCommentDetector());
			fSuccessToken= successToken;
			addWord("/**/", fSuccessToken); //$NON-NLS-1$
		}
		
		
		public IToken evaluate(ICharacterScanner scanner, boolean resume) {
			return super.evaluate(scanner);
		}

		
		public IToken getSuccessToken() {
			return fSuccessToken;
		}
	}

	/**
	 * Creates the partitioner and sets up the appropriate rules.
	 */
	public ScriptPartitionScanner() {
		super();

		IToken javaDoc= new Token(SCRIPT_DOC);
		IToken comment= new Token(SCRIPT_MULTILINE_COMMENT);

		List rules= new ArrayList();
		/*BeanshellEditorPlugin fginstance = BeanshellEditorPlugin.getDefault();
		JavaCodeScanner scanner = (JavaCodeScanner) fginstance.getJavaCodeScanner();*/

		// Add rule for single line comments.
		rules.add(new EndOfLineRule("//", Token.UNDEFINED)); //$NON-NLS-1$

		// Add rule for strings and character constants.
		rules.add(new SingleLineRule("\"", "\"", Token.UNDEFINED, '\\')); //$NON-NLS-2$ //$NON-NLS-1$
		rules.add(new SingleLineRule("'", "'", Token.UNDEFINED, '\\')); //$NON-NLS-2$ //$NON-NLS-1$

		// Add special case word rule.
		rules.add(new WordPredicateRule(comment));

		// Add rules for multi-line comments and javadoc.
		rules.add(new MultiLineRule("/**", "*/", javaDoc, (char) 0, true)); //$NON-NLS-1$ //$NON-NLS-2$
		rules.add(new MultiLineRule("/*", "*/", comment, (char) 0, true)); //$NON-NLS-1$ //$NON-NLS-2$

		IPredicateRule[] result= new IPredicateRule[rules.size()];
		rules.toArray(result);
		setPredicateRules(result);
	}
}
