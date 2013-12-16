package com.eco.bio7.reditor.antlr;

// Generated from R.g4 by ANTLR 4.1
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link RParser}.
 */
public interface RListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link RParser#sub}.
	 * @param ctx the parse tree
	 */
	void enterSub(@NotNull RParser.SubContext ctx);
	/**
	 * Exit a parse tree produced by {@link RParser#sub}.
	 * @param ctx the parse tree
	 */
	void exitSub(@NotNull RParser.SubContext ctx);

	/**
	 * Enter a parse tree produced by {@link RParser#prog}.
	 * @param ctx the parse tree
	 */
	void enterProg(@NotNull RParser.ProgContext ctx);
	/**
	 * Exit a parse tree produced by {@link RParser#prog}.
	 * @param ctx the parse tree
	 */
	void exitProg(@NotNull RParser.ProgContext ctx);

	/**
	 * Enter a parse tree produced by {@link RParser#form}.
	 * @param ctx the parse tree
	 */
	void enterForm(@NotNull RParser.FormContext ctx);
	/**
	 * Exit a parse tree produced by {@link RParser#form}.
	 * @param ctx the parse tree
	 */
	void exitForm(@NotNull RParser.FormContext ctx);

	/**
	 * Enter a parse tree produced by {@link RParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterExpr(@NotNull RParser.ExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link RParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitExpr(@NotNull RParser.ExprContext ctx);

	/**
	 * Enter a parse tree produced by {@link RParser#sublist}.
	 * @param ctx the parse tree
	 */
	void enterSublist(@NotNull RParser.SublistContext ctx);
	/**
	 * Exit a parse tree produced by {@link RParser#sublist}.
	 * @param ctx the parse tree
	 */
	void exitSublist(@NotNull RParser.SublistContext ctx);

	/**
	 * Enter a parse tree produced by {@link RParser#formlist}.
	 * @param ctx the parse tree
	 */
	void enterFormlist(@NotNull RParser.FormlistContext ctx);
	/**
	 * Exit a parse tree produced by {@link RParser#formlist}.
	 * @param ctx the parse tree
	 */
	void exitFormlist(@NotNull RParser.FormlistContext ctx);

	/**
	 * Enter a parse tree produced by {@link RParser#exprlist}.
	 * @param ctx the parse tree
	 */
	void enterExprlist(@NotNull RParser.ExprlistContext ctx);
	/**
	 * Exit a parse tree produced by {@link RParser#exprlist}.
	 * @param ctx the parse tree
	 */
	void exitExprlist(@NotNull RParser.ExprlistContext ctx);
}