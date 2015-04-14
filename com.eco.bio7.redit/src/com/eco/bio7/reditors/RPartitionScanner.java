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
 *     M.Austenfeld - Changes for the Bio7 application.
 *******************************************************************************/
package com.eco.bio7.reditors;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.rules.EndOfLineRule;
import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.MultiLineRule;
import org.eclipse.jface.text.rules.PatternRule;
import org.eclipse.jface.text.rules.RuleBasedPartitionScanner;
import org.eclipse.jface.text.rules.Token;



/*Leave this class for maybe later use!*/
public class RPartitionScanner extends RuleBasedPartitionScanner {

	public final static String[] R_PARTITION_TYPES = new String[] { IDocument.DEFAULT_CONTENT_TYPE,"R_MULTILINE_STRING","R_COMMENT" };

	public Token rString;

	private Token rComment;

	

	/**
	 * Creates the partitioner and sets up the appropriate rules.
	 */
	public RPartitionScanner() {
		super();
	   /*Added a comment Token else if a quotation occurs in a comment the syntax coloring fails!*/
		rComment = new Token("R_COMMENT");
		rString = new Token("R_MULTILINE_STRING");
	
		List<PatternRule> rules = new ArrayList<PatternRule>();
		rules.add(new EndOfLineRule("#", rComment));
		rules.add(new MultiLineRule("\"", "\"", rString,'\\'));
		rules.add(new MultiLineRule("'", "'", rString,'\\'));

		// Add rule for single line comments.
		
		

		IPredicateRule[] result = new IPredicateRule[rules.size()];
		rules.toArray(result);
		setPredicateRules(result);
	}
}
