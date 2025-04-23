package com.eco.bio7.reditor.antlr;

/*Source from: http://stackoverflow.com/questions/15050137/once-grammar-is-complete-whats-the-best-way-to-walk-an-antlr-v4-tree*/
import java.util.HashSet;
import java.util.Map;

import com.eco.bio7.reditor.antlr.ref.Scope;
import com.eco.bio7.reditor.antlr.ref.RSymbol;

public class RScope extends HashSet<String> implements Scope {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	final RScope parent;

	public RScope(RScope parent) {
		this.parent = parent;
	}

	boolean inScope(String varName) {
		if (super.contains(varName)) {
			return true;
		}
		return parent == null ? false : parent.inScope(varName);
	}

	@Override
	public String getScopeName() {
		return null;
	}

	@Override
	public Scope getEnclosingScope() {
		return null;
	}

	@Override
	public void define(RSymbol sym) {

	}

	@Override
	public RSymbol resolve(String name) {
		return null;
	}

	@Override
	public Map<String, RSymbol> getArguments() {
		return null;
	}

	@Override
	public boolean isUsed() {
		return false;
	}

	@Override
	public void resetUsed() {

	}

	@Override
	public int getScopeNumber() {
		// TODO Auto-generated method stub
		return 0;
	}

}