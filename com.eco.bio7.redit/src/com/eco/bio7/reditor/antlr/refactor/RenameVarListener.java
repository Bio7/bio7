package com.eco.bio7.reditor.antlr.refactor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.runtime.tree.TerminalNodeImpl;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.link.LinkedPosition;
import org.eclipse.jface.text.link.LinkedPositionGroup;
import org.eclipse.swt.graphics.Point;
import com.eco.bio7.reditor.antlr.RBaseListen;
import com.eco.bio7.reditor.antlr.RBaseListener;
import com.eco.bio7.reditor.antlr.RParser;
import com.eco.bio7.reditor.antlr.RParser.E30Context;
import com.eco.bio7.reditor.antlr.RParser.FormContext;
import com.eco.bio7.reditor.antlr.ref.RBaseScope;
import com.eco.bio7.reditor.antlr.ref.RFunctionSymbol;
import com.eco.bio7.reditor.antlr.ref.RGlobalScope;
import com.eco.bio7.reditor.antlr.ref.Scope;
import com.eco.bio7.reditor.antlr.util.Utils;

public class RenameVarListener extends RBaseListener {
	private RParser parser;
	private CommonTokenStream tokens;
	private boolean captureId = true;
	private HashSet<String> capId = new HashSet<String>();
	private LinkedPositionGroup linkedPositionGroup;
	private IDocument document;
	private String selectedText;
	private ParseTreeProperty<Scope> scopes;
	private Scope currentScope; // Resolve symbols starting in this scope
	private RGlobalScope globals;
	private Scope currentSelectedScope;
	private int scopeStart;
	private int scopeStop;
	private ArrayList<LinkedPosition> arrList = new ArrayList<LinkedPosition>();
	private Point wordOffsetAndLen;
	private int scopeNumber = 0;// 0=global scope!
	private int selectedScopeNumber;// from the RefPhase the selected scope number!

	public RenameVarListener(CommonTokenStream tokens, RBaseListen list, RParser parser, IDocument document, String selectedText, LinkedPositionGroup linkedPositionGroup, Scope currentScope, int selectedScopeNumber, Point wordOffsetAndLen) {
		this.scopes = list.scopeNew;
		this.globals = list.globals;
		this.tokens = tokens;
		this.parser = parser;
		this.document = document;
		this.selectedText = selectedText;
		this.linkedPositionGroup = linkedPositionGroup;
		this.currentSelectedScope = currentScope;
		this.wordOffsetAndLen = wordOffsetAndLen;
		this.selectedScopeNumber = selectedScopeNumber;
		

	}

	public void enterProg(RParser.ProgContext ctx) {

		currentScope = currentSelectedScope;

		// scopes.push(new RefactorScopeVar());

	}

	public void exitProg(RParser.ProgContext ctx) {
		for (int i = 0; i < arrList.size(); i++) {
			try {
				linkedPositionGroup.addPosition(arrList.get(i));
			} catch (BadLocationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		// scopes.pop();
	}

	/*
	 * Here we don't separate between function and variable declaration like in the regular parse process!
	 */
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

		// String isFunc = ctx.expr(1).start.getText();

		int lineStart = firstToken.getStartIndex();

		Token assignOp = ((TerminalNodeImpl) ctx.getChild(1)).getSymbol();

		String op = assignOp.getText();
		if (op.equals("<-") || op.equals("<<-")) {
			String name = firstToken.getText();

			if (name.equals(selectedText)) {

				if (currentScope instanceof RFunctionSymbol) {
					// System.out.println(wordOffsetAndLen.x);
					if (lineStart == wordOffsetAndLen.x) {

						/*
						 * Delete all previous marked links because a new local variable has been defined!
						 */
						/*
						 * RFunctionSymbol sco = (RFunctionSymbol) currentScope; sco.resetUsed(); arrList.clear();
						 * 
						 * arrList.add(new LinkedPosition(document, firstToken.getStartIndex(), name.length()));
						 */

					} else {

						/*
						 * RFunctionSymbol sco = (RFunctionSymbol) currentScope; sco.setUsed(true);
						 */
					}

				} else {
					/* Global scope! */
					/*
					 * if (lineStart == wordOffsetAndLen.x) { arrList.clear();
					 * 
					 * arrList.add(new LinkedPosition(document, firstToken.getStartIndex(), name.length()));
					 * 
					 * }
					 */
				}
			}

		}
		/* We leave out the class detection for S3 here! */
		else if (op.equals("->") || op.equals("->>")) {
			String name = tokens.get(start + 2).getText();
			if (name.equals(selectedText)) {

				if (currentScope instanceof RFunctionSymbol) {
					RFunctionSymbol sco = (RFunctionSymbol) currentScope;
					sco.setUsed(true);
				}
			}

		}

		else if (op.equals("=")) {

			boolean isSubTrue = Utils.getCtxParent(ctx.expr(0));

			if (isSubTrue == true) {
				return;
			}

			String name = firstToken.getText();

		}

	}

	public void enterE19DefFunction(RParser.E19DefFunctionContext ctx) {
		currentScope = scopes.get(ctx);
		/* We can parse here for a function symbol! */
		RFunctionSymbol sco = (RFunctionSymbol) currentScope;
		/* Get the function number to identify the current function! */
		scopeNumber = sco.getScopeNumber();
		/* Scope number to detect if we are in the selected scope and not a nested scope! */

		if (ctx.formlist() != null) {
			List<FormContext> formList = ctx.formlist().form();
			int functionDefSize = formList.size();
			for (int i = 0; i < functionDefSize; i++) {
				FormContext fo = formList.get(i);

				TerminalNode ar = fo.ID();
				if (ar != null) {
					Token tok = ar.getSymbol();
					int startIndex = ar.getSymbol().getStartIndex();
					String text = ar.getText();
					if (ar.getText().equals(selectedText)) {

						/*
						 * Detect nested scope by name. Problem: Scopes are always different (from earlier parse process). Solution: We just enumerate the scopes with a scope number1.
						 */

						if (scopeNumber != selectedScopeNumber) {

							// RFunctionSymbol sco = (RFunctionSymbol) currentScope;
							sco.setUsed(true);
						}

						/*
						 * Here we detected the scope name. We have a declared var here so we reset the used variable to mark the following id's
						 */
						else {
							// RFunctionSymbol sco = (RFunctionSymbol) currentScope;
							sco.resetUsed();
							/*
							 * Delete all previous marked links because a new local variable has been defined!
							 */

							arrList.add(new LinkedPosition(document, startIndex, text.length()));

						}

					} else {

					}

				}

			}
		} else {
			/*
			 * If the function definition is empty we check if we are in the function!
			 */
			if (scopeNumber != selectedScopeNumber) {

				// RFunctionSymbol sco = (RFunctionSymbol) currentScope;
				sco.resetUsed();
			}

		}
	}

	public void exitE19DefFunction(RParser.E19DefFunctionContext ctx) {
		currentScope = currentScope.getEnclosingScope(); // pop scope
		/* Keep track of the scope number which we created in the base listener! */
		scopeNumber = currentScope.getScopeNumber();

		// scopes.pop();

	}

	/* For loop ID needed! */
	public void enterForLoop(RParser.E23ForLoopContext ctx) {
		// Token tok = ctx.ID().getSymbol();
		int startIndex = ctx.ID().getSymbol().getStartIndex();
		int length = ctx.ID().getText().length();

		if (ctx.ID().getText().equals(selectedText)) {

			if (currentScope instanceof RFunctionSymbol) {
				if (scopeNumber == selectedScopeNumber) {
					RFunctionSymbol sco = (RFunctionSymbol) currentScope;
					if (sco.isUsed() == false) {
						arrList.add(new LinkedPosition(document, startIndex, length));

					}
				}
			} else if (currentSelectedScope instanceof RBaseScope) {

				arrList.add(new LinkedPosition(document, startIndex, length));

			}

		}

	}

	/** Listen to matches of classDeclaration */

	public void enterE20CallFunction(RParser.E20CallFunctionContext ctx) {
		// System.out.println(ctx.getText());

	}

	/* ID call (variables) Need to calculate position of <- */
	public void enterE30(RParser.E30Context ctx) {

		Token tok = ctx.ID().getSymbol();
		String varName = tok.getText();

		if (varName.equals(selectedText)) {
			/*
			 * Get the current scope and look if a var with the selected name was defined already!
			 */

			if (currentScope instanceof RFunctionSymbol) {
				if (scopeNumber == selectedScopeNumber) {
					RFunctionSymbol sco = (RFunctionSymbol) currentScope;
					if (sco.isUsed() == false) {
						arrList.add(new LinkedPosition(document, tok.getStartIndex(), varName.length()));

					}
				}
			} else if (currentSelectedScope instanceof RBaseScope) {
				arrList.add(new LinkedPosition(document, tok.getStartIndex(), varName.length()));

			}

		}

		int index = tok.getTokenIndex();
		/* Filter whitespace out because we use Token channel hidden! */
		Token idNextToken = Utils.whitespaceTokenFilter(index, ctx.stop.getStopIndex(), tokens);

		// Token idNextToken = tokens.get(index + 1);

		if (idNextToken != null) {
			if (idNextToken.getText().equals("=") || idNextToken.getText().equals("<-") || idNextToken.getText().equals("(")) {

				return;
			}

			else {
				if (captureId) {
					// System.out.println(captureId);
					capId.add(varName);

				}

			}

		}
	}

	@Override
	public void exitE30(RParser.E30Context ctx) {

	}

}
