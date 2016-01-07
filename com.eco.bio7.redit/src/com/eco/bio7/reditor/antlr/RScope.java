package com.eco.bio7.reditor.antlr;
/*Source from: http://stackoverflow.com/questions/15050137/once-grammar-is-complete-whats-the-best-way-to-walk-an-antlr-v4-tree*/
import java.util.HashSet;

import com.eco.bio7.reditor.antlr.ref.Scope;
import com.eco.bio7.reditor.antlr.ref.RSymbol;

class RScope extends HashSet<String> implements Scope{

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	final RScope parent;

    public RScope(RScope parent) {
        this.parent = parent;
    }

    boolean inScope(String varName) {
        if(super.contains(varName)) {
            return true;
        }
        return parent == null ? false : parent.inScope(varName);
    }

	@Override
	public String getScopeName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Scope getEnclosingScope() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void define(RSymbol sym) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public RSymbol resolve(String name) {
		// TODO Auto-generated method stub
		return null;
	}
}