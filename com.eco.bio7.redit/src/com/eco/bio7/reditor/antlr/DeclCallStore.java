package com.eco.bio7.reditor.antlr;

import java.util.HashSet;
import java.util.Set;

/**
 * @author M. Austenfeld
 * A class to substract two Sets with the purpose to use in a stack to find unused method and var declarations.
 */
public class DeclCallStore {
	Set<String> decl = new HashSet<String>();
	Set<String> call = new HashSet<String>();
	
	public Set<String> substract(){
		decl.removeAll(call);
		
		return decl;
	}
}

