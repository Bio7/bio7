package com.eco.bio7.reditor.antlr.ref;

import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import com.eco.bio7.reditor.antlr.RBaseListener;
import com.eco.bio7.reditor.antlr.RParser;

public class RRefPhaseListen extends RBaseListener {
	ParseTreeProperty<Scope> scopes;
	RGlobalScope globals;
	Scope currentScope; // resolve symbols starting in this scope
	private CommonTokenStream tokens;

	public RRefPhaseListen(CommonTokenStream tokens,RGlobalScope globals, ParseTreeProperty<Scope> scopes) {
		this.scopes = scopes;
		this.globals = globals;
		this.tokens=tokens;
	}

	public void enterProg(RParser.ProgContext ctx) {
		currentScope = globals;

	}

	public void enterE30(RParser.E30Context ctx) {

	}

	/* Variable call! */
	public void exitE30(RParser.E30Context ctx) {

		/*String name = ctx.ID().getSymbol().getText();

		RSymbol var = currentScope.resolve(name);
		
		if (var == null) {
			System.out.println("Var: " + name + " is not available!");
		}
		if (var instanceof FunctionSymbol) {
			//System.out.println("Var: " + name + " is not available!");
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
		/*Get the last token which should be the name of the called function!*/
		Token stop = ctx.expr().getStop();
		//Token lastToken = tokens.get(sourceInterval.b);
		
		String funcName = stop.getText();
		RSymbol meth = currentScope.resolve(funcName);
		if (meth == null)
			System.out.println("Function: " + funcName + " is not available!");

		if (meth instanceof RVariableSymbol) {
			// System.out.println("Function: "+funcName+" is not a function!");

		}

	}

	public void exitE20CallFunction(RParser.E20CallFunctionContext ctx) {

	}

}
