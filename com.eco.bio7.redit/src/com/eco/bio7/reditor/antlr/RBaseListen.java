/*******************************************************************************
 * Copyright (c) 2007-2014 M. Austenfeld
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
import java.util.Stack;

import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.texteditor.IDocumentProvider;

import com.eco.bio7.reditor.Bio7REditorPlugin;
import com.eco.bio7.reditor.antlr.ref.FunctionSymbol;
import com.eco.bio7.reditor.antlr.ref.GlobalScope;
import com.eco.bio7.reditor.antlr.ref.Scope;
import com.eco.bio7.reditor.antlr.ref.RVariableSymbol;
import com.eco.bio7.reditor.outline.REditorOutlineNode;
import com.eco.bio7.reditors.REditor;

public class RBaseListen extends RBaseListener {

	private CommonTokenStream tokens;
	public ArrayList<String> startStop = new ArrayList<String>();
	// public ClassModel cm = new ClassModel();
	private REditor editor;
	private Parser parser;
	private Stack<REditorOutlineNode> methods = new Stack<REditorOutlineNode>();// A
																				// stack
																				// for
																				// nested
																				// nodes!
	private Stack<RScope> scopes = new Stack<RScope>();;
	private IPreferenceStore store;
	public ParseTreeProperty<Scope> scopeNew = new ParseTreeProperty<Scope>();
	public GlobalScope globals;
	public Scope currentScope; // define symbols in this scop

	public RBaseListen(CommonTokenStream tokens, REditor editor, Parser parser) {
		this.tokens = tokens;
		this.editor = editor;
		this.parser = parser;

		store = Bio7REditorPlugin.getDefault().getPreferenceStore();

	}

	public void enterProg(RParser.ProgContext ctx) {

		scopes.push(new RScope(null));

		globals = new GlobalScope(null);
		currentScope = globals;

	}

	public void exitProg(RParser.ProgContext ctx) {

		System.out.println(globals);
	}

	@Override
	/*
	 * public void exitExprError(@NotNull RParser.ExprErrorContext ctx) {
	 * 
	 * parser.notifyErrorListeners(ctx.start, "One Opening Parentheses to much!"
	 * , null);
	 * 
	 * }
	 *//**
		 * {@inheritDoc}
		 * <p/>
		 * The default implementation does nothing.
		 *//*
		 * @Override public void exitExprError2(@NotNull
		 * RParser.ExprError2Context ctx) {
		 * 
		 * 
		 * 
		 * Notify the parser! parser.notifyErrorListeners(ctx.stop,
		 * "One Closing Parentheses to much!", null); }
		 */
	/*
	 * public void enterAmountRightBraceError(@NotNull
	 * RParser.AmountRightBraceErrorContext ctx) { }
	 * 
	 * public void exitAmountRightBraceError(@NotNull
	 * RParser.AmountRightBraceErrorContext ctx) {
	 * 
	 * Interval sourceInterval = ctx.getSourceInterval(); int count = -1; int
	 * start = sourceInterval.a; We calculate the token position from the
	 * expression! List<Token> firstToken = tokens.get(sourceInterval.a,
	 * sourceInterval.b); for (int i = 0; i < firstToken.size(); i++) { //
	 * System.out.println(firstToken.get(i).getText()); if
	 * (firstToken.get(i).getText().equals("(")) { count = i; break; } } //
	 * System.out.println(count);
	 * 
	 * Notify the parser! parser.notifyErrorListeners(ctx.stop,
	 * "One Closing Brace to much!", null); }
	 */
	/*
	 * public void enterAmountLeftBraceError2(@NotNull
	 * RParser.AmountLeftBraceError2Context ctx) {
	 * 
	 * 
	 * }
	 * 
	 * public void exitAmountLeftBraceError2(@NotNull
	 * RParser.AmountLeftBraceError2Context ctx) { Interval sourceInterval =
	 * ctx.getSourceInterval();
	 * 
	 * int count = -1; int start = sourceInterval.a; We calculate the token
	 * position from the expression! List<Token> firstToken =
	 * tokens.get(sourceInterval.a, sourceInterval.b); for (int i = 0; i <
	 * firstToken.size(); i++) { //
	 * System.out.println(firstToken.get(i).getText()); if
	 * (firstToken.get(i).getText().equals("(")) { count = i; break; } }
	 * 
	 * ctx.getChild(2).getSourceInterval(); System.out.println(
	 * "This brace is at:" +ctx.start.getCharPositionInLine());
	 * 
	 * Notify the parser! parser.notifyErrorListeners(ctx.start,
	 * "One Opening Brace to much!", null);
	 * 
	 * }
	 */
	/*
	 * public void enterClosingRightBraceError(@NotNull
	 * RParser.ClosingRightBraceErrorContext ctx) {
	 * 
	 * 
	 * }
	 * 
	 * public void exitClosingRightBraceError(@NotNull
	 * RParser.ClosingRightBraceErrorContext ctx) {
	 * 
	 * Notify the parser! parser.notifyErrorListeners(ctx.stop,
	 * "Closing Brace Missing!", null);
	 * 
	 * }
	 * 
	 * public void exitClosingLeftBraceError(@NotNull
	 * RParser.ClosingLeftBraceErrorContext ctx) {
	 * 
	 * Notify the parser! parser.notifyErrorListeners(ctx.start,
	 * "Opening Brace Missing!", null);
	 * 
	 * }
	 */
	/*
	 * public void exitClosingLeftEmptyBraceError(@NotNull
	 * RParser.ClosingLeftEmptyBraceErrorContext ctx) {
	 * 
	 * Notify the parser! parser.notifyErrorListeners(ctx.start,
	 * "Right Brace Missing!", null);
	 * 
	 * }
	 */
	public void exitE19DefFunction(RParser.E19DefFunctionContext ctx) {
		/* Exit scope! */
		scopes.pop();
		if (methods.empty() == false) {
			methods.pop();
		}
		if (currentScope.getEnclosingScope() != null) {
			currentScope = currentScope.getEnclosingScope(); // pop scope
		}

	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * The default implementation does nothing.
	 */
	@Override
	public void enterE19DefFunction(RParser.E19DefFunctionContext ctx) {
		/*
		 * Insert function as current scope with a parent current scope
		 * (scope.peek)!
		 */

		scopes.push(new RScope(scopes.peek()));

		Interval sourceInterval = ctx.getSourceInterval();

		Token firstToken = tokens.get(sourceInterval.a);
		// System.out.println(ctx.getParent().getChild(0).getText());
		int lineStart = firstToken.getStartIndex();
		// String ct=ctx.getText();

		// System.out.println("function start at line:"+lineStart);
		Token lastToken = tokens.get(sourceInterval.b);
		int lineEnd = lastToken.getStopIndex() + 1 - lineStart;
		// String ct2=ctx.getText();

		// Add to the editor folding action if enabled in the preferences!
		if (store.getBoolean("FUNCTIONS_FOLDING")) {
			startStop.add(lineStart + "," + lineEnd);
		}
		int lineMethod = calculateLine(lineStart);
		int childs = ctx.getParent().getChildCount();
		int posTree = 0;
		for (int i = 0; i < childs; i++) {
			if (ctx.getText().equals(ctx.getParent().getChild(i).getText())) {
				posTree = i;
			}
		}
		if (ctx.getParent().getChild(posTree - 1) != null && ctx.getParent().getChild(posTree - 2) != null) {
			String op = ctx.getParent().getChild(posTree - 1).getText();
			String name = ctx.getParent().getChild(posTree - 2).getText();

			

			if (op.equals("<-") || op.equals("<<-")|| op.equals("=")) {
				
				// push new scope by making new one that points to enclosing scope
				FunctionSymbol function = new FunctionSymbol(name, currentScope);
				currentScope.define(function); // Define function in current scope
				scopeNew.put(ctx, function); // Push: set function's parent to

				if (methods.size() == 0) {

					methods.push(new REditorOutlineNode(name, lineMethod, "function", editor.baseNode));

				} else {
					methods.push(new REditorOutlineNode(name, lineMethod, "function", methods.peek()));

				}

			}
		} else if (posTree == 0) {
			// push new scope by making new one that points to enclosing scope
			FunctionSymbol function = new FunctionSymbol(ctx.getText(), currentScope);
			currentScope.define(function); // Define function in current scope
			scopeNew.put(ctx, function); // Push: set function's parent to
			if (methods.size() == 0) {

				methods.push(new REditorOutlineNode(ctx.getText(), lineMethod, "function", editor.baseNode));

			} else {
				methods.push(new REditorOutlineNode(ctx.getText(), lineMethod, "function", methods.peek()));

			}

		}

	}

	/* if condition! */
	public void enterE21(RParser.E21Context ctx) {

		Interval sourceInterval = ctx.getSourceInterval();

		Token firstToken = tokens.get(sourceInterval.a);
		int lineStart = firstToken.getStartIndex();

		Token lastToken = tokens.get(sourceInterval.b);
		int lineEnd = lastToken.getStopIndex() + 1 - lineStart;

		// Add to the editor folding action if enabled in the preferences!
		if (store.getBoolean("IF_CONDITION_FOLDING")) {
			startStop.add(lineStart + "," + lineEnd);
		}

	}

	/* if condition 2 of grammar file! */
	public void enterE22(RParser.E22Context ctx) {

		Interval sourceInterval = ctx.getSourceInterval();

		Token firstToken = tokens.get(sourceInterval.a);
		int lineStart = firstToken.getStartIndex();

		Token lastToken = tokens.get(sourceInterval.b);
		int lineEnd = lastToken.getStopIndex() + 1 - lineStart;

		// Add to the editor folding action if enabled in the preferences!
		if (store.getBoolean("IF_CONDITION_FOLDING")) {
			startStop.add(lineStart + "," + lineEnd);
		}

	}

	/* for loop! */
	public void enterE23(RParser.E23Context ctx) {

		Interval sourceInterval = ctx.getSourceInterval();

		Token firstToken = tokens.get(sourceInterval.a);
		int lineStart = firstToken.getStartIndex();

		Token lastToken = tokens.get(sourceInterval.b);
		int lineEnd = lastToken.getStopIndex() + 1 - lineStart;

		// Add to the editor folding action if enabled in the preferences!
		if (store.getBoolean("FOR_LOOP_FOLDING")) {
			startStop.add(lineStart + "," + lineEnd);
		}

	}

	/* while loop! */
	public void enterE24(RParser.E24Context ctx) {

		Interval sourceInterval = ctx.getSourceInterval();

		Token firstToken = tokens.get(sourceInterval.a);
		int lineStart = firstToken.getStartIndex();

		Token lastToken = tokens.get(sourceInterval.b);
		int lineEnd = lastToken.getStopIndex() + 1 - lineStart;

		// Add to the editor folding action if enabled in the preferences!
		if (store.getBoolean("WHILE_LOOP_FOLDING")) {
			startStop.add(lineStart + "," + lineEnd);
		}

	}

	/* repeat loop! */
	public void enterE25(RParser.E25Context ctx) {

		Interval sourceInterval = ctx.getSourceInterval();

		Token firstToken = tokens.get(sourceInterval.a);

		int lineStart = firstToken.getStartIndex();

		Token lastToken = tokens.get(sourceInterval.b);
		int lineEnd = lastToken.getStopIndex() + 1 - lineStart;

		// Add to the editor folding action if enabled in the preferences!
		if (store.getBoolean("REPEAT_LOOP_FOLDING")) {
			startStop.add(lineStart + "," + lineEnd);
		}

	}

	@Override
	public void enterE17VariableDeclaration(RParser.E17VariableDeclarationContext ctx) {

		Interval sourceInterval = ctx.getSourceInterval();
		int start = sourceInterval.a;
		Token assign = tokens.get(start + 2);
       //System.out.println(ctx.ASSIGN_OP().getText());
		String subExpr = assign.getText();

		if (subExpr.equals("function") == false) {
			Token firstToken = tokens.get(start);

			int lineStart = firstToken.getStartIndex();

			int line = calculateLine(lineStart);

			if (ctx.getParent().getChild(1) != null) {

				String op = tokens.get(start + 1).getText();

				if (op.equals("<-") || op.equals("<<-") || op.equals("=")) {
					String name = tokens.get(start).getText();
					if (methods.size() == 0) {
						if (checkVarName(name)) {
							RScope scope = scopes.peek();
							scope.add(name);
							
							 RVariableSymbol var = new RVariableSymbol(name);
						      currentScope.define(var); // Define symbol in current scope

							new REditorOutlineNode(name, line, "variable", editor.baseNode);
						}

					} else {
						if (checkVarName(name)) {
							RScope scope = scopes.peek();
							scope.add(name);
							
							RVariableSymbol var = new RVariableSymbol(name);
						      currentScope.define(var); // Define symbol in current scope

							new REditorOutlineNode(name, line, "variable", methods.peek());
						}

					}

				}

				else if (op.equals("->") || op.equals("->>")) {
					String name = tokens.get(start + 2).getText();
					if (methods.size() == 0) {
						if (checkVarName(name)) {
							RScope scope = scopes.peek();
							scope.add(name);

							new REditorOutlineNode(name, line, "variable", editor.baseNode);
						}

					} else {
						if (checkVarName(name)) {
							RScope scope = scopes.peek();
							scope.add(name);

							new REditorOutlineNode(name, line, "variable", methods.peek());
						}
					}

				}
			}
		}

	}

	/* Calculates the line from the editor document! */
	private int calculateLine(int lineStart) {
		IDocumentProvider provider = editor.getDocumentProvider();
		IDocument document = provider.getDocument(editor.getEditorInput());
		int line = 0;
		try {
			line = document.getLineOfOffset(lineStart) + 1;
		} catch (BadLocationException e) {
			// TODO Auto-generated catch block
			System.out.println("Bad line location!");
			// e.printStackTrace();
		}
		return line;
	}

	@Override
	public void exitE17VariableDeclaration(RParser.E17VariableDeclarationContext ctx) {

	}

	@Override
	public void enterE20CallFunction(RParser.E20CallFunctionContext ctx) {
		/*
		 * Interval sourceInterval = ctx.getSourceInterval(); int start =
		 * sourceInterval.a; Token assign = tokens.get(start);
		 */
		Token start = ctx.start;
		String subExpr = start.getText();
		/* Detect libraries and add them to the outline! */
		if (subExpr.equals("library") || subExpr.equals("require")) {
			Token firstToken = start;

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

	}

	@Override
	public void exitE20CallFunction(RParser.E20CallFunctionContext ctx) {
		/*
		 * Interval sourceInterval = ctx.getSourceInterval(); int start =
		 * sourceInterval.a; String name = tokens.get(start).getText(); String
		 * op = tokens.get(start + 1).getText();
		 * if(op.equals("<-")||op.equals("=")||op.equals("<<-")){
		 * name=tokens.get(start + 2).getText(); }
		 * System.out.println("function:"+name);
		 */
	}

	/*
	 * Adapted method source from:
	 * http://stackoverflow.com/questions/15050137/once
	 * -grammar-is-complete-whats-the-best-way-to-walk-an-antlr-v4-tree
	 */
	private boolean checkVarName(String varName) {
		boolean check;
		RScope scope = scopes.peek();
		if (scope.inScope(varName)) {

			check = false;
		} else {
			check = true;
		}
		return check;
	}

	public void exitErr1(RParser.Err1Context ctx) {

		parser.notifyErrorListeners(ctx.extra, "Err1:Too many parentheses!", null);

	}

	public void exitErr3(RParser.Err3Context ctx) {

		parser.notifyErrorListeners(ctx.extra, "Err3:Too many parentheses in function call!", null);

	}

	public void exitErr5(RParser.Err5Context ctx) {

		parser.notifyErrorListeners(ctx.extra, "Err5:Too many parentheses in function definition!", null);

	}

	/*
	 * public void exitErr6(@NotNull RParser.Err6Context ctx) {
	 * 
	 * parser.notifyErrorListeners(ctx.extra,
	 * "Err6:Too many parentheses in left if definition!", null);
	 * 
	 * 
	 * 
	 * }
	 */

	public void exitErr7(RParser.Err7Context ctx) {

		// int index = ctx.extra.getStartIndex();

		parser.notifyErrorListeners(ctx.extra, "Err7:Too many parentheses in if condition!", null);

	}

	public void exitErr8(RParser.Err8Context ctx) {

		// int index = ctx.extra.getStartIndex();

		parser.notifyErrorListeners(ctx.extra, "Err8:Too many brackets!", null);

	}

	public void exitErr9(RParser.Err9Context ctx) {

		parser.notifyErrorListeners(ctx.extra, "Err9:Too many brackets!", null);

	}

	public void exitErr11(RParser.Err11Context ctx) {

		parser.notifyErrorListeners(ctx.extra, "Err11:Too many braces!", null);

	}

	/* Here we create some warnings from the parser! */
	public void exitWarn12(RParser.Warn12Context ctx) {

		parser.notifyErrorListeners(ctx.extra, "Warn12:Wrong constant: 'TRUE' required!", null);

	}

	public void exitWarn13(RParser.Warn13Context ctx) {

		parser.notifyErrorListeners(ctx.extra, "Warn13:Wrong constant: 'FALSE' required!", null);

	}

	public void exitWarn14(RParser.Warn14Context ctx) {

		parser.notifyErrorListeners(ctx.extra, "Warn14:Wrong constant: 'NULL' required!", null);

	}

	public void exitWarn15(RParser.Warn15Context ctx) {

		parser.notifyErrorListeners(ctx.extra, "Warn15:Wrong constant: 'NA' required!", null);

	}

	/*
	 * With this error message we produce QuickFixes. The errors start with
	 * 'Err' to seperate them later in the RBaseListen class!
	 */
	public void exitErr16(RParser.Err16Context ctx) {

		parser.notifyErrorListeners(ctx.extra, "Err16:Too many braces in while statement!", null);

	}

	public void exitErr18(RParser.Err18Context ctx) {

		parser.notifyErrorListeners(ctx.extra, "Err18:Too many braces in for statement!", null);

	}

	public void exitErr20(RParser.Err20Context ctx) {

		Token firstToken = tokens.get(ctx.getChild(1).getSourceInterval().a);
		parser.notifyErrorListeners(firstToken, "Err20:Wrong comparison!", null);

	}

	public void exitErr21(RParser.Err21Context ctx) {

		Token firstToken = tokens.get(ctx.getChild(1).getSourceInterval().a);
		parser.notifyErrorListeners(firstToken, "Err21:Wrong comparison!", null);

	}

	public void exitErr22(RParser.Err22Context ctx) {

		Token firstToken = ctx.start;
		parser.notifyErrorListeners(firstToken, "Err22:Unknown Token!", null);

	}

	@Override
	public void exitE30(RParser.E30Context ctx) {
		/*
		 * Interval sourceInterval = ctx.getSourceInterval(); int start =
		 * sourceInterval.a; String name = tokens.get(start).getText(); String
		 * op = tokens.get(start + 1).getText();
		 * if(op.equals("<-")||op.equals("=")||op.equals("<<-")){
		 * name=tokens.get(start + 2).getText(); }
		 * System.out.println("ID:"+name);
		 */
	}

}