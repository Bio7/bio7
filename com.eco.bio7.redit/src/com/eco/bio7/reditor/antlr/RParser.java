package com.eco.bio7.reditor.antlr;

// Generated from R.g4 by ANTLR 4.1
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
		"<INVALID>", "'&'", "'['", "'*'", "'<'", "'!='", "'<='", "'<<-'", "'next'", 
		"'TRUE'", "'}'", "'[['", "'->'", "'->>'", "')'", "'NaN'", "'::'", "'@'", 
		"'='", "'repeat'", "'NA'", "'|'", "'!'", "']'", "'in'", "','", "'while'", 
		"'-'", "'('", "':'", "'if'", "'?'", "'{'", "'...'", "'break'", "'else'", 
		"'<-'", "'$'", "':::'", "'FALSE'", "'^'", "'NULL'", "'function'", "'+'", 
		"'for'", "';'", "'&&'", "'||'", "'>'", "':='", "'/'", "'=='", "'~'", "'Inf'", 
		"'>='", "HEX", "INT", "FLOAT", "COMPLEX", "STRING", "ID", "USER_OP", "NL", 
		"WS"
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
	public ATN getATN() { return _ATN; }

	public RParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}
	public static class ProgContext extends ParserRuleContext {
		public List<TerminalNode> NL() { return getTokens(RParser.NL); }
		public TerminalNode EOF() { return getToken(RParser.EOF, 0); }
		public TerminalNode NL(int i) {
			return getToken(RParser.NL, i);
		}
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
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
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << 8) | (1L << 9) | (1L << 15) | (1L << 19) | (1L << 20) | (1L << 22) | (1L << 26) | (1L << 27) | (1L << 28) | (1L << 30) | (1L << 31) | (1L << 32) | (1L << 34) | (1L << 39) | (1L << 41) | (1L << 42) | (1L << 43) | (1L << 44) | (1L << 52) | (1L << 53) | (1L << HEX) | (1L << INT) | (1L << FLOAT) | (1L << COMPLEX) | (1L << STRING) | (1L << ID) | (1L << NL))) != 0)) {
				{
				setState(18);
				switch (_input.LA(1)) {
				case 8:
				case 9:
				case 15:
				case 19:
				case 20:
				case 22:
				case 26:
				case 27:
				case 28:
				case 30:
				case 31:
				case 32:
				case 34:
				case 39:
				case 41:
				case 42:
				case 43:
				case 44:
				case 52:
				case 53:
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
					if ( !(_la==45 || _la==NL) ) {
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
		public int _p;
		public ExprlistContext exprlist() {
			return getRuleContext(ExprlistContext.class,0);
		}
		public TerminalNode FLOAT() { return getToken(RParser.FLOAT, 0); }
		public TerminalNode INT() { return getToken(RParser.INT, 0); }
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public TerminalNode ID() { return getToken(RParser.ID, 0); }
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public FormlistContext formlist() {
			return getRuleContext(FormlistContext.class,0);
		}
		public SublistContext sublist() {
			return getRuleContext(SublistContext.class,0);
		}
		public TerminalNode STRING() { return getToken(RParser.STRING, 0); }
		public TerminalNode USER_OP() { return getToken(RParser.USER_OP, 0); }
		public TerminalNode COMPLEX() { return getToken(RParser.COMPLEX, 0); }
		public TerminalNode HEX() { return getToken(RParser.HEX, 0); }
		public ExprContext(ParserRuleContext parent, int invokingState) { super(parent, invokingState); }
		public ExprContext(ParserRuleContext parent, int invokingState, int _p) {
			super(parent, invokingState);
			this._p = _p;
		}
		@Override public int getRuleIndex() { return RULE_expr; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).enterExpr(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RListener ) ((RListener)listener).exitExpr(this);
		}
	}

	public final ExprContext expr(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		ExprContext _localctx = new ExprContext(_ctx, _parentState, _p);
		ExprContext _prevctx = _localctx;
		int _startState = 2;
		enterRecursionRule(_localctx, RULE_expr);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(93);
			switch ( getInterpreter().adaptivePredict(_input,3,_ctx) ) {
			case 1:
				{
				setState(26);
				_la = _input.LA(1);
				if ( !(_la==27 || _la==43) ) {
				_errHandler.recoverInline(this);
				}
				consume();
				setState(27); expr(36);
				}
				break;

			case 2:
				{
				setState(28); match(22);
				setState(29); expr(30);
				}
				break;

			case 3:
				{
				setState(30); match(52);
				setState(31); expr(27);
				}
				break;

			case 4:
				{
				setState(32); match(42);
				setState(33); match(28);
				setState(35);
				_la = _input.LA(1);
				if (_la==33 || _la==ID) {
					{
					setState(34); formlist();
					}
				}

				setState(37); match(14);
				setState(38); expr(24);
				}
				break;

			case 5:
				{
				setState(39); match(19);
				setState(40); expr(17);
				}
				break;

			case 6:
				{
				setState(41); match(31);
				setState(42); expr(16);
				}
				break;

			case 7:
				{
				setState(43); match(32);
				setState(44); exprlist();
				setState(45); match(10);
				}
				break;

			case 8:
				{
				setState(47); match(30);
				setState(48); match(28);
				setState(49); expr(0);
				setState(50); match(14);
				setState(51); expr(0);
				}
				break;

			case 9:
				{
				setState(53); match(30);
				setState(54); match(28);
				setState(55); expr(0);
				setState(56); match(14);
				setState(57); expr(0);
				setState(58); match(35);
				setState(59); expr(0);
				}
				break;

			case 10:
				{
				setState(61); match(44);
				setState(62); match(28);
				setState(63); match(ID);
				setState(64); match(24);
				setState(65); expr(0);
				setState(66); match(14);
				setState(67); expr(0);
				}
				break;

			case 11:
				{
				setState(69); match(26);
				setState(70); match(28);
				setState(71); expr(0);
				setState(72); match(14);
				setState(73); expr(0);
				}
				break;

			case 12:
				{
				setState(75); match(8);
				}
				break;

			case 13:
				{
				setState(76); match(34);
				}
				break;

			case 14:
				{
				setState(77); match(28);
				setState(78); expr(0);
				setState(79); match(14);
				}
				break;

			case 15:
				{
				setState(81); match(ID);
				}
				break;

			case 16:
				{
				setState(82); match(STRING);
				}
				break;

			case 17:
				{
				setState(83); match(HEX);
				}
				break;

			case 18:
				{
				setState(84); match(INT);
				}
				break;

			case 19:
				{
				setState(85); match(FLOAT);
				}
				break;

			case 20:
				{
				setState(86); match(COMPLEX);
				}
				break;

			case 21:
				{
				setState(87); match(41);
				}
				break;

			case 22:
				{
				setState(88); match(20);
				}
				break;

			case 23:
				{
				setState(89); match(53);
				}
				break;

			case 24:
				{
				setState(90); match(15);
				}
				break;

			case 25:
				{
				setState(91); match(9);
				}
				break;

			case 26:
				{
				setState(92); match(39);
				}
				break;
			}
			_ctx.stop = _input.LT(-1);
			setState(149);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,5,_ctx);
			while ( _alt!=2 && _alt!=-1 ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(147);
					switch ( getInterpreter().adaptivePredict(_input,4,_ctx) ) {
					case 1:
						{
						_localctx = new ExprContext(_parentctx, _parentState, _p);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(95);
						if (!(39 >= _localctx._p)) throw new FailedPredicateException(this, "39 >= $_p");
						setState(96);
						_la = _input.LA(1);
						if ( !(_la==16 || _la==38) ) {
						_errHandler.recoverInline(this);
						}
						consume();
						setState(97); expr(40);
						}
						break;

					case 2:
						{
						_localctx = new ExprContext(_parentctx, _parentState, _p);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(98);
						if (!(38 >= _localctx._p)) throw new FailedPredicateException(this, "38 >= $_p");
						setState(99);
						_la = _input.LA(1);
						if ( !(_la==17 || _la==37) ) {
						_errHandler.recoverInline(this);
						}
						consume();
						setState(100); expr(39);
						}
						break;

					case 3:
						{
						_localctx = new ExprContext(_parentctx, _parentState, _p);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(101);
						if (!(37 >= _localctx._p)) throw new FailedPredicateException(this, "37 >= $_p");
						setState(102); match(40);
						setState(103); expr(37);
						}
						break;

					case 4:
						{
						_localctx = new ExprContext(_parentctx, _parentState, _p);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(104);
						if (!(35 >= _localctx._p)) throw new FailedPredicateException(this, "35 >= $_p");
						setState(105); match(29);
						setState(106); expr(36);
						}
						break;

					case 5:
						{
						_localctx = new ExprContext(_parentctx, _parentState, _p);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(107);
						if (!(34 >= _localctx._p)) throw new FailedPredicateException(this, "34 >= $_p");
						setState(108); match(USER_OP);
						setState(109); expr(35);
						}
						break;

					case 6:
						{
						_localctx = new ExprContext(_parentctx, _parentState, _p);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(110);
						if (!(33 >= _localctx._p)) throw new FailedPredicateException(this, "33 >= $_p");
						setState(111);
						_la = _input.LA(1);
						if ( !(_la==3 || _la==50) ) {
						_errHandler.recoverInline(this);
						}
						consume();
						setState(112); expr(34);
						}
						break;

					case 7:
						{
						_localctx = new ExprContext(_parentctx, _parentState, _p);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(113);
						if (!(32 >= _localctx._p)) throw new FailedPredicateException(this, "32 >= $_p");
						setState(114);
						_la = _input.LA(1);
						if ( !(_la==27 || _la==43) ) {
						_errHandler.recoverInline(this);
						}
						consume();
						setState(115); expr(33);
						}
						break;

					case 8:
						{
						_localctx = new ExprContext(_parentctx, _parentState, _p);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(116);
						if (!(31 >= _localctx._p)) throw new FailedPredicateException(this, "31 >= $_p");
						setState(117);
						_la = _input.LA(1);
						if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << 4) | (1L << 5) | (1L << 6) | (1L << 48) | (1L << 51) | (1L << 54))) != 0)) ) {
						_errHandler.recoverInline(this);
						}
						consume();
						setState(118); expr(32);
						}
						break;

					case 9:
						{
						_localctx = new ExprContext(_parentctx, _parentState, _p);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(119);
						if (!(29 >= _localctx._p)) throw new FailedPredicateException(this, "29 >= $_p");
						setState(120);
						_la = _input.LA(1);
						if ( !(_la==1 || _la==46) ) {
						_errHandler.recoverInline(this);
						}
						consume();
						setState(121); expr(30);
						}
						break;

					case 10:
						{
						_localctx = new ExprContext(_parentctx, _parentState, _p);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(122);
						if (!(28 >= _localctx._p)) throw new FailedPredicateException(this, "28 >= $_p");
						setState(123);
						_la = _input.LA(1);
						if ( !(_la==21 || _la==47) ) {
						_errHandler.recoverInline(this);
						}
						consume();
						setState(124); expr(29);
						}
						break;

					case 11:
						{
						_localctx = new ExprContext(_parentctx, _parentState, _p);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(125);
						if (!(26 >= _localctx._p)) throw new FailedPredicateException(this, "26 >= $_p");
						setState(126); match(52);
						setState(127); expr(27);
						}
						break;

					case 12:
						{
						_localctx = new ExprContext(_parentctx, _parentState, _p);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(128);
						if (!(25 >= _localctx._p)) throw new FailedPredicateException(this, "25 >= $_p");
						setState(129);
						_la = _input.LA(1);
						if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << 7) | (1L << 12) | (1L << 13) | (1L << 18) | (1L << 36) | (1L << 49))) != 0)) ) {
						_errHandler.recoverInline(this);
						}
						consume();
						setState(130); expr(26);
						}
						break;

					case 13:
						{
						_localctx = new ExprContext(_parentctx, _parentState, _p);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(131);
						if (!(41 >= _localctx._p)) throw new FailedPredicateException(this, "41 >= $_p");
						setState(132); match(11);
						setState(133); sublist();
						setState(134); match(23);
						setState(135); match(23);
						}
						break;

					case 14:
						{
						_localctx = new ExprContext(_parentctx, _parentState, _p);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(137);
						if (!(40 >= _localctx._p)) throw new FailedPredicateException(this, "40 >= $_p");
						setState(138); match(2);
						setState(139); sublist();
						setState(140); match(23);
						}
						break;

					case 15:
						{
						_localctx = new ExprContext(_parentctx, _parentState, _p);
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(142);
						if (!(23 >= _localctx._p)) throw new FailedPredicateException(this, "23 >= $_p");
						setState(143); match(28);
						setState(144); sublist();
						setState(145); match(14);
						}
						break;
					}
					} 
				}
				setState(151);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,5,_ctx);
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
		public TerminalNode NL(int i) {
			return getToken(RParser.NL, i);
		}
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
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
	}

	public final ExprlistContext exprlist() throws RecognitionException {
		ExprlistContext _localctx = new ExprlistContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_exprlist);
		int _la;
		try {
			setState(163);
			switch (_input.LA(1)) {
			case 8:
			case 9:
			case 15:
			case 19:
			case 20:
			case 22:
			case 26:
			case 27:
			case 28:
			case 30:
			case 31:
			case 32:
			case 34:
			case 39:
			case 41:
			case 42:
			case 43:
			case 44:
			case 52:
			case 53:
			case HEX:
			case INT:
			case FLOAT:
			case COMPLEX:
			case STRING:
			case ID:
				enterOuterAlt(_localctx, 1);
				{
				setState(152); expr(0);
				setState(159);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==45 || _la==NL) {
					{
					{
					setState(153);
					_la = _input.LA(1);
					if ( !(_la==45 || _la==NL) ) {
					_errHandler.recoverInline(this);
					}
					consume();
					setState(155);
					_la = _input.LA(1);
					if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << 8) | (1L << 9) | (1L << 15) | (1L << 19) | (1L << 20) | (1L << 22) | (1L << 26) | (1L << 27) | (1L << 28) | (1L << 30) | (1L << 31) | (1L << 32) | (1L << 34) | (1L << 39) | (1L << 41) | (1L << 42) | (1L << 43) | (1L << 44) | (1L << 52) | (1L << 53) | (1L << HEX) | (1L << INT) | (1L << FLOAT) | (1L << COMPLEX) | (1L << STRING) | (1L << ID))) != 0)) {
						{
						setState(154); expr(0);
						}
					}

					}
					}
					setState(161);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
				break;
			case 10:
				enterOuterAlt(_localctx, 2);
				{
				}
				break;
			default:
				throw new NoViableAltException(this);
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
		public List<FormContext> form() {
			return getRuleContexts(FormContext.class);
		}
		public FormContext form(int i) {
			return getRuleContext(FormContext.class,i);
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
	}

	public final FormlistContext formlist() throws RecognitionException {
		FormlistContext _localctx = new FormlistContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_formlist);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(165); form();
			setState(170);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==25) {
				{
				{
				setState(166); match(25);
				setState(167); form();
				}
				}
				setState(172);
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
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public TerminalNode ID() { return getToken(RParser.ID, 0); }
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
	}

	public final FormContext form() throws RecognitionException {
		FormContext _localctx = new FormContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_form);
		try {
			setState(178);
			switch ( getInterpreter().adaptivePredict(_input,10,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(173); match(ID);
				}
				break;

			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(174); match(ID);
				setState(175); match(18);
				setState(176); expr(0);
				}
				break;

			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(177); match(33);
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
	}

	public final SublistContext sublist() throws RecognitionException {
		SublistContext _localctx = new SublistContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_sublist);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(180); sub();
			setState(185);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==25) {
				{
				{
				setState(181); match(25);
				setState(182); sub();
				}
				}
				setState(187);
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

	public static class SubContext extends ParserRuleContext {
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public TerminalNode ID() { return getToken(RParser.ID, 0); }
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
	}

	public final SubContext sub() throws RecognitionException {
		SubContext _localctx = new SubContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_sub);
		try {
			setState(206);
			switch ( getInterpreter().adaptivePredict(_input,12,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(188); expr(0);
				}
				break;

			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(189); match(ID);
				setState(190); match(18);
				}
				break;

			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(191); match(ID);
				setState(192); match(18);
				setState(193); expr(0);
				}
				break;

			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(194); match(STRING);
				setState(195); match(18);
				}
				break;

			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(196); match(STRING);
				setState(197); match(18);
				setState(198); expr(0);
				}
				break;

			case 6:
				enterOuterAlt(_localctx, 6);
				{
				setState(199); match(41);
				setState(200); match(18);
				}
				break;

			case 7:
				enterOuterAlt(_localctx, 7);
				{
				setState(201); match(41);
				setState(202); match(18);
				setState(203); expr(0);
				}
				break;

			case 8:
				enterOuterAlt(_localctx, 8);
				{
				setState(204); match(33);
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
		case 0: return 39 >= _localctx._p;

		case 1: return 38 >= _localctx._p;

		case 2: return 37 >= _localctx._p;

		case 3: return 35 >= _localctx._p;

		case 4: return 34 >= _localctx._p;

		case 5: return 33 >= _localctx._p;

		case 6: return 32 >= _localctx._p;

		case 7: return 31 >= _localctx._p;

		case 8: return 29 >= _localctx._p;

		case 9: return 28 >= _localctx._p;

		case 10: return 26 >= _localctx._p;

		case 11: return 25 >= _localctx._p;

		case 12: return 41 >= _localctx._p;

		case 13: return 40 >= _localctx._p;

		case 14: return 23 >= _localctx._p;
		}
		return true;
	}

	public static final String _serializedATN =
		"\3\uacf5\uee8c\u4f5d\u8b0d\u4a45\u78bd\u1b2f\u3378\3A\u00d3\4\2\t\2\4"+
		"\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\3\2\3\2\3\2\3\2\7\2\25"+
		"\n\2\f\2\16\2\30\13\2\3\2\3\2\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3"+
		"\5\3&\n\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3"+
		"\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3"+
		"\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3"+
		"\3\3\3\3\3\3\3\3\3\3\3\3\5\3`\n\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3"+
		"\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3"+
		"\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3"+
		"\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\3\7\3\u0096\n\3\f\3\16\3\u0099\13\3\3\4"+
		"\3\4\3\4\5\4\u009e\n\4\7\4\u00a0\n\4\f\4\16\4\u00a3\13\4\3\4\5\4\u00a6"+
		"\n\4\3\5\3\5\3\5\7\5\u00ab\n\5\f\5\16\5\u00ae\13\5\3\6\3\6\3\6\3\6\3\6"+
		"\5\6\u00b5\n\6\3\7\3\7\3\7\7\7\u00ba\n\7\f\7\16\7\u00bd\13\7\3\b\3\b\3"+
		"\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\3\b\5\b\u00d1"+
		"\n\b\3\b\2\t\2\4\6\b\n\f\16\2\13\4\2//@@\4\2\35\35--\4\2\22\22((\4\2\23"+
		"\23\'\'\4\2\5\5\64\64\6\2\6\b\62\62\65\6588\4\2\3\3\60\60\4\2\27\27\61"+
		"\61\7\2\t\t\16\17\24\24&&\63\63\u0105\2\26\3\2\2\2\4_\3\2\2\2\6\u00a5"+
		"\3\2\2\2\b\u00a7\3\2\2\2\n\u00b4\3\2\2\2\f\u00b6\3\2\2\2\16\u00d0\3\2"+
		"\2\2\20\21\5\4\3\2\21\22\t\2\2\2\22\25\3\2\2\2\23\25\7@\2\2\24\20\3\2"+
		"\2\2\24\23\3\2\2\2\25\30\3\2\2\2\26\24\3\2\2\2\26\27\3\2\2\2\27\31\3\2"+
		"\2\2\30\26\3\2\2\2\31\32\7\2\2\3\32\3\3\2\2\2\33\34\b\3\1\2\34\35\t\3"+
		"\2\2\35`\5\4\3\2\36\37\7\30\2\2\37`\5\4\3\2 !\7\66\2\2!`\5\4\3\2\"#\7"+
		",\2\2#%\7\36\2\2$&\5\b\5\2%$\3\2\2\2%&\3\2\2\2&\'\3\2\2\2\'(\7\20\2\2"+
		"(`\5\4\3\2)*\7\25\2\2*`\5\4\3\2+,\7!\2\2,`\5\4\3\2-.\7\"\2\2./\5\6\4\2"+
		"/\60\7\f\2\2\60`\3\2\2\2\61\62\7 \2\2\62\63\7\36\2\2\63\64\5\4\3\2\64"+
		"\65\7\20\2\2\65\66\5\4\3\2\66`\3\2\2\2\678\7 \2\289\7\36\2\29:\5\4\3\2"+
		":;\7\20\2\2;<\5\4\3\2<=\7%\2\2=>\5\4\3\2>`\3\2\2\2?@\7.\2\2@A\7\36\2\2"+
		"AB\7>\2\2BC\7\32\2\2CD\5\4\3\2DE\7\20\2\2EF\5\4\3\2F`\3\2\2\2GH\7\34\2"+
		"\2HI\7\36\2\2IJ\5\4\3\2JK\7\20\2\2KL\5\4\3\2L`\3\2\2\2M`\7\n\2\2N`\7$"+
		"\2\2OP\7\36\2\2PQ\5\4\3\2QR\7\20\2\2R`\3\2\2\2S`\7>\2\2T`\7=\2\2U`\79"+
		"\2\2V`\7:\2\2W`\7;\2\2X`\7<\2\2Y`\7+\2\2Z`\7\26\2\2[`\7\67\2\2\\`\7\21"+
		"\2\2]`\7\13\2\2^`\7)\2\2_\33\3\2\2\2_\36\3\2\2\2_ \3\2\2\2_\"\3\2\2\2"+
		"_)\3\2\2\2_+\3\2\2\2_-\3\2\2\2_\61\3\2\2\2_\67\3\2\2\2_?\3\2\2\2_G\3\2"+
		"\2\2_M\3\2\2\2_N\3\2\2\2_O\3\2\2\2_S\3\2\2\2_T\3\2\2\2_U\3\2\2\2_V\3\2"+
		"\2\2_W\3\2\2\2_X\3\2\2\2_Y\3\2\2\2_Z\3\2\2\2_[\3\2\2\2_\\\3\2\2\2_]\3"+
		"\2\2\2_^\3\2\2\2`\u0097\3\2\2\2ab\6\3\2\3bc\t\4\2\2c\u0096\5\4\3\2de\6"+
		"\3\3\3ef\t\5\2\2f\u0096\5\4\3\2gh\6\3\4\3hi\7*\2\2i\u0096\5\4\3\2jk\6"+
		"\3\5\3kl\7\37\2\2l\u0096\5\4\3\2mn\6\3\6\3no\7?\2\2o\u0096\5\4\3\2pq\6"+
		"\3\7\3qr\t\6\2\2r\u0096\5\4\3\2st\6\3\b\3tu\t\3\2\2u\u0096\5\4\3\2vw\6"+
		"\3\t\3wx\t\7\2\2x\u0096\5\4\3\2yz\6\3\n\3z{\t\b\2\2{\u0096\5\4\3\2|}\6"+
		"\3\13\3}~\t\t\2\2~\u0096\5\4\3\2\177\u0080\6\3\f\3\u0080\u0081\7\66\2"+
		"\2\u0081\u0096\5\4\3\2\u0082\u0083\6\3\r\3\u0083\u0084\t\n\2\2\u0084\u0096"+
		"\5\4\3\2\u0085\u0086\6\3\16\3\u0086\u0087\7\r\2\2\u0087\u0088\5\f\7\2"+
		"\u0088\u0089\7\31\2\2\u0089\u008a\7\31\2\2\u008a\u0096\3\2\2\2\u008b\u008c"+
		"\6\3\17\3\u008c\u008d\7\4\2\2\u008d\u008e\5\f\7\2\u008e\u008f\7\31\2\2"+
		"\u008f\u0096\3\2\2\2\u0090\u0091\6\3\20\3\u0091\u0092\7\36\2\2\u0092\u0093"+
		"\5\f\7\2\u0093\u0094\7\20\2\2\u0094\u0096\3\2\2\2\u0095a\3\2\2\2\u0095"+
		"d\3\2\2\2\u0095g\3\2\2\2\u0095j\3\2\2\2\u0095m\3\2\2\2\u0095p\3\2\2\2"+
		"\u0095s\3\2\2\2\u0095v\3\2\2\2\u0095y\3\2\2\2\u0095|\3\2\2\2\u0095\177"+
		"\3\2\2\2\u0095\u0082\3\2\2\2\u0095\u0085\3\2\2\2\u0095\u008b\3\2\2\2\u0095"+
		"\u0090\3\2\2\2\u0096\u0099\3\2\2\2\u0097\u0095\3\2\2\2\u0097\u0098\3\2"+
		"\2\2\u0098\5\3\2\2\2\u0099\u0097\3\2\2\2\u009a\u00a1\5\4\3\2\u009b\u009d"+
		"\t\2\2\2\u009c\u009e\5\4\3\2\u009d\u009c\3\2\2\2\u009d\u009e\3\2\2\2\u009e"+
		"\u00a0\3\2\2\2\u009f\u009b\3\2\2\2\u00a0\u00a3\3\2\2\2\u00a1\u009f\3\2"+
		"\2\2\u00a1\u00a2\3\2\2\2\u00a2\u00a6\3\2\2\2\u00a3\u00a1\3\2\2\2\u00a4"+
		"\u00a6\3\2\2\2\u00a5\u009a\3\2\2\2\u00a5\u00a4\3\2\2\2\u00a6\7\3\2\2\2"+
		"\u00a7\u00ac\5\n\6\2\u00a8\u00a9\7\33\2\2\u00a9\u00ab\5\n\6\2\u00aa\u00a8"+
		"\3\2\2\2\u00ab\u00ae\3\2\2\2\u00ac\u00aa\3\2\2\2\u00ac\u00ad\3\2\2\2\u00ad"+
		"\t\3\2\2\2\u00ae\u00ac\3\2\2\2\u00af\u00b5\7>\2\2\u00b0\u00b1\7>\2\2\u00b1"+
		"\u00b2\7\24\2\2\u00b2\u00b5\5\4\3\2\u00b3\u00b5\7#\2\2\u00b4\u00af\3\2"+
		"\2\2\u00b4\u00b0\3\2\2\2\u00b4\u00b3\3\2\2\2\u00b5\13\3\2\2\2\u00b6\u00bb"+
		"\5\16\b\2\u00b7\u00b8\7\33\2\2\u00b8\u00ba\5\16\b\2\u00b9\u00b7\3\2\2"+
		"\2\u00ba\u00bd\3\2\2\2\u00bb\u00b9\3\2\2\2\u00bb\u00bc\3\2\2\2\u00bc\r"+
		"\3\2\2\2\u00bd\u00bb\3\2\2\2\u00be\u00d1\5\4\3\2\u00bf\u00c0\7>\2\2\u00c0"+
		"\u00d1\7\24\2\2\u00c1\u00c2\7>\2\2\u00c2\u00c3\7\24\2\2\u00c3\u00d1\5"+
		"\4\3\2\u00c4\u00c5\7=\2\2\u00c5\u00d1\7\24\2\2\u00c6\u00c7\7=\2\2\u00c7"+
		"\u00c8\7\24\2\2\u00c8\u00d1\5\4\3\2\u00c9\u00ca\7+\2\2\u00ca\u00d1\7\24"+
		"\2\2\u00cb\u00cc\7+\2\2\u00cc\u00cd\7\24\2\2\u00cd\u00d1\5\4\3\2\u00ce"+
		"\u00d1\7#\2\2\u00cf\u00d1\3\2\2\2\u00d0\u00be\3\2\2\2\u00d0\u00bf\3\2"+
		"\2\2\u00d0\u00c1\3\2\2\2\u00d0\u00c4\3\2\2\2\u00d0\u00c6\3\2\2\2\u00d0"+
		"\u00c9\3\2\2\2\u00d0\u00cb\3\2\2\2\u00d0\u00ce\3\2\2\2\u00d0\u00cf\3\2"+
		"\2\2\u00d1\17\3\2\2\2\17\24\26%_\u0095\u0097\u009d\u00a1\u00a5\u00ac\u00b4"+
		"\u00bb\u00d0";
	public static final ATN _ATN =
		ATNSimulator.deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}