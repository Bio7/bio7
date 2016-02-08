package com.eco.bio7.reditor.antlr.refactor;

import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;

import com.eco.bio7.reditor.antlr.DeclCallStore;
import com.eco.bio7.reditor.antlr.RBaseListener;
import com.eco.bio7.reditor.antlr.RParser;

public class ExtractInterfaceListener extends RBaseListener {
	RParser parser;
	private CommonTokenStream tokens;
	private boolean captureId;
	StringBuffer capId=new StringBuffer();

	public ExtractInterfaceListener(CommonTokenStream tokens, RParser parser,boolean captureId) {
		this.parser = parser;
		this.tokens=tokens;
		this.captureId=captureId;
	}

	/** Listen to matches of classDeclaration */

	public void enterE20CallFunction(RParser.E20CallFunctionContext ctx) {
		// System.out.println(ctx.getText());

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

	/*
	 * public void exitErr20(RParser.Err20Context ctx) {
	 * 
	 * Token firstToken = tokens.get(ctx.getChild(1).getSourceInterval().a);
	 * parser.notifyErrorListeners(firstToken, "Err20:Wrong comparison!", null);
	 * 
	 * }
	 * 
	 * public void exitErr21(RParser.Err21Context ctx) {
	 * 
	 * Token firstToken = tokens.get(ctx.getChild(1).getSourceInterval().a);
	 * parser.notifyErrorListeners(firstToken, "Err21:Wrong comparison!", null);
	 * 
	 * }
	 */

	public void exitErr22(RParser.Err22Context ctx) {

		Token firstToken = ctx.start;
		parser.notifyErrorListeners(firstToken, "Unknown Token!", null);

	}

	/* ID call (variables) Need to calculate position of <- */
	public void enterE30(RParser.E30Context ctx) {
		
		Token tok = ctx.ID().getSymbol();
		// System.out.println("Token Text: "+tok.getText());
		String varName = tok.getText();
		int index = tok.getTokenIndex();
		
		Token idNextToken =whitespaceTokenFilter(index,ctx.stop.getStopIndex());
		
		//Token idNextToken = tokens.get(index + 1);
		
		if (idNextToken != null) {
			if (idNextToken.getText().equals("=") || idNextToken.getText().equals("<-") || idNextToken.getText().equals("(")) {
				
				return;
			}

			else {
				if(captureId){
					capId.append(varName);
					capId.append(",");
				}

			}
		}
	}
	/*
	 * Extract the token with the assignment operator and (to exclude whitespace
	 * in stream because of the hidden() rule the whitespace is present in the
	 * CommonTokenStream!)
	 */

	private Token whitespaceTokenFilter(int start, int stop) {

		int i = start + 1;
		Token assignOp = null;
		while (i <= stop) {
			Token tok = tokens.get(i);
			if (tok.getType() != RParser.WS) {
				assignOp = tok;
				break;
			}
			if (tok.getType() == RParser.EOF) {
				break;
			}
			i++;
		}
		
		return assignOp;
	}

	@Override
	public void exitE30(RParser.E30Context ctx) {

	}

}
