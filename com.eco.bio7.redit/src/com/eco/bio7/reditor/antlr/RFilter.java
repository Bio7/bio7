// Generated from C:\Users\elk\git\bio7new\com.eco.bio7.redit\src\com\eco\bio7\reditor\antlr\RFilter.g4 by ANTLR 4.2-SNAPSHOT
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
public class RFilter extends Parser {
	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__29=25, T__28=26, T__27=27, T__26=28, T__25=29, T__24=30, T__23=31, 
		T__22=32, T__21=33, T__20=34, COMPLEX=58, FLOAT=57, ID=60, T__9=45, T__8=46, 
		T__7=47, T__6=48, T__5=49, T__4=50, T__19=35, T__51=3, T__16=38, T__52=2, 
		T__15=39, T__53=1, T__18=36, T__17=37, T__12=42, HEX=55, T__11=43, T__14=40, 
		T__13=41, T__10=44, USER_OP=61, NL=62, T__50=4, T__42=12, T__43=11, T__40=14, 
		T__41=13, T__46=8, T__47=7, T__44=10, T__45=9, T__48=6, T__49=5, INT=56, 
		T__30=24, T__31=23, T__32=22, WS=63, T__33=21, T__34=20, T__35=19, T__36=18, 
		T__37=17, T__38=16, T__39=15, T__1=53, T__0=54, T__3=51, T__2=52, STRING=59;
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
		RULE_stream = 0, RULE_eat = 1, RULE_elem = 2, RULE_atom = 3, RULE_op = 4;
	public static final String[] ruleNames = {
		"stream", "eat", "elem", "atom", "op"
	};

	@Override
	public String getGrammarFileName() { return "RFilter.g4"; }

	@Override
	public String[] getTokenNames() { return tokenNames; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }


	protected int curlies = 0;

	public RFilter(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}
	public static class StreamContext extends ParserRuleContext {
		public List<TerminalNode> NL() { return getTokens(RFilter.NL); }
		public TerminalNode EOF() { return getToken(RFilter.EOF, 0); }
		public List<ElemContext> elem() {
			return getRuleContexts(ElemContext.class);
		}
		public TerminalNode NL(int i) {
			return getToken(RFilter.NL, i);
		}
		public ElemContext elem(int i) {
			return getRuleContext(ElemContext.class,i);
		}
		public StreamContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_stream; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RFilterListener ) ((RFilterListener)listener).enterStream(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RFilterListener ) ((RFilterListener)listener).exitStream(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RFilterVisitor ) return ((RFilterVisitor<? extends T>)visitor).visitStream(this);
			else return visitor.visitChildren(this);
		}
	}

	public final StreamContext stream() throws RecognitionException {
		StreamContext _localctx = new StreamContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_stream);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(15);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << 1) | (1L << 2) | (1L << 3) | (1L << 4) | (1L << 5) | (1L << 6) | (1L << 7) | (1L << 8) | (1L << 9) | (1L << 11) | (1L << 12) | (1L << 13) | (1L << 15) | (1L << 16) | (1L << 17) | (1L << 18) | (1L << 19) | (1L << 20) | (1L << 21) | (1L << 22) | (1L << 24) | (1L << 25) | (1L << 26) | (1L << 27) | (1L << 28) | (1L << 29) | (1L << 30) | (1L << 31) | (1L << 32) | (1L << 33) | (1L << 34) | (1L << 35) | (1L << 36) | (1L << 37) | (1L << 38) | (1L << 39) | (1L << 40) | (1L << 41) | (1L << 42) | (1L << 43) | (1L << 44) | (1L << 45) | (1L << 46) | (1L << 47) | (1L << 48) | (1L << 50) | (1L << 51) | (1L << 52) | (1L << 53) | (1L << 54) | (1L << HEX) | (1L << INT) | (1L << FLOAT) | (1L << COMPLEX) | (1L << STRING) | (1L << ID) | (1L << USER_OP) | (1L << NL))) != 0)) {
				{
				setState(13);
				switch ( getInterpreter().adaptivePredict(_input,0,_ctx) ) {
				case 1:
					{
					setState(10); elem();
					}
					break;

				case 2:
					{
					setState(11); match(NL);
					}
					break;

				case 3:
					{
					setState(12); match(45);
					}
					break;
				}
				}
				setState(17);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(18); match(EOF);
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

	public static class EatContext extends ParserRuleContext {
		public Token NL;
		public List<TerminalNode> NL() { return getTokens(RFilter.NL); }
		public TerminalNode NL(int i) {
			return getToken(RFilter.NL, i);
		}
		public EatContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_eat; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RFilterListener ) ((RFilterListener)listener).enterEat(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RFilterListener ) ((RFilterListener)listener).exitEat(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RFilterVisitor ) return ((RFilterVisitor<? extends T>)visitor).visitEat(this);
			else return visitor.visitChildren(this);
		}
	}

	public final EatContext eat() throws RecognitionException {
		EatContext _localctx = new EatContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_eat);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(22); 
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,2,_ctx);
			do {
				switch (_alt) {
				case 1:
					{
					{
					setState(20); ((EatContext)_localctx).NL = match(NL);
					((WritableToken)((EatContext)_localctx).NL).setChannel(Token.HIDDEN_CHANNEL);
					}
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(24); 
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,2,_ctx);
			} while ( _alt!=2 && _alt!=-1 );
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

	public static class ElemContext extends ParserRuleContext {
		public List<TerminalNode> NL() { return getTokens(RFilter.NL); }
		public List<ElemContext> elem() {
			return getRuleContexts(ElemContext.class);
		}
		public TerminalNode NL(int i) {
			return getToken(RFilter.NL, i);
		}
		public List<EatContext> eat() {
			return getRuleContexts(EatContext.class);
		}
		public OpContext op() {
			return getRuleContext(OpContext.class,0);
		}
		public ElemContext elem(int i) {
			return getRuleContext(ElemContext.class,i);
		}
		public EatContext eat(int i) {
			return getRuleContext(EatContext.class,i);
		}
		public AtomContext atom() {
			return getRuleContext(AtomContext.class,0);
		}
		public ElemContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_elem; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RFilterListener ) ((RFilterListener)listener).enterElem(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RFilterListener ) ((RFilterListener)listener).exitElem(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RFilterVisitor ) return ((RFilterVisitor<? extends T>)visitor).visitElem(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ElemContext elem() throws RecognitionException {
		ElemContext _localctx = new ElemContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_elem);
		int _la;
		try {
			setState(146);
			switch (_input.LA(1)) {
			case 1:
			case 3:
			case 4:
			case 5:
			case 6:
			case 7:
			case 12:
			case 13:
			case 16:
			case 17:
			case 18:
			case 19:
			case 21:
			case 22:
			case 24:
			case 25:
			case 27:
			case 29:
			case 31:
			case 33:
			case 36:
			case 37:
			case 38:
			case 40:
			case 43:
			case 46:
			case 47:
			case 48:
			case 50:
			case 51:
			case 52:
			case 54:
			case USER_OP:
				enterOuterAlt(_localctx, 1);
				{
				setState(26); op();
				setState(28);
				switch ( getInterpreter().adaptivePredict(_input,3,_ctx) ) {
				case 1:
					{
					setState(27); eat();
					}
					break;
				}
				}
				break;
			case 8:
			case 9:
			case 15:
			case 20:
			case 34:
			case 39:
			case 41:
			case 53:
			case HEX:
			case INT:
			case FLOAT:
			case COMPLEX:
			case STRING:
			case ID:
				enterOuterAlt(_localctx, 2);
				{
				setState(30); atom();
				}
				break;
			case 32:
				enterOuterAlt(_localctx, 3);
				{
				setState(31); match(32);
				setState(33);
				switch ( getInterpreter().adaptivePredict(_input,4,_ctx) ) {
				case 1:
					{
					setState(32); eat();
					}
					break;
				}
				curlies++;
				setState(41);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << 1) | (1L << 2) | (1L << 3) | (1L << 4) | (1L << 5) | (1L << 6) | (1L << 7) | (1L << 8) | (1L << 9) | (1L << 11) | (1L << 12) | (1L << 13) | (1L << 15) | (1L << 16) | (1L << 17) | (1L << 18) | (1L << 19) | (1L << 20) | (1L << 21) | (1L << 22) | (1L << 24) | (1L << 25) | (1L << 26) | (1L << 27) | (1L << 28) | (1L << 29) | (1L << 30) | (1L << 31) | (1L << 32) | (1L << 33) | (1L << 34) | (1L << 35) | (1L << 36) | (1L << 37) | (1L << 38) | (1L << 39) | (1L << 40) | (1L << 41) | (1L << 42) | (1L << 43) | (1L << 44) | (1L << 45) | (1L << 46) | (1L << 47) | (1L << 48) | (1L << 50) | (1L << 51) | (1L << 52) | (1L << 53) | (1L << 54) | (1L << HEX) | (1L << INT) | (1L << FLOAT) | (1L << COMPLEX) | (1L << STRING) | (1L << ID) | (1L << USER_OP) | (1L << NL))) != 0)) {
					{
					setState(39);
					switch ( getInterpreter().adaptivePredict(_input,5,_ctx) ) {
					case 1:
						{
						setState(36); elem();
						}
						break;

					case 2:
						{
						setState(37); match(NL);
						}
						break;

					case 3:
						{
						setState(38); match(45);
						}
						break;
					}
					}
					setState(43);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				curlies--;
				setState(45); match(10);
				}
				break;
			case 28:
				enterOuterAlt(_localctx, 4);
				{
				setState(46); match(28);
				setState(51);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << 1) | (1L << 2) | (1L << 3) | (1L << 4) | (1L << 5) | (1L << 6) | (1L << 7) | (1L << 8) | (1L << 9) | (1L << 11) | (1L << 12) | (1L << 13) | (1L << 15) | (1L << 16) | (1L << 17) | (1L << 18) | (1L << 19) | (1L << 20) | (1L << 21) | (1L << 22) | (1L << 24) | (1L << 25) | (1L << 26) | (1L << 27) | (1L << 28) | (1L << 29) | (1L << 30) | (1L << 31) | (1L << 32) | (1L << 33) | (1L << 34) | (1L << 35) | (1L << 36) | (1L << 37) | (1L << 38) | (1L << 39) | (1L << 40) | (1L << 41) | (1L << 42) | (1L << 43) | (1L << 44) | (1L << 46) | (1L << 47) | (1L << 48) | (1L << 50) | (1L << 51) | (1L << 52) | (1L << 53) | (1L << 54) | (1L << HEX) | (1L << INT) | (1L << FLOAT) | (1L << COMPLEX) | (1L << STRING) | (1L << ID) | (1L << USER_OP) | (1L << NL))) != 0)) {
					{
					setState(49);
					switch ( getInterpreter().adaptivePredict(_input,7,_ctx) ) {
					case 1:
						{
						setState(47); elem();
						}
						break;

					case 2:
						{
						setState(48); eat();
						}
						break;
					}
					}
					setState(53);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(54); match(14);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 5);
				{
				setState(55); match(2);
				setState(60);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << 1) | (1L << 2) | (1L << 3) | (1L << 4) | (1L << 5) | (1L << 6) | (1L << 7) | (1L << 8) | (1L << 9) | (1L << 11) | (1L << 12) | (1L << 13) | (1L << 15) | (1L << 16) | (1L << 17) | (1L << 18) | (1L << 19) | (1L << 20) | (1L << 21) | (1L << 22) | (1L << 24) | (1L << 25) | (1L << 26) | (1L << 27) | (1L << 28) | (1L << 29) | (1L << 30) | (1L << 31) | (1L << 32) | (1L << 33) | (1L << 34) | (1L << 35) | (1L << 36) | (1L << 37) | (1L << 38) | (1L << 39) | (1L << 40) | (1L << 41) | (1L << 42) | (1L << 43) | (1L << 44) | (1L << 46) | (1L << 47) | (1L << 48) | (1L << 50) | (1L << 51) | (1L << 52) | (1L << 53) | (1L << 54) | (1L << HEX) | (1L << INT) | (1L << FLOAT) | (1L << COMPLEX) | (1L << STRING) | (1L << ID) | (1L << USER_OP) | (1L << NL))) != 0)) {
					{
					setState(58);
					switch ( getInterpreter().adaptivePredict(_input,9,_ctx) ) {
					case 1:
						{
						setState(56); elem();
						}
						break;

					case 2:
						{
						setState(57); eat();
						}
						break;
					}
					}
					setState(62);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(63); match(23);
				}
				break;
			case 11:
				enterOuterAlt(_localctx, 6);
				{
				setState(64); match(11);
				setState(69);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << 1) | (1L << 2) | (1L << 3) | (1L << 4) | (1L << 5) | (1L << 6) | (1L << 7) | (1L << 8) | (1L << 9) | (1L << 11) | (1L << 12) | (1L << 13) | (1L << 15) | (1L << 16) | (1L << 17) | (1L << 18) | (1L << 19) | (1L << 20) | (1L << 21) | (1L << 22) | (1L << 24) | (1L << 25) | (1L << 26) | (1L << 27) | (1L << 28) | (1L << 29) | (1L << 30) | (1L << 31) | (1L << 32) | (1L << 33) | (1L << 34) | (1L << 35) | (1L << 36) | (1L << 37) | (1L << 38) | (1L << 39) | (1L << 40) | (1L << 41) | (1L << 42) | (1L << 43) | (1L << 44) | (1L << 46) | (1L << 47) | (1L << 48) | (1L << 50) | (1L << 51) | (1L << 52) | (1L << 53) | (1L << 54) | (1L << HEX) | (1L << INT) | (1L << FLOAT) | (1L << COMPLEX) | (1L << STRING) | (1L << ID) | (1L << USER_OP) | (1L << NL))) != 0)) {
					{
					setState(67);
					switch ( getInterpreter().adaptivePredict(_input,11,_ctx) ) {
					case 1:
						{
						setState(65); elem();
						}
						break;

					case 2:
						{
						setState(66); eat();
						}
						break;
					}
					}
					setState(71);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(72); match(23);
				setState(73); match(23);
				}
				break;
			case 42:
				enterOuterAlt(_localctx, 7);
				{
				setState(74); match(42);
				setState(76);
				_la = _input.LA(1);
				if (_la==NL) {
					{
					setState(75); eat();
					}
				}

				setState(78); match(28);
				setState(83);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << 1) | (1L << 2) | (1L << 3) | (1L << 4) | (1L << 5) | (1L << 6) | (1L << 7) | (1L << 8) | (1L << 9) | (1L << 11) | (1L << 12) | (1L << 13) | (1L << 15) | (1L << 16) | (1L << 17) | (1L << 18) | (1L << 19) | (1L << 20) | (1L << 21) | (1L << 22) | (1L << 24) | (1L << 25) | (1L << 26) | (1L << 27) | (1L << 28) | (1L << 29) | (1L << 30) | (1L << 31) | (1L << 32) | (1L << 33) | (1L << 34) | (1L << 35) | (1L << 36) | (1L << 37) | (1L << 38) | (1L << 39) | (1L << 40) | (1L << 41) | (1L << 42) | (1L << 43) | (1L << 44) | (1L << 46) | (1L << 47) | (1L << 48) | (1L << 50) | (1L << 51) | (1L << 52) | (1L << 53) | (1L << 54) | (1L << HEX) | (1L << INT) | (1L << FLOAT) | (1L << COMPLEX) | (1L << STRING) | (1L << ID) | (1L << USER_OP) | (1L << NL))) != 0)) {
					{
					setState(81);
					switch ( getInterpreter().adaptivePredict(_input,14,_ctx) ) {
					case 1:
						{
						setState(79); elem();
						}
						break;

					case 2:
						{
						setState(80); eat();
						}
						break;
					}
					}
					setState(85);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(86); match(14);
				setState(88);
				switch ( getInterpreter().adaptivePredict(_input,16,_ctx) ) {
				case 1:
					{
					setState(87); eat();
					}
					break;
				}
				}
				break;
			case 44:
				enterOuterAlt(_localctx, 8);
				{
				setState(90); match(44);
				setState(92);
				_la = _input.LA(1);
				if (_la==NL) {
					{
					setState(91); eat();
					}
				}

				setState(94); match(28);
				setState(99);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << 1) | (1L << 2) | (1L << 3) | (1L << 4) | (1L << 5) | (1L << 6) | (1L << 7) | (1L << 8) | (1L << 9) | (1L << 11) | (1L << 12) | (1L << 13) | (1L << 15) | (1L << 16) | (1L << 17) | (1L << 18) | (1L << 19) | (1L << 20) | (1L << 21) | (1L << 22) | (1L << 24) | (1L << 25) | (1L << 26) | (1L << 27) | (1L << 28) | (1L << 29) | (1L << 30) | (1L << 31) | (1L << 32) | (1L << 33) | (1L << 34) | (1L << 35) | (1L << 36) | (1L << 37) | (1L << 38) | (1L << 39) | (1L << 40) | (1L << 41) | (1L << 42) | (1L << 43) | (1L << 44) | (1L << 46) | (1L << 47) | (1L << 48) | (1L << 50) | (1L << 51) | (1L << 52) | (1L << 53) | (1L << 54) | (1L << HEX) | (1L << INT) | (1L << FLOAT) | (1L << COMPLEX) | (1L << STRING) | (1L << ID) | (1L << USER_OP) | (1L << NL))) != 0)) {
					{
					setState(97);
					switch ( getInterpreter().adaptivePredict(_input,18,_ctx) ) {
					case 1:
						{
						setState(95); elem();
						}
						break;

					case 2:
						{
						setState(96); eat();
						}
						break;
					}
					}
					setState(101);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(102); match(14);
				setState(104);
				switch ( getInterpreter().adaptivePredict(_input,20,_ctx) ) {
				case 1:
					{
					setState(103); eat();
					}
					break;
				}
				}
				break;
			case 26:
				enterOuterAlt(_localctx, 9);
				{
				setState(106); match(26);
				setState(108);
				_la = _input.LA(1);
				if (_la==NL) {
					{
					setState(107); eat();
					}
				}

				setState(110); match(28);
				setState(115);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << 1) | (1L << 2) | (1L << 3) | (1L << 4) | (1L << 5) | (1L << 6) | (1L << 7) | (1L << 8) | (1L << 9) | (1L << 11) | (1L << 12) | (1L << 13) | (1L << 15) | (1L << 16) | (1L << 17) | (1L << 18) | (1L << 19) | (1L << 20) | (1L << 21) | (1L << 22) | (1L << 24) | (1L << 25) | (1L << 26) | (1L << 27) | (1L << 28) | (1L << 29) | (1L << 30) | (1L << 31) | (1L << 32) | (1L << 33) | (1L << 34) | (1L << 35) | (1L << 36) | (1L << 37) | (1L << 38) | (1L << 39) | (1L << 40) | (1L << 41) | (1L << 42) | (1L << 43) | (1L << 44) | (1L << 46) | (1L << 47) | (1L << 48) | (1L << 50) | (1L << 51) | (1L << 52) | (1L << 53) | (1L << 54) | (1L << HEX) | (1L << INT) | (1L << FLOAT) | (1L << COMPLEX) | (1L << STRING) | (1L << ID) | (1L << USER_OP) | (1L << NL))) != 0)) {
					{
					setState(113);
					switch ( getInterpreter().adaptivePredict(_input,22,_ctx) ) {
					case 1:
						{
						setState(111); elem();
						}
						break;

					case 2:
						{
						setState(112); eat();
						}
						break;
					}
					}
					setState(117);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(118); match(14);
				setState(120);
				switch ( getInterpreter().adaptivePredict(_input,24,_ctx) ) {
				case 1:
					{
					setState(119); eat();
					}
					break;
				}
				}
				break;
			case 30:
				enterOuterAlt(_localctx, 10);
				{
				setState(122); match(30);
				setState(124);
				_la = _input.LA(1);
				if (_la==NL) {
					{
					setState(123); eat();
					}
				}

				setState(126); match(28);
				setState(131);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << 1) | (1L << 2) | (1L << 3) | (1L << 4) | (1L << 5) | (1L << 6) | (1L << 7) | (1L << 8) | (1L << 9) | (1L << 11) | (1L << 12) | (1L << 13) | (1L << 15) | (1L << 16) | (1L << 17) | (1L << 18) | (1L << 19) | (1L << 20) | (1L << 21) | (1L << 22) | (1L << 24) | (1L << 25) | (1L << 26) | (1L << 27) | (1L << 28) | (1L << 29) | (1L << 30) | (1L << 31) | (1L << 32) | (1L << 33) | (1L << 34) | (1L << 35) | (1L << 36) | (1L << 37) | (1L << 38) | (1L << 39) | (1L << 40) | (1L << 41) | (1L << 42) | (1L << 43) | (1L << 44) | (1L << 46) | (1L << 47) | (1L << 48) | (1L << 50) | (1L << 51) | (1L << 52) | (1L << 53) | (1L << 54) | (1L << HEX) | (1L << INT) | (1L << FLOAT) | (1L << COMPLEX) | (1L << STRING) | (1L << ID) | (1L << USER_OP) | (1L << NL))) != 0)) {
					{
					setState(129);
					switch ( getInterpreter().adaptivePredict(_input,26,_ctx) ) {
					case 1:
						{
						setState(127); elem();
						}
						break;

					case 2:
						{
						setState(128); eat();
						}
						break;
					}
					}
					setState(133);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(134); match(14);
				setState(136);
				switch ( getInterpreter().adaptivePredict(_input,28,_ctx) ) {
				case 1:
					{
					setState(135); eat();
					}
					break;
				}
				}
				break;
			case 35:
			case NL:
				enterOuterAlt(_localctx, 11);
				{
				setState(139);
				_la = _input.LA(1);
				if (_la==NL) {
					{
					setState(138); eat();
					}
				}

				setState(141); match(35);
				setState(143);
				switch ( getInterpreter().adaptivePredict(_input,30,_ctx) ) {
				case 1:
					{
					setState(142); eat();
					}
					break;
				}

				        // ``inside a compound expression, a newline before else is discarded,
				        // whereas at the outermost level, the newline terminates the if
				        // construction and a subsequent else causes a syntax error.''
				        /*
				        Works here
				            if (1==0) { print(1) } else { print(2) }

				        and correctly gets error here:

				            if (1==0) { print(1) }
				            else { print(2) }
				 
				        this works too:
				 
				            if (1==0) {
				              if (2==0) print(1)
				              else print(2)
				            }
				        */
				        WritableToken tok = (WritableToken)_input.LT(-2);
				        if (curlies>0&&tok.getType()==NL) tok.setChannel(Token.HIDDEN_CHANNEL);
				        
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

	public static class AtomContext extends ParserRuleContext {
		public TerminalNode FLOAT() { return getToken(RFilter.FLOAT, 0); }
		public TerminalNode INT() { return getToken(RFilter.INT, 0); }
		public TerminalNode ID() { return getToken(RFilter.ID, 0); }
		public TerminalNode STRING() { return getToken(RFilter.STRING, 0); }
		public TerminalNode COMPLEX() { return getToken(RFilter.COMPLEX, 0); }
		public TerminalNode HEX() { return getToken(RFilter.HEX, 0); }
		public AtomContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_atom; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RFilterListener ) ((RFilterListener)listener).enterAtom(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RFilterListener ) ((RFilterListener)listener).exitAtom(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RFilterVisitor ) return ((RFilterVisitor<? extends T>)visitor).visitAtom(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AtomContext atom() throws RecognitionException {
		AtomContext _localctx = new AtomContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_atom);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(148);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << 8) | (1L << 9) | (1L << 15) | (1L << 20) | (1L << 34) | (1L << 39) | (1L << 41) | (1L << 53) | (1L << HEX) | (1L << INT) | (1L << FLOAT) | (1L << COMPLEX) | (1L << STRING) | (1L << ID))) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			consume();
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

	public static class OpContext extends ParserRuleContext {
		public TerminalNode USER_OP() { return getToken(RFilter.USER_OP, 0); }
		public OpContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_op; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof RFilterListener ) ((RFilterListener)listener).enterOp(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof RFilterListener ) ((RFilterListener)listener).exitOp(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof RFilterVisitor ) return ((RFilterVisitor<? extends T>)visitor).visitOp(this);
			else return visitor.visitChildren(this);
		}
	}

	public final OpContext op() throws RecognitionException {
		OpContext _localctx = new OpContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_op);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(150);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << 1) | (1L << 3) | (1L << 4) | (1L << 5) | (1L << 6) | (1L << 7) | (1L << 12) | (1L << 13) | (1L << 16) | (1L << 17) | (1L << 18) | (1L << 19) | (1L << 21) | (1L << 22) | (1L << 24) | (1L << 25) | (1L << 27) | (1L << 29) | (1L << 31) | (1L << 33) | (1L << 36) | (1L << 37) | (1L << 38) | (1L << 40) | (1L << 43) | (1L << 46) | (1L << 47) | (1L << 48) | (1L << 50) | (1L << 51) | (1L << 52) | (1L << 54) | (1L << USER_OP))) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			consume();
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

	public static final String _serializedATN =
		"\3\u0430\ud6d1\u8206\uad2d\u4417\uaef1\u8d80\uaadd\3A\u009b\4\2\t\2\4"+
		"\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\3\2\3\2\3\2\7\2\20\n\2\f\2\16\2\23\13\2"+
		"\3\2\3\2\3\3\3\3\6\3\31\n\3\r\3\16\3\32\3\4\3\4\5\4\37\n\4\3\4\3\4\3\4"+
		"\5\4$\n\4\3\4\3\4\3\4\3\4\7\4*\n\4\f\4\16\4-\13\4\3\4\3\4\3\4\3\4\3\4"+
		"\7\4\64\n\4\f\4\16\4\67\13\4\3\4\3\4\3\4\3\4\7\4=\n\4\f\4\16\4@\13\4\3"+
		"\4\3\4\3\4\3\4\7\4F\n\4\f\4\16\4I\13\4\3\4\3\4\3\4\3\4\5\4O\n\4\3\4\3"+
		"\4\3\4\7\4T\n\4\f\4\16\4W\13\4\3\4\3\4\5\4[\n\4\3\4\3\4\5\4_\n\4\3\4\3"+
		"\4\3\4\7\4d\n\4\f\4\16\4g\13\4\3\4\3\4\5\4k\n\4\3\4\3\4\5\4o\n\4\3\4\3"+
		"\4\3\4\7\4t\n\4\f\4\16\4w\13\4\3\4\3\4\5\4{\n\4\3\4\3\4\5\4\177\n\4\3"+
		"\4\3\4\3\4\7\4\u0084\n\4\f\4\16\4\u0087\13\4\3\4\3\4\5\4\u008b\n\4\3\4"+
		"\5\4\u008e\n\4\3\4\3\4\5\4\u0092\n\4\3\4\5\4\u0095\n\4\3\5\3\5\3\6\3\6"+
		"\3\6\2\2\7\2\4\6\b\n\2\4\n\2\n\13\21\21\26\26$$))++\67\679>\23\2\3\3\5"+
		"\t\16\17\22\25\27\30\32\33\35\35\37\37!!##&(**--\60\62\64\6688??\u00c0"+
		"\2\21\3\2\2\2\4\30\3\2\2\2\6\u0094\3\2\2\2\b\u0096\3\2\2\2\n\u0098\3\2"+
		"\2\2\f\20\5\6\4\2\r\20\7@\2\2\16\20\7/\2\2\17\f\3\2\2\2\17\r\3\2\2\2\17"+
		"\16\3\2\2\2\20\23\3\2\2\2\21\17\3\2\2\2\21\22\3\2\2\2\22\24\3\2\2\2\23"+
		"\21\3\2\2\2\24\25\7\2\2\3\25\3\3\2\2\2\26\27\7@\2\2\27\31\b\3\1\2\30\26"+
		"\3\2\2\2\31\32\3\2\2\2\32\30\3\2\2\2\32\33\3\2\2\2\33\5\3\2\2\2\34\36"+
		"\5\n\6\2\35\37\5\4\3\2\36\35\3\2\2\2\36\37\3\2\2\2\37\u0095\3\2\2\2 \u0095"+
		"\5\b\5\2!#\7\"\2\2\"$\5\4\3\2#\"\3\2\2\2#$\3\2\2\2$%\3\2\2\2%+\b\4\1\2"+
		"&*\5\6\4\2\'*\7@\2\2(*\7/\2\2)&\3\2\2\2)\'\3\2\2\2)(\3\2\2\2*-\3\2\2\2"+
		"+)\3\2\2\2+,\3\2\2\2,.\3\2\2\2-+\3\2\2\2./\b\4\1\2/\u0095\7\f\2\2\60\65"+
		"\7\36\2\2\61\64\5\6\4\2\62\64\5\4\3\2\63\61\3\2\2\2\63\62\3\2\2\2\64\67"+
		"\3\2\2\2\65\63\3\2\2\2\65\66\3\2\2\2\668\3\2\2\2\67\65\3\2\2\28\u0095"+
		"\7\20\2\29>\7\4\2\2:=\5\6\4\2;=\5\4\3\2<:\3\2\2\2<;\3\2\2\2=@\3\2\2\2"+
		"><\3\2\2\2>?\3\2\2\2?A\3\2\2\2@>\3\2\2\2A\u0095\7\31\2\2BG\7\r\2\2CF\5"+
		"\6\4\2DF\5\4\3\2EC\3\2\2\2ED\3\2\2\2FI\3\2\2\2GE\3\2\2\2GH\3\2\2\2HJ\3"+
		"\2\2\2IG\3\2\2\2JK\7\31\2\2K\u0095\7\31\2\2LN\7,\2\2MO\5\4\3\2NM\3\2\2"+
		"\2NO\3\2\2\2OP\3\2\2\2PU\7\36\2\2QT\5\6\4\2RT\5\4\3\2SQ\3\2\2\2SR\3\2"+
		"\2\2TW\3\2\2\2US\3\2\2\2UV\3\2\2\2VX\3\2\2\2WU\3\2\2\2XZ\7\20\2\2Y[\5"+
		"\4\3\2ZY\3\2\2\2Z[\3\2\2\2[\u0095\3\2\2\2\\^\7.\2\2]_\5\4\3\2^]\3\2\2"+
		"\2^_\3\2\2\2_`\3\2\2\2`e\7\36\2\2ad\5\6\4\2bd\5\4\3\2ca\3\2\2\2cb\3\2"+
		"\2\2dg\3\2\2\2ec\3\2\2\2ef\3\2\2\2fh\3\2\2\2ge\3\2\2\2hj\7\20\2\2ik\5"+
		"\4\3\2ji\3\2\2\2jk\3\2\2\2k\u0095\3\2\2\2ln\7\34\2\2mo\5\4\3\2nm\3\2\2"+
		"\2no\3\2\2\2op\3\2\2\2pu\7\36\2\2qt\5\6\4\2rt\5\4\3\2sq\3\2\2\2sr\3\2"+
		"\2\2tw\3\2\2\2us\3\2\2\2uv\3\2\2\2vx\3\2\2\2wu\3\2\2\2xz\7\20\2\2y{\5"+
		"\4\3\2zy\3\2\2\2z{\3\2\2\2{\u0095\3\2\2\2|~\7 \2\2}\177\5\4\3\2~}\3\2"+
		"\2\2~\177\3\2\2\2\177\u0080\3\2\2\2\u0080\u0085\7\36\2\2\u0081\u0084\5"+
		"\6\4\2\u0082\u0084\5\4\3\2\u0083\u0081\3\2\2\2\u0083\u0082\3\2\2\2\u0084"+
		"\u0087\3\2\2\2\u0085\u0083\3\2\2\2\u0085\u0086\3\2\2\2\u0086\u0088\3\2"+
		"\2\2\u0087\u0085\3\2\2\2\u0088\u008a\7\20\2\2\u0089\u008b\5\4\3\2\u008a"+
		"\u0089\3\2\2\2\u008a\u008b\3\2\2\2\u008b\u0095\3\2\2\2\u008c\u008e\5\4"+
		"\3\2\u008d\u008c\3\2\2\2\u008d\u008e\3\2\2\2\u008e\u008f\3\2\2\2\u008f"+
		"\u0091\7%\2\2\u0090\u0092\5\4\3\2\u0091\u0090\3\2\2\2\u0091\u0092\3\2"+
		"\2\2\u0092\u0093\3\2\2\2\u0093\u0095\b\4\1\2\u0094\34\3\2\2\2\u0094 \3"+
		"\2\2\2\u0094!\3\2\2\2\u0094\60\3\2\2\2\u00949\3\2\2\2\u0094B\3\2\2\2\u0094"+
		"L\3\2\2\2\u0094\\\3\2\2\2\u0094l\3\2\2\2\u0094|\3\2\2\2\u0094\u008d\3"+
		"\2\2\2\u0095\7\3\2\2\2\u0096\u0097\t\2\2\2\u0097\t\3\2\2\2\u0098\u0099"+
		"\t\3\2\2\u0099\13\3\2\2\2\"\17\21\32\36#)+\63\65<>EGNSUZ^cejnsuz~\u0083"+
		"\u0085\u008a\u008d\u0091\u0094";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}