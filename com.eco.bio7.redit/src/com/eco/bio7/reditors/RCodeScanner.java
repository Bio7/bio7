package com.eco.bio7.reditors;

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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IWordDetector;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WhitespaceRule;
import org.eclipse.jface.text.rules.WordRule;
import org.eclipse.swt.graphics.RGB;

import com.eco.bio7.reditor.Bio7REditorPlugin;

/**
 * A R code scanner.
 */
public class RCodeScanner extends RuleBasedScanner {

	private static String[] fgKeywords = { "if", "else", "repeat", "while", "function", "for", "in", "next", "break" };

	private static String[] fgConstants = { "NULL", "NA", "NaN", "Inf", "TRUE", "FALSE" };

	public static final String[] fgSeperators = { ";", "," };

	public static final String[] fgAssignmentoperators = { "<<-", "->>", "->", "<-", "=", ":=" };

	public static final String[] fgdefaultoperators = { "+", "-", "*", "/", "%%", "^", // arithmetic
			">", ">=", "<", "<=", "==", "!=", // relational
			"!", "&", "|", // logical
			"~", // model formulae
			":" // sequence
	};

	public static final String[] Predefinedinfix = {
			// "%%",
			"%*%", "%/%", "%in%", "%o%", "%x%", };

	public static final String[] fg_grouping = { "{", "}", "(", ")", };

	public static final String[] fg_subelement = { "[", "]", "$", "@" };

	public static final String[] fg_namespace = { "::", ":::", };

	// ----

	public static final char[] fg_seperatorchars = { '!', '$', '%', '&', '(', ')', '*', '+', ',', '-', '/', ':', ';', '<', '=', '>', '[', ']', '^', '{', '|', '}', '~', '@', };

	public static final char[] fg_whitespacechars = { ' ', '\t', };

	public static final char[] fg_newlinechars = { '\r', '\n', };
	public Token keyword;
	public Token type;
	public Token string;
	public Token comment;
	public Token other;
	public Token operators;
	public Token braces;
	public Token numbers;
	public Token assignment;

	/**
	 * Creates a R code scanner with the given color provider.
	 * 
	 * @param provider
	 *            the color provider
	 */
	public RCodeScanner(RColorProvider provider) {

		IPreferenceStore store = Bio7REditorPlugin.getDefault().getPreferenceStore();
		RGB rgbkey = PreferenceConverter.getColor(store, "colourkey");

		RGB rgbkey1 = PreferenceConverter.getColor(store, "colourkey1");
		//RGB rgbkey2 = PreferenceConverter.getColor(store, "colourkey2");
		//RGB rgbkey3 = PreferenceConverter.getColor(store, "colourkey3");
		RGB rgbkey4 = PreferenceConverter.getColor(store, "colourkey4");
		RGB rgbkey5 = PreferenceConverter.getColor(store, "colourkey5");
		RGB rgbkey6 = PreferenceConverter.getColor(store, "colourkey6");
		RGB rgbkey7 = PreferenceConverter.getColor(store, "colourkey7");
		RGB rgbkey8 = PreferenceConverter.getColor(store, "colourkey8");

		keyword = new Token(new TextAttribute(provider.getColor(rgbkey), null, isBold("BOLD_COLOURKEY")));
		type = new Token(new TextAttribute(provider.getColor(rgbkey1), null, isBold("BOLD_COLOURKEY1")));
		/*
		 * String and comment have a special Single Token scanner in the R
		 * configuration class!
		 */
		//string = new Token(new TextAttribute(provider.getColor(rgbkey2), null, isBold("BOLD_COLOURKEY2")));
		//comment = new Token(new TextAttribute(provider.getColor(rgbkey3), null, isBold("BOLD_COLOURKEY3")));

		other = new Token(new TextAttribute(provider.getColor(rgbkey4), null, isBold("BOLD_COLOURKEY4")));
		operators = new Token(new TextAttribute(provider.getColor(rgbkey5), null, isBold("BOLD_COLOURKEY5")));
		braces = new Token(new TextAttribute(provider.getColor(rgbkey6), null, isBold("BOLD_COLOURKEY6")));
		numbers = new Token(new TextAttribute(provider.getColor(rgbkey7), null, isBold("BOLD_COLOURKEY7")));
		assignment = new Token(new TextAttribute(provider.getColor(rgbkey8), null, isBold("BOLD_COLOURKEY8")));

		List<IRule> rules = new ArrayList<IRule>();
		/*
		 * Comments and multiline strings (tokens) are treated specially in the
		 * RConfiguration class!
		 */
		// Add rule for single line comments.
		// rules.add(new EndOfLineRule("#", comment));
		
        /*Here we need special rules to detect the char combinations!*/
		rules.add(new WhitespaceRule(new RWhitespaceDetector()));
		rules.add(new AssignmentRule(operators, assignment));
		rules.add(new COperatorRule(operators));
		rules.add(new REditorBraceRule(braces));
		// rules.add(new NumberRule(numbers));
		rules.add(new WordRule(new NumberDetector(), numbers));

		// ORDER is important - try to switch order of '=' and '==' !

		RWordDetector wd = new RWordDetector();
		/*for (int i = 0; i < fgKeywords.length; i++) {

		}*/
		/*Here now we add the different tokens which don't need a special treatment to the word rule!*/			
		/*Other is the default token!*/
		WordRule wordRule = new WordRule(wd, other); 

		for (int i = 0; i < fgKeywords.length; i++)
			wordRule.addWord(fgKeywords[i], keyword);
		/*
		 * for (int i = 0; i < fgdefaultoperators.length; i++)
		 * wordRule.addWord(fgdefaultoperators[i], type);
		 */
		for (int i = 0; i < fgConstants.length; i++)
			wordRule.addWord(fgConstants[i], type);
		/*
		 * for (int i = 0; i < fgAssignmentoperators.length; i++)
		 * wordRule.addWord(fgAssignmentoperators[i], assignmentOps);
		 */

		rules.add(wordRule);
		/*
		 * Comments and multiline strings (tokens) are treated specially in the
		 * RConfiguration class!
		 */
		// rules.add(new MultiLineRule("\"", "\"", string, '\\'));
		// rules.add(new MultiLineRule("'", "'", string, '\\'));

		IRule[] result = new IRule[rules.size()];
		rules.toArray(result);
		setRules(result);
	}

	private int isBold(String string2) {
		int style = 0;
		IPreferenceStore store = Bio7REditorPlugin.getDefault().getPreferenceStore();
		if (store.getBoolean(string2)) {
			style = 1;
		}

		return style;
	}

	static public class NumberDetector implements IWordDetector {

		/**
		 * Used to keep the state of the token
		 */
		private FastStringBuffer buffer = new FastStringBuffer();

		/**
		 * Defines if we are at an hexa number
		 */
		private boolean isInHexa;

		/**
		 * @see org.eclipse.jface.text.rules.IWordDetector#isWordStart(char)
		 */
		public boolean isWordStart(char c) {
			isInHexa = false;
			buffer.clear();
			buffer.append(c);
			return Character.isDigit(c);
		}

		/**
		 * Check if we are still in the number
		 */
		public boolean isWordPart(char c) {
			// Test for scientific notation e.g.: 10.9e10

			if ((c == 'x' || c == 'X') && buffer.length() == 1 && buffer.charAt(0) == '0') {
				// Hexadecimal!
				buffer.append(c);
				isInHexa = true;
				return true;
			} else {
				buffer.append(c);
			}

			if (isInHexa) {
				return Character.isDigit(c) || c == 'a' || c == 'A' || c == 'b' || c == 'B' || c == 'c' || c == 'C' || c == 'd' || c == 'D' || c == 'e' || c == 'E' || c == 'f' || c == 'F';

			} else {
				return Character.isDigit(c) || c == 'e' || c == 'E' || c == 'i' || c == '.';
			}
		}

	}

}
