package com.eco.bio7.reditor.antlr;

import java.util.HashSet;
import java.util.Set;

/**
 * @author M. Austenfeld A class to substract two Sets (methods variables) with the purpose to use
 *         in a stack to find unused method and var declarations.
 */
public class DeclCallStore {
	Set<String> methDecl = new HashSet<String>();
	Set<String> methCall = new HashSet<String>();

	Set<String> varDecl = new HashSet<String>();
	Set<String> varCall = new HashSet<String>();

	/* Substract the method calls from the method declarations! */
	public Set<String> substract() {
		methDecl.removeAll(methCall);

		return methDecl;
	}

	/* Substract the variable calls from the method declarations! */
	public Set<String> substractVars() {
		varDecl.removeAll(varCall);

		return varDecl;
	}
}
