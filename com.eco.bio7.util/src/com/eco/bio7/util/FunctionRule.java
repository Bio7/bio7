/*******************************************************************************
 * Copyright (c) 2007-2026 M. Austenfeld
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 *
 * Contributors:
 * Marcel Austenfeld - initial API and implementation
 *******************************************************************************/
package com.eco.bio7.util;

import java.util.Set;

import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;

/**
 * Colors the functions of a code editor (ImageJ macro, Beanshell/Groovy,
 * Python): a name followed by '(' is a call; after a definition word (e.g.,
 * 'function', 'def', 'void', a type) it is a definition. The words to skip
 * (keywords, constants) and the names after other words (e.g., 'class') are
 * left to the other rules.
 */
public class FunctionRule implements IRule {

	private final IToken call;
	private final IToken definition;
	private final Set<String> skip;
	private final Set<String> definitionWords;
	private final Set<String> ignoreAfter;
	/* The word before (e.g., 'function') */
	private String lastWord = "";

	/**
	 * @param call            the token of a call
	 * @param definition      the token of a definition (null: none)
	 * @param skip            the words which are no functions (keywords, ...)
	 * @param definitionWords the words before the name of a definition
	 * @param ignoreAfter     the words after which a name is left to other rules
	 */
	public FunctionRule(IToken call, IToken definition, Set<String> skip, Set<String> definitionWords, Set<String> ignoreAfter) {
		this.call = call;
		this.definition = definition;
		this.skip = skip;
		this.definitionWords = definitionWords;
		this.ignoreAfter = ignoreAfter;
	}

	@Override
	public IToken evaluate(ICharacterScanner scanner) {
		int c = scanner.read();
		if (c == ICharacterScanner.EOF || !(Character.isLetter(c) || c == '_')) {
			scanner.unread();
			return Token.UNDEFINED;
		}
		StringBuilder name = new StringBuilder().append((char) c);
		int read = 1;
		while (true) {
			c = scanner.read();
			read++;
			if (c == ICharacterScanner.EOF || !(Character.isLetterOrDigit(c) || c == '_')) {
				break;
			}
			name.append((char) c);
		}
		String word = name.toString();
		String before = lastWord;
		lastWord = word;
		if (skip.contains(word) || ignoreAfter.contains(before)) {
			unread(scanner, read);
			return Token.UNDEFINED;
		}
		/* The next character which is not a space */
		int extra = 0;
		int x = c;
		while (x == ' ' || x == '\t') {
			x = scanner.read();
			extra++;
		}
		for (int i = 0; i < extra; i++) {
			scanner.unread();
		}
		/* A call or (after a definition word) a definition: name( */
		IToken token = null;
		if (x == '(') {
			token = definition != null && definitionWords.contains(before) ? definition : call;
		}
		if (token == null) {
			unread(scanner, read);
			return Token.UNDEFINED;
		}
		/* The token is the name (the character after it is unread) */
		scanner.unread();
		return token;
	}

	private static void unread(ICharacterScanner scanner, int count) {
		for (int i = 0; i < count; i++) {
			scanner.unread();
		}
	}
}
