package com.eco.bio7.reditor.antlr.ref;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.antlr.v4.runtime.tree.TerminalNode;
import com.eco.bio7.reditor.antlr.RBaseListen;
import com.eco.bio7.reditor.antlr.RBaseListener;
import com.eco.bio7.reditor.antlr.RParser;
import com.eco.bio7.reditor.antlr.RParser.FormContext;
import com.eco.bio7.reditor.antlr.RParser.SubContext;
import com.eco.bio7.reditor.antlr.RParser.SublistContext;
import com.eco.bio7.rpreferences.template.CalculateRProposals;

public class RRefPhaseListen extends RBaseListener {
	private ParseTreeProperty<Scope> scopes;
	private RGlobalScope globals;
	private Scope currentScope; // resolve symbols starting in this scope
	private CommonTokenStream tokens;
	private Parser parser;
	private Set<String> finalFuncDecl;
	private Set<String> finalVarDecl;
	private int offsetCodeCompl;
	private StringBuffer methodCallVars;

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
		System.out.println(offsetCodeCompl);
	}

	public void enterProg(RParser.ProgContext ctx) {
		currentScope = globals;
		/*
		 * Iterator<String> itr = finalVarDecl.iterator(); while(itr.hasNext()){
		 * System.out.println("object: " + itr.next()); }
		 */

	}

	public void exitProg(RParser.ProgContext ctx) {

	}

	public void enterE30(RParser.E30Context ctx) {

	}

	/* Variable call! To do: Need to calculate position of <- */
	public void exitE30(RParser.E30Context ctx) {

		Token tok = ctx.ID().getSymbol();
		// System.out.println("Token Text: "+tok.getText());
		String varName = tok.getText();
		int index = tok.getTokenIndex();
		Token idNextToken = tokens.get(index + 1);
		// System.out.println("Next Symbol= "+idNextToken.getText());
		if (idNextToken != null) {
			if (idNextToken.getText().equals("=") || idNextToken.getText().equals("<-") || idNextToken.getText().equals("(")) {
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
					// System.out.println("Var: " + name + " is not
					// available!");
					parser.notifyErrorListeners(tok, "Warn16:Variable not available?: " + varName + " seems to be missing!", null);

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

			parser.notifyErrorListeners(firstToken, "Warn16:Variable " + name + " is defined but not used!:", null);

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

					parser.notifyErrorListeners(st, "Warn16:Function " + name + " is defined but not used!:", null);

				}

			}

		}

	}

	public void exitE19DefFunction(RParser.E19DefFunctionContext ctx) {

		currentScope = currentScope.getEnclosingScope(); // pop scope

	}

	public void enterE20CallFunction(RParser.E20CallFunctionContext ctx) {
		/* For code completion detetct the parentheses! */
		int startIndex = ctx.getStart().getStartIndex();
		int stopIndex = ctx.getStop().getStopIndex();

		// Get the last token which should be the name of the called function!
		Token stop = ctx.expr().getStop();
		// Token lastToken = tokens.get(sourceInterval.b);
		SublistContext subList = ctx.sublist();
		List<SubContext> sub = subList.sub();
		String argText = subList.getText();

		// System.out.println("subList Text= "+subList.getText());
		// System.out.println("Argument size: "+sub.size());
		int callSize = sub.size();
		String callText = sub.get(0).getText();

		String funcName = stop.getText();

		/* Return number of args and names after function call! */
		RSymbol meth = currentScope.resolve(funcName);

		if (meth == null) {
			/*
			 * If we do not find the functions in the current file we search in
			 * the loaded packages!
			 */
			if (CalculateRProposals.stat != null) {
				if (funcName != null && CalculateRProposals.stat.containsValue(funcName)) {
					return;
				}
			}
			parser.notifyErrorListeners(stop, "Warn16:Function not available?: " + funcName + " seems to be missing!", null);

			// System.out.println("Function: " + funcName + " is not
			// available!");
		} else if (meth instanceof RFunctionSymbol) {
			RFunctionSymbol me = (RFunctionSymbol) meth;
			/* Add boolean true to mark the method as used! */
			me.setUsed(true);
			/* If the function has arguments! */

			if (me.getFormlist() != null) {
				String arguments = me.getFormlist().getText();

				List<FormContext> formList = me.getFormlist().form();
				int functionDefSize = formList.size();
				// System.out.println("Arguments: "+me.getFormlist().getText());
				//
				StringBuffer str = new StringBuffer();
				if (arguments.contains("...") == false) {
					if (argText.isEmpty() == false) {
						if (callSize < functionDefSize) {
							for (int i = callSize; i < formList.size(); i++) {
								FormContext fo = formList.get(i);

								TerminalNode ar = fo.ID();

								str.append(ar);

								if (i < formList.size() - 1) {
									str.append(",");
								}

							}

							parser.notifyErrorListeners(stop, "Warn16:The following args are missing -> " + str.toString() + ": ", null);
							/* Store function call args for code completion! */
							if (offsetCodeCompl >= startIndex && offsetCodeCompl <= stopIndex) {
								// System.out.println("Name
								// is:"+ctx.start.getText()+"proposal:
								// "+str.toString());
								methodCallVars = str;

							}

						}

						else if (callSize > functionDefSize) {
							/*
							 * Test for functions where the last argument is a
							 * ellipsis '...' allows any number of arguments!
							 */
							FormContext fo = formList.get(functionDefSize - 1);
							String ar = fo.getText();
							// System.out.println("text is: " + ar);

							parser.notifyErrorListeners(stop, "Warn16:To many args in function call!: ", null);

						}

					} else {

						StringBuffer str2 = new StringBuffer();

						for (int i = 0; i < formList.size(); i++) {
							FormContext fo = formList.get(i);
							TerminalNode ar = fo.ID();
							str2.append(ar);
							if (i < formList.size() - 1) {
								str2.append(",");
							}

						}
						// System.out.println("Empty comma calls: " +
						// formList.size());
						parser.notifyErrorListeners(stop, "Warn16:The following args are missing -> " + str2.toString() + ": ", null);

						/* Store function call args for code completion! */
						if (offsetCodeCompl >= startIndex && offsetCodeCompl <= stopIndex) {

							// System.out.println("Name
							// is:"+ctx.start.getText()+"proposal:
							// "+str2.toString());
							methodCallVars = str2;
						}

					}

				} else {
					// System.out.println("Ellipsis: ...");

				}

			}
			/*
			 * The function definition has no arguments but the function call
			 * has!
			 */
			else {
				if (argText.isEmpty() == false) {
					parser.notifyErrorListeners(stop, "Warn16:The function definiton has no arguments to call! ", null);
					System.out.println("calltext " + callText);
				}
			}

		}

		else if (meth instanceof RVariableSymbol) {
			// System.out.println("Function: "+funcName+" is not a function!");
			// System.out.println("Function: " + funcName + " is not
			// available!");
			parser.notifyErrorListeners(stop, "Warn16:Function not available? " + funcName + ": seems to be missing!", null);

		}

	}

	public void exitE20CallFunction(RParser.E20CallFunctionContext ctx) {
		/* For code completion detetct the parentheses! */
		int startIndex = ctx.getStart().getStartIndex();
		int stopIndex = ctx.getStop().getStopIndex();

		/* Store function call args for code completion! */
		if (offsetCodeCompl >= startIndex && offsetCodeCompl <= stopIndex) {
			/* Return number of args and names after function call! */
			if (currentScope instanceof RFunctionSymbol) {
				RFunctionSymbol rfu = (RFunctionSymbol) currentScope;
				// System.out.println("Name is:"+ctx.start.getText()+"proposal:
				// "+str.toString());
				Map map = rfu.getArguments();
				map.keySet();
				Iterator it = map.entrySet().iterator();
				while (it.hasNext()) {

					Map.Entry pair = (Map.Entry) it.next();

					if (pair.getValue() instanceof RVariableSymbol) {
						System.out.println(pair.getKey() + " = " + pair.getValue());
						it.remove(); // avoids a ConcurrentModificationException
					}
				}
			}
		}

	}

}
