// Generated from R.g4 by ANTLR 4.4
package com.eco.bio7.reditor.antlr;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class RParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.4", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__53=1, T__52=2, T__51=3, T__50=4, T__49=5, T__48=6, T__47=7, T__46=8, 
		T__45=9, T__44=10, T__43=11, T__42=12, T__41=13, T__40=14, T__39=15, T__38=16, 
		T__37=17, T__36=18, T__35=19, T__34=20, T__33=21, T__32=22, T__31=23, 
		T__30=24, T__29=25, T__28=26, T__27=27, T__26=28, T__25=29, T__24=30, 
		T__23=31, T__22=32, T__21=33, T__20=34, T__19=35, T__18=36, T__17=37, 
		T__16=38, T__15=39, T__14=40, T__13=41, T__12=42, T__11=43, T__10=44, 
		T__9=45, T__8=46, T__7=47, T__6=48, T__5=49, T__4=50, T__3=51, T__2=52, 
		T__1=53, T__0=54, HEX=55, INT=56, FLOAT=57, COMPLEX=58, STRING=59, ID=60, 
		USER_OP=61, NL=62, WS=63;
	public static final String[] tokenNames = {
		"<INVALID>", "'->>'", "'!='", "'while'", "'{'", "'&&'", "'::'", "'='", 
		"'for'", "'^'", "'$'", "'('", "'Inf'", "','", "'repeat'", "'NA'", "'<-'", 
		"'FALSE'", "':::'", "'>='", "'[['", "'<'", "']'", "'~'", "'@'", "'function'", 
		"'NULL'", "'+'", "'TRUE'", "'/'", "'||'", "';'", "'}'", "'if'", "'?'", 
		"':='", "'<='", "'break'", "'&'", "'*'", "'->'", "'...'", "'NaN'", "':'", 
		"'['", "'|'", "'=='", "'>'", "'!'", "'in'", "'else'", "'next'", "')'", 
		"'-'", "'<<-'", "HEX", "INT", "FLOAT", "COMPLEX", "STRING", "ID", "USER_OP", 
		"NL", "WS"
	};
	public static final int
		RULE_prog = 0, RULE_expr = 1, RULE_exprlist = 2, RULE_formlist = 3, RULE_form = 4, 
		RULE_sublist = 5, RULE_sub = 6;
	public static final String[] ruleNames = {
		"prog", "expr", "exprlist", "formlist", "form", "sublist", "sub"
	};

	@Override
	public String getGrammarFileName() { return "R.g4"; }

	@Override
	public String[] getTokenNames() { return tokenNames; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public RParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}
	public static class ProgContext extends ParserRuleContext {
		public List<TerminalNode> NL() { return getTokens(RParser.NL); }
		public TerminalNode EOF(int i) {
			return getToken(RParser.EOF, i);
		}
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public List<TerminalNode> EOF() { return getTokens(RParser.EOF); }
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public TerminalNode NL(int i) {
			return getToken(RParser.NL, i);
		}
		public ProgContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_prog; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).enterProg(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).exitProg(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RVisitor ) return ((RVisitor<? extends T>)visitor).visitProg(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ProgContext prog() throws RecognitionException {
		ProgContext _localctx = new ProgContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_prog);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(20);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__51) | (1L << T__50) | (1L << T__46) | (1L << T__43) | (1L << T__42) | (1L << T__40) | (1L << T__39) | (1L << T__37) | (1L << T__31) | (1L << T__29) | (1L << T__28) | (1L << T__27) | (1L << T__26) | (1L << T__21) | (1L << T__20) | (1L << T__17) | (1L << T__12) | (1L << T__6) | (1L << T__3) | (1L << T__1) | (1L << HEX) | (1L << INT) | (1L << FLOAT) | (1L << COMPLEX) | (1L << STRING) | (1L << ID) | (1L << NL))) != 0)) {
				{
				setState(18);
				switch (_input.LA(1)) {
				case T__51:
				case T__50:
				case T__46:
				case T__43:
				case T__42:
				case T__40:
				case T__39:
				case T__37:
				case T__31:
				case T__29:
				case T__28:
				case T__27:
				case T__26:
				case T__21:
				case T__20:
				case T__17:
				case T__12:
				case T__6:
				case T__3:
				case T__1:
				case HEX:
				case INT:
				case FLOAT:
				case COMPLEX:
				case STRING:
				case ID:
					{
					setState(14); expr(0);
					setState(15);
					_la = _input.LA(1);
					if ( !(((((_la - -1)) & ~0x3f) == 0 && ((1L << (_la - -1)) & ((1L << (EOF - -1)) | (1L << (T__23 - -1)) | (1L << (NL - -1)))) != 0)) ) {
					_errHandler.recoverInline(this);
					}
					consume();
					}
					break;
				case NL:
					{
					setState(17); match(NL);
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				}
				setState(22);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(23); match(EOF);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ExprContext extends ParserRuleContext {
		public ExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_expr; }
	 
		public ExprContext() { }
		public void copyFrom(ExprContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class E19Error1Context extends ExprContext {
		public ExprlistContext exprlist() {
			return getRuleContext(ExprlistContext.class,0);
		}
		public E19Error1Context(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).enterE19Error1(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).exitE19Error1(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RVisitor ) return ((RVisitor<? extends T>)visitor).visitE19Error1(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class E19Error2Context extends ExprContext {
		public ExprlistContext exprlist() {
			return getRuleContext(ExprlistContext.class,0);
		}
		public E19Error2Context(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).enterE19Error2(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).exitE19Error2(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RVisitor ) return ((RVisitor<? extends T>)visitor).visitE19Error2(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class E31Context extends ExprContext {
		public TerminalNode HEX() { return getToken(RParser.HEX, 0); }
		public E31Context(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).enterE31(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).exitE31(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RVisitor ) return ((RVisitor<? extends T>)visitor).visitE31(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class E30Context extends ExprContext {
		public TerminalNode STRING() { return getToken(RParser.STRING, 0); }
		public E30Context(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).enterE30(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).exitE30(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RVisitor ) return ((RVisitor<? extends T>)visitor).visitE30(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class E33Context extends ExprContext {
		public TerminalNode COMPLEX() { return getToken(RParser.COMPLEX, 0); }
		public E33Context(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).enterE33(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).exitE33(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RVisitor ) return ((RVisitor<? extends T>)visitor).visitE33(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class E11Context extends ExprContext {
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public E11Context(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).enterE11(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).exitE11(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RVisitor ) return ((RVisitor<? extends T>)visitor).visitE11(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class E32Context extends ExprContext {
		public TerminalNode FLOAT() { return getToken(RParser.FLOAT, 0); }
		public E32Context(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).enterE32(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).exitE32(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RVisitor ) return ((RVisitor<? extends T>)visitor).visitE32(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class E10Context extends ExprContext {
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public E10Context(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).enterE10(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).exitE10(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RVisitor ) return ((RVisitor<? extends T>)visitor).visitE10(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class E35Context extends ExprContext {
		public E35Context(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).enterE35(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).exitE35(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RVisitor ) return ((RVisitor<? extends T>)visitor).visitE35(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class E13Context extends ExprContext {
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public E13Context(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).enterE13(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).exitE13(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RVisitor ) return ((RVisitor<? extends T>)visitor).visitE13(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class E12Context extends ExprContext {
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public E12Context(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).enterE12(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).exitE12(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RVisitor ) return ((RVisitor<? extends T>)visitor).visitE12(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class E34Context extends ExprContext {
		public E34Context(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).enterE34(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).exitE34(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RVisitor ) return ((RVisitor<? extends T>)visitor).visitE34(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class E15Context extends ExprContext {
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public E15Context(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).enterE15(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).exitE15(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RVisitor ) return ((RVisitor<? extends T>)visitor).visitE15(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class E37Context extends ExprContext {
		public E37Context(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).enterE37(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).exitE37(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RVisitor ) return ((RVisitor<? extends T>)visitor).visitE37(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class E36Context extends ExprContext {
		public E36Context(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).enterE36(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).exitE36(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RVisitor ) return ((RVisitor<? extends T>)visitor).visitE36(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class E14Context extends ExprContext {
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public E14Context(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).enterE14(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).exitE14(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RVisitor ) return ((RVisitor<? extends T>)visitor).visitE14(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class E39Context extends ExprContext {
		public E39Context(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).enterE39(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).exitE39(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RVisitor ) return ((RVisitor<? extends T>)visitor).visitE39(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class E38Context extends ExprContext {
		public E38Context(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).enterE38(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).exitE38(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RVisitor ) return ((RVisitor<? extends T>)visitor).visitE38(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class E16Context extends ExprContext {
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public E16Context(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).enterE16(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).exitE16(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RVisitor ) return ((RVisitor<? extends T>)visitor).visitE16(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class E19Context extends ExprContext {
		public ExprlistContext exprlist() {
			return getRuleContext(ExprlistContext.class,0);
		}
		public E19Context(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).enterE19(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).exitE19(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RVisitor ) return ((RVisitor<? extends T>)visitor).visitE19(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class E20Error1Context extends ExprContext {
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public E20Error1Context(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).enterE20Error1(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).exitE20Error1(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RVisitor ) return ((RVisitor<? extends T>)visitor).visitE20Error1(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class CallFunctionError2Context extends ExprContext {
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public SublistContext sublist() {
			return getRuleContext(SublistContext.class,0);
		}
		public CallFunctionError2Context(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).enterCallFunctionError2(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).exitCallFunctionError2(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RVisitor ) return ((RVisitor<? extends T>)visitor).visitCallFunctionError2(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class VariableDeclarationContext extends ExprContext {
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public VariableDeclarationContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).enterVariableDeclaration(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).exitVariableDeclaration(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RVisitor ) return ((RVisitor<? extends T>)visitor).visitVariableDeclaration(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class CallFunctionContext extends ExprContext {
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public SublistContext sublist() {
			return getRuleContext(SublistContext.class,0);
		}
		public CallFunctionContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).enterCallFunction(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).exitCallFunction(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RVisitor ) return ((RVisitor<? extends T>)visitor).visitCallFunction(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class E20exprContext extends ExprContext {
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public E20exprContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).enterE20expr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).exitE20expr(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RVisitor ) return ((RVisitor<? extends T>)visitor).visitE20expr(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class E22Context extends ExprContext {
		public TerminalNode ID() { return getToken(RParser.ID, 0); }
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public E22Context(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).enterE22(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).exitE22(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RVisitor ) return ((RVisitor<? extends T>)visitor).visitE22(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class E21Context extends ExprContext {
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public E21Context(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).enterE21(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).exitE21(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RVisitor ) return ((RVisitor<? extends T>)visitor).visitE21(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class E28Error1Context extends ExprContext {
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public SublistContext sublist() {
			return getRuleContext(SublistContext.class,0);
		}
		public E28Error1Context(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).enterE28Error1(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).exitE28Error1(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RVisitor ) return ((RVisitor<? extends T>)visitor).visitE28Error1(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class E24Context extends ExprContext {
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public E24Context(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).enterE24(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).exitE24(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RVisitor ) return ((RVisitor<? extends T>)visitor).visitE24(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class E23Context extends ExprContext {
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public E23Context(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).enterE23(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).exitE23(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RVisitor ) return ((RVisitor<? extends T>)visitor).visitE23(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class E26Context extends ExprContext {
		public E26Context(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).enterE26(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).exitE26(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RVisitor ) return ((RVisitor<? extends T>)visitor).visitE26(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class E25Context extends ExprContext {
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public E25Context(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).enterE25(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).exitE25(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RVisitor ) return ((RVisitor<? extends T>)visitor).visitE25(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class CallFunctionError1Context extends ExprContext {
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public SublistContext sublist() {
			return getRuleContext(SublistContext.class,0);
		}
		public CallFunctionError1Context(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).enterCallFunctionError1(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).exitCallFunctionError1(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RVisitor ) return ((RVisitor<? extends T>)visitor).visitCallFunctionError1(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class E28Context extends ExprContext {
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public E28Context(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).enterE28(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).exitE28(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RVisitor ) return ((RVisitor<? extends T>)visitor).visitE28(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class E27Context extends ExprContext {
		public E27Context(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).enterE27(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).exitE27(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RVisitor ) return ((RVisitor<? extends T>)visitor).visitE27(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class E1Context extends ExprContext {
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public SublistContext sublist() {
			return getRuleContext(SublistContext.class,0);
		}
		public E1Context(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).enterE1(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).exitE1(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RVisitor ) return ((RVisitor<? extends T>)visitor).visitE1(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class E29Context extends ExprContext {
		public TerminalNode ID() { return getToken(RParser.ID, 0); }
		public E29Context(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).enterE29(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).exitE29(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RVisitor ) return ((RVisitor<? extends T>)visitor).visitE29(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class E2Context extends ExprContext {
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public SublistContext sublist() {
			return getRuleContext(SublistContext.class,0);
		}
		public TerminalNode INT() { return getToken(RParser.INT, 0); }
		public E2Context(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).enterE2(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).exitE2(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RVisitor ) return ((RVisitor<? extends T>)visitor).visitE2(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class DefFunctionError1Context extends ExprContext {
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public FormlistContext formlist() {
			return getRuleContext(FormlistContext.class,0);
		}
		public DefFunctionError1Context(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).enterDefFunctionError1(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).exitDefFunctionError1(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RVisitor ) return ((RVisitor<? extends T>)visitor).visitDefFunctionError1(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class E3Context extends ExprContext {
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public E3Context(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).enterE3(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).exitE3(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RVisitor ) return ((RVisitor<? extends T>)visitor).visitE3(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class DefFunctionError2Context extends ExprContext {
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public FormlistContext formlist() {
			return getRuleContext(FormlistContext.class,0);
		}
		public DefFunctionError2Context(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).enterDefFunctionError2(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).exitDefFunctionError2(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RVisitor ) return ((RVisitor<? extends T>)visitor).visitDefFunctionError2(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class E4Context extends ExprContext {
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public E4Context(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).enterE4(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).exitE4(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RVisitor ) return ((RVisitor<? extends T>)visitor).visitE4(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class E5Context extends ExprContext {
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public E5Context(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).enterE5(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).exitE5(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RVisitor ) return ((RVisitor<? extends T>)visitor).visitE5(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class E6Context extends ExprContext {
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public E6Context(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).enterE6(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).exitE6(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RVisitor ) return ((RVisitor<? extends T>)visitor).visitE6(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class E7Context extends ExprContext {
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public E7Context(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).enterE7(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).exitE7(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RVisitor ) return ((RVisitor<? extends T>)visitor).visitE7(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class E8Context extends ExprContext {
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public TerminalNode USER_OP() { return getToken(RParser.USER_OP, 0); }
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public E8Context(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).enterE8(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).exitE8(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RVisitor ) return ((RVisitor<? extends T>)visitor).visitE8(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class E9Context extends ExprContext {
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public E9Context(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).enterE9(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).exitE9(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RVisitor ) return ((RVisitor<? extends T>)visitor).visitE9(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class DefFunctionContext extends ExprContext {
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public FormlistContext formlist() {
			return getRuleContext(FormlistContext.class,0);
		}
		public DefFunctionContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).enterDefFunction(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).exitDefFunction(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RVisitor ) return ((RVisitor<? extends T>)visitor).visitDefFunction(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExprContext expr() throws RecognitionException {
		return expr(0);
	}

	private ExprContext expr(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		ExprContext _localctx = new ExprContext(_ctx, _parentState);
		ExprContext _prevctx = _localctx;
		int _startState = 2;
		enterRecursionRule(_localctx, 2, RULE_expr, _p);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(141);
			switch ( getInterpreter().adaptivePredict(_input,5,_ctx) ) {
			case 1:
				{
				_localctx = new E6Context(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;

				setState(26);
				_la = _input.LA(1);
				if ( !(_la==T__27 || _la==T__1) ) {
				_errHandler.recoverInline(this);
				}
				consume();
				setState(27); expr(47);
				}
				break;
			case 2:
				{
				_localctx = new E12Context(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(28); match(T__6);
				setState(29); expr(41);
				}
				break;
			case 3:
				{
				_localctx = new E15Context(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(30); match(T__31);
				setState(31); expr(38);
				}
				break;
			case 4:
				{
				_localctx = new DefFunctionContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(32); match(T__29);
				setState(33); match(T__43);
				setState(35);
				_la = _input.LA(1);
				if (_la==T__13 || _la==ID) {
					{
					setState(34); formlist();
					}
				}

				setState(37); match(T__2);
				setState(38); expr(34);
				}
				break;
			case 5:
				{
				_localctx = new E24Context(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(39); match(T__40);
				setState(40); expr(28);
				}
				break;
			case 6:
				{
				_localctx = new E25Context(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(41); match(T__20);
				setState(42); expr(27);
				}
				break;
			case 7:
				{
				_localctx = new DefFunctionError1Context(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(43); match(T__29);
				setState(44); match(T__43);
				setState(46);
				switch ( getInterpreter().adaptivePredict(_input,3,_ctx) ) {
				case 1:
					{
					setState(45); formlist();
					}
					break;
				}
				setState(48); expr(9);
				notifyErrorListeners("Missing closing parentheses in function definition!");
				}
				break;
			case 8:
				{
				_localctx = new DefFunctionError2Context(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(51); match(T__29);
				setState(52); match(T__43);
				setState(54);
				_la = _input.LA(1);
				if (_la==T__13 || _la==ID) {
					{
					setState(53); formlist();
					}
				}

				setState(56); match(T__2);
				setState(57); match(T__2);
				setState(58); expr(8);
				notifyErrorListeners("Too many parentheses in function definition!");
				}
				break;
			case 9:
				{
				_localctx = new E19Context(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(61); match(T__50);
				setState(62); exprlist();
				setState(63); match(T__22);
				}
				break;
			case 10:
				{
				_localctx = new E20exprContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(65); match(T__21);
				setState(66); match(T__43);
				setState(67); expr(0);
				setState(68); match(T__2);
				setState(69); expr(0);
				}
				break;
			case 11:
				{
				_localctx = new E21Context(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(71); match(T__21);
				setState(72); match(T__43);
				setState(73); expr(0);
				setState(74); match(T__2);
				setState(75); expr(0);
				setState(76); match(T__4);
				setState(77); expr(0);
				}
				break;
			case 12:
				{
				_localctx = new E22Context(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(79); match(T__46);
				setState(80); match(T__43);
				setState(81); match(ID);
				setState(82); match(T__5);
				setState(83); expr(0);
				setState(84); match(T__2);
				setState(85); expr(0);
				}
				break;
			case 13:
				{
				_localctx = new E23Context(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(87); match(T__51);
				setState(88); match(T__43);
				setState(89); expr(0);
				setState(90); match(T__2);
				setState(91); expr(0);
				}
				break;
			case 14:
				{
				_localctx = new E26Context(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(93); match(T__3);
				}
				break;
			case 15:
				{
				_localctx = new E27Context(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(94); match(T__17);
				}
				break;
			case 16:
				{
				_localctx = new E28Context(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(95); match(T__43);
				setState(96); expr(0);
				setState(97); match(T__2);
				}
				break;
			case 17:
				{
				_localctx = new E29Context(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(99); match(ID);
				}
				break;
			case 18:
				{
				_localctx = new E30Context(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(100); match(STRING);
				}
				break;
			case 19:
				{
				_localctx = new E31Context(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(101); match(HEX);
				}
				break;
			case 20:
				{
				_localctx = new E2Context(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(102); match(INT);
				}
				break;
			case 21:
				{
				_localctx = new E32Context(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(103); match(FLOAT);
				}
				break;
			case 22:
				{
				_localctx = new E33Context(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(104); match(COMPLEX);
				}
				break;
			case 23:
				{
				_localctx = new E34Context(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(105); match(T__28);
				}
				break;
			case 24:
				{
				_localctx = new E35Context(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(106); match(T__39);
				}
				break;
			case 25:
				{
				_localctx = new E36Context(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(107); match(T__42);
				}
				break;
			case 26:
				{
				_localctx = new E37Context(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(108); match(T__12);
				}
				break;
			case 27:
				{
				_localctx = new E38Context(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(109); match(T__26);
				}
				break;
			case 28:
				{
				_localctx = new E39Context(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(110); match(T__37);
				}
				break;
			case 29:
				{
				_localctx = new E20Error1Context(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(111); match(T__21);
				setState(112); match(T__43);
				setState(113); expr(0);
				setState(114); expr(0);
				notifyErrorListeners("Missing closing parentheses in if condition!!");
				}
				break;
			case 30:
				{
				_localctx = new E20Error1Context(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(117); match(T__21);
				setState(118); match(T__43);
				setState(119); expr(0);
				setState(120); match(T__2);
				setState(121); match(T__2);
				setState(122); expr(0);
				notifyErrorListeners("Too many parentheses in if condition!");
				}
				break;
			case 31:
				{
				_localctx = new E28Error1Context(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(125); match(T__43);
				setState(126); expr(0);
				setState(127); match(T__2);
				setState(128); match(T__2);
				notifyErrorListeners("Too many parentheses!");
				}
				break;
			case 32:
				{
				_localctx = new E19Error1Context(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(131); match(T__50);
				setState(132); exprlist();
				notifyErrorListeners("Missing closing braces!");
				}
				break;
			case 33:
				{
				_localctx = new E19Error2Context(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(135); match(T__50);
				setState(136); exprlist();
				setState(137); match(T__22);
				setState(138); match(T__22);
				notifyErrorListeners("Too many braces!");
				}
				break;
			}
			_ctx.stop = _input.LT(-1);
			setState(221);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,7,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(219);
					switch ( getInterpreter().adaptivePredict(_input,6,_ctx) ) {
					case 1:
						{
						_localctx = new E3Context(new ExprContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(143);
						if (!(precpred(_ctx, 50))) throw new FailedPredicateException(this, "precpred(_ctx, 50)");
						setState(144);
						_la = _input.LA(1);
						if ( !(_la==T__48 || _la==T__36) ) {
						_errHandler.recoverInline(this);
						}
						consume();
						setState(145); expr(51);
						}
						break;
					case 2:
						{
						_localctx = new E4Context(new ExprContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(146);
						if (!(precpred(_ctx, 49))) throw new FailedPredicateException(this, "precpred(_ctx, 49)");
						setState(147);
						_la = _input.LA(1);
						if ( !(_la==T__44 || _la==T__30) ) {
						_errHandler.recoverInline(this);
						}
						consume();
						setState(148); expr(50);
						}
						break;
					case 3:
						{
						_localctx = new E5Context(new ExprContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(149);
						if (!(precpred(_ctx, 48))) throw new FailedPredicateException(this, "precpred(_ctx, 48)");
						setState(150); match(T__45);
						setState(151); expr(49);
						}
						break;
					case 4:
						{
						_localctx = new E7Context(new ExprContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(152);
						if (!(precpred(_ctx, 46))) throw new FailedPredicateException(this, "precpred(_ctx, 46)");
						setState(153); match(T__11);
						setState(154); expr(47);
						}
						break;
					case 5:
						{
						_localctx = new E8Context(new ExprContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(155);
						if (!(precpred(_ctx, 45))) throw new FailedPredicateException(this, "precpred(_ctx, 45)");
						setState(156); match(USER_OP);
						setState(157); expr(46);
						}
						break;
					case 6:
						{
						_localctx = new E9Context(new ExprContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(158);
						if (!(precpred(_ctx, 44))) throw new FailedPredicateException(this, "precpred(_ctx, 44)");
						setState(159);
						_la = _input.LA(1);
						if ( !(_la==T__25 || _la==T__15) ) {
						_errHandler.recoverInline(this);
						}
						consume();
						setState(160); expr(45);
						}
						break;
					case 7:
						{
						_localctx = new E10Context(new ExprContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(161);
						if (!(precpred(_ctx, 43))) throw new FailedPredicateException(this, "precpred(_ctx, 43)");
						setState(162);
						_la = _input.LA(1);
						if ( !(_la==T__27 || _la==T__1) ) {
						_errHandler.recoverInline(this);
						}
						consume();
						setState(163); expr(44);
						}
						break;
					case 8:
						{
						_localctx = new E11Context(new ExprContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(164);
						if (!(precpred(_ctx, 42))) throw new FailedPredicateException(this, "precpred(_ctx, 42)");
						setState(165);
						_la = _input.LA(1);
						if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__52) | (1L << T__35) | (1L << T__33) | (1L << T__18) | (1L << T__8) | (1L << T__7))) != 0)) ) {
						_errHandler.recoverInline(this);
						}
						consume();
						setState(166); expr(43);
						}
						break;
					case 9:
						{
						_localctx = new E13Context(new ExprContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(167);
						if (!(precpred(_ctx, 40))) throw new FailedPredicateException(this, "precpred(_ctx, 40)");
						setState(168);
						_la = _input.LA(1);
						if ( !(_la==T__49 || _la==T__16) ) {
						_errHandler.recoverInline(this);
						}
						consume();
						setState(169); expr(41);
						}
						break;
					case 10:
						{
						_localctx = new E14Context(new ExprContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(170);
						if (!(precpred(_ctx, 39))) throw new FailedPredicateException(this, "precpred(_ctx, 39)");
						setState(171);
						_la = _input.LA(1);
						if ( !(_la==T__24 || _la==T__9) ) {
						_errHandler.recoverInline(this);
						}
						consume();
						setState(172); expr(40);
						}
						break;
					case 11:
						{
						_localctx = new E16Context(new ExprContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(173);
						if (!(precpred(_ctx, 37))) throw new FailedPredicateException(this, "precpred(_ctx, 37)");
						setState(174); match(T__31);
						setState(175); expr(38);
						}
						break;
					case 12:
						{
						_localctx = new VariableDeclarationContext(new ExprContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(176);
						if (!(precpred(_ctx, 36))) throw new FailedPredicateException(this, "precpred(_ctx, 36)");
						setState(177);
						_la = _input.LA(1);
						if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__53) | (1L << T__47) | (1L << T__38) | (1L << T__19) | (1L << T__14) | (1L << T__0))) != 0)) ) {
						_errHandler.recoverInline(this);
						}
						consume();
						setState(178); expr(37);
						}
						break;
					case 13:
						{
						_localctx = new E1Context(new ExprContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(179);
						if (!(precpred(_ctx, 52))) throw new FailedPredicateException(this, "precpred(_ctx, 52)");
						setState(180); match(T__34);
						setState(181); sublist();
						setState(182); match(T__32);
						setState(183); match(T__32);
						}
						break;
					case 14:
						{
						_localctx = new E2Context(new ExprContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(185);
						if (!(precpred(_ctx, 51))) throw new FailedPredicateException(this, "precpred(_ctx, 51)");
						setState(186); match(T__10);
						setState(187); sublist();
						setState(188); match(T__32);
						}
						break;
					case 15:
						{
						_localctx = new CallFunctionContext(new ExprContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(190);
						if (!(precpred(_ctx, 33))) throw new FailedPredicateException(this, "precpred(_ctx, 33)");
						setState(191); match(T__43);
						setState(192); sublist();
						setState(193); match(T__2);
						}
						break;
					case 16:
						{
						_localctx = new CallFunctionError1Context(new ExprContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(195);
						if (!(precpred(_ctx, 11))) throw new FailedPredicateException(this, "precpred(_ctx, 11)");
						setState(196); match(T__43);
						setState(197); sublist();
						notifyErrorListeners("Missing closing parentheses in function call!");
						}
						break;
					case 17:
						{
						_localctx = new CallFunctionError2Context(new ExprContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(200);
						if (!(precpred(_ctx, 10))) throw new FailedPredicateException(this, "precpred(_ctx, 10)");
						setState(201); match(T__43);
						setState(202); sublist();
						setState(203); match(T__2);
						setState(204); match(T__2);
						notifyErrorListeners("Too many parentheses in function call!");
						}
						break;
					case 18:
						{
						_localctx = new E28Error1Context(new ExprContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(207);
						if (!(precpred(_ctx, 5))) throw new FailedPredicateException(this, "precpred(_ctx, 5)");
						setState(208); match(T__10);
						setState(209); sublist();
						notifyErrorListeners("Missing closing brackets!");
						}
						break;
					case 19:
						{
						_localctx = new E28Error1Context(new ExprContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(212);
						if (!(precpred(_ctx, 4))) throw new FailedPredicateException(this, "precpred(_ctx, 4)");
						setState(213); match(T__10);
						setState(214); sublist();
						setState(215); match(T__32);
						setState(216); match(T__32);
						notifyErrorListeners("Too many brackets!");
						}
						break;
					}
					} 
				}
				setState(223);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,7,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			unrollRecursionContexts(_parentctx);
		}
		return _localctx;
	}

	public static class ExprlistContext extends ParserRuleContext {
		public List<TerminalNode> NL() { return getTokens(RParser.NL); }
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public TerminalNode NL(int i) {
			return getToken(RParser.NL, i);
		}
		public ExprlistContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_exprlist; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).enterExprlist(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).exitExprlist(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RVisitor ) return ((RVisitor<? extends T>)visitor).visitExprlist(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExprlistContext exprlist() throws RecognitionException {
		ExprlistContext _localctx = new ExprlistContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_exprlist);
		int _la;
		try {
			int _alt;
			setState(235);
			switch ( getInterpreter().adaptivePredict(_input,10,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(224); expr(0);
				setState(231);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,9,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(225);
						_la = _input.LA(1);
						if ( !(_la==T__23 || _la==NL) ) {
						_errHandler.recoverInline(this);
						}
						consume();
						setState(227);
						switch ( getInterpreter().adaptivePredict(_input,8,_ctx) ) {
						case 1:
							{
							setState(226); expr(0);
							}
							break;
						}
						}
						} 
					}
					setState(233);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,9,_ctx);
				}
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class FormlistContext extends ParserRuleContext {
		public FormContext form(int i) {
			return getRuleContext(FormContext.class,i);
		}
		public List<FormContext> form() {
			return getRuleContexts(FormContext.class);
		}
		public FormlistContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_formlist; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).enterFormlist(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).exitFormlist(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RVisitor ) return ((RVisitor<? extends T>)visitor).visitFormlist(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FormlistContext formlist() throws RecognitionException {
		FormlistContext _localctx = new FormlistContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_formlist);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(237); form();
			setState(242);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__41) {
				{
				{
				setState(238); match(T__41);
				setState(239); form();
				}
				}
				setState(244);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class FormContext extends ParserRuleContext {
		public TerminalNode ID() { return getToken(RParser.ID, 0); }
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public FormContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_form; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).enterForm(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).exitForm(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RVisitor ) return ((RVisitor<? extends T>)visitor).visitForm(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FormContext form() throws RecognitionException {
		FormContext _localctx = new FormContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_form);
		try {
			setState(250);
			switch ( getInterpreter().adaptivePredict(_input,12,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(245); match(ID);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(246); match(ID);
				setState(247); match(T__47);
				setState(248); expr(0);
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(249); match(T__13);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class SublistContext extends ParserRuleContext {
		public SubContext sub(int i) {
			return getRuleContext(SubContext.class,i);
		}
		public List<SubContext> sub() {
			return getRuleContexts(SubContext.class);
		}
		public SublistContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_sublist; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).enterSublist(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).exitSublist(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RVisitor ) return ((RVisitor<? extends T>)visitor).visitSublist(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SublistContext sublist() throws RecognitionException {
		SublistContext _localctx = new SublistContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_sublist);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(252); sub();
			setState(257);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,13,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(253); match(T__41);
					setState(254); sub();
					}
					} 
				}
				setState(259);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,13,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class SubContext extends ParserRuleContext {
		public TerminalNode ID() { return getToken(RParser.ID, 0); }
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public TerminalNode STRING() { return getToken(RParser.STRING, 0); }
		public SubContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_sub; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).enterSub(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).exitSub(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RVisitor ) return ((RVisitor<? extends T>)visitor).visitSub(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SubContext sub() throws RecognitionException {
		SubContext _localctx = new SubContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_sub);
		try {
			setState(278);
			switch ( getInterpreter().adaptivePredict(_input,14,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(260); expr(0);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(261); match(ID);
				setState(262); match(T__47);
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(263); match(ID);
				setState(264); match(T__47);
				setState(265); expr(0);
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(266); match(STRING);
				setState(267); match(T__47);
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(268); match(STRING);
				setState(269); match(T__47);
				setState(270); expr(0);
				}
				break;
			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(271); match(T__28);
				setState(272); match(T__47);
				}
				break;
			case 7:
				enterOuterAlt(_localctx, 7);
				{
				setState(273); match(T__28);
				setState(274); match(T__47);
				setState(275); expr(0);
				}
				break;
			case 8:
				enterOuterAlt(_localctx, 8);
				{
				setState(276); match(T__13);
				}
				break;
			case 9:
				enterOuterAlt(_localctx, 9);
				{
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public boolean sempred(RuleContext _localctx, int ruleIndex, int predIndex) {
		switch (ruleIndex) {
		case 1: return expr_sempred((ExprContext)_localctx, predIndex);
		}
		return true;
	}
	private boolean expr_sempred(ExprContext _localctx, int predIndex) {
		switch (predIndex) {
		case 0: return precpred(_ctx, 50);
		case 1: return precpred(_ctx, 49);
		case 2: return precpred(_ctx, 48);
		case 3: return precpred(_ctx, 46);
		case 4: return precpred(_ctx, 45);
		case 5: return precpred(_ctx, 44);
		case 6: return precpred(_ctx, 43);
		case 7: return precpred(_ctx, 42);
		case 8: return precpred(_ctx, 40);
		case 9: return precpred(_ctx, 39);
		case 10: return precpred(_ctx, 37);
		case 11: return precpred(_ctx, 36);
		case 12: return precpred(_ctx, 52);
		case 13: return precpred(_ctx, 51);
		case 14: return precpred(_ctx, 33);
		case 15: return precpred(_ctx, 11);
		case 16: return precpred(_ctx, 10);
		case 17: return precpred(_ctx, 5);
		case 18: return precpred(_ctx, 4);
		}
		return true;
	}

	public static final String _serializedATN =
		"\3\u0430\ud6d1\u8206\uad2d\u4417\uaef1\u8d80\uaadd\3A\u011b\4\2\t\2\4"+
		"\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\3\2\3\2\3\2\3\2\7\2\25"+
		"\n\2\f\2\16\2\30\13\2\3\2\3\2\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3"+
		"\5\3&\n\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\5\3\61\n\3\3\3\3\3\3\3\3"+
		"\3\3\3\3\3\5\39\n\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3"+
		"\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3"+
		"\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3"+
		"\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3"+
		"\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3"+
		"\3\3\3\3\3\5\3\u0090\n\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3"+
		"\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3"+
		"\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3"+
		"\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3"+
		"\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\7\3\u00de\n\3\f\3\16"+
		"\3\u00e1\13\3\3\4\3\4\3\4\5\4\u00e6\n\4\7\4\u00e8\n\4\f\4\16\4\u00eb\13"+
		"\4\3\4\5\4\u00ee\n\4\3\5\3\5\3\5\7\5\u00f3\n\5\f\5\16\5\u00f6\13\5\3\6"+
		"\3\6\3\6\3\6\3\6\5\6\u00fd\n\6\3\7\3\7\3\7\7\7\u0102\n\7\f\7\16\7\u0105"+
		"\13\7\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b"+
		"\3\b\3\b\5\b\u0119\n\b\3\b\2\3\4\t\2\4\6\b\n\f\16\2\f\4\3!!@@\4\2\35\35"+
		"\67\67\4\2\b\b\24\24\4\2\f\f\32\32\4\2\37\37))\7\2\4\4\25\25\27\27&&\60"+
		"\61\4\2\7\7((\4\2  //\b\2\3\3\t\t\22\22%%**88\4\2!!@@\u015a\2\26\3\2\2"+
		"\2\4\u008f\3\2\2\2\6\u00ed\3\2\2\2\b\u00ef\3\2\2\2\n\u00fc\3\2\2\2\f\u00fe"+
		"\3\2\2\2\16\u0118\3\2\2\2\20\21\5\4\3\2\21\22\t\2\2\2\22\25\3\2\2\2\23"+
		"\25\7@\2\2\24\20\3\2\2\2\24\23\3\2\2\2\25\30\3\2\2\2\26\24\3\2\2\2\26"+
		"\27\3\2\2\2\27\31\3\2\2\2\30\26\3\2\2\2\31\32\7\2\2\3\32\3\3\2\2\2\33"+
		"\34\b\3\1\2\34\35\t\3\2\2\35\u0090\5\4\3\61\36\37\7\62\2\2\37\u0090\5"+
		"\4\3+ !\7\31\2\2!\u0090\5\4\3(\"#\7\33\2\2#%\7\r\2\2$&\5\b\5\2%$\3\2\2"+
		"\2%&\3\2\2\2&\'\3\2\2\2\'(\7\66\2\2(\u0090\5\4\3$)*\7\20\2\2*\u0090\5"+
		"\4\3\36+,\7$\2\2,\u0090\5\4\3\35-.\7\33\2\2.\60\7\r\2\2/\61\5\b\5\2\60"+
		"/\3\2\2\2\60\61\3\2\2\2\61\62\3\2\2\2\62\63\5\4\3\13\63\64\b\3\1\2\64"+
		"\u0090\3\2\2\2\65\66\7\33\2\2\668\7\r\2\2\679\5\b\5\28\67\3\2\2\289\3"+
		"\2\2\29:\3\2\2\2:;\7\66\2\2;<\7\66\2\2<=\5\4\3\n=>\b\3\1\2>\u0090\3\2"+
		"\2\2?@\7\6\2\2@A\5\6\4\2AB\7\"\2\2B\u0090\3\2\2\2CD\7#\2\2DE\7\r\2\2E"+
		"F\5\4\3\2FG\7\66\2\2GH\5\4\3\2H\u0090\3\2\2\2IJ\7#\2\2JK\7\r\2\2KL\5\4"+
		"\3\2LM\7\66\2\2MN\5\4\3\2NO\7\64\2\2OP\5\4\3\2P\u0090\3\2\2\2QR\7\n\2"+
		"\2RS\7\r\2\2ST\7>\2\2TU\7\63\2\2UV\5\4\3\2VW\7\66\2\2WX\5\4\3\2X\u0090"+
		"\3\2\2\2YZ\7\5\2\2Z[\7\r\2\2[\\\5\4\3\2\\]\7\66\2\2]^\5\4\3\2^\u0090\3"+
		"\2\2\2_\u0090\7\65\2\2`\u0090\7\'\2\2ab\7\r\2\2bc\5\4\3\2cd\7\66\2\2d"+
		"\u0090\3\2\2\2e\u0090\7>\2\2f\u0090\7=\2\2g\u0090\79\2\2h\u0090\7:\2\2"+
		"i\u0090\7;\2\2j\u0090\7<\2\2k\u0090\7\34\2\2l\u0090\7\21\2\2m\u0090\7"+
		"\16\2\2n\u0090\7,\2\2o\u0090\7\36\2\2p\u0090\7\23\2\2qr\7#\2\2rs\7\r\2"+
		"\2st\5\4\3\2tu\5\4\3\2uv\b\3\1\2v\u0090\3\2\2\2wx\7#\2\2xy\7\r\2\2yz\5"+
		"\4\3\2z{\7\66\2\2{|\7\66\2\2|}\5\4\3\2}~\b\3\1\2~\u0090\3\2\2\2\177\u0080"+
		"\7\r\2\2\u0080\u0081\5\4\3\2\u0081\u0082\7\66\2\2\u0082\u0083\7\66\2\2"+
		"\u0083\u0084\b\3\1\2\u0084\u0090\3\2\2\2\u0085\u0086\7\6\2\2\u0086\u0087"+
		"\5\6\4\2\u0087\u0088\b\3\1\2\u0088\u0090\3\2\2\2\u0089\u008a\7\6\2\2\u008a"+
		"\u008b\5\6\4\2\u008b\u008c\7\"\2\2\u008c\u008d\7\"\2\2\u008d\u008e\b\3"+
		"\1\2\u008e\u0090\3\2\2\2\u008f\33\3\2\2\2\u008f\36\3\2\2\2\u008f \3\2"+
		"\2\2\u008f\"\3\2\2\2\u008f)\3\2\2\2\u008f+\3\2\2\2\u008f-\3\2\2\2\u008f"+
		"\65\3\2\2\2\u008f?\3\2\2\2\u008fC\3\2\2\2\u008fI\3\2\2\2\u008fQ\3\2\2"+
		"\2\u008fY\3\2\2\2\u008f_\3\2\2\2\u008f`\3\2\2\2\u008fa\3\2\2\2\u008fe"+
		"\3\2\2\2\u008ff\3\2\2\2\u008fg\3\2\2\2\u008fh\3\2\2\2\u008fi\3\2\2\2\u008f"+
		"j\3\2\2\2\u008fk\3\2\2\2\u008fl\3\2\2\2\u008fm\3\2\2\2\u008fn\3\2\2\2"+
		"\u008fo\3\2\2\2\u008fp\3\2\2\2\u008fq\3\2\2\2\u008fw\3\2\2\2\u008f\177"+
		"\3\2\2\2\u008f\u0085\3\2\2\2\u008f\u0089\3\2\2\2\u0090\u00df\3\2\2\2\u0091"+
		"\u0092\f\64\2\2\u0092\u0093\t\4\2\2\u0093\u00de\5\4\3\65\u0094\u0095\f"+
		"\63\2\2\u0095\u0096\t\5\2\2\u0096\u00de\5\4\3\64\u0097\u0098\f\62\2\2"+
		"\u0098\u0099\7\13\2\2\u0099\u00de\5\4\3\63\u009a\u009b\f\60\2\2\u009b"+
		"\u009c\7-\2\2\u009c\u00de\5\4\3\61\u009d\u009e\f/\2\2\u009e\u009f\7?\2"+
		"\2\u009f\u00de\5\4\3\60\u00a0\u00a1\f.\2\2\u00a1\u00a2\t\6\2\2\u00a2\u00de"+
		"\5\4\3/\u00a3\u00a4\f-\2\2\u00a4\u00a5\t\3\2\2\u00a5\u00de\5\4\3.\u00a6"+
		"\u00a7\f,\2\2\u00a7\u00a8\t\7\2\2\u00a8\u00de\5\4\3-\u00a9\u00aa\f*\2"+
		"\2\u00aa\u00ab\t\b\2\2\u00ab\u00de\5\4\3+\u00ac\u00ad\f)\2\2\u00ad\u00ae"+
		"\t\t\2\2\u00ae\u00de\5\4\3*\u00af\u00b0\f\'\2\2\u00b0\u00b1\7\31\2\2\u00b1"+
		"\u00de\5\4\3(\u00b2\u00b3\f&\2\2\u00b3\u00b4\t\n\2\2\u00b4\u00de\5\4\3"+
		"\'\u00b5\u00b6\f\66\2\2\u00b6\u00b7\7\26\2\2\u00b7\u00b8\5\f\7\2\u00b8"+
		"\u00b9\7\30\2\2\u00b9\u00ba\7\30\2\2\u00ba\u00de\3\2\2\2\u00bb\u00bc\f"+
		"\65\2\2\u00bc\u00bd\7.\2\2\u00bd\u00be\5\f\7\2\u00be\u00bf\7\30\2\2\u00bf"+
		"\u00de\3\2\2\2\u00c0\u00c1\f#\2\2\u00c1\u00c2\7\r\2\2\u00c2\u00c3\5\f"+
		"\7\2\u00c3\u00c4\7\66\2\2\u00c4\u00de\3\2\2\2\u00c5\u00c6\f\r\2\2\u00c6"+
		"\u00c7\7\r\2\2\u00c7\u00c8\5\f\7\2\u00c8\u00c9\b\3\1\2\u00c9\u00de\3\2"+
		"\2\2\u00ca\u00cb\f\f\2\2\u00cb\u00cc\7\r\2\2\u00cc\u00cd\5\f\7\2\u00cd"+
		"\u00ce\7\66\2\2\u00ce\u00cf\7\66\2\2\u00cf\u00d0\b\3\1\2\u00d0\u00de\3"+
		"\2\2\2\u00d1\u00d2\f\7\2\2\u00d2\u00d3\7.\2\2\u00d3\u00d4\5\f\7\2\u00d4"+
		"\u00d5\b\3\1\2\u00d5\u00de\3\2\2\2\u00d6\u00d7\f\6\2\2\u00d7\u00d8\7."+
		"\2\2\u00d8\u00d9\5\f\7\2\u00d9\u00da\7\30\2\2\u00da\u00db\7\30\2\2\u00db"+
		"\u00dc\b\3\1\2\u00dc\u00de\3\2\2\2\u00dd\u0091\3\2\2\2\u00dd\u0094\3\2"+
		"\2\2\u00dd\u0097\3\2\2\2\u00dd\u009a\3\2\2\2\u00dd\u009d\3\2\2\2\u00dd"+
		"\u00a0\3\2\2\2\u00dd\u00a3\3\2\2\2\u00dd\u00a6\3\2\2\2\u00dd\u00a9\3\2"+
		"\2\2\u00dd\u00ac\3\2\2\2\u00dd\u00af\3\2\2\2\u00dd\u00b2\3\2\2\2\u00dd"+
		"\u00b5\3\2\2\2\u00dd\u00bb\3\2\2\2\u00dd\u00c0\3\2\2\2\u00dd\u00c5\3\2"+
		"\2\2\u00dd\u00ca\3\2\2\2\u00dd\u00d1\3\2\2\2\u00dd\u00d6\3\2\2\2\u00de"+
		"\u00e1\3\2\2\2\u00df\u00dd\3\2\2\2\u00df\u00e0\3\2\2\2\u00e0\5\3\2\2\2"+
		"\u00e1\u00df\3\2\2\2\u00e2\u00e9\5\4\3\2\u00e3\u00e5\t\13\2\2\u00e4\u00e6"+
		"\5\4\3\2\u00e5\u00e4\3\2\2\2\u00e5\u00e6\3\2\2\2\u00e6\u00e8\3\2\2\2\u00e7"+
		"\u00e3\3\2\2\2\u00e8\u00eb\3\2\2\2\u00e9\u00e7\3\2\2\2\u00e9\u00ea\3\2"+
		"\2\2\u00ea\u00ee\3\2\2\2\u00eb\u00e9\3\2\2\2\u00ec\u00ee\3\2\2\2\u00ed"+
		"\u00e2\3\2\2\2\u00ed\u00ec\3\2\2\2\u00ee\7\3\2\2\2\u00ef\u00f4\5\n\6\2"+
		"\u00f0\u00f1\7\17\2\2\u00f1\u00f3\5\n\6\2\u00f2\u00f0\3\2\2\2\u00f3\u00f6"+
		"\3\2\2\2\u00f4\u00f2\3\2\2\2\u00f4\u00f5\3\2\2\2\u00f5\t\3\2\2\2\u00f6"+
		"\u00f4\3\2\2\2\u00f7\u00fd\7>\2\2\u00f8\u00f9\7>\2\2\u00f9\u00fa\7\t\2"+
		"\2\u00fa\u00fd\5\4\3\2\u00fb\u00fd\7+\2\2\u00fc\u00f7\3\2\2\2\u00fc\u00f8"+
		"\3\2\2\2\u00fc\u00fb\3\2\2\2\u00fd\13\3\2\2\2\u00fe\u0103\5\16\b\2\u00ff"+
		"\u0100\7\17\2\2\u0100\u0102\5\16\b\2\u0101\u00ff\3\2\2\2\u0102\u0105\3"+
		"\2\2\2\u0103\u0101\3\2\2\2\u0103\u0104\3\2\2\2\u0104\r\3\2\2\2\u0105\u0103"+
		"\3\2\2\2\u0106\u0119\5\4\3\2\u0107\u0108\7>\2\2\u0108\u0119\7\t\2\2\u0109"+
		"\u010a\7>\2\2\u010a\u010b\7\t\2\2\u010b\u0119\5\4\3\2\u010c\u010d\7=\2"+
		"\2\u010d\u0119\7\t\2\2\u010e\u010f\7=\2\2\u010f\u0110\7\t\2\2\u0110\u0119"+
		"\5\4\3\2\u0111\u0112\7\34\2\2\u0112\u0119\7\t\2\2\u0113\u0114\7\34\2\2"+
		"\u0114\u0115\7\t\2\2\u0115\u0119\5\4\3\2\u0116\u0119\7+\2\2\u0117\u0119"+
		"\3\2\2\2\u0118\u0106\3\2\2\2\u0118\u0107\3\2\2\2\u0118\u0109\3\2\2\2\u0118"+
		"\u010c\3\2\2\2\u0118\u010e\3\2\2\2\u0118\u0111\3\2\2\2\u0118\u0113\3\2"+
		"\2\2\u0118\u0116\3\2\2\2\u0118\u0117\3\2\2\2\u0119\17\3\2\2\2\21\24\26"+
		"%\608\u008f\u00dd\u00df\u00e5\u00e9\u00ed\u00f4\u00fc\u0103\u0118";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}