package com.eco.bio7.reditor.antlr;
/*Source from: http://stackoverflow.com/questions/15050137/once-grammar-is-complete-whats-the-best-way-to-walk-an-antlr-v4-tree*/
import java.util.HashSet;

class RScope extends HashSet<String> {

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
}