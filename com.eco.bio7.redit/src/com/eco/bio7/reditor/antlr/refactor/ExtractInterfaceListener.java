package com.eco.bio7.reditor.antlr.refactor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNodeImpl;
import com.eco.bio7.reditor.antlr.RBaseListener;
import com.eco.bio7.reditor.antlr.RParser;
import com.eco.bio7.reditor.antlr.util.Utils;

public class ExtractInterfaceListener extends RBaseListener {
	RParser parser;
	private CommonTokenStream tokens;
	private boolean captureId = true;
	HashSet<String> capId = new HashSet<String>();
	private boolean isInMethodCall = false;
	private int offset;
	private String funcName;
	private boolean isInMatrixBracketCall;
	private boolean isInMatrixDoubleBracketCall;
	private int offsetCodeCompl = -1;
	private String bracketMatrixName;
	private int matrixArgState = 0;
	private int bracketCommaCount;
	private boolean isInPipeFunction;
	private String currentPipeData;
	private Token currentPipeToken;
	private HashMap<String, String> currentPackageImports;

	public ExtractInterfaceListener(CommonTokenStream tokens, RParser parser, boolean captureId, int offset) {
		this.parser = parser;
		this.tokens = tokens;
		this.captureId = captureId;
		this.offset = offset;
		isInMethodCall = false;
		this.offsetCodeCompl = offset;
		currentPackageImports = new HashMap<String, String>();
	}

	public Map<String, String> getCurrentPackageImports() {
		return currentPackageImports;
	}

	public boolean isInPipeFunction() {
		return isInPipeFunction;
	}

	public String getCurrentPipeData() {
		return currentPipeData;
	}

	/* Function for the R-Shell! */
	public boolean isInVarCall() {
		return isInMethodCall;
	}

	/* Function for the R-Shell! */
	public String getFuncName() {
		return funcName;
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

	/*
	 * Check for one or two arguments, if left or right argument! 1= one arg, 21 =
	 * two args, left arg, 22= two args, right site!
	 */
	public int getMatrixArgState() {
		return matrixArgState;
	}

	public int getBracketCommaCount() {
		return bracketCommaCount;
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
			/*
			 * int size = ctx.sublist().sub().size(); if (size == 2) { Get the comma
			 * position in the sublist! Token comma = ((TerminalNodeImpl)
			 * ctx.sublist().getChild(1)).getSymbol(); int commaStart =
			 * comma.getStartIndex();
			 * 
			 * if (offsetCodeCompl > commaStart) { matrixArgState = 22; //
			 * System.out.println("Second arg");
			 * 
			 * } else { matrixArgState = 21; // System.out.println("First arg"); } } else {
			 * matrixArgState = 1; // System.out.println("one arg"); }
			 */

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
	 * This is the detection of the pipe operator '|>'!
	 * In the grammar file the operator must precede the function call!
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
		if (userOp.equals("%>%")||userOp.equals("%$%")) {
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

	/** Listen to matches of classDeclaration */

	public void enterE20CallFunction(RParser.E20CallFunctionContext ctx) {
		// System.out.println(ctx.getText());
		Token stop = ctx.expr().getStop();
		Token parenth = ((TerminalNodeImpl) ctx.getChild(1)).getSymbol();
		int startIndex = parenth.getStopIndex();
		int stopIndex = ctx.getStop().getStopIndex();
		if (offset > startIndex && offset <= stopIndex) {
			isInMethodCall = true;
			/* We are not in a matrix, dataframe call! */
			isInMatrixBracketCall = false;
			isInMatrixDoubleBracketCall = false;
			funcName = stop.getText();
			currentPipeToken = stop;
		}
		if (funcName != null) {
			if (funcName.equals("library") || funcName.equals("require")) {
				if (ctx.sublist() != null) {
					String lib = ctx.sublist().getText();
					if (lib.isEmpty() == false) {

						currentPackageImports.put(lib, lib);
					}
				}
			}
		}

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
	 * With this error message we produce QuickFixes. The errors start with 'Err' to
	 * seperate them later in the RBaseListen class!
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
