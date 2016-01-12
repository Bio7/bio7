package com.eco.bio7.reditor.antlr.ref;

import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;

import com.eco.bio7.reditor.antlr.RBaseListener;
import com.eco.bio7.reditor.antlr.RParser;
import com.eco.bio7.reditors.REditor;
import com.eco.bio7.rpreferences.template.CalculateRProposals;
import com.eco.bio7.rpreferences.template.RCompletionProcessor;

public class RRefPhaseListen extends RBaseListener {
	ParseTreeProperty<Scope> scopes;
	RGlobalScope globals;
	Scope currentScope; // resolve symbols starting in this scope
	private CommonTokenStream tokens;
	private Parser parser;

	public RRefPhaseListen(CommonTokenStream tokens,RGlobalScope globals, ParseTreeProperty<Scope> scopes,Parser parser) {
		this.scopes = scopes;
		this.globals = globals;
		this.tokens=tokens;
		this.parser=parser;
	}

	public void enterProg(RParser.ProgContext ctx) {
		currentScope = globals;

	}

	public void enterE30(RParser.E30Context ctx) {

	}

	/* Variable call! */
	public void exitE30(RParser.E30Context ctx) {
        /*Token tok=ctx.ID().getSymbol();
		String varName = tok.getText();
		System.out.println(ctx.getText());
		//Token lastToken = tokens.get(sourceInterval.b);
		
		
		RSymbol var = currentScope.resolve(varName);
		if (var instanceof RFunctionSymbol) {
			return;
			//System.out.println("Var: " + name + " is not available!");
		}
		if (var == null) {
			//System.out.println("Var: " + name + " is not available!");
			parser.notifyErrorListeners(tok, "Warn16:Variable not available?: " + varName + " seems to be missing!", null);

		}*/
		

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
		//Get the last token which should be the name of the called function!
		Token stop = ctx.expr().getStop();
		//Token lastToken = tokens.get(sourceInterval.b);
		
		String funcName = stop.getText();
		 /*IEditorPart editor = (IEditorPart) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		    if(editor instanceof REditor){
		   REditor edit=(REditor) editor;
		RCompletionProcessor processor = edit.getRconf().getProcessor();*/
		if(CalculateRProposals.stat!=null){
		if (funcName!=null&&CalculateRProposals.stat.containsValue(funcName)){
			return;
		}
		}
		RSymbol meth = currentScope.resolve(funcName);
		if (meth == null){
			parser.notifyErrorListeners(stop, "Warn16:Function not available?: " + funcName + " seems to be missing!", null);

			//System.out.println("Function: " + funcName + " is not available!");
		}

		if (meth instanceof RVariableSymbol) {
			// System.out.println("Function: "+funcName+" is not a function!");
			//System.out.println("Function: " + funcName + " is not available!");
			parser.notifyErrorListeners(stop, "Warn16:Function not available?: " + funcName + " seems to be missing!", null);

		}

	}

	public void exitE20CallFunction(RParser.E20CallFunctionContext ctx) {

	}

}
