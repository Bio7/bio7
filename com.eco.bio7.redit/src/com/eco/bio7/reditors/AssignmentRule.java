package com.eco.bio7.reditors;

import org.eclipse.jface.text.rules.*;

public class AssignmentRule implements IRule {
	private final IToken token;

	public AssignmentRule(IToken token) {
		this.token = token;
	}

	public IToken evaluate(ICharacterScanner scanner) {
		int c = scanner.read();
		if (c == '<') {
			c = scanner.read();
			if (c == '-') {

				return token;
			}

			else if (c == '<') {
				c = scanner.read();
				if (c == '-') {

					return token;
				}
			}
			scanner.unread();
		} else if (c == '-') {

			c = scanner.read();
			if (c == '>') {

				c = scanner.read();
				if (c == '>') {
					return token;
				} 

			}
			else{
				scanner.unread();
			}
		}
		
		scanner.unread();
		return Token.UNDEFINED;
	}
}