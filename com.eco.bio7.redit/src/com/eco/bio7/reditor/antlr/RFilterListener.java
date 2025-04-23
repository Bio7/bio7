// Generated from RFilter.g4 by ANTLR 4.13.1
package com.eco.bio7.reditor.antlr;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link RFilter}.
 */
public interface RFilterListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link RFilter#stream}.
	 * @param ctx the parse tree
	 */
	void enterStream(RFilter.StreamContext ctx);
	/**
	 * Exit a parse tree produced by {@link RFilter#stream}.
	 * @param ctx the parse tree
	 */
	void exitStream(RFilter.StreamContext ctx);
	/**
	 * Enter a parse tree produced by {@link RFilter#eat}.
	 * @param ctx the parse tree
	 */
	void enterEat(RFilter.EatContext ctx);
	/**
	 * Exit a parse tree produced by {@link RFilter#eat}.
	 * @param ctx the parse tree
	 */
	void exitEat(RFilter.EatContext ctx);
	/**
	 * Enter a parse tree produced by {@link RFilter#elem}.
	 * @param ctx the parse tree
	 */
	void enterElem(RFilter.ElemContext ctx);
	/**
	 * Exit a parse tree produced by {@link RFilter#elem}.
	 * @param ctx the parse tree
	 */
	void exitElem(RFilter.ElemContext ctx);
	/**
	 * Enter a parse tree produced by {@link RFilter#atom}.
	 * @param ctx the parse tree
	 */
	void enterAtom(RFilter.AtomContext ctx);
	/**
	 * Exit a parse tree produced by {@link RFilter#atom}.
	 * @param ctx the parse tree
	 */
	void exitAtom(RFilter.AtomContext ctx);
	/**
	 * Enter a parse tree produced by {@link RFilter#op}.
	 * @param ctx the parse tree
	 */
	void enterOp(RFilter.OpContext ctx);
	/**
	 * Exit a parse tree produced by {@link RFilter#op}.
	 * @param ctx the parse tree
	 */
	void exitOp(RFilter.OpContext ctx);
}