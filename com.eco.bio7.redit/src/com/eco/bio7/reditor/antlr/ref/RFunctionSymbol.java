/*Adapted from: https://github.com/antlr/symtab/tree/master/src/org/antlr/symtab */

package com.eco.bio7.reditor.antlr.ref;

import java.util.LinkedHashMap;
import java.util.Map;

import com.eco.bio7.reditor.antlr.RParser.FormlistContext;

public class RFunctionSymbol extends RSymbol implements Scope {

	private Scope enclosingScope;
	private FormlistContext formlist;
	private boolean used;
	private Map<String, RSymbol> arguments = new LinkedHashMap<String, RSymbol>();
	private int scopeNumber;

	public int getScopeNumber() {
		return scopeNumber;
	}

	public Map<String, RSymbol> getArguments() {
		return arguments;
	}

	/*
	 * public boolean isUsed() { return used; }
	 */

	public void setUsed(boolean used) {
		this.used = used;
	}

	public FormlistContext getFormlist() {
		return formlist;
	}

	public RFunctionSymbol(String name, Scope enclosingScope, FormlistContext formlistContext, int scopeNumber) {
		super(name);
		this.enclosingScope = enclosingScope;
		this.formlist = formlistContext;
		this.scopeNumber=scopeNumber;
	}

	public RSymbol resolve(String name) {
		/* Resolve symbol in nested environments for R! */
		RSymbol s = arguments.get(name);
		if (s != null)
			return s;
		if (getEnclosingScope() != null) {
			return getEnclosingScope().resolve(name);
		}
		return null;

	}

	/* Resolve a used var for the rename refactor method! */
	public boolean isUsed() {
		/* If we are not in the base scope! */
		if (getEnclosingScope() != null) {
			boolean var = getEnclosingScope().isUsed();
			if (var == true) {
				return var;
			}

		}

		return used;

	}

	/* Resolve a used var for the rename refactor method! */
	public void resetUsed() {
		/* If we are not in the base scope! */
		used=false;
		if (getEnclosingScope() != null) {
			getEnclosingScope().resetUsed();

		}

	}

	public void define(RSymbol sym) {
		arguments.put(sym.name, sym);
		sym.scope = this;
	}

	public Scope getEnclosingScope() {
		return enclosingScope;
	}

	public String getScopeName() {
		return name;
	}

	public String toString() {
		return "function" + super.toString() + ": " + arguments.values();
	}
}
