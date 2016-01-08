/*Adapted from: https://github.com/antlr/symtab/tree/master/src/org/antlr/symtab */
package com.eco.bio7.reditor.antlr.ref;

public class RSymbol { 
   

    String name;      
  
    Scope scope;      //The scope!

    public RSymbol(String name) { this.name = name; }
    
    public String getName() { return name; }

    public String toString() {
        
        return getName();
    }
}
