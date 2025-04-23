/*Adapted from: https://github.com/antlr/symtab/tree/master/src/org/antlr/symtab */
package com.eco.bio7.reditor.antlr.ref;

public class RGlobalScope extends RBaseScope {
	public RGlobalScope(Scope enclosingScope) {
		super(enclosingScope);
	}

	public String getScopeName() {
		return "globals";
	}

	@Override
	public boolean isUsed() {

		return false;
	}

	/* Empty method in the global scope! */
	public void resetUsed() {

	}

	@Override
	public int getScopeNumber() {
		// TODO Auto-generated method stub
		return 0;
	}

}
