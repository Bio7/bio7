/*Adapted from: https://github.com/antlr/symtab/tree/master/src/org/antlr/symtab */

package com.eco.bio7.reditor.antlr.ref;

import java.util.Map;

public interface Scope {
	public String getScopeName();
	
	public int getScopeNumber();

	public Map<String, RSymbol> getArguments();

	/** Where to look next for symbols */
	public Scope getEnclosingScope();

	/** Define a symbol in the current scope */
	public void define(RSymbol sym);

	/** Look up name in this scope or in enclosing scope if not here */
	public RSymbol resolve(String name);

	/** A special method for the refactor rename method! */
	public boolean isUsed();

	/** A special method for the refactor rename method! */
	public void resetUsed();
}
