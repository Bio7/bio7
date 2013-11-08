package com.eco.bio7.pythoneditors;

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
 *      M.Austenfeld - Minor changes for the Bio7 application
 *******************************************************************************/

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.EndOfLineRule;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.IWhitespaceDetector;
import org.eclipse.jface.text.rules.IWordDetector;
import org.eclipse.jface.text.rules.MultiLineRule;
import org.eclipse.jface.text.rules.NumberRule;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.SingleLineRule;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WhitespaceRule;
import org.eclipse.jface.text.rules.WordRule;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

import com.eco.bio7.editors.python.PythonScriptColorProvider;
import com.eco.bio7.pythonedit.PythonEditorPlugin;



/**
 * A Java code scanner.
 */
public class ScriptCodeScanner extends RuleBasedScanner {

 
	// keywords list has to be alphabetized for the keyword detector to work properly
	    static final public String[] DEFAULT_KEYWORDS = { "and", "as", "assert", "break", "class", "continue", "def",
	            "del", "elif", "else", "except", "exec", "finally", "for", "from", "global", "if", "import", "in", "is",
	            "lambda", "nonlocal", "not", "or", "pass", "print", "raise", "return", "self", "try", "while", "with",
	            "yield", "False", "None", "True",

	    };

	private static String[] fgTypes = { "void", "boolean", "char", "byte", "short", "int", "long", "float", "double" };

	private static String[] fgConstants = { "false", "null", "true" };

	public Token keyword;
	public Token type;
	public Token string;
	public Token comment;
	public Token defaultOther;
	public Token operators;
	//public Token braces;
	public Token numbers;
	public Token decorator;
	public Token self;
	public Token className;
	public Token funcName;
	public Token parenthesis;

	/**
	 * Creates a Java code scanner with the given color provider.
	 * 
	 * @param pythonScriptColorProvider
	 *            the color provider
	 */
	public ScriptCodeScanner(PythonScriptColorProvider pythonScriptColorProvider) {

		IPreferenceStore store = PythonEditorPlugin.getDefault().getPreferenceStore();
		RGB rgbkey = PreferenceConverter.getColor(store, "colourkey");
		RGB rgbkey1 = PreferenceConverter.getColor(store, "colourkey1");
		RGB rgbkey2 = PreferenceConverter.getColor(store, "colourkey2");
		RGB rgbkey3 = PreferenceConverter.getColor(store, "colourkey3");
		RGB rgbkey4 = PreferenceConverter.getColor(store, "colourkey4");
		RGB rgbkey5 = PreferenceConverter.getColor(store, "colourkey5");
		//RGB rgbkey6 = PreferenceConverter.getColor(store, "colourkey6");
		RGB rgbkey7 = PreferenceConverter.getColor(store, "colourkey7");
		RGB rgbkey8 = PreferenceConverter.getColor(store, "colourkey8");
		RGB rgbkey9 = PreferenceConverter.getColor(store, "colourkey9");
		RGB rgbkey10 = PreferenceConverter.getColor(store, "colourkey10");
		//RGB rgbkey11 = PreferenceConverter.getColor(store, "colourkey11");
		RGB rgbkey12 = PreferenceConverter.getColor(store, "colourkey12");
		
		
		FontData f=PreferenceConverter.getFontData(store, "colourkeyfont");
		FontData f1=PreferenceConverter.getFontData(store, "colourkeyfont1");
		FontData f2=PreferenceConverter.getFontData(store, "colourkeyfont2");
		FontData f3=PreferenceConverter.getFontData(store, "colourkeyfont3");
		FontData f4=PreferenceConverter.getFontData(store, "colourkeyfont4");
		FontData f5=PreferenceConverter.getFontData(store, "colourkeyfont5");
		//FontData f6=PreferenceConverter.getFontData(store, "colourkeyfont6");
		FontData f7=PreferenceConverter.getFontData(store, "colourkeyfont7");
		FontData f8=PreferenceConverter.getFontData(store, "colourkeyfont8");
		FontData f9=PreferenceConverter.getFontData(store, "colourkeyfont9");
		FontData f10=PreferenceConverter.getFontData(store, "colourkeyfont10");
		//FontData f11=PreferenceConverter.getFontData(store, "colourkeyfont11");
		FontData f12=PreferenceConverter.getFontData(store, "colourkeyfont12");
		

		keyword = new Token(new TextAttribute(pythonScriptColorProvider.getColor(rgbkey), null, 1,new Font(Display.getCurrent(),f)));	
		type = new Token(new TextAttribute(pythonScriptColorProvider.getColor(rgbkey1), null, 1,new Font(Display.getCurrent(),f1)));
		string = new Token(new TextAttribute(pythonScriptColorProvider.getColor(rgbkey2), null, 1,new Font(Display.getCurrent(),f2)));
		comment = new Token(new TextAttribute(pythonScriptColorProvider.getColor(rgbkey3), null, 1,new Font(Display.getCurrent(),f3)));
		defaultOther = new Token(new TextAttribute(pythonScriptColorProvider.getColor(rgbkey4), null, 1,new Font(Display.getCurrent(),f4)));
		operators = new Token(new TextAttribute(pythonScriptColorProvider.getColor(rgbkey5), null, 1,new Font(Display.getCurrent(),f5)));
		//braces = new Token(new TextAttribute(pythonScriptColorProvider.getColor(rgbkey6), null, 1,new Font(Display.getCurrent(),f6)));
		numbers = new Token(new TextAttribute(pythonScriptColorProvider.getColor(rgbkey7), null, 1,new Font(Display.getCurrent(),f7)));
		decorator = new Token(new TextAttribute(pythonScriptColorProvider.getColor(rgbkey8), null, 1,new Font(Display.getCurrent(),f8)));
		className = new Token(new TextAttribute(pythonScriptColorProvider.getColor(rgbkey9), null, 1,new Font(Display.getCurrent(),f9)));
		funcName = new Token(new TextAttribute(pythonScriptColorProvider.getColor(rgbkey10), null, 1,new Font(Display.getCurrent(),f10)));
		//self = new Token(new TextAttribute(pythonScriptColorProvider.getColor(rgbkey11), null, 1,new Font(Display.getCurrent(),f11)));
		parenthesis = new Token(new TextAttribute(pythonScriptColorProvider.getColor(rgbkey12), null, 1,new Font(Display.getCurrent(),f12)));
		
		List<IRule> rules = new ArrayList<IRule>();
		rules.add(new MultiLineRule("\"\"\"", "\"\"\"", string,'\\'));
		// Add rule for single line comments.
		rules.add(new EndOfLineRule("#", comment));
		
	
		rules.add(new SingleLineRule("\"", "\"", string, '\\'));
		rules.add(new SingleLineRule("'", "'", string, '\\'));

		
		
		WhitespaceRule whitespaceRule;
        try {
            whitespaceRule = new WhitespaceRule(new GreatWhite(), defaultOther);
        } catch (Throwable e) {
            //Compatibility with Eclipse 3.4 and below.
            whitespaceRule = new WhitespaceRule(new GreatWhite());
        }
        rules.add(whitespaceRule);

        PyWordRule wordRule = new PyWordRule(new GreatKeywordDetector(), defaultOther, className, funcName,
        		parenthesis, operators);
        
        for (int i = 0; i < DEFAULT_KEYWORDS.length; i++)
			wordRule.addWord(DEFAULT_KEYWORDS[i], keyword);

        rules.add(wordRule);
        
        rules.add(new WordRule(new DecoratorDetector(), decorator));
        rules.add(new WordRule(new NumberDetector(), numbers));
		

		IRule[] result = new IRule[rules.size()];
		rules.toArray(result);
		setRules(result);
	}

	public IToken getComment() {
		return comment;
	}

	public IToken getKeyword() {
		return keyword;
	}

	public IToken getOther() {
		return defaultOther;
	}

	public IToken getString() {
		return string;
	}

	public IToken getType() {
		
		return type;
	}
	 /**
	* Whitespace detector.
	*
	* I know, naming the class after a band that burned
	* is not funny, but I've got to get my brain off my
	* annoyance with the abstractions of JFace.
	* So many classes and interfaces for a single method?
	* f$%@#$!!
	*/
	    static private class GreatWhite implements IWhitespaceDetector {
	        public boolean isWhitespace(char c) {
	            return Character.isWhitespace(c);
	        }
	    }
	
	 /**
	* Python keyword detector
	*/
	    static private class GreatKeywordDetector implements IWordDetector {

	        public GreatKeywordDetector() {
	        }

	        public boolean isWordStart(char c) {
	            return Character.isJavaIdentifierStart(c);
	        }

	        public boolean isWordPart(char c) {
	            return Character.isJavaIdentifierPart(c);
	        }
	    }

	    static private class DecoratorDetector implements IWordDetector {

	        /**
	* @see org.eclipse.jface.text.rules.IWordDetector#isWordStart(char)
	*/
	        public boolean isWordStart(char c) {
	            return c == '@';
	        }

	        /**
	* @see org.eclipse.jface.text.rules.IWordDetector#isWordPart(char)
	*/
	        public boolean isWordPart(char c) {
	            return c != '\n' && c != '\r' && c != '(';
	        }

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
	            //ok, we have to test for scientific notation e.g.: 10.9e10

	            if ((c == 'x' || c == 'X') && buffer.length() == 1 && buffer.charAt(0) == '0') {
	                //it is an hexadecimal
	                buffer.append(c);
	                isInHexa = true;
	                return true;
	            } else {
	                buffer.append(c);
	            }

	            if (isInHexa) {
	                return Character.isDigit(c) || c == 'a' || c == 'A' || c == 'b' || c == 'B' || c == 'c' || c == 'C'
	                        || c == 'd' || c == 'D' || c == 'e' || c == 'E' || c == 'f' || c == 'F';

	            } else {
	                return Character.isDigit(c) || c == 'e' || c == '.';
	            }
	        }

	    }



	/**
	 * Update the text attributes associated with the tokens of this scanner as
	 * a color preference has been changed.
	 */

}
