package com.eco.bio7.javaeditors;

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
 *     M.Austenfeld - Minor changes for the Bio7 application
 *******************************************************************************/

import java.util.ArrayList;
import java.util.List;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.*;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import com.eco.bio7.javaeditor.Bio7EditorPlugin;


/**
 * A Java code scanner.
 */
public class JavaCodeScanner extends RuleBasedScanner {

	private static String[] fgKeywords= { "abstract","assert", "break", "case", "catch", "class", "continue", "default", "do", "else","enum", "extends", "final", "finally", "for", "if", "implements", "import", "instanceof", "interface", "native", "new", "package", "private", "protected", "public", "return", "static","strictfp","super", "switch", "synchronized", "this", "throw", "throws", "transient", "try", "volatile", "while" }; 
	private static String[] fgTypes= { "void", "boolean", "char", "byte", "short", "int", "long", "float", "double" }; 
	private static String[] fgConstants= { "false", "null", "true" }; 
	public Token keyword;
	public Token type;
	public Token string;
	public Token comment;
	public Token other;
	public Token operators;
	public Token braces;
	public Token numbers;
	public Token multiLinecomment;
	/**
	 * Creates a Java code scanner with the given color provider.
	 * 
	 * @param provider the color provider
	 */
	public JavaCodeScanner(JavaColorProvider provider) {

		IPreferenceStore store = Bio7EditorPlugin.getDefault().getPreferenceStore();
		RGB rgbkey = PreferenceConverter.getColor(store, "colourkey");
		RGB rgbkey1 = PreferenceConverter.getColor(store, "colourkey1");
		RGB rgbkey2 = PreferenceConverter.getColor(store, "colourkey2");
		RGB rgbkey3 = PreferenceConverter.getColor(store, "colourkey3");
		RGB rgbkey4 = PreferenceConverter.getColor(store, "colourkey4");
		RGB rgbkey5 = PreferenceConverter.getColor(store, "colourkey5");
		RGB rgbkey6 = PreferenceConverter.getColor(store, "colourkey6");
		RGB rgbkey7 = PreferenceConverter.getColor(store, "colourkey7");
		//RGB rgbkey8 = PreferenceConverter.getColor(store, "colourkey8");
		
		FontData f=PreferenceConverter.getFontData(store, "colourkeyfont");
		FontData f1=PreferenceConverter.getFontData(store, "colourkeyfont1");
		FontData f2=PreferenceConverter.getFontData(store, "colourkeyfont2");
		FontData f3=PreferenceConverter.getFontData(store, "colourkeyfont3");
		FontData f4=PreferenceConverter.getFontData(store, "colourkeyfont4");
		FontData f5=PreferenceConverter.getFontData(store, "colourkeyfont5");
		FontData f6=PreferenceConverter.getFontData(store, "colourkeyfont6");
		FontData f7=PreferenceConverter.getFontData(store, "colourkeyfont7");
		//FontData f8=PreferenceConverter.getFontData(store, "colourkeyfont8");
		

		keyword = new Token(new TextAttribute(provider.getColor(rgbkey), null, 1,new Font(Display.getCurrent(),f)));
		type = new Token(new TextAttribute(provider.getColor(rgbkey1), null, 1,new Font(Display.getCurrent(),f1)));
		string = new Token(new TextAttribute(provider.getColor(rgbkey2), null, 1,new Font(Display.getCurrent(),f2)));
		comment = new Token(new TextAttribute(provider.getColor(rgbkey3), null, 1,new Font(Display.getCurrent(),f3)));
		other = new Token(new TextAttribute(provider.getColor(rgbkey4), null, 1,new Font(Display.getCurrent(),f4)));
		operators = new Token(new TextAttribute(provider.getColor(rgbkey5), null, 1,new Font(Display.getCurrent(),f5)));
		braces = new Token(new TextAttribute(provider.getColor(rgbkey6), null, 1,new Font(Display.getCurrent(),f6)));
		numbers = new Token(new TextAttribute(provider.getColor(rgbkey7), null, 1,new Font(Display.getCurrent(),f7)));
		//multiLinecomment = new Token(new TextAttribute(provider.getColor(rgbkey8), null, 1,new Font(Display.getCurrent(),f8)));

		List rules= new ArrayList();
		//rules.add(new MultiLineRule("/*", "*/", multiLinecomment));
		// Add rule for single line comments.
		rules.add(new EndOfLineRule("//", comment)); 
		
		// Add rule for strings and character constants.
		rules.add(new SingleLineRule("\"", "\"", string, '\\')); 
		rules.add(new SingleLineRule("'", "'", string, '\\')); 

		// Add generic whitespace rule.
		rules.add(new WhitespaceRule(new JavaWhitespaceDetector()));
		rules.add(new COperatorRule(operators));
		rules.add(new JavaEditorBraceRule(braces));
		rules.add(new NumberRule(numbers));
		
		// Add word rule for keywords, types, and constants.
		WordRule wordRule= new WordRule(new JavaWordDetector(), other);
		for (int i= 0; i < fgKeywords.length; i++)
			wordRule.addWord(fgKeywords[i], keyword);
		for (int i= 0; i < fgTypes.length; i++)
			wordRule.addWord(fgTypes[i], type);
		for (int i= 0; i < fgConstants.length; i++)
			wordRule.addWord(fgConstants[i], type);
		rules.add(wordRule);

		IRule[] result= new IRule[rules.size()];
		rules.toArray(result);
		setRules(result);
	}
}
