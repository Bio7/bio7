/*Adapted from: https://github.com/antlr/symtab/tree/master/src/org/antlr/symtab */

package com.eco.bio7.reditor.antlr.ref;

import java.util.LinkedHashMap;
import java.util.Map;

public class RFunctionSymbol extends RSymbol implements Scope {
    Map<String, RSymbol> arguments = new LinkedHashMap<String, RSymbol>();
    Scope enclosingScope;
	
    
    public RFunctionSymbol(String name, Scope enclosingScope) {
        super(name);
        this.enclosingScope = enclosingScope;
        
    }

    public RSymbol resolve(String name) {
    /*Resolve symbol in nested environments for R!*/
    	RSymbol s = arguments.get(name);
        if ( s!=null ) return s;
        if ( getEnclosingScope() != null ) {
            return getEnclosingScope().resolve(name);
        }
        return null; 


        
    }

    public void define(RSymbol sym) {
        arguments.put(sym.name, sym);
        sym.scope = this; 
    }

    public Scope getEnclosingScope() { return enclosingScope; }
    public String getScopeName() { return name; }

    public String toString() { return "function"+super.toString()+":"+arguments.values(); }
}
