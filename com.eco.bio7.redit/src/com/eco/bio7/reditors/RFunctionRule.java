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
package com.eco.bio7.reditors;

import java.util.Set;

import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.Token;

/**
 * Colors the functions of R code: a name followed by '(' (a call, also
 * na.omit(), data.frame()), the name of a function definition (name <-
 * function(...), name = function, \(x)) and a package prefix (pkg::name). The
 * keywords and constants (if (, function(, TRUE) are left to the word rule.
 */
public class RFunctionRule implements IRule {

	private final IToken call;
	private final IToken definition;
	private final IToken namespace;
	private final Set<String> skip;

	/**
	 * @param skip the words which are not functions (keywords, constants)
	 */
	public RFunctionRule(IToken call, IToken definition, IToken namespace, Set<String> skip) {
		this.call = call;
		this.definition = definition;
		this.namespace = namespace;
		this.skip = skip;
	}

	private static boolean isStart(int c) {
		return Character.isLetter(c) || c == '.';
	}

	private static boolean isPart(int c) {
		return Character.isLetterOrDigit(c) || c == '.' || c == '_';
	}

	@Override
	public IToken evaluate(ICharacterScanner scanner) {
		int c = scanner.read();
		if (c == ICharacterScanner.EOF || !isStart(c)) {
			scanner.unread();
			return Token.UNDEFINED;
		}
		/* A number like .5 is no name */
		if (c == '.') {
			int d = scanner.read();
			scanner.unread();
			if (Character.isDigit(d)) {
				scanner.unread();
				return Token.UNDEFINED;
			}
		}
		StringBuilder name = new StringBuilder().append((char) c);
		int read = 1;
		while (true) {
			c = scanner.read();
			read++;
			if (c == ICharacterScanner.EOF || !isPart(c)) {
				break;
			}
			name.append((char) c);
		}
		/* The character after the name was read */
		String word = name.toString();
		if (skip.contains(word)) {
			unread(scanner, read);
			return Token.UNDEFINED;
		}
		/* Look ahead (the characters are unread afterwards) */
		StringBuilder ahead = new StringBuilder();
		ahead.append((char) c);
		int extra = 0;
		while (ahead.length() < 24) {
			int x = scanner.read();
			extra++;
			if (x == ICharacterScanner.EOF || x == '\n' || x == '\r') {
				break;
			}
			ahead.append((char) x);
		}
		for (int i = 0; i < extra; i++) {
			scanner.unread();
		}
		String a = ahead.toString();
		IToken token = null;
		if (a.startsWith("::")) {
			token = namespace;
		} else {
			String t = a.stripLeading();
			if (t.startsWith("(")) {
				token = call;
			} else if (t.matches("^(<<-|<-|=(?!=))\\s*(function\\b|\\\\\\().*")) {
				token = definition;
			}
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
