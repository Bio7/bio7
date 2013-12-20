// Generated from C:\Users\elk\git\bio7new\com.eco.bio7.redit\src\com\eco\bio7\reditor\antlr\R.g4 by ANTLR 4.1
package com.eco.bio7.reditor.antlr;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link RParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface RVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link RParser#sub}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSub(@NotNull RParser.SubContext ctx);

	/**
	 * Visit a parse tree produced by {@link RParser#prog}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProg(@NotNull RParser.ProgContext ctx);

	/**
	 * Visit a parse tree produced by {@link RParser#form}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForm(@NotNull RParser.FormContext ctx);

	/**
	 * Visit a parse tree produced by {@link RParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpr(@NotNull RParser.ExprContext ctx);

	/**
	 * Visit a parse tree produced by {@link RParser#sublist}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSublist(@NotNull RParser.SublistContext ctx);

	/**
	 * Visit a parse tree produced by {@link RParser#formlist}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFormlist(@NotNull RParser.FormlistContext ctx);

	/**
	 * Visit a parse tree produced by {@link RParser#exprlist}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExprlist(@NotNull RParser.ExprlistContext ctx);
}