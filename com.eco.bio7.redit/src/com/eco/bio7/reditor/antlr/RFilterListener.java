package com.eco.bio7.reditor.antlr;
// Generated from RFilter.g4 by ANTLR 4.1
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link RFilter}.
 */
public interface RFilterListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link RFilter#elem}.
	 * @param ctx the parse tree
	 */
	void enterElem(@NotNull RFilter.ElemContext ctx);
	/**
	 * Exit a parse tree produced by {@link RFilter#elem}.
	 * @param ctx the parse tree
	 */
	void exitElem(@NotNull RFilter.ElemContext ctx);

	/**
	 * Enter a parse tree produced by {@link RFilter#atom}.
	 * @param ctx the parse tree
	 */
	void enterAtom(@NotNull RFilter.AtomContext ctx);
	/**
	 * Exit a parse tree produced by {@link RFilter#atom}.
	 * @param ctx the parse tree
	 */
	void exitAtom(@NotNull RFilter.AtomContext ctx);

	/**
	 * Enter a parse tree produced by {@link RFilter#stream}.
	 * @param ctx the parse tree
	 */
	void enterStream(@NotNull RFilter.StreamContext ctx);
	/**
	 * Exit a parse tree produced by {@link RFilter#stream}.
	 * @param ctx the parse tree
	 */
	void exitStream(@NotNull RFilter.StreamContext ctx);

	/**
	 * Enter a parse tree produced by {@link RFilter#op}.
	 * @param ctx the parse tree
	 */
	void enterOp(@NotNull RFilter.OpContext ctx);
	/**
	 * Exit a parse tree produced by {@link RFilter#op}.
	 * @param ctx the parse tree
	 */
	void exitOp(@NotNull RFilter.OpContext ctx);

	/**
	 * Enter a parse tree produced by {@link RFilter#eat}.
	 * @param ctx the parse tree
	 */
	void enterEat(@NotNull RFilter.EatContext ctx);
	/**
	 * Exit a parse tree produced by {@link RFilter#eat}.
	 * @param ctx the parse tree
	 */
	void exitEat(@NotNull RFilter.EatContext ctx);
}