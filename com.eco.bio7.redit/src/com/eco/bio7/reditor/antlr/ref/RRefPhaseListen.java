package com.eco.bio7.reditor.antlr.ref;

import java.util.List;

import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;

import com.eco.bio7.reditor.antlr.RBaseListener;
import com.eco.bio7.reditor.antlr.RParser;
import com.eco.bio7.reditor.antlr.RParser.FormContext;
import com.eco.bio7.reditor.antlr.RParser.SubContext;
import com.eco.bio7.reditor.antlr.RParser.SublistContext;
import com.eco.bio7.reditors.REditor;
import com.eco.bio7.rpreferences.template.CalculateRProposals;
import com.eco.bio7.rpreferences.template.RCompletionProcessor;

public class RRefPhaseListen extends RBaseListener {
	ParseTreeProperty<Scope> scopes;
	RGlobalScope globals;
	Scope currentScope; // resolve symbols starting in this scope
	private CommonTokenStream tokens;
	private Parser parser;

	public RRefPhaseListen(CommonTokenStream tokens, RGlobalScope globals, ParseTreeProperty<Scope> scopes, Parser parser) {
		this.scopes = scopes;
		this.globals = globals;
		this.tokens = tokens;
		this.parser = parser;
	}

	public void enterProg(RParser.ProgContext ctx) {
		currentScope = globals;

	}

	public void enterE30(RParser.E30Context ctx) {

	}

	/* Variable call! */
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
			} else {
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

	}

	public void exitE17VariableDeclaration(RParser.E17VariableDeclarationContext ctx) {

	}

	public void enterE19DefFunction(RParser.E19DefFunctionContext ctx) {
		currentScope = scopes.get(ctx);

	}

	public void exitE19DefFunction(RParser.E19DefFunctionContext ctx) {

		currentScope = currentScope.getEnclosingScope(); // pop scope

	}

	public void enterE20CallFunction(RParser.E20CallFunctionContext ctx) {
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
		// System.out.println("Argument: "+ sub.get(0).getText());
		// System.out.println("***********************************************************");

		String funcName = stop.getText();
		/*
		 * IEditorPart editor = (IEditorPart)
		 * PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().
		 * getActiveEditor(); if(editor instanceof REditor){ REditor
		 * edit=(REditor) editor; RCompletionProcessor processor =
		 * edit.getRconf().getProcessor();
		 */
		if (CalculateRProposals.stat != null) {
			if (funcName != null && CalculateRProposals.stat.containsValue(funcName)) {
				return;
			}
		}
		/* Return number of args and names after function call! */
		RSymbol meth = currentScope.resolve(funcName);

		if (meth == null) {
			parser.notifyErrorListeners(stop, "Warn16:Function not available?: " + funcName + " seems to be missing!", null);

			// System.out.println("Function: " + funcName + " is not
			// available!");
		} else if (meth != null && meth instanceof RFunctionSymbol) {
			RFunctionSymbol me = (RFunctionSymbol) meth;
			/* If the function has arguments! */
			
			if (me.getFormlist() != null) {
				String arguments = me.getFormlist().getText();

				List<FormContext> formList = me.getFormlist().form();
				int functionDefSize = formList.size();
				// System.out.println("Arguments: "+me.getFormlist().getText());
				if (argText.isEmpty() == false) {
					StringBuffer str = new StringBuffer();
					if (arguments.contains("...") == false) {
					
						if (callSize < functionDefSize) {
							for (int i = callSize; i < formList.size(); i++) {
								FormContext fo = formList.get(i);

								TerminalNode ar = fo.ID();
								
								str.append(ar);

								if (i < formList.size() - 1) {
									str.append(", ");
								}

							}

							System.out.println("sie: " + formList.size());
							System.out.println("The following arg is missing: " + str.toString());
							parser.notifyErrorListeners(stop, "Warn16:The following arg is missing-> " + str.toString() + ": ", null);
						}

						else if (callSize > functionDefSize) {
							/*
							 * Test for functions where the last argument is a
							 * ellipsis '...' allows any number of arguments!
							 */
							FormContext fo = formList.get(functionDefSize - 1);
							String ar = fo.getText();
							System.out.println("text is: " + ar);
							
								parser.notifyErrorListeners(stop, "Warn16:To many args in function call!: ", null);
								
							
						}
					}
					else{
						System.out.println("Ellipsis: ...");
					}
					/*
					 * If we have no arguments in the function call (we have to
					 * control the sublist because a sub token could also be an
					 * empty string according to the grammar definition!)
					 */
				} else {

					StringBuffer str = new StringBuffer();
					// List<FormContext> formList = me.getFormlist().form();
					for (int i = 0; i < formList.size(); i++) {
						FormContext fo = formList.get(i);
						TerminalNode ar = fo.ID();
						str.append(ar);
						if (i < formList.size() - 1) {
							str.append(", ");
						}

					}
					System.out.println("Empty comma calls: " + formList.size());
					parser.notifyErrorListeners(stop, "Warn16:The following arg is missing-> " + str.toString() + ": ", null);
					System.out.println("The following arg is missing: " + str.toString());

				}

			}
			/*
			 * The function definition has no arguments but the function call
			 * has!
			 */
			else {
				if (callText.isEmpty() == false) {
					parser.notifyErrorListeners(stop, "Warn16:The function definiton has no arguments to call! ", null);
					System.out.println("The function definiton has no arguments to call!");
				}
			}

		}

		if (meth instanceof RVariableSymbol) {
			// System.out.println("Function: "+funcName+" is not a function!");
			// System.out.println("Function: " + funcName + " is not
			// available!");
			parser.notifyErrorListeners(stop, "Warn16:Function not available? " + funcName + ": seems to be missing!", null);

		}

	}

	public void exitE20CallFunction(RParser.E20CallFunctionContext ctx) {

	}

}
