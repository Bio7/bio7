package com.eco.bio7.reditor.antlr;

import java.util.ArrayList;

import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.misc.NotNull;

public class RBaseListen extends RBaseListener {

	private CommonTokenStream tokens;
	public ArrayList<String> startStop = new ArrayList<String>();

	public RBaseListen(CommonTokenStream tokens) {
		this.tokens = tokens;
	}

	@Override
	public void enterDefFunction(@NotNull RParser.DefFunctionContext ctx) {

		Interval sourceInterval = ctx.getSourceInterval();
		Token firstToken = tokens.get(sourceInterval.a);
		int lineStart = firstToken.getStartIndex();
		// String ct=ctx.getText();

		// System.out.println("function start at line:"+lineStart);
		Token lastToken = tokens.get(sourceInterval.b);
		int lineEnd = lastToken.getStopIndex() - lineStart;
		// String ct2=ctx.getText();

		// System.out.println("function end at line:"+lineEnd);
		startStop.add(lineStart + "," + lineEnd);

	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * The default implementation does nothing.
	 */
	@Override
	public void exitDefFunction(@NotNull RParser.DefFunctionContext ctx) {

	}

}