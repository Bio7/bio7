/*******************************************************************************
 * Copyright (c) 2007-2025 M. Austenfeld
 * 
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     M. Austenfeld
 *******************************************************************************/
package com.eco.bio7.reditor.antlr.ref;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.runtime.tree.TerminalNodeImpl;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.Util;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

import com.eco.bio7.reditor.Bio7REditorPlugin;
import com.eco.bio7.reditor.antlr.RBaseListen;
import com.eco.bio7.reditor.antlr.RBaseListener;
import com.eco.bio7.reditor.antlr.RParser;
import com.eco.bio7.reditor.antlr.RParser.E17VariableDeclarationContext;
import com.eco.bio7.reditor.antlr.RParser.E19DefFunctionContext;
import com.eco.bio7.reditor.antlr.RParser.E20CallFunctionContext;
import com.eco.bio7.reditor.antlr.RParser.ExprContext;
import com.eco.bio7.reditor.antlr.RParser.FormContext;
import com.eco.bio7.reditor.antlr.RParser.SubContext;
import com.eco.bio7.reditor.antlr.RParser.SublistContext;
import com.eco.bio7.reditor.antlr.util.Utils;
import com.eco.bio7.rpreferences.template.CalculateRProposals;

public class RRefPhaseListen extends RBaseListener {
	private ParseTreeProperty<Scope> scopes;
	private RGlobalScope globals;
	private Scope currentScope; // Resolve symbols starting in this scope
	private CommonTokenStream tokens;
	private Parser parser;
	private Set<String> finalFuncDecl;
	private Set<String> finalVarDecl;
	private int offsetCodeCompl = -1;
	private StringBuffer methodCallVars;
	private StringBuffer buffScopeVars;
	private StringBuffer buffScopeFunctions;
	private int tempDifferenceStart = 10000000;
	private int tempDifferenceStartFunction = 10000000;
	private Scope tempCodeComplScope;
	private int tempCodeStartIndex;
	private int tempCodeStopIndex;
	private boolean isInMethodCall;
	private String proposalFuncFound;
	private IPreferenceStore store;
	private Scope selectedRefactorScope;
	private int selectedScopeNumber = 0;// 0=global scope!
	/* Token for roxygen comments from cursor selected function offset! */
	private Token selectedFunOffsetStart;
	private Token selectedFunOffsetEnd;
	/*
	 * Function name and arguments for roxygen comments from cursor selected
	 * function offset!
	 */
	private String selectedFunOffsetName;
	private String[] selFunctionCallVars;
	private Object selectedFunOffsetContext;
	private boolean isInMatrixBracketCall;
	private boolean isInMatrixDoubleBracketCall;
	private String bracketMatrixName;
	private int matrixArgState = 0;
	private int bracketCommaCount;
	private Map<String, String> currentPackageImports;// A map of library statement names!
	private boolean isInPipeFunction;
	private String currentPipeData;
	private Token currentPipeToken;

	public boolean isInPipeFunction() {
		return isInPipeFunction;
	}

	public String getCurrentPipeData() {
		return currentPipeData;
	}

	public Map<String, String> getCurrentPackageImports() {
		return currentPackageImports;
	}

	public CommonTokenStream getTokens() {
		return tokens;
	}

	public Object getSelectedFunOffsetContext() {
		return selectedFunOffsetContext;
	}

	public String[] getSelFunctionCallVars() {
		return selFunctionCallVars;
	}

	public String getSelectedFunOffsetName() {
		return selectedFunOffsetName;
	}

	public Token getSelectedFunOffsetStart() {
		return selectedFunOffsetStart;
	}

	public Token getSelectedFunOffsetEnd() {
		return selectedFunOffsetEnd;
	}

	public Scope getSelectedRefactorScope() {
		return selectedRefactorScope;
	}

	public int getSelectedScopeNumber() {
		return selectedScopeNumber;
	}

	public int getTempCodeStartIndex() {
		return tempCodeStartIndex;
	}

	public int getTempCodeStopIndex() {
		return tempCodeStopIndex;
	}

	public String getProposalFuncFound() {
		return proposalFuncFound;
	}

	public Scope getTempCodeComplScope() {
		return tempCodeComplScope;
	}

	public boolean isInMethodCall() {
		return isInMethodCall;
	}

	public boolean isInMatrixBracketCall() {
		return isInMatrixBracketCall;
	}

	public boolean isInMatrixDoubleBracketCall() {
		return isInMatrixDoubleBracketCall;
	}

	public String getBracketMatrixName() {
		return bracketMatrixName;
	}

	public int getBracketCommaCount() {
		return bracketCommaCount;
	}

	/*
	 * Get the number of matrix, dataframe, etc. arguments!
	 */
	public int getMatrixArgState() {
		return matrixArgState;
	}

	public StringBuffer getBuffScopeFunctions() {
		return buffScopeFunctions;
	}

	public StringBuffer getBuffScopeVars() {
		return buffScopeVars;
	}

	public StringBuffer getMethodCallVars() {
		return methodCallVars;
	}

	public RRefPhaseListen(CommonTokenStream tokens, RBaseListen list, Parser parser) {
		this.scopes = list.scopeNew;
		this.globals = list.globals;
		this.finalFuncDecl = list.finalFuncDecl;
		this.finalVarDecl = list.finalVarDecl;
		this.tokens = tokens;
		this.parser = parser;
		store = Bio7REditorPlugin.getDefault().getPreferenceStore();
		currentPackageImports = new HashMap<String, String>();
	}

	public RGlobalScope getGlobals() {
		return globals;
	}

	public void setGlobals(RGlobalScope globals) {
		this.globals = globals;
	}

	/* Constructor for code completion! */
	public RRefPhaseListen(CommonTokenStream tokens, RBaseListen list, Parser parser, int offset) {
		this.scopes = list.scopeNew;
		this.globals = list.globals;
		this.finalFuncDecl = list.finalFuncDecl;
		this.finalVarDecl = list.finalVarDecl;
		this.tokens = tokens;
		this.parser = parser;
		this.offsetCodeCompl = offset;
		store = Bio7REditorPlugin.getDefault().getPreferenceStore();
		currentPackageImports = new HashMap<String, String>();
	}

	public void enterProg(RParser.ProgContext ctx) {
		currentScope = globals;

		selectedRefactorScope = globals;

	}

	public void exitProg(RParser.ProgContext ctx) {
		/*
		 * This calculates the available functions/variables for code completion
		 * recursively through all available scopes for code completion!
		 */

		buffScopeVars = new StringBuffer();
		buffScopeFunctions = new StringBuffer();
		if (tempCodeComplScope != null) {

			getScopeDefFunctions(tempCodeComplScope);
			getScopeVars(tempCodeComplScope);
		} else {
			getScopeDefFunctions(currentScope);
			getScopeVars(currentScope);
		}
	}

	/* For code completion detect the double brackets! */
	public void enterE1(RParser.E1Context ctx) {
		Token parenth = ((TerminalNodeImpl) ctx.getChild(1)).getSymbol();
		Token parenthStop = ((TerminalNodeImpl) ctx.getChild(3)).getSymbol();

		int startIndex = parenth.getStopIndex();
		int stopIndex = parenthStop.getStopIndex();
		if (offsetCodeCompl > startIndex && offsetCodeCompl <= stopIndex) {
			/* Variable for double and single bracket! */
			bracketMatrixName = ctx.expr().getText();
			isInMatrixDoubleBracketCall = true;
			isInMatrixBracketCall = false;
			/* We are not in a function call! */
			isInMethodCall = false;
			/* Variable to store in which comma we are! */
			matrixArgState = 0;

		}

	}

	/* For code completion detect the brackets! */
	public void enterE2(RParser.E2Context ctx) {
		Token parenth = ((TerminalNodeImpl) ctx.getChild(1)).getSymbol();
		Token parenthStop = ((TerminalNodeImpl) ctx.getChild(3)).getSymbol();

		int startIndex = parenth.getStopIndex();
		int stopIndex = parenthStop.getStopIndex();
		/* Get the nearest bracket position! */
		if (offsetCodeCompl > startIndex && offsetCodeCompl <= stopIndex) {
			/* Variable for double and single bracket! */
			bracketMatrixName = ctx.expr().getText();
			isInMatrixBracketCall = true;
			isInMatrixDoubleBracketCall = false;
			/* We are not in a function call! */
			isInMethodCall = false;
			int size = ctx.sublist().getChildCount();
			/*
			 * Variable to hold the count number of comma (TerminalNodeImpl) which is
			 * different from the general child count!
			 */
			bracketCommaCount = 0;
			/* Variable to store in which comma we are! */
			matrixArgState = 0;

			for (int i = 0; i < size; i++) {

				/* Get the comma position in the sublist! */

				if (ctx.sublist().getChild(i) instanceof TerminalNodeImpl) {
					Token comma = ((TerminalNodeImpl) ctx.sublist().getChild(i)).getSymbol();
					/*
					 * Count the number of comma (TerminalNodeImpl) which is different from the
					 * general child count!
					 */
					bracketCommaCount++;
					int commaStart = comma.getStartIndex();

					if (offsetCodeCompl > commaStart) {
						/* Determine in which comma we are! */
						matrixArgState = bracketCommaCount;

					}

				}

			}

		}

	}

	/*
	 * This is the detection of the pipe bind operator '=>'!
	 */
	public void enterE44(RParser.E44Context ctx) {

	}

	/*
	 * This is the detection of the pipe operator '|>'! In the grammar file the
	 * operator must precede the function call!
	 */
	public void enterE43(RParser.E43Context ctx) {
		if (currentPipeToken != null) {
			Token afterPipeOperator = ctx.expr(1).start;
			if (afterPipeOperator == currentPipeToken) {
				isInPipeFunction = true;
				/* Here we get the name of the data (first element!) */
				String pipeData = ctx.getStart().getText();
				currentPipeData = pipeData;
			}
		}
	}

	/*
	 * This is the detection of the exp USER_OP exp expression grammar used for the
	 * pipe operator '%>%'!
	 */
	public void enterE8(RParser.E8Context ctx) {

		/*
		 * We can't detect a function expr call here, if e.g., a variable is declared
		 * from the piping operation. The method call operation of the grammar is done
		 * before this operation is entered in the listener. So we store the current
		 * method call token (see enterE20CallFunction) and compare it with the detected
		 * tokens which follow the USER_OP expression (expr(1)). If both tokens are
		 * equal the function call is in a pipe. We also store the current pipe data
		 * (first expression in a pipe) for code completion (typically to extract the
		 * columns of the data) !
		 */
		String userOp = ctx.USER_OP().getText();
		if (userOp.equals("%>%") || userOp.equals("%$%")) {
			if (currentPipeToken != null) {
				Token afterPipeOperator = ctx.expr(1).start;
				if (afterPipeOperator == currentPipeToken) {
					isInPipeFunction = true;
					/* Here we get the name of the data (first element!) */
					String pipeData = ctx.getStart().getText();
					currentPipeData = pipeData;
				}
			}
		}

	}

	/* Comparison operators! */
	public void enterE11(RParser.E11Context ctx) {
		if (store.getBoolean("WRONG_NULL_COMPARISON")) {

			ExprContext tokLeft = ctx.expr(0);
			ExprContext tokRight = ctx.expr(1);
			int startIndex = tokLeft.getStart().getStartIndex();
			int stopIndex = tokRight.getStop().getStopIndex();
			/*
			 * Here I create a new Token to underline the whole expression. The text is from
			 * the first expression as argument for the quickfix function!!
			 */
			CommonToken to = new CommonToken(Token.DEFAULT_CHANNEL);
			to.setText(tokLeft.getText());
			to.setStartIndex(startIndex);
			to.setStopIndex(stopIndex);
			Token operator = ((TerminalNodeImpl) ctx.getChild(1)).getSymbol();
			if (operator.getText().equals("==")) {

				String na = ctx.expr(1).getText();
				if (na.equals("NA") || na.equals("na")) {
					parser.notifyErrorListeners(to,
							"Warn20####Comparision syntax incorrect.####is.na(" + to.getText() + ")", null);

				} else if (na.equals("NULL") || na.equals("null")) {
					parser.notifyErrorListeners(to,
							"Warn20####Comparision syntax incorrect.####is.null(" + to.getText() + ")", null);
				}

				else if (na.equals("NaN")) {
					parser.notifyErrorListeners(to,
							"Warn20####Comparision syntax incorrect.####is.nan(" + to.getText() + ")", null);
				} else {

					parser.notifyErrorListeners(to,
							"Warn21####Use identical function to test equality of two objects.####identical("
									+ to.getText() + "," + na + ")",
							null);
				}
			}
		}
	}

	public void enterE30(RParser.E30Context ctx) {

	}

	/* Variable call! To do: Need to calculate position of <- */
	public void exitE30(RParser.E30Context ctx) {
		if (store.getBoolean("MISSING_VAR")) {
			Token tok = ctx.ID().getSymbol();

			String varName = tok.getText();
			int index = tok.getTokenIndex();

			/* Filter whitespace out because we use Token channel hidden! */
			Token idNextToken = Utils.whitespaceTokenFilter(index, ctx.stop.getStopIndex(), tokens);
			// System.out.println("Next Symbol= "+idNextToken.getText());
			if (idNextToken != null) {
				if (idNextToken.getText().equals("=") || idNextToken.getText().equals("<-")
						|| idNextToken.getText().equals("<<-") || idNextToken.getText().equals("(")) {
					return;
				}

				else {
					RSymbol var = currentScope.resolve(varName);

					if (var == null) {
						/*
						 * Find out (if enabled in the preferences) recursively (bottom up) if the
						 * variable assignment is in a sublist to avoid to many warnings in the
						 * analysis! Sublist occurs in method calls and vector, array square parentheses
						 * (rule E1, E2)!
						 */
						if (store.getBoolean("CHECK_VARIABLES_IN_FUNCTION_CALLS") == false) {
							boolean isSubTrue = Utils.getCtxParent(ctx);

							// System.out.println("has Sub?: " + isSubTrue);
							if (isSubTrue == true) {
								return;
							}
						}

						parser.notifyErrorListeners(tok,
								"Warn17####Variable not available?#### " + varName + " seems to be missing!", null);

					} else {
						if (var instanceof RFunctionSymbol) {
							return;

						}
					}
				}
			}
		}

	}

	public void enterE17VariableDeclaration(RParser.E17VariableDeclarationContext ctx) {
		if (store.getBoolean("UNUSED_VAR")) {
			Token firstToken = ctx.getStart();
			String name = firstToken.getText();

			/*
			 * Look up is the variable definition is used somewhere in the scope and nested
			 * scopes!
			 */
			boolean isNotCalled = finalVarDecl.contains(name);
			if (isNotCalled) {

				parser.notifyErrorListeners(firstToken, "Warn17####Variable " + name + " is defined but not used!",
						null);

			}
		}
	}

	public void exitE17VariableDeclaration(RParser.E17VariableDeclarationContext ctx) {

	}

	public void enterE19DefFunction(RParser.E19DefFunctionContext ctx) {
		currentScope = scopes.get(ctx);

		// int stopIndex = ctx.expr().getStop().getStopIndex();
		if (store.getBoolean("UNUSED_FUNCTION")) {
			Interval sourceInterval = ctx.getSourceInterval();
			int start = sourceInterval.a;

			/*
			 * If we have at least 2 tokens else we create a function without variable
			 * assignment!
			 */
			if ((start - 2) >= 0 && ctx.getParent().getChild(1) != null && ctx.getParent().getChild(2) != null) {

				String op = ctx.getParent().getChild(1).getText();
				String name = ctx.getParent().getChild(0).getText();
				/*
				 * Check if we have an assignment symbol available!
				 */
				if (op.equals("<-") || op.equals("<<-") || op.equals("=")) {
					/*
					 * Check if this method is called in the current or a parent scope!
					 */
					Token st = tokens.get(ctx.getParent().getChild(0).getSourceInterval().a);

					/*
					 * Calculate the closest function to the offset when closest found at the exit
					 * of the prog calculate the functions in the scope! This function is used to
					 * generate a function documentation with roxygen!
					 */
					setCurrentScopeFromOffsetFunctionName(ctx);

					boolean isNotCalled = finalFuncDecl.contains(name);
					if (isNotCalled) {

						parser.notifyErrorListeners(st, "Warn17####Function " + name
								+ " is defined but not called! \nProbably used as an argument!", null);

					}

				}

			}
		}

	}

	public void exitE19DefFunction(RParser.E19DefFunctionContext ctx) {

		/*
		 * For code completion detect the parentheses index according to grammar!
		 */
		int startIndexFun = ctx.getStart().getStartIndex();
		int stopIndexFun = ctx.getStop().getStopIndex();
		int startIndex = ctx.expr().getStart().getStartIndex();
		int stopIndex = ctx.expr().getStop().getStopIndex();
		/*
		 * Calculate the closest function to the offset when closest found at the exit
		 * of the prog calculate the functions in the scope!
		 */
		setCurrentScopeFromOffset(startIndex, stopIndex);
		/*
		 * Calculate the same but for the whole function (used for the refactor method
		 * rename to include function parameter variables)!
		 */
		setCurrentScopeFromOffsetFunctionBody(startIndexFun, stopIndexFun);

		currentScope = currentScope.getEnclosingScope(); // pop scope

	}

	public void enterE20CallFunction(RParser.E20CallFunctionContext ctx) {
		/* For code completion detect the parentheses! */
		Token parenth = ((TerminalNodeImpl) ctx.getChild(1)).getSymbol();

		int startIndex = parenth.getStopIndex();
		int stopIndex = ctx.getStop().getStopIndex();

		// Get the last token which should be the name of the called function!
		Token stop = ctx.expr().getStop();
		// Token lastToken = tokens.get(sourceInterval.b);
		SublistContext subList = ctx.sublist();
		List<SubContext> sub = subList.sub();
		String argText = subList.getText();

		int callSize = sub.size();

		String funcName = stop.getText();
		/*
		 * Here we set the current method call token (if we are in the parentheses) for
		 * the pipe operator detection (E8, E43)!
		 */
		if (offsetCodeCompl >= startIndex && offsetCodeCompl <= stopIndex) {
			currentPipeToken = stop;
		}

		/* Ignore functions without assigned names! */
		if (funcName.equals("function") || funcName.equals("\\")) {
			return;
		}

		/* Return number of args and names after function call! */
		RSymbol meth = currentScope.resolve(funcName);

		if (meth == null) {

			/*
			 * If we do not find the functions in the current file we search in the loaded
			 * packages!
			 */
			if (CalculateRProposals.stat != null) {

				if (CalculateRProposals.stat.containsValue(funcName)) {

					/* Store function call args for code completion! */
					if (offsetCodeCompl >= startIndex && offsetCodeCompl <= stopIndex) {
						isInMethodCall = true;
						/* We are not in a matrix, dataframe call! */
						isInMatrixBracketCall = false;
						isInMatrixDoubleBracketCall = false;
						proposalFuncFound = funcName;

					}

				} else {
					if (store.getBoolean("MISSING_FUNCTION")) {
						parser.notifyErrorListeners(stop,
								"Warn16####Function not available?#### " + funcName + " seems to be missing!", null);
					}

				}

			}
			/*
			 * In the function symbol we have also stored the arguments which we use here!
			 */
		} else {

			if (meth instanceof RFunctionSymbol) {
				RFunctionSymbol me = (RFunctionSymbol) meth;
				/* Add boolean true to mark the method as used! */
				me.setUsed(true);
				/* If the function has arguments! */

				if (me.getFormlist() != null) {
					String arguments = me.getFormlist().getText();

					List<FormContext> formList = me.getFormlist().form();
					int functionDefSize = formList.size();

					/* Store function call args for code completion! */
					if (offsetCodeCompl > startIndex && offsetCodeCompl <= stopIndex) {
						/*
						 * Set the proposalFuncFound to null for nested function calls in code
						 * completion if this is the closest match!
						 */
						proposalFuncFound = null;
						/*
						 * We collect all vars here for code completion unlike the warning below!
						 */
						StringBuffer strAll = new StringBuffer();
						for (int i = 0; i < formList.size(); i++) {
							FormContext fo = formList.get(i);

							TerminalNode ar = fo.ID();
							if (ar == null) {
								strAll.append("...");
							} else {
								strAll.append(ar);
							}
							/*
							 * We set a '=' to mark a function argument. ',' is the seperator char!
							 */
							if (i < formList.size()) {
								strAll.append(" = ,");
							}

						}

						methodCallVars = strAll;
						isInMethodCall = true;
						/* We are not in a matrix, dataframe call! */
						isInMatrixBracketCall = false;
						isInMatrixDoubleBracketCall = false;
					}
					/*
					 * Here we generate warnings/errors if the amount of functions vars differs from
					 * the implementation!
					 */
					StringBuffer str = new StringBuffer();
					/*
					 * Function call with Ellipsis can have multiple arguments so we test if that
					 * applies here!
					 */
					if (arguments.contains("...") == false) {
						if (argText.isEmpty() == false) {

							if (callSize < functionDefSize) {

								for (int i = callSize; i < formList.size(); i++) {
									FormContext fo = formList.get(i);

									TerminalNode ar = fo.ID();

									str.append(ar);

									if (i < formList.size() - 1) {
										str.append(" , ");
									}

								}
								if (store.getBoolean("CHECK_MISSING_FUNCTION_CALL_ARGS")) {
									parser.notifyErrorListeners(stop,
											"Warn17####The following args are missing: " + str.toString(), null);
								}

							}

							else if (callSize > functionDefSize) {
								/*
								 * Test for functions where the last argument is a ellipsis '...' allows any
								 * number of arguments!
								 */
								if (store.getBoolean("CHECK_EXCESSIVE_FUNCTION_CALL_ARGS")) {
									parser.notifyErrorListeners(stop,
											"Err12####To many args in function call (unused arguments)!", null);
								}
							}
							/*
							 * Control if the assigned argument, e.g., x=3 can be found in the function
							 * definition!
							 */
							/*
							 * Extract the variable assignment in function calls!
							 */
							if (store.getBoolean("CHECK_VARIABLE_ASSIGNMENT_FUNCTION_CALL_ARGS")) {
								Token tempFuncCallArray[] = new Token[callSize];
								for (int i = 0; i < callSize; i++) {

									ParseTree tree = sub.get(i).getChild(0);
									if (tree != null) {
										// System.out.println(tree.getText());
										if (tree instanceof E17VariableDeclarationContext) {

											E17VariableDeclarationContext tr = (E17VariableDeclarationContext) tree;
											/* Get the token! */
											tempFuncCallArray[i] = tr.expr(0).start;

										}

									}

								}
								for (int i = 0; i < formList.size(); i++) {
									FormContext fo = formList.get(i);

									TerminalNode ar = fo.ID();
									if (i < tempFuncCallArray.length) {

										if (tempFuncCallArray[i] != null) {
											String expectedArg = ar.getText();
											if (tempFuncCallArray[i].getText().equals(expectedArg) == false) {

												/*
												 * Text is splitted with'####' and can have three different messages!
												 * Here we use two '####'!
												 */
												parser.notifyErrorListeners(tempFuncCallArray[i],
														"Warn18####Wrong function call parameter name!####"
																+ expectedArg + "",
														null);
											}

										}
									}
								}
							}

						} else {

							StringBuffer str2 = new StringBuffer();

							for (int i = 0; i < formList.size(); i++) {
								FormContext fo = formList.get(i);
								TerminalNode ar = fo.ID();
								str2.append(ar);
								if (i < formList.size() - 1) {
									str2.append(" ,");
								}

							}
							if (store.getBoolean("CHECK_MISSING_FUNCTION_CALL_ARGS")) {
								parser.notifyErrorListeners(stop,
										"Warn17####The following args are missing: " + str2.toString(), null);
							}

							/*
							 * Store function call args for code completion!
							 */
							if (offsetCodeCompl > startIndex && offsetCodeCompl <= stopIndex) {

								methodCallVars = str2;
								isInMethodCall = true;
								/* We are not in a matrix, dataframe call! */
								isInMatrixBracketCall = false;
								isInMatrixDoubleBracketCall = false;
							}

						}

					}
					/*
					 * Check the variable assignment in function calls. Offer a quickfix to replace
					 * '<-' with '='!
					 */
					if (store.getBoolean("WRONG_FUNCTION_CALL_OPERATOR_ASSIGNMENT")) {
						if (argText.isEmpty() == false) {
							// Token tempFuncCallArray[] = new Token[callSize];
							for (int i = 0; i < callSize; i++) {

								ParseTree tree = sub.get(i).getChild(0);
								if (tree != null) {
									// System.out.println(tree.getText());
									if (tree instanceof E17VariableDeclarationContext) {

										E17VariableDeclarationContext tr = (E17VariableDeclarationContext) tree;
										/* Get the <- token! */
										ParseTree parseTree = tr.getChild(1);
										if (parseTree.getText().equals("<-")) {
											Token tok = tokens.get(parseTree.getSourceInterval().a);
											parser.notifyErrorListeners(tok,
													"Warn22####Possibly wrong assignment operator in function call argument.####=",
													null);
										}

									}

								}

							}
						}
					}

				}

				/*
				 * The function definition has no arguments but the function call has!
				 */
				else {
					if (argText.isEmpty() == false) {
						if (store.getBoolean("CHECK_FOR_EMPTY_ARG_FUNCTION"))
							parser.notifyErrorListeners(stop,
									"Warn17####The function definiton has no arguments to call! ", null);
						// System.out.println("calltext " + callText);
					}
				}

			}

			else if (meth instanceof RVariableSymbol) {

				return;

			}

		}

		if (funcName.equals("class") || funcName.equals("setClass") || funcName.equals("setRefClass")
				|| funcName.equals("R6Class")) {

			setCurrentScopeFromOffsetFunctionCall(ctx);
		}
		if (funcName.equals("library") || funcName.equals("require")) {
			String lib = ctx.sublist().getText();
			if (lib.isEmpty() == false) {

				currentPackageImports.put(lib, lib);
			}
			/* Check if the package folder exists! */

			if (store.getBoolean("CHECK_INSTALLED_LIBRARY")) {
				Path path = null;
				Path path2 = null;

				/* We need some paths from the com.eco.bio7 plugin! */
				IPreferenceStore storeBio7Plugin = new ScopedPreferenceStore(InstanceScope.INSTANCE, "com.eco.bio7");
				/*
				 * First we check the path of the Bio7 preferences then the most common default
				 * paths of the OS!
				 */
				/* We replace quotations here when we calculate a possible Java path! */
				lib = lib.replace("\"", "");
				try {
					if (Util.isWindows()) {
						path = Paths.get(storeBio7Plugin.getString("InstallLocation") + "/" + lib);
						path2 = Paths.get(storeBio7Plugin.getString("r") + "/library" + "/" + lib);
						if (Files.exists(path) || Files.exists(path2)) {
							return;
						} else {
							parser.notifyErrorListeners(ctx.sublist().stop,
									"Warn23####The library is not installed: " + lib, null);
						}
					} else if (Util.isMac()) {
						path = Paths.get(storeBio7Plugin.getString("InstallLocation") + "/" + lib);
						path2 = Paths.get("/Library/Frameworks/R.framework/Resources/library" + "/" + lib);
						if (Files.exists(path) || Files.exists(path2)) {
							return;
						} else {
							parser.notifyErrorListeners(ctx.sublist().stop,
									"Warn23####The library is not installed: " + lib, null);
						}
					} else if (Util.isLinux()) {
						Path path3 = null;
						Path path4 = null;
						path = Paths.get(storeBio7Plugin.getString("InstallLocation") + "/" + lib);
						path2 = Paths.get("/usr/local/lib/R/site-library" + "/" + lib);
						path3 = Paths.get("/usr/lib/R/library" + "/" + lib);
						path4 = Paths.get("/usr/lib/R/site-library" + "/" + lib);
						if (Files.exists(path) || Files.exists(path2) || Files.exists(path3) || Files.exists(path4)) {
							return;
						} else {
							parser.notifyErrorListeners(ctx.sublist().stop,
									"Warn23####The library is not installed: " + lib, null);
						}
					}
				} catch (InvalidPathException e) {
					// We do not fire an error message here in rare cases the path without
					// quotations is invalid!
					// e.printStackTrace();
				}

			}

		}
	}

	/*
	 * Calculate the current scope of the given (selected) offset (in which function
	 * the offset is embedded)
	 */
	private void setCurrentScopeFromOffset(int startIndex, int stopIndex) {
		/* If we have a selected offset (not by opening the file)! */
		if (offsetCodeCompl >= 0) {
			int distanceFromStart = offsetCodeCompl - startIndex;
			int distanceFromStop = offsetCodeCompl - stopIndex;
			/* If we have an positive offset after function parentheses! */
			if (distanceFromStart > 0 && distanceFromStop < 0) {

				/*
				 * Lookup if we have already the closest distance. If not take this distance as
				 * closest!
				 */
				if (distanceFromStart < tempDifferenceStart) {
					tempDifferenceStart = distanceFromStart;
					/*
					 * Store temporary the scope! Used at program in this file exit!
					 */
					tempCodeComplScope = currentScope;

					// }
				}

			}
		}
	}

	/*
	 * Calculate the current selected function by the given (selected) offset from
	 * the ID to the end of the expression!
	 */
	private void setCurrentScopeFromOffsetFunctionName(E19DefFunctionContext ctx) {
		// st.getStartIndex(), stopIndex,st,ctx.expr().getStop(),ctx
		/* If we have a selected offset (not by opening the file)! */
		int stopIndex = ctx.expr().getStop().getStopIndex();
		Token st = tokens.get(ctx.getParent().getChild(0).getSourceInterval().a);
		if (offsetCodeCompl >= 0) {
			int distanceFromStart = offsetCodeCompl - st.getStartIndex();
			int distanceFromStop = offsetCodeCompl - stopIndex;
			/* If we have an positive offset after function parentheses! */
			if (distanceFromStart > 0 && distanceFromStop < 0) {

				/*
				 * Lookup if we have already the closest distance. If not take this distance as
				 * closest!
				 */
				if (distanceFromStart < tempDifferenceStart) {
					tempDifferenceStart = distanceFromStart;
					/*
					 * Store temporary the scope! Used at program in this file exit!
					 */
					// tempCodeComplScope = currentScope;
					/* We store the selected function offset to create roxygen comments! */
					selectedFunOffsetContext = ctx;

					// }
				}

			}
		}
	}

	/*
	 * Calculate the current selected function by the given (selected) offset from
	 * the ID to the end of the expression!
	 */
	private void setCurrentScopeFromOffsetFunctionCall(E20CallFunctionContext ctx) {
		int tempStart = ctx.getStart().getStartIndex();
		int tempStop = ctx.stop.getStopIndex();
		/* If we have a selected offset (not by opening the file)! */
		if (offsetCodeCompl >= 0) {
			int distanceFromStart = offsetCodeCompl - tempStart;
			int distanceFromStop = offsetCodeCompl - tempStop;
			/* If we have an positive offset after function parentheses! */
			if (distanceFromStart > 0 && distanceFromStop < 0) {

				/*
				 * Lookup if we have already the closest distance. If not take this distance as
				 * closest!
				 */
				if (distanceFromStart < tempDifferenceStart) {
					tempDifferenceStart = distanceFromStart;
					/*
					 * Store temporary the scope! Used at program in this file exit!
					 */
					/* We store the selected function offset to create roxygen comments! */
					selectedFunOffsetContext = ctx;

				}

			}
		}
	}

	/*
	 * Calculate the current scope of the given (selected) offset (in which function
	 * the offset is embedded) from the function body!
	 */
	private void setCurrentScopeFromOffsetFunctionBody(int funStartIndex, int funStopIndex) {
		/* If we have a selected offset (not by opening the file)! */
		if (offsetCodeCompl >= 0) {
			int distanceFromStart = offsetCodeCompl - funStartIndex;
			int distanceFromStop = offsetCodeCompl - funStopIndex;
			/* If we have an positive offset after function parentheses! */
			if (distanceFromStart > 0 && distanceFromStop < 0) {

				/*
				 * Lookup if we have already the closest distance. If not take this distance as
				 * closest!
				 */
				if (distanceFromStart < tempDifferenceStartFunction) {
					tempDifferenceStartFunction = distanceFromStart;
					/*
					 * Store temporary the scope! Used at program in this file exit!
					 */
					// tempCodeComplScope = currentScope;
					/*
					 * Next we store the start and stop positions of the current function (scope) of
					 * the selection used for the rename refactor method!
					 */
					tempCodeStartIndex = funStartIndex;
					tempCodeStopIndex = funStopIndex;
					/*
					 * Store temporary the scope! Used at program in this file exit!
					 */
					selectedRefactorScope = currentScope;

					selectedScopeNumber = currentScope.getScopeNumber();

				}

			}
		}
	}

	/*
	 * Recursive get all visible function definitions in the current and parent
	 * scopes!
	 */
	public void getScopeDefFunctions(Scope currentScope) {

		/* Get the defined variables! */
		Map<String, RSymbol> map = currentScope.getArguments();
		map.keySet();
		Iterator<Entry<String, RSymbol>> it = map.entrySet().iterator();
		while (it.hasNext()) {

			Map.Entry<String, RSymbol> pair = it.next();

			if (pair.getValue() instanceof RFunctionSymbol) {

				buffScopeFunctions.append(pair.getKey());
				buffScopeFunctions.append(",");

				it.remove(); // avoids a ConcurrentModificationException

			}
		}
		/* Go through all parent scopes! */
		if (currentScope.getEnclosingScope() != null) {
			getScopeDefFunctions(currentScope.getEnclosingScope());
		}

	}

	/* Recursive get all visible vars in the current and parent scopes! */
	public void getScopeVars(Scope currentScope) {

		/* Get the defined variables! */
		Map<String, RSymbol> map = currentScope.getArguments();
		map.keySet();
		Iterator<Entry<String, RSymbol>> it = map.entrySet().iterator();
		while (it.hasNext()) {

			Map.Entry<String, RSymbol> pair = it.next();

			if (pair.getValue() instanceof RVariableSymbol) {

				buffScopeVars.append(pair.getKey());
				buffScopeVars.append(",");

				it.remove(); // avoids a ConcurrentModificationException

			}
		}
		/* Go through all parent scopes! */
		if (currentScope.getEnclosingScope() != null) {
			getScopeVars(currentScope.getEnclosingScope());
		}

	}

}
