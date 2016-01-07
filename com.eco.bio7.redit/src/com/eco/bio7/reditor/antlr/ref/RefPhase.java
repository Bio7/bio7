package com.eco.bio7.reditor.antlr.ref;

import org.antlr.v4.runtime.tree.ParseTreeProperty;

import com.eco.bio7.reditor.antlr.RBaseListen;
import com.eco.bio7.reditor.antlr.RBaseListener;
import com.eco.bio7.reditor.antlr.RParser;

public class RefPhase extends RBaseListener {
    ParseTreeProperty<Scope> scopes;
    GlobalScope globals;
    Scope currentScope; // resolve symbols starting in this scope

    public RefPhase(GlobalScope globals, ParseTreeProperty<Scope> scopes) {
        this.scopes = scopes;
        this.globals = globals;
    }
    
	public void enterProg(RParser.ProgContext ctx) {
        currentScope = globals;

	}
	
	 public void enterE30(RParser.E30Context ctx) { 
		 
	 }
	/*Variable call!*/
	 public void exitE30(RParser.E30Context ctx) {
		 /*String name = ctx.ID().getSymbol().getText();
	        RSymbol var = currentScope.resolve(name);
	        if ( var==null ) {
	            System.out.println("Var: "+name+" is not available!");
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
		String funcName = ctx.start.getText();
        RSymbol meth = currentScope.resolve(funcName);
        if(meth==null)
        System.out.println("Function: "+funcName+" is not available!");
        
        if ( meth instanceof RVariableSymbol ) {
            System.out.println("Function: "+funcName+" is not a function!");

        }
        
	}
	
	public void exitE20CallFunction(RParser.E20CallFunctionContext ctx) {
		
	}
	
	

		
    
}
