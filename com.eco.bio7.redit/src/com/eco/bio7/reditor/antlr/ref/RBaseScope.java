/*Adapted from: https://github.com/antlr/symtab/tree/master/src/org/antlr/symtab */
package com.eco.bio7.reditor.antlr.ref;

import java.util.LinkedHashMap;
import java.util.Map;

public abstract class RBaseScope implements Scope {
	Scope enclosingScope; // Returns null if scope is base scope!
	Map<String, RSymbol> symbols = new LinkedHashMap<String, RSymbol>();
	
	 public Map<String, RSymbol> getArguments() {
			return symbols;
		}

	public RBaseScope(Scope enclosingScope) {
		this.enclosingScope = enclosingScope;
	}

	/*
	 * Only resolve symbol in local scope and parent scopes up to the global
	 * scope!
	 */
	public RSymbol resolve(String name) {
		 /*Resolve symbol in nested environments for R!*/
		RSymbol s = symbols.get(name);
		if (s != null)
			return s;
		if (enclosingScope != null) {
			return enclosingScope.resolve(name);
		}
		return null; 

	}

	public void define(RSymbol sym) {
		symbols.put(sym.name, sym);
		sym.scope = this; // track the scope in each symbol
	}

	public Scope getEnclosingScope() {
		return enclosingScope;
	}

	public String toString() {
		return getScopeName() + ":" + symbols.keySet().toString();
	}
}
