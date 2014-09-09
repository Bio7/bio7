// Generated from R.g4 by ANTLR 4.4
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
	 * Visit a parse tree produced by the {@code e31}
	 * labeled alternative in {@link RParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitE31(@NotNull RParser.E31Context ctx);
	/**
	 * Visit a parse tree produced by the {@code e30}
	 * labeled alternative in {@link RParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitE30(@NotNull RParser.E30Context ctx);
	/**
	 * Visit a parse tree produced by the {@code e33}
	 * labeled alternative in {@link RParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitE33(@NotNull RParser.E33Context ctx);
	/**
	 * Visit a parse tree produced by the {@code e32}
	 * labeled alternative in {@link RParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitE32(@NotNull RParser.E32Context ctx);
	/**
	 * Visit a parse tree produced by the {@code e35}
	 * labeled alternative in {@link RParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitE35(@NotNull RParser.E35Context ctx);
	/**
	 * Visit a parse tree produced by the {@code e34}
	 * labeled alternative in {@link RParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitE34(@NotNull RParser.E34Context ctx);
	/**
	 * Visit a parse tree produced by the {@code e37}
	 * labeled alternative in {@link RParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitE37(@NotNull RParser.E37Context ctx);
	/**
	 * Visit a parse tree produced by the {@code e36}
	 * labeled alternative in {@link RParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitE36(@NotNull RParser.E36Context ctx);
	/**
	 * Visit a parse tree produced by the {@code e39}
	 * labeled alternative in {@link RParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitE39(@NotNull RParser.E39Context ctx);
	/**
	 * Visit a parse tree produced by the {@code e38}
	 * labeled alternative in {@link RParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitE38(@NotNull RParser.E38Context ctx);
	/**
	 * Visit a parse tree produced by the {@code e20expr}
	 * labeled alternative in {@link RParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitE20expr(@NotNull RParser.E20exprContext ctx);
	/**
	 * Visit a parse tree produced by {@link RParser#formlist}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFormlist(@NotNull RParser.FormlistContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ExprError}
	 * labeled alternative in {@link RParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExprError(@NotNull RParser.ExprErrorContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ExprError2}
	 * labeled alternative in {@link RParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExprError2(@NotNull RParser.ExprError2Context ctx);
	/**
	 * Visit a parse tree produced by {@link RParser#sub}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSub(@NotNull RParser.SubContext ctx);
	/**
	 * Visit a parse tree produced by the {@code e11}
	 * labeled alternative in {@link RParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitE11(@NotNull RParser.E11Context ctx);
	/**
	 * Visit a parse tree produced by the {@code e10}
	 * labeled alternative in {@link RParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitE10(@NotNull RParser.E10Context ctx);
	/**
	 * Visit a parse tree produced by the {@code e13}
	 * labeled alternative in {@link RParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitE13(@NotNull RParser.E13Context ctx);
	/**
	 * Visit a parse tree produced by the {@code e12}
	 * labeled alternative in {@link RParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitE12(@NotNull RParser.E12Context ctx);
	/**
	 * Visit a parse tree produced by the {@code e15}
	 * labeled alternative in {@link RParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitE15(@NotNull RParser.E15Context ctx);
	/**
	 * Visit a parse tree produced by the {@code e14}
	 * labeled alternative in {@link RParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitE14(@NotNull RParser.E14Context ctx);
	/**
	 * Visit a parse tree produced by {@link RParser#exprlist}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExprlist(@NotNull RParser.ExprlistContext ctx);
	/**
	 * Visit a parse tree produced by the {@code e16}
	 * labeled alternative in {@link RParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitE16(@NotNull RParser.E16Context ctx);
	/**
	 * Visit a parse tree produced by the {@code e19}
	 * labeled alternative in {@link RParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitE19(@NotNull RParser.E19Context ctx);
	/**
	 * Visit a parse tree produced by the {@code VariableDeclaration}
	 * labeled alternative in {@link RParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariableDeclaration(@NotNull RParser.VariableDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by the {@code CallFunction}
	 * labeled alternative in {@link RParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCallFunction(@NotNull RParser.CallFunctionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ClosingRightBraceError}
	 * labeled alternative in {@link RParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClosingRightBraceError(@NotNull RParser.ClosingRightBraceErrorContext ctx);
	/**
	 * Visit a parse tree produced by the {@code e22}
	 * labeled alternative in {@link RParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitE22(@NotNull RParser.E22Context ctx);
	/**
	 * Visit a parse tree produced by the {@code e21}
	 * labeled alternative in {@link RParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitE21(@NotNull RParser.E21Context ctx);
	/**
	 * Visit a parse tree produced by the {@code e24}
	 * labeled alternative in {@link RParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitE24(@NotNull RParser.E24Context ctx);
	/**
	 * Visit a parse tree produced by the {@code e23}
	 * labeled alternative in {@link RParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitE23(@NotNull RParser.E23Context ctx);
	/**
	 * Visit a parse tree produced by the {@code e26}
	 * labeled alternative in {@link RParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitE26(@NotNull RParser.E26Context ctx);
	/**
	 * Visit a parse tree produced by the {@code e25}
	 * labeled alternative in {@link RParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitE25(@NotNull RParser.E25Context ctx);
	/**
	 * Visit a parse tree produced by the {@code e28}
	 * labeled alternative in {@link RParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitE28(@NotNull RParser.E28Context ctx);
	/**
	 * Visit a parse tree produced by the {@code e27}
	 * labeled alternative in {@link RParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitE27(@NotNull RParser.E27Context ctx);
	/**
	 * Visit a parse tree produced by the {@code e1}
	 * labeled alternative in {@link RParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitE1(@NotNull RParser.E1Context ctx);
	/**
	 * Visit a parse tree produced by the {@code e29}
	 * labeled alternative in {@link RParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitE29(@NotNull RParser.E29Context ctx);
	/**
	 * Visit a parse tree produced by the {@code e2}
	 * labeled alternative in {@link RParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitE2(@NotNull RParser.E2Context ctx);
	/**
	 * Visit a parse tree produced by {@link RParser#prog}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProg(@NotNull RParser.ProgContext ctx);
	/**
	 * Visit a parse tree produced by the {@code e3}
	 * labeled alternative in {@link RParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitE3(@NotNull RParser.E3Context ctx);
	/**
	 * Visit a parse tree produced by the {@code e4}
	 * labeled alternative in {@link RParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitE4(@NotNull RParser.E4Context ctx);
	/**
	 * Visit a parse tree produced by the {@code e5}
	 * labeled alternative in {@link RParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitE5(@NotNull RParser.E5Context ctx);
	/**
	 * Visit a parse tree produced by the {@code e6}
	 * labeled alternative in {@link RParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitE6(@NotNull RParser.E6Context ctx);
	/**
	 * Visit a parse tree produced by the {@code e7}
	 * labeled alternative in {@link RParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitE7(@NotNull RParser.E7Context ctx);
	/**
	 * Visit a parse tree produced by the {@code e8}
	 * labeled alternative in {@link RParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitE8(@NotNull RParser.E8Context ctx);
	/**
	 * Visit a parse tree produced by the {@code e9}
	 * labeled alternative in {@link RParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitE9(@NotNull RParser.E9Context ctx);
	/**
	 * Visit a parse tree produced by {@link RParser#form}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForm(@NotNull RParser.FormContext ctx);
	/**
	 * Visit a parse tree produced by {@link RParser#sublist}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSublist(@NotNull RParser.SublistContext ctx);
	/**
	 * Visit a parse tree produced by the {@code DefFunction}
	 * labeled alternative in {@link RParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDefFunction(@NotNull RParser.DefFunctionContext ctx);
}