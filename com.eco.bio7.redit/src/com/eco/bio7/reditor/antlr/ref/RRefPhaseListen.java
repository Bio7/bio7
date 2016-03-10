package com.eco.bio7.reditor.antlr.ref;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.runtime.tree.TerminalNodeImpl;

import com.eco.bio7.reditor.antlr.RBaseListen;
import com.eco.bio7.reditor.antlr.RBaseListener;
import com.eco.bio7.reditor.antlr.RParser;
import com.eco.bio7.reditor.antlr.RParser.E17VariableDeclarationContext;
import com.eco.bio7.reditor.antlr.RParser.E20CallFunctionContext;
import com.eco.bio7.reditor.antlr.RParser.FormContext;
import com.eco.bio7.reditor.antlr.RParser.SubContext;
import com.eco.bio7.reditor.antlr.RParser.SublistContext;
import com.eco.bio7.reditor.antlr.util.Utils;
import com.eco.bio7.rpreferences.template.CalculateRProposals;

public class RRefPhaseListen extends RBaseListener {
	private ParseTreeProperty<Scope> scopes;
	private RGlobalScope globals;
	private Scope currentScope; // resolve symbols starting in this scope
	private CommonTokenStream tokens;
	private Parser parser;
	private Set<String> finalFuncDecl;
	private Set<String> finalVarDecl;
	private int offsetCodeCompl = -1;
	private StringBuffer methodCallVars;
	private StringBuffer buffScopeVars;
	private StringBuffer buffScopeFunctions;
	private int tempDifferenceStart = 10000000;
	private Scope tempCodeComplScope;
	private boolean isInVarCall;
	private String proposalFuncFound;

	public String getProposalFuncFound() {
		return proposalFuncFound;
	}

	public boolean isInVarCall() {
		return isInVarCall;
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

	}

	public void enterProg(RParser.ProgContext ctx) {
		currentScope = globals;
		/*
		 * Iterator<String> itr = finalVarDecl.iterator(); while(itr.hasNext()){
		 * System.out.println("object: " + itr.next()); }
		 */

	}

	public void exitProg(RParser.ProgContext ctx) {
		/*
		 * This calculates the available functions/variables for code completion
		 * recursively through all available scopes for code completion!
		 */
		// System.out.println("exit found func "+proposalFuncFound);
		buffScopeVars = new StringBuffer();
		buffScopeFunctions = new StringBuffer();
		if (tempCodeComplScope != null) {
			// System.out.println("Next scope is:" + tempCodeComplScope);
			getScopeDefFunctions(tempCodeComplScope);
			getScopeVars(tempCodeComplScope);
		} else {
			getScopeDefFunctions(currentScope);
			getScopeVars(currentScope);
		}
	}

	public void enterE30(RParser.E30Context ctx) {

	}
	/*public boolean getCtxParent(ParserRuleContext p) {

		if (p.getParent() != null) {
			ParserRuleContext parent = p.getParent();
			if (parent instanceof SubContext || parent instanceof SublistContext) {
				// System.out.println("p is SubContetx"+p.getText());

				//System.out.println(parent.getText() + ": p is " + parent.getClass());
				return true;

			} else {

				return getCtxParent(parent);
			}

		}

		return false;
	}*/

	/* Variable call! To do: Need to calculate position of <- */
	public void exitE30(RParser.E30Context ctx) {

		Token tok = ctx.ID().getSymbol();
		// System.out.println("Token Text: "+tok.getText());
		String varName = tok.getText();
		int index = tok.getTokenIndex();
		//Token idNextToken = tokens.get(index + 1);
		/*Filter whitespace out because we use Token channel hidden!*/
		Token idNextToken = Utils.whitespaceTokenFilter(index, ctx.stop.getStopIndex(),tokens);
		// System.out.println("Next Symbol= "+idNextToken.getText());
		if (idNextToken != null) {
			if (idNextToken.getText().equals("=") || idNextToken.getText().equals("<-") || idNextToken.getText().equals("<<-") ||idNextToken.getText().equals("(")) {
				return;
			}

			else {
				RSymbol var = currentScope.resolve(varName);
				
				
				
				
				if (var instanceof RFunctionSymbol) {
					return;
					// System.out.println("Var: " + name + " is not
					// available!");
				}

				if (var == null) {
					/*Find out recursively if the variable assignment is in a method call (sublist)!*/
					boolean isSubTrue = Utils.getCtxParent(ctx);

					//System.out.println("has Sub?: " + isSubTrue);
					if (isSubTrue == true) {
						return;
					}

					parser.notifyErrorListeners(tok, "Warn17:Variable not available?: " + varName + " seems to be missing!", null);

				}
			}
		}

		// Token lastToken = tokens.get(sourceInterval.b);

	}

	public void enterE17VariableDeclaration(RParser.E17VariableDeclarationContext ctx) {
		Token firstToken = ctx.getStart();
		String name = firstToken.getText();
		// System.out.println(name);
		/*
		 * Look up is the variable definition is used somewhere in the scope and
		 * nested scopes!
		 */
		boolean isNotCalled = finalVarDecl.contains(name);
		if (isNotCalled) {

			parser.notifyErrorListeners(firstToken, "Warn17:Variable " + name + " is defined but not used!:", null);

		}
	}

	public void exitE17VariableDeclaration(RParser.E17VariableDeclarationContext ctx) {

	}

	public void enterE19DefFunction(RParser.E19DefFunctionContext ctx) {
		currentScope = scopes.get(ctx);

		Interval sourceInterval = ctx.getSourceInterval();
		int start = sourceInterval.a;

		/*
		 * If we have at least 2 tokens else we create a function without
		 * variable assignment!
		 */
		if ((start - 2) >= 0 && ctx.getParent().getChild(1) != null && ctx.getParent().getChild(2) != null) {

			String op = ctx.getParent().getChild(1).getText();
			String name = ctx.getParent().getChild(0).getText();
			/*
			 * Check if we have an assignment symbol available! else we create a
			 * function without variable assignment!
			 */
			if (op.equals("<-") || op.equals("<<-") || op.equals("=")) {
				/*
				 * Check if this method is called in the current or a parent
				 * scope!
				 */
				Token st = tokens.get(ctx.getParent().getChild(0).getSourceInterval().a);
				// System.out.println("Name is: "+name);
				/*
				 * RSymbol meth = currentScope.resolveFuncCalls(name);
				 * 
				 */
				boolean isNotCalled = finalFuncDecl.contains(name);
				if (isNotCalled) {

					parser.notifyErrorListeners(st, "Warn17:Function " + name + " is defined but not used!:", null);

				}

			}

		}

	}

	public void exitE19DefFunction(RParser.E19DefFunctionContext ctx) {

		/*
		 * For code completion detect the parentheses index according to
		 * grammar!
		 */
		int startIndex = ctx.expr().getStart().getStartIndex();
		int stopIndex = ctx.expr().getStop().getStopIndex();
		/*
		 * Calculate the closest function to the offset when closest found at
		 * the exit of the prog calculate the functions in the scope!
		 */
		setCurrentScopeFromOffset(startIndex, stopIndex);

		currentScope = currentScope.getEnclosingScope(); // pop scope

	}

	public void enterE20CallFunction(RParser.E20CallFunctionContext ctx) {
		/* For code completion detect the parentheses! */
		Token parenth = ((TerminalNodeImpl) ctx.getChild(1)).getSymbol();
		// System.out.println(ctx.getChild(1).getText());

		int startIndex = parenth.getStopIndex();
		int stopIndex = ctx.getStop().getStopIndex();
		// System.out.println(offsetCodeCompl+" "+startIndex+" "+stopIndex);
		// Get the last token which should be the name of the called function!
		Token stop = ctx.expr().getStop();
		// Token lastToken = tokens.get(sourceInterval.b);
		SublistContext subList = ctx.sublist();
		List<SubContext> sub = subList.sub();
		String argText = subList.getText();

		int callSize = sub.size();

		// String callText = sub.get(0).getText();

		String funcName = stop.getText();
		// System.out.println(funcName);
		/* Return number of args and names after function call! */
		RSymbol meth = currentScope.resolve(funcName);

		if (meth == null) {
			/*
			 * If we do not find the functions in the current file we search in
			 * the loaded packages!
			 */
			if (CalculateRProposals.stat != null) {
				// System.out.println("Is there: " + funcName);
				if (CalculateRProposals.stat.containsValue(funcName)) {

					/* Store function call args for code completion! */
					if (offsetCodeCompl >= startIndex && offsetCodeCompl <= stopIndex) {
						isInVarCall = true;
						proposalFuncFound = funcName;

					}

				} else {
					parser.notifyErrorListeners(stop, "Warn16:Function not available?: " + funcName + " seems to be missing!", null);

				}

			}
			/*
			 * In the function symbol we have also stored the arguments which we
			 * use here!
			 */
		} else {
			// System.out.println("resolved "+funcName);
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
						 * We collect all vars here for code completion unlike
						 * the warning below!
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
							 * We set a '=' to mark a function argument. ',' is
							 * the seperator char!
							 */
							if (i < formList.size()) {
								strAll.append(" = ,");
							}

						}

						methodCallVars = strAll;
						isInVarCall = true;
					}
					/*
					 * Here we generate warnings/errors if the amount of
					 * functions vars differs from the implementation!
					 */
					StringBuffer str = new StringBuffer();
					/*
					 * Function call with Ellipsis can have multiple arguments
					 * so we test if that applies here!
					 */
					if (arguments.contains("...") == false) {
						if (argText.isEmpty() == false) {

							if (callSize < functionDefSize) {
								for (int i = callSize; i < formList.size(); i++) {
									FormContext fo = formList.get(i);

									TerminalNode ar = fo.ID();

									str.append(ar);

									if (i < formList.size()) {
										str.append("=,");
									}

								}

								parser.notifyErrorListeners(stop, "Warn17:The following args are missing -> " + str.toString() + ": ", null);

							}

							else if (callSize > functionDefSize) {
								/*
								 * Test for functions where the last argument is
								 * a ellipsis '...' allows any number of
								 * arguments!
								 */

								parser.notifyErrorListeners(stop, "Warn17:To many args in function call!: ", null);

							}
							/*
							 * Control if the assigned argument, e.g., x=3 can
							 * be found in the function definition!
							 */
							/*
							 * Extract the variable assignment in function
							 * calls!
							 */
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
										if (tempFuncCallArray[i].getText().equals(ar.getText()) == false) {

											// System.out.println("wrong
											// function call assignment!" + "
											// definition: " + ar.getText() + "
											// array: " + tempFuncCallArray[i]);
											parser.notifyErrorListeners(tempFuncCallArray[i], "Err23:Wrong function call assignment!" + " definition: " + ar.getText() + " array: " + tempFuncCallArray[i] + ": ", null);
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
								if (i < formList.size()) {
									str2.append(" = ,");
								}

							}

							parser.notifyErrorListeners(stop, "Warn17:The following args are missing -> " + str2.toString() + ": ", null);

							/* Store function call args for code completion! */
							if (offsetCodeCompl > startIndex && offsetCodeCompl <= stopIndex) {

								methodCallVars = str2;
								isInVarCall = true;
							}

						}

					} else {

					}

				}
				/*
				 * The function definition has no arguments but the function
				 * call has!
				 */
				else {
					if (argText.isEmpty() == false) {
						parser.notifyErrorListeners(stop, "Warn17:The function definiton has no arguments to call! ", null);
						// System.out.println("calltext " + callText);
					}
				}

			}

			else if (meth instanceof RVariableSymbol) {

				return;

			}
			/*
			 * else{ parser.notifyErrorListeners(stop,
			 * "Warn16:Function not available? " + funcName +
			 * ": seems to be missing!", null); }
			 */
		}
	}

	/*
	 * Calculate the current scope of the given (selected) offset (in which
	 * function the offset is embedded)
	 */
	private void setCurrentScopeFromOffset(int startIndex, int stopIndex) {
		/* If we have a selected offset (not by opening the file)! */
		if (offsetCodeCompl >= 0) {
			int distanceFromStart = offsetCodeCompl - startIndex;
			int distanceFromStop = offsetCodeCompl - stopIndex;
			/* If we have an positive offset after function parentheses! */
			if (distanceFromStart > 0 && distanceFromStop < 0) {
				// System.out.println(distanceFromStart);
				/*
				 * lookup if we have already the closest distance. If not take
				 * this distance as closest!
				 */
				if (distanceFromStart < tempDifferenceStart) {
					tempDifferenceStart = distanceFromStart;
					/*
					 * Store temporary the scope! Used at program in this file
					 * exit!
					 */
					tempCodeComplScope = currentScope;

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

				// buffScopeVars.append(",");
				buffScopeFunctions.append(pair.getKey());
				buffScopeFunctions.append(",");
				// System.out.println(pair.getKey());
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

				// buffScopeVars.append(",");
				buffScopeVars.append(pair.getKey());
				buffScopeVars.append(",");
				// System.out.println(pair.getKey() + " = " +
				// pair.getValue());
				it.remove(); // avoids a ConcurrentModificationException

			}
		}
		/* Go through all parent scopes! */
		if (currentScope.getEnclosingScope() != null) {
			getScopeVars(currentScope.getEnclosingScope());
		}

	}

}
