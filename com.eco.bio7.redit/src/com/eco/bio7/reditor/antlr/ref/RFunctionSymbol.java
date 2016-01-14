/*Adapted from: https://github.com/antlr/symtab/tree/master/src/org/antlr/symtab */

package com.eco.bio7.reditor.antlr.ref;

import java.util.LinkedHashMap;
import java.util.Map;

public class RFunctionSymbol extends RSymbol implements Scope {
    Map<String, RSymbol> arguments = new LinkedHashMap<String, RSymbol>();
    Scope enclosingScope;
	private RGlobalScope globalScope;
    
    public RFunctionSymbol(String name, Scope enclosingScope, RGlobalScope globals) {
        super(name);
        this.enclosingScope = enclosingScope;
        this.globalScope=globals;
    }

    public RSymbol resolve(String name) {
    /*Only resolve symbol in local environment for R!*/
        RSymbol s = arguments.get(name);
        if ( s!=null ) 
        	return s;
        
     /*Check global environment*/
        if ( globalScope != null ) {
            return globalScope.resolve(name);
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
