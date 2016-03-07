package com.eco.bio7.reditor.antlr.util;

import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;

import com.eco.bio7.reditor.antlr.RParser;
import com.eco.bio7.reditor.antlr.RParser.SublistContext;

public class Utils {
	/*Find out recursively if the variable assignment is in a method call (sublist)!*/
	public static boolean getCtxParent(ParserRuleContext p) {

		if (p.getParent() != null) {
			ParserRuleContext parent = p.getParent();
			if (parent instanceof SublistContext) {
				
				return true;

			} else {

				return getCtxParent(parent);
			}

		}

		return false;
	}
	/*
	 * Extract the token with the assignment operator and (to exclude whitespace
	 * in stream because of the hidden() rule the whitespace is present in the
	 * CommonTokenStream!)
	 */

	public static Token whitespaceTokenFilter(int start, int stop,CommonTokenStream tokens) {

		int i = start + 1;
		Token assignOp = null;
		while (i <= stop) {
			Token tok = tokens.get(i);
			if (tok.getType() != RParser.WS) {
				assignOp = tok;
				break;
			}
			if (tok.getType() == RParser.EOF) {
				break;
			}
			i++;
		}

		return assignOp;
	}

}
