/*******************************************************************************
 * Copyright (c) 2005-2017 M. Austenfeld
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/
package com.eco.bio7.reditor.antlr;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.runtime.tree.TerminalNodeImpl;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.texteditor.IDocumentProvider;
import com.eco.bio7.reditor.Bio7REditorPlugin;
import com.eco.bio7.reditor.antlr.RParser.E17VariableDeclarationContext;
import com.eco.bio7.reditor.antlr.RParser.E20CallFunctionContext;
import com.eco.bio7.reditor.antlr.RParser.E30Context;
import com.eco.bio7.reditor.antlr.RParser.ExprContext;
import com.eco.bio7.reditor.antlr.RParser.FormContext;
import com.eco.bio7.reditor.antlr.RParser.SubContext;
import com.eco.bio7.reditor.antlr.RParser.SublistContext;
import com.eco.bio7.reditor.antlr.ref.RFunctionSymbol;
import com.eco.bio7.reditor.antlr.ref.RGlobalScope;
import com.eco.bio7.reditor.antlr.ref.RSymbol;
import com.eco.bio7.reditor.antlr.ref.RVariableSymbol;
import com.eco.bio7.reditor.antlr.ref.Scope;
import com.eco.bio7.reditor.antlr.util.Utils;
import com.eco.bio7.reditor.outline.REditorOutlineNode;
import com.eco.bio7.reditors.REditor;

public class RBaseListen extends RBaseListener {

	private CommonTokenStream tokens;
	public ArrayList<String> startStop = new ArrayList<String>();
	private REditor editor;
	private Parser parser;
	/* A stack for nested nodes! */
	private Stack<REditorOutlineNode> methods = new Stack<REditorOutlineNode>();
	// private Stack<RScope> scopes = new Stack<RScope>();// Just for variable
	// lookup in current
	// scope!
	/* A stack to store the method declarations and calls in scope! */
	private Stack<DeclCallStore> storeDeclCall = new Stack<DeclCallStore>();
	public Set<String> finalFuncDecl = new HashSet<String>();
	public Set<String> finalVarDecl = new HashSet<String>();

	private IPreferenceStore store;
	public ParseTreeProperty<Scope> scopeNew = new ParseTreeProperty<Scope>();
	public RGlobalScope globals;
	public Scope currentScope; // Define symbols in this scope.
	private int scopeNumber = 0;// 0=global scope! Incremental function marker for the refactor rename action!

	public RBaseListen(CommonTokenStream tokens, REditor editor, Parser parser) {
		this.tokens = tokens;
		this.editor = editor;
		this.parser = parser;

		store = Bio7REditorPlugin.getDefault().getPreferenceStore();

	}

	public void enterProg(RParser.ProgContext ctx) {

		// scopes.push(new RScope(null));

		globals = new RGlobalScope(null);
		currentScope = globals;

		storeDeclCall.push(new DeclCallStore());

	}

	public void exitProg(RParser.ProgContext ctx) {
		// storeDeclCall.pop();
		// System.out.println(globals);
		/* Exit scope! */
		// scopes.pop();
		if (methods.empty() == false) {
			methods.pop();
		}

		/* Has the function be called in this scope? */
		DeclCallStore st = storeDeclCall.peek();
		/* Avoid duplicates! */
		Set<String> subScope = st.substract();
		Set<String> subScopeVar = st.substractVars();

		/* Add the scoped uncalled functions to the global store! */
		finalFuncDecl.addAll(subScope);
		/* Add the scoped uncalled variables to the global store! */
		finalVarDecl.addAll(subScopeVar);
		/* Leave the scope (remove from stack)! */
		storeDeclCall.pop();
		currentScope = currentScope.getEnclosingScope(); // pop scope
	}

	public void exitE19DefFunction(RParser.E19DefFunctionContext ctx) {
		/* Exit scope! */
		// scopes.pop();
		if (methods.empty() == false) {
			methods.pop();
		}
		/* Has the function be called in this scope? */
		DeclCallStore st = storeDeclCall.peek();
		/* Make a snapshot of function call for bottom up transfer! */
		Set<String> tempCall = st.methCall;
		/* Make a snapshot of variable call for bottom up transfer! */
		Set<String> tempVarCall = st.varCall;

		Set<String> subScope = st.substract();

		Set<String> subScopeVar = st.substractVars();
		/* Add the scoped uncalled functions to the global store! */

		finalFuncDecl.addAll(subScope);

		finalVarDecl.addAll(subScopeVar);
		/* Leave the scope (remove from stack)! */
		storeDeclCall.pop();

		DeclCallStore stAdd = storeDeclCall.peek();
		/*
		 * Add the function calls to the parent scope (function) if it is called there!
		 */
		stAdd.methCall.addAll(tempCall);

		stAdd.varCall.addAll(tempVarCall);
		currentScope = currentScope.getEnclosingScope(); // pop scope

	}

	public void enterE19DefFunction(RParser.E19DefFunctionContext ctx) {
		/*
		 * Insert function as current scope with a parent current scope (scope.peek)!
		 */

		// scopes.push(new RScope(scopes.peek()));
		/* Incremental function counter to identify functions! */
		scopeNumber++;
		Token firstToken = ctx.getStart();
		Token lastToken = ctx.getStop();

		Interval sourceInterval = ctx.getSourceInterval();
		int start = sourceInterval.a;

		int lineStart = firstToken.getStartIndex();

		int lineEnd = lastToken.getStopIndex() + 1 - lineStart;

		// Add to the editor folding action if enabled in the preferences!
		if (store.getBoolean("FUNCTIONS_FOLDING")) {
			startStop.add(lineStart + "," + lineEnd);
		}
		int lineMethod = calculateLine(lineStart);

		/*
		 * If we have at least 2 tokens else we create a function without variable
		 * assignment!
		 */
		if ((start - 2) >= 0 && ctx.getParent().getChild(1) != null && ctx.getParent().getChild(2) != null) {

			String op = ctx.getParent().getChild(1).getText();
			String name = ctx.getParent().getChild(0).getText();

			/*
			 * Check if we have an assignment symbol available! else we create a function
			 * without variable assignment!
			 */
			if (op.equals("<-") || op.equals("<<-")) {
				/*
				 * Here we check for already defined functions without to return because we add
				 * the call arguments to the known symbol table. We would also get with this
				 * implementation a null pointer exception
				 */
				alreadyDefined(firstToken, name);
				/* Create a new scope and add the function (symbol)! */
				RFunctionSymbol function = new RFunctionSymbol(name, currentScope, ctx.formlist(), scopeNumber);
				currentScope.define(function); // Define function in current
												// //
												// scope
				scopeNew.put(ctx, function);
				currentScope = function;

				/* Put the method declaration name in the call set! */
				/* Get the current scope stack elements! */
				DeclCallStore st = storeDeclCall.peek();

				/* add the called method to the call set! */
				st.methDecl.add(name);
				storeDeclCall.push(new DeclCallStore());
				/*
				 * Create the function arguments as known symbol table vars in current scope!
				 */
				if (ctx.formlist() != null) {
					List<FormContext> formList = ctx.formlist().form();
					int functionDefSize = formList.size();
					for (int i = 0; i < functionDefSize; i++) {
						FormContext fo = formList.get(i);

						TerminalNode ar = fo.ID();
						if (ar != null) {
							RVariableSymbol var = new RVariableSymbol(ar.getText());
							currentScope.define(var);
						}

					}
				}

				/* Here we create the outline nodes in the Outline view! */
				if (methods.size() == 0) {

					methods.push(new REditorOutlineNode(name, lineMethod, "function", editor.baseNode));

				} else {
					methods.push(new REditorOutlineNode(name, lineMethod, "function", methods.peek()));

				}

			}
			/*
			 * We treat the equal assignment different which could occur in function calls
			 * (e.g. in reference classes). Defined variables are ignored for code analysis
			 * and added to the declared and called variables!
			 */
			else if (op.equals("=")) {

				/*
				 * Check and ignore if this assignment is inside a function call!
				 */
				/*
				 * boolean isSubTrue = Utils.getCtxParent(ctx);
				 * 
				 * 
				 * if (isSubTrue == true) { return; }
				 */
				/*
				 * Here we check for already defined functions without to return because we add
				 * the call arguments to the known symbol table. We would also get with this
				 * implementation a null pointer exception
				 */
				alreadyDefined(firstToken, name);
				/* Create a new scope and add the function (symbol)! */
				RFunctionSymbol function = new RFunctionSymbol(name, currentScope, ctx.formlist(), scopeNumber);
				currentScope.define(function); // Define function in current
												// //
												// scope
				scopeNew.put(ctx, function);
				currentScope = function;

				/* Put the method declaration name in the call set! */
				/* Get the current scope stack elements! */
				DeclCallStore st = storeDeclCall.peek();

				/*
				 * Add the method to the declare and call set even when it is on in a function
				 * call!!!
				 */
				st.methDecl.add(name);
				st.methCall.add(name);

				storeDeclCall.push(new DeclCallStore());
				/*
				 * Create the function arguments as known symbol table vars in current scope!
				 */
				if (ctx.formlist() != null) {
					List<FormContext> formList = ctx.formlist().form();
					int functionDefSize = formList.size();
					for (int i = 0; i < functionDefSize; i++) {
						FormContext fo = formList.get(i);

						TerminalNode ar = fo.ID();
						if (ar != null) {
							RVariableSymbol var = new RVariableSymbol(ar.getText());
							currentScope.define(var);
						}

					}
				}

				/* Here we create the outline nodes in the Outline view! */
				if (methods.size() == 0) {

					methods.push(new REditorOutlineNode(name, lineMethod, "function", editor.baseNode));

				} else {
					methods.push(new REditorOutlineNode(name, lineMethod, "function", methods.peek()));

				}

			}

			/*
			 * We need this extra case for embedded functions like
			 * 'MA<-markcorrint(amacrine,function(m1,m2){m1==m2})'
			 */
			else {

				createAnonymousFunction(ctx, lineMethod);

			}
		}

		else {
			createAnonymousFunction(ctx, lineMethod);

		}

	}

	private void createAnonymousFunction(RParser.E19DefFunctionContext ctx, int lineMethod) {
		/* Create a new scope and add the function (symbol)! */
		/*Replace the shorthand notation for anonymous functions!*/
		String afunText = ctx.start.getText().replace("\\", "function");
		RFunctionSymbol function = new RFunctionSymbol(afunText, currentScope, ctx.formlist(), scopeNumber);
		currentScope.define(function); // Define function in current scope
		scopeNew.put(ctx, function);
		currentScope = function;
		/* Put the method declaration name in the call set! */
		/* Get the current scope stack elements! */
		DeclCallStore st = storeDeclCall.peek();
		/* add the method to the call set! */
		st.methDecl.add(afunText);
		storeDeclCall.push(new DeclCallStore());
		/* Here we create the outline nodes in the Outline view! */
		if (methods.size() == 0) {

			methods.push(new REditorOutlineNode("aFun", lineMethod, "anonymousFunction", editor.baseNode));

		} else {
			methods.push(new REditorOutlineNode("aFun", lineMethod, "anonymousFunction", methods.peek()));

		}
	}

	/* If condition! */
	public void enterE21(RParser.E21Context ctx) {

		Token firstToken = ctx.getStart();
		Token lastToken = ctx.getStop();
		int lineStart = firstToken.getStartIndex();

		int lineEnd = lastToken.getStopIndex() + 1 - lineStart;

		// Add to the editor folding action if enabled in the preferences!
		if (store.getBoolean("IF_CONDITION_FOLDING")) {
			startStop.add(lineStart + "," + lineEnd);
		}

	}

	/* If condition 2 of grammar file! */
	public void enterE22(RParser.E22Context ctx) {

		Token firstToken = ctx.getStart();
		Token lastToken = ctx.getStop();
		int lineStart = firstToken.getStartIndex();

		int lineEnd = lastToken.getStopIndex() + 1 - lineStart;

		// Add to the editor folding action if enabled in the preferences!
		if (store.getBoolean("IF_CONDITION_FOLDING")) {
			startStop.add(lineStart + "," + lineEnd);
		}

	}

	/* For loop! */
	public void enterE23ForLoop(RParser.E23ForLoopContext ctx) {

		Token firstToken = ctx.getStart();
		String loopVar = ctx.ID().getText();
		Token lastToken = ctx.getStop();
		int lineStart = firstToken.getStartIndex();

		int lineEnd = lastToken.getStopIndex() + 1 - lineStart;

		// Add to the editor folding action if enabled in the preferences!
		if (store.getBoolean("FOR_LOOP_FOLDING")) {
			startStop.add(lineStart + "," + lineEnd);
		}

		/* Create a new a new var of the loop in current scope! */
		// RVariableSymbol var = new RVariableSymbol(loopVar);
		// currentScope.define(var);

		if (methods.size() == 0) {
			if (alreadyDefined(ctx.ID().getSymbol(), loopVar)) {
				// RScope scope = scopes.peek();
				// scope.add(name);
				int line = calculateLine(lineStart);

				/* Create a new a new var in current scope! */
				RVariableSymbol var = new RVariableSymbol(loopVar);
				currentScope.define(var);

				DeclCallStore st = storeDeclCall.peek();
				/*
				 * Add the called var to the call set to detect unused variables!
				 */

				st.varDecl.add(loopVar);

				new REditorOutlineNode(loopVar, line, "loopVariable", editor.baseNode);
			}

		} else {
			if (alreadyDefined(ctx.ID().getSymbol(), loopVar)) {
				// RScope scope = scopes.peek();
				// scope.add(name);
				int line = calculateLine(lineStart);

				/* Create a new a new var in current scope! */
				RVariableSymbol var = new RVariableSymbol(loopVar);
				currentScope.define(var); // Define symbol in
											// current scope
				DeclCallStore st = storeDeclCall.peek();
				/*
				 * Add the called var to the call set to detect unused vaiables!
				 */
				st.varDecl.add(loopVar);

				new REditorOutlineNode(loopVar, line, "loopVariable", methods.peek());
			}

		}

	}

	/* While loop! */
	public void enterE24(RParser.E24Context ctx) {

		Token firstToken = ctx.getStart();
		Token lastToken = ctx.getStop();

		int lineStart = firstToken.getStartIndex();

		int lineEnd = lastToken.getStopIndex() + 1 - lineStart;

		// Add to the editor folding action if enabled in the preferences!
		if (store.getBoolean("WHILE_LOOP_FOLDING")) {
			startStop.add(lineStart + "," + lineEnd);
		}

	}

	/* repeat loop! */
	public void enterE25(RParser.E25Context ctx) {

		Token firstToken = ctx.getStart();
		Token lastToken = ctx.getStop();

		int lineStart = firstToken.getStartIndex();

		int lineEnd = lastToken.getStopIndex() + 1 - lineStart;

		// Add to the editor folding action if enabled in the preferences!
		if (store.getBoolean("REPEAT_LOOP_FOLDING")) {
			startStop.add(lineStart + "," + lineEnd);
		}

	}

	@Override
	public void enterE17VariableDeclaration(RParser.E17VariableDeclarationContext ctx) {

		Interval sourceInterval = ctx.getSourceInterval();

		Token firstToken = ctx.getStart();
		int start = sourceInterval.a;
		// int stop = sourceInterval.b;

		/* Throw out variable assignment of function calls! */

		/*
		 * if (ctx.expr(0) instanceof E20CallFunctionContext) {
		 * 
		 * return;
		 * 
		 * 
		 * }
		 */

		/* Now we only want to consider variables with an ID! (E30Context) */
		if (ctx.expr(0) instanceof E30Context == false) {
			return;
		}
		/* Check and ignore if this assignment is inside a function call! */
		/*
		 * boolean isSubTrue = Utils.getCtxParent(ctx.expr(0));
		 * 
		 * // System.out.println("has Sub?: " + isSubTrue); if (isSubTrue == true) {
		 * return; }
		 */

		String isFunc = ctx.expr(1).start.getText();
		// System.out.println("fun detetcted?= " + ctx.expr(1).getText() + "Is
		// Type! " + ctx.expr(1).getClass());
		if (isFunc.equals("function") == false&&isFunc.equals("\\") == false) {

			int lineStart = firstToken.getStartIndex();

			Token assignOp = ((TerminalNodeImpl) ctx.getChild(1)).getSymbol();

			// Token assignOp = whitespaceTokenFilter(start, stop);

			String op = assignOp.getText();
			if (op.equals("<-") || op.equals("<<-")) {
				String name = firstToken.getText();
				// System.out.println("varName=" +name+ "Is Type!"+
				// ctx.expr(0).getParent().getParent().getClass());

				if (methods.size() == 0) {
					if (alreadyDefined(ctx.getStart(), name)) {
						// RScope scope = scopes.peek();
						// scope.add(name);
						int line = calculateLine(lineStart);
						/* Create a new a new var in current scope! */
						RVariableSymbol var = new RVariableSymbol(name);
						currentScope.define(var);

						DeclCallStore st = storeDeclCall.peek();
						/*
						 * Add the called var to the call set to detect unused variables!
						 */

						st.varDecl.add(name);

						if (isFunc.equals("setRefClass")) {
							new REditorOutlineNode(name, line, "refClass", editor.baseNode);
						} else if (isFunc.equals("R6Class")) {
							new REditorOutlineNode(name, line, "r6Class", editor.baseNode);
						}

						else {
							new REditorOutlineNode(name, line, "variable", editor.baseNode);
						}
					}

				} else {
					if (alreadyDefined(ctx.getStart(), name)) {
						// RScope scope = scopes.peek();
						// scope.add(name);
						int line = calculateLine(lineStart);
						/* Create a new a new var in current scope! */
						RVariableSymbol var = new RVariableSymbol(name);
						currentScope.define(var); // Define symbol in
													// current scope
						DeclCallStore st = storeDeclCall.peek();
						/*
						 * Add the called var to the call set to detect unused vaiables!
						 */
						st.varDecl.add(name);

						if (isFunc.equals("setRefClass")) {
							new REditorOutlineNode(name, line, "refClass", methods.peek());
						} else if (isFunc.equals("R6Class")) {
							new REditorOutlineNode(name, line, "r6Class", methods.peek());
						}

						else {
							new REditorOutlineNode(name, line, "variable", methods.peek());
						}
					}

				}
				checkConstantInVarAssignment(ctx);
			}
			/* We leave out the class detection for S3 here! */
			else if (op.equals("->") || op.equals("->>")) {
				String name = tokens.get(start + 2).getText();
				if (methods.size() == 0) {
					/* Is this variable already defined in the scope? */
					if (alreadyDefined(ctx.getStart(), name)) {
						// RScope scope = scopes.peek();
						// scope.add(name);
						int line = calculateLine(lineStart);
						/* Create a new a new var in current scope! */
						RVariableSymbol var = new RVariableSymbol(name);
						currentScope.define(var); // Define symbol in
													// current scope
						DeclCallStore st = storeDeclCall.peek();
						/*
						 * Add the called var to the call set to detect unused variables!
						 */
						st.varDecl.add(name);

						new REditorOutlineNode(name, line, "variable", editor.baseNode);
					}

				} else {
					/* Is this variable already defined in the scope? */
					if (alreadyDefined(ctx.getStart(), name)) {
						// RScope scope = scopes.peek();
						// scope.add(name);
						int line = calculateLine(lineStart);
						/* Create a new a new var in current scope! */
						RVariableSymbol var = new RVariableSymbol(name);
						currentScope.define(var);

						DeclCallStore st = storeDeclCall.peek();
						/*
						 * Add the called var to the call set to detect unused variables!
						 */
						st.varDecl.add(name);

						new REditorOutlineNode(name, line, "variable", methods.peek());
					}
				}

			}

			else if (op.equals("=")) {
				/*
				 * We check for wrong constants in function calls before the next check
				 * (variables ignored in function calls!)
				 */
				checkConstantInVarAssignment(ctx);

				/*
				 * Check and ignore if this assignment is inside a function call!
				 */
				boolean isSubTrue = Utils.getCtxParent(ctx.expr(0));

				if (isSubTrue == true) {
					return;
				}

				String name = firstToken.getText();
				// System.out.println("varName=" +name+ "Is Type!"+
				// ctx.expr(0).getParent().getParent().getClass());

				if (methods.size() == 0) {
					if (alreadyDefined(ctx.getStart(), name)) {
						// RScope scope = scopes.peek();
						// scope.add(name);
						int line = calculateLine(lineStart);
						/* Create a new a new var in current scope! */
						RVariableSymbol var = new RVariableSymbol(name);
						currentScope.define(var);

						DeclCallStore st = storeDeclCall.peek();
						/*
						 * Add the called var to the call set to detect unused variables!
						 */

						st.varDecl.add(name);

						if (isFunc.equals("setRefClass")) {
							new REditorOutlineNode(name, line, "refClass", editor.baseNode);
						}

						else if (isFunc.equals("R6Class")) {
							new REditorOutlineNode(name, line, "r6Class", editor.baseNode);
						} else {
							new REditorOutlineNode(name, line, "variable", editor.baseNode);
						}
					}

				} else {
					if (alreadyDefined(ctx.getStart(), name)) {
						// RScope scope = scopes.peek();
						// scope.add(name);
						int line = calculateLine(lineStart);
						/* Create a new a new var in current scope! */
						RVariableSymbol var = new RVariableSymbol(name);
						currentScope.define(var); // Define symbol in
													// current scope
						DeclCallStore st = storeDeclCall.peek();
						/*
						 * Add the called var to the call set to detect unused vaiables!
						 */
						st.varDecl.add(name);

						if (isFunc.equals("setRefClass")) {
							new REditorOutlineNode(name, line, "refClass", methods.peek());
						}

						else if (isFunc.equals("R6Class")) {
							new REditorOutlineNode(name, line, "r6Class", methods.peek());
						}

						else {
							new REditorOutlineNode(name, line, "variable", methods.peek());
						}
					}

				}

			}

		}

	}

	private void checkConstantInVarAssignment(RParser.E17VariableDeclarationContext ctx) {
		/* Check for wrong constants only for left assignment operators '<-, <<-,=' */
		if (store.getBoolean("CHECK_CONSTANTS")) {
			ExprContext ctxExpr = ctx.expr(1);
			if (ctxExpr != null) {
				Token startctxExpr = ctxExpr.getStart();
				String wrongConstants = ctxExpr.getStart().getText();
				switch (wrongConstants) {
				case "true":
					parser.notifyErrorListeners(startctxExpr, "Warn12####Wrong constant 'TRUE' required!", null);
					break;
				case "false":
					parser.notifyErrorListeners(startctxExpr, "Warn13####Wrong constant 'FALSE' required!", null);
					break;

				case "na":
					parser.notifyErrorListeners(startctxExpr, "Warn15####Wrong constant 'NA' required!", null);
					break;
				case "null":
					parser.notifyErrorListeners(startctxExpr, "Warn14####Wrong constant 'NULL' required!", null);
					break;

				default:
					break;
				}
			}
		}
	}

	/* Calculates the line from the editor document! */
	private int calculateLine(int offsetStart) {
		IDocument document = null;
		int line = 0;
		if (editor.getEditorInput() != null && editor.getDocumentProvider() != null) {
			IDocumentProvider provider = editor.getDocumentProvider();
			document = provider.getDocument(editor.getEditorInput());
			int len = document.getLength();
			if (offsetStart >= 0 && offsetStart < len) {
				try {
					line = document.getLineOfOffset(offsetStart) + 1;
				} catch (BadLocationException e) {

					System.out.println("Bad line location!");
					System.out.println("\nLine Number: " + line);
					// e.printStackTrace();
				}
			}
		}
		return line;
	}

	@Override
	public void exitE17VariableDeclaration(RParser.E17VariableDeclarationContext ctx) {

	}

	@Override
	public void enterE20CallFunction(RParser.E20CallFunctionContext ctx) {

		Token stop = ctx.expr().getStop();
		String stopText = stop.getText();
		/* Get the current scope stack elements! */
		DeclCallStore st = storeDeclCall.peek();
		/* add the called method to the call set! */
		// System.out.println(stop.getText());
		st.methCall.add(stopText);

		/* Detect libraries and add them to the outline! */
		if (stopText.equals("library") || stopText.equals("require")) {
			Token firstToken = stop;

			int lineStart = firstToken.getStartIndex();

			int line = calculateLine(lineStart);

			if (ctx.getParent().getChild(1) != null) {
				String name = ctx.getChild(2).getText();
				// The third token should be a parenthesis!

				String parenthesis = ctx.getChild(3).getText();
				if (parenthesis.equals(")")) {
					if (methods.size() == 0) {

						new REditorOutlineNode(name, line, "library", editor.baseNode);

					} else {

						new REditorOutlineNode(name, line, "library", methods.peek());

					}
				}
			}
		}

		/*
		 * Detect function call with id class to add the S3 name only to the outline
		 * view!
		 */
		else if (stopText.equals("class")) {

			String name = ctx.sublist().getStart().getText();
			/*If name is empty replace the default text!*/
			if (name.equals(")")) {
				name = "()";
			}
			int lineStart = stop.getStartIndex();
			int line = calculateLine(lineStart);

			if (methods.size() == 0) {
				new REditorOutlineNode(name, line, "s3Class", editor.baseNode);
			} else {
				new REditorOutlineNode(name, line, "s3Class", methods.peek());
			}

		}
		/*
		 * Detect function call with setClass to add the S4 name only to the outline
		 * view!
		 */
		else if (stopText.equals("setClass")) {

			String name = ctx.sublist().getStart().getText().replace("\"", "");

			int lineStart = stop.getStartIndex();
			int line = calculateLine(lineStart);

			if (methods.size() == 0) {
				new REditorOutlineNode(name, line, "s4Class", editor.baseNode);
			} else {
				new REditorOutlineNode(name, line, "s4Class", methods.peek());
			}

		}

	}

	/* Here we filter out variable declarations in method calls! */
	public void exitE20CallFunction(RParser.E20CallFunctionContext ctx) {
		/*
		 * SublistContext subList = ctx.sublist(); extractVariableAssignments(subList);
		 */

	}

	/*
	 * Here we filter out variable declarations in parentheses! expr '[['sublist ']'
	 * ']'
	 */
	public void exitE1(RParser.E1Context ctx) {
		/*
		 * SublistContext subList = ctx.sublist(); extractVariableAssignments(subList);
		 */
	}

	/*
	 * Here we filter out variable declarations in parentheses! expr '[' sublist ']'
	 */
	public void exitE2(RParser.E2Context ctx) {
		/*
		 * SublistContext subList = ctx.sublist(); extractVariableAssignments(subList);
		 */
	}

	/*
	 * In this method we filter out variable declarations in method, calls and
	 * parentheses!
	 */
	private void extractVariableAssignments(SublistContext subList) {
		/* Here we filter out variable declarations in method calls! */

		List<SubContext> subL = subList.sub();
		String resultedVar = null;
		for (int i = 0; i < subL.size(); i++) {
			if (subL.get(i) instanceof SubContext) {

				SubContext su = (SubContext) subL.get(i);
				// System.out.println("var: "+su.expr().getText());
				if (su.expr() != null) {

					if (su.expr() instanceof E17VariableDeclarationContext) {

						resultedVar = su.start.getText();
						DeclCallStore st = storeDeclCall.peek();
						/*
						 * We add the var assignment name to the declaration and call list to ignore it
						 * in the analysis!
						 */

						st.varDecl.add(resultedVar);
						st.varCall.add(resultedVar);
						if (su.expr().getText().contains("<-") || su.expr().getText().contains("<<-") || su.expr().getText().contains("->") || su.expr().getText().contains("->>")) {
							/* Create a new a new var in current scope! */
							if (alreadyDefined(su.start, resultedVar)) {
								RVariableSymbol var = new RVariableSymbol(resultedVar);
								currentScope.define(var); // Define symbol in
															// current scope
								int lineStart = su.start.getStartIndex();

								int line = calculateLine(lineStart);
								new REditorOutlineNode(resultedVar, line, "methodCallField", editor.baseNode);
							}
						}
					}

					else if (su.expr() instanceof E20CallFunctionContext) {
						/*
						 * System.out.println("expr: "+su.expr().getText()+" " +su.expr().getClass());
						 * System.out.println("expr: " +su.start.getText()+" "+su.expr().getClass());
						 */
					}
				}

			}
		}
	}

	/* Is this variable already defined in the scope? */
	/*
	 * private boolean checkVarName(String varName) { boolean check;
	 * 
	 * RScope scope = scopes.peek(); if (scope.inScope(varName)) {
	 * 
	 * check = false; } else { check = true; }
	 * 
	 * RSymbol var = currentScope.resolve(varName); if (var instanceof
	 * RVariableSymbol) { check = false; } else { check = true; } return check; }
	 */

	private boolean alreadyDefined(Token firstToken, String name) {
		/*
		 * Do we have already defined the same name for a function or variable? If so
		 * create a warning!
		 */
		boolean check = false;

		RSymbol funThere = currentScope.resolve(name);
		if (funThere != null) {
			if (funThere instanceof RFunctionSymbol) {
				check = false;
				if (store.getBoolean("FUNCTION_ALREADY_DEFINED")) {
					parser.notifyErrorListeners(firstToken, "Warn19####A function with name '" + name + "' is already defined!", null);
				}
			} else if (funThere instanceof RVariableSymbol) {
				check = false;
				if (store.getBoolean("VARIABLE_ALREADY_DEFINED")) {
					parser.notifyErrorListeners(firstToken, "Warn19####A variable with name '" + name + "' is already defined!", null);
				}
			}

		} else {
			check = true;
		}

		return check;
	}

	public void exitErr1(RParser.Err1Context ctx) {

		parser.notifyErrorListeners(ctx.extra, "Err1####Too many parentheses!", null);

	}

	public void exitErr3(RParser.Err3Context ctx) {

		parser.notifyErrorListeners(ctx.extra, "Err3####Too many parentheses in function call!", null);

	}

	public void exitErr5(RParser.Err5Context ctx) {

		parser.notifyErrorListeners(ctx.extra, "Err5####Too many parentheses in function definition!", null);

	}

	public void exitErr7(RParser.Err7Context ctx) {

		parser.notifyErrorListeners(ctx.extra, "Err7####Too many parentheses in if condition!", null);

	}

	public void exitErr8(RParser.Err8Context ctx) {

		// int index = ctx.extra.getStartIndex();

		parser.notifyErrorListeners(ctx.extra, "Err8####Too many brackets!", null);

	}

	public void exitErr9(RParser.Err9Context ctx) {

		parser.notifyErrorListeners(ctx.extra, "Err9####Too many brackets!", null);

	}

	public void exitErr11(RParser.Err11Context ctx) {

		parser.notifyErrorListeners(ctx.extra, "Err11####Too many braces!", null);

	}

	/*
	 * With this error message we produce QuickFixes. The errors start with 'Err' to
	 * seperate them later in the RBaseListen class!
	 */
	public void exitErr16(RParser.Err16Context ctx) {

		parser.notifyErrorListeners(ctx.extra, "Err16####Too many braces in while statement!", null);

	}

	public void exitErr18(RParser.Err18Context ctx) {

		parser.notifyErrorListeners(ctx.extra, "Err18####Too many braces in for statement!", null);

	}

	public void exitErr22(RParser.Err22Context ctx) {

		Token firstToken = ctx.start;
		parser.notifyErrorListeners(firstToken, "Err22####Unknown Token!", null);

	}

	/* ID call (variables) Need to calculate position of <- */
	public void enterE30(RParser.E30Context ctx) {

		Token tok = ctx.ID().getSymbol();
		// System.out.println("Token Text: "+tok.getText());
		String varName = tok.getText();
		int index = tok.getTokenIndex();

		/* Filter whitespace out because we use Token channel hidden! */
		Token idNextToken = Utils.whitespaceTokenFilter(index, ctx.stop.getStopIndex(), tokens);

		/* Filter out ID's which are assignments and ID's of method calls */
		if (idNextToken != null) {
			if (idNextToken.getText().equals("=") || idNextToken.getText().equals("<-") || idNextToken.getText().equals("<<-") || idNextToken.getText().equals("(")) {
				return;
			}

			else {
				/* Get the current scope stack elements! */
				DeclCallStore st = storeDeclCall.peek();
				/* Add the called var to the call set! */
				st.varCall.add(varName);

			}
		}
	}

}