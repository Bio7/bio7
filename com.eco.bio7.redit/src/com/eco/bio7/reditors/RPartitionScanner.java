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

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.*;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

import com.eco.bio7.reditor.Bio7REditorPlugin;



/*Leave this class for maybe later use!*/
public class RPartitionScanner extends RuleBasedPartitionScanner {

	//public final static String R_DEFAULT = "RDefault"; //$NON-NLS-1$

	//public final static String R_DOC = "__java_javadoc"; //$NON-NLS-1$
	
	//public final static String R_STRING = "__r_string";

	public final static String[] R_PARTITION_TYPES = new String[] { IDocument.DEFAULT_CONTENT_TYPE,"R_MULTILINE_STRING" };

	public Token rString;

	/**
	 * Detector for empty comments.
	 */
	/*static class EmptyCommentDetector implements IWordDetector {

		
		
		 
		public boolean isWordStart(char c) {
			return (c == '/');
		}

		
	
		 
		public boolean isWordPart(char c) {
			return (c == '*' || c == '/');
		}
	}*/

	/**
	 * 
	 */
	/*static class WordPredicateRule extends WordRule implements IPredicateRule {

		private IToken fSuccessToken;

		public WordPredicateRule(IToken successToken) {
			super(new EmptyCommentDetector());
			fSuccessToken = successToken;*/
			//addWord("/**/", fSuccessToken);
		//}

		
		/*public IToken evaluate(ICharacterScanner scanner, boolean resume) {
			return super.evaluate(scanner);
		}

		
		
		 
		public IToken getSuccessToken() {
			return fSuccessToken;
		}
	}*/

	/**
	 * Creates the partitioner and sets up the appropriate rules.
	 */
	public RPartitionScanner() {
		super();

		//IToken comment = new Token(R_MULTILINE_COMMENT);
		
		
		 rString = new Token("R_MULTILINE_STRING");
		
		 /*RColorProvider provider = Bio7REditorPlugin.getDefault().getRColorProvider();
		 IPreferenceStore store = Bio7REditorPlugin.getDefault().getPreferenceStore();
		
			
			RGB rgbkey2 = PreferenceConverter.getColor(store, "colourkey2");
			

			
		FontData f2 = PreferenceConverter.getFontData(store, "colourkeyfont2");
		 rString.setData(new TextAttribute(provider.getColor(rgbkey2), null, 1, new Font(Display.getCurrent(), f2)));*/
		
		List<PatternRule> rules = new ArrayList<PatternRule>();
		rules.add(new MultiLineRule("\"", "\"", rString));
		rules.add(new MultiLineRule("\'", "\'", rString));

		// Add rule for single line comments.
		rules.add(new EndOfLineRule("#", Token.UNDEFINED)); //$NON-NLS-1$
		
		// Add rule for strings and character constants.
		//rules.add(new SingleLineRule("\"", "\"", Token.UNDEFINED, '\\')); 
		//rules.add(new SingleLineRule("'", "'", Token.UNDEFINED, '\\')); 

		// Add special case word rule.
		//rules.add(new WordPredicateRule(comment));
		
		
		

		IPredicateRule[] result = new IPredicateRule[rules.size()];
		rules.toArray(result);
		setPredicateRules(result);
	}
}
