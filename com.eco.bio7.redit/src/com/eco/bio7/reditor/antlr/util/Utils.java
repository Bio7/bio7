package com.eco.bio7.reditor.antlr.util;

import org.antlr.v4.runtime.ParserRuleContext;

import com.eco.bio7.reditor.antlr.RParser.SublistContext;

public class Utils {
	
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

}
