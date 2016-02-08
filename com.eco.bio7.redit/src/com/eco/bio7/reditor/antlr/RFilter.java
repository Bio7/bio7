// Generated from RFilter.g4 by ANTLR 4.5.1
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
	static { RuntimeMetaData.checkVersion("4.5.1", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, T__2=3, T__3=4, T__4=5, T__5=6, T__6=7, T__7=8, T__8=9, 
		T__9=10, T__10=11, T__11=12, T__12=13, T__13=14, T__14=15, T__15=16, T__16=17, 
		T__17=18, T__18=19, T__19=20, T__20=21, T__21=22, T__22=23, T__23=24, 
		T__24=25, T__25=26, T__26=27, T__27=28, T__28=29, T__29=30, T__30=31, 
		T__31=32, T__32=33, T__33=34, T__34=35, T__35=36, T__36=37, T__37=38, 
		T__38=39, T__39=40, T__40=41, T__41=42, T__42=43, T__43=44, T__44=45, 
		T__45=46, T__46=47, T__47=48, T__48=49, T__49=50, T__50=51, T__51=52, 
		T__52=53, T__53=54, HEX=55, INT=56, FLOAT=57, COMPLEX=58, STRING=59, ID=60, 
		USER_OP=61, NL=62, WS=63, UNKNOWN=64;
	public static final int
		RULE_stream = 0, RULE_eat = 1, RULE_elem = 2, RULE_atom = 3, RULE_op = 4;
	public static final String[] ruleNames = {
		"stream", "eat", "elem", "atom", "op"
	};

	private static final String[] _LITERAL_NAMES = {
		null, "';'", "'[['", "']'", "'['", "'::'", "':::'", "'$'", "'@'", "'^'", 
		"'-'", "'+'", "':'", "'*'", "'/'", "'>'", "'>='", "'<'", "'<='", "'=='", 
		"'!='", "'!'", "'&'", "'&&'", "'|'", "'||'", "'~'", "'<-'", "'<<-'", "'='", 
		"'->'", "'->>'", "':='", "'{'", "'}'", "'function'", "'('", "')'", "'if'", 
		"'else'", "'for'", "'in'", "'while'", "'repeat'", "'?'", "'next'", "'break'", 
		"'NULL'", "'NA'", "'Inf'", "'NaN'", "'TRUE'", "'FALSE'", "','", "'...'"
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, null, null, null, null, null, null, null, null, null, null, null, 
		null, null, null, null, null, null, null, null, null, null, null, null, 
		null, null, null, null, null, null, null, null, null, null, null, null, 
		null, null, null, null, null, null, null, null, null, null, null, null, 
		null, null, null, null, null, null, null, "HEX", "INT", "FLOAT", "COMPLEX", 
		"STRING", "ID", "USER_OP", "NL", "WS", "UNKNOWN"
	};
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}

	@Override
	public String getGrammarFileName() { return "RFilter.g4"; }

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
		public TerminalNode EOF() { return getToken(RFilter.EOF, 0); }
		public List<ElemContext> elem() {
			return getRuleContexts(ElemContext.class);
		}
		public ElemContext elem(int i) {
			return getRuleContext(ElemContext.class,i);
		}
		public List<TerminalNode> NL() { return getTokens(RFilter.NL); }
		public TerminalNode NL(int i) {
			return getToken(RFilter.NL, i);
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
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__0) | (1L << T__1) | (1L << T__3) | (1L << T__4) | (1L << T__5) | (1L << T__6) | (1L << T__7) | (1L << T__8) | (1L << T__9) | (1L << T__10) | (1L << T__11) | (1L << T__12) | (1L << T__13) | (1L << T__14) | (1L << T__15) | (1L << T__16) | (1L << T__17) | (1L << T__18) | (1L << T__19) | (1L << T__20) | (1L << T__21) | (1L << T__22) | (1L << T__23) | (1L << T__24) | (1L << T__25) | (1L << T__26) | (1L << T__27) | (1L << T__28) | (1L << T__29) | (1L << T__30) | (1L << T__31) | (1L << T__32) | (1L << T__34) | (1L << T__35) | (1L << T__37) | (1L << T__38) | (1L << T__39) | (1L << T__40) | (1L << T__41) | (1L << T__42) | (1L << T__43) | (1L << T__44) | (1L << T__45) | (1L << T__46) | (1L << T__47) | (1L << T__48) | (1L << T__49) | (1L << T__50) | (1L << T__51) | (1L << T__52) | (1L << T__53) | (1L << HEX) | (1L << INT) | (1L << FLOAT) | (1L << COMPLEX) | (1L << STRING) | (1L << ID) | (1L << USER_OP) | (1L << NL))) != 0)) {
				{
				setState(13);
				switch ( getInterpreter().adaptivePredict(_input,0,_ctx) ) {
				case 1:
					{
					setState(10);
					elem();
					}
					break;
				case 2:
					{
					setState(11);
					match(NL);
					}
					break;
				case 3:
					{
					setState(12);
					match(T__0);
					}
					break;
				}
				}
				setState(17);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(18);
			match(EOF);
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
			_alt = 1;
			do {
				switch (_alt) {
				case 1:
					{
					{
					setState(20);
					((EatContext)_localctx).NL = match(NL);
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
			} while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER );
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
		public OpContext op() {
			return getRuleContext(OpContext.class,0);
		}
		public List<EatContext> eat() {
			return getRuleContexts(EatContext.class);
		}
		public EatContext eat(int i) {
			return getRuleContext(EatContext.class,i);
		}
		public AtomContext atom() {
			return getRuleContext(AtomContext.class,0);
		}
		public List<ElemContext> elem() {
			return getRuleContexts(ElemContext.class);
		}
		public ElemContext elem(int i) {
			return getRuleContext(ElemContext.class,i);
		}
		public List<TerminalNode> NL() { return getTokens(RFilter.NL); }
		public TerminalNode NL(int i) {
			return getToken(RFilter.NL, i);
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
	}

	public final ElemContext elem() throws RecognitionException {
		ElemContext _localctx = new ElemContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_elem);
		int _la;
		try {
			setState(146);
			switch (_input.LA(1)) {
			case T__4:
			case T__5:
			case T__6:
			case T__7:
			case T__8:
			case T__9:
			case T__10:
			case T__11:
			case T__12:
			case T__13:
			case T__14:
			case T__15:
			case T__16:
			case T__17:
			case T__18:
			case T__19:
			case T__20:
			case T__21:
			case T__22:
			case T__23:
			case T__24:
			case T__25:
			case T__26:
			case T__27:
			case T__28:
			case T__29:
			case T__30:
			case T__31:
			case T__40:
			case T__42:
			case T__43:
			case T__52:
			case T__53:
			case USER_OP:
				enterOuterAlt(_localctx, 1);
				{
				setState(26);
				op();
				setState(28);
				switch ( getInterpreter().adaptivePredict(_input,3,_ctx) ) {
				case 1:
					{
					setState(27);
					eat();
					}
					break;
				}
				}
				break;
			case T__44:
			case T__45:
			case T__46:
			case T__47:
			case T__48:
			case T__49:
			case T__50:
			case T__51:
			case HEX:
			case INT:
			case FLOAT:
			case COMPLEX:
			case STRING:
			case ID:
				enterOuterAlt(_localctx, 2);
				{
				setState(30);
				atom();
				}
				break;
			case T__32:
				enterOuterAlt(_localctx, 3);
				{
				setState(31);
				match(T__32);
				setState(33);
				switch ( getInterpreter().adaptivePredict(_input,4,_ctx) ) {
				case 1:
					{
					setState(32);
					eat();
					}
					break;
				}
				curlies++;
				setState(41);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__0) | (1L << T__1) | (1L << T__3) | (1L << T__4) | (1L << T__5) | (1L << T__6) | (1L << T__7) | (1L << T__8) | (1L << T__9) | (1L << T__10) | (1L << T__11) | (1L << T__12) | (1L << T__13) | (1L << T__14) | (1L << T__15) | (1L << T__16) | (1L << T__17) | (1L << T__18) | (1L << T__19) | (1L << T__20) | (1L << T__21) | (1L << T__22) | (1L << T__23) | (1L << T__24) | (1L << T__25) | (1L << T__26) | (1L << T__27) | (1L << T__28) | (1L << T__29) | (1L << T__30) | (1L << T__31) | (1L << T__32) | (1L << T__34) | (1L << T__35) | (1L << T__37) | (1L << T__38) | (1L << T__39) | (1L << T__40) | (1L << T__41) | (1L << T__42) | (1L << T__43) | (1L << T__44) | (1L << T__45) | (1L << T__46) | (1L << T__47) | (1L << T__48) | (1L << T__49) | (1L << T__50) | (1L << T__51) | (1L << T__52) | (1L << T__53) | (1L << HEX) | (1L << INT) | (1L << FLOAT) | (1L << COMPLEX) | (1L << STRING) | (1L << ID) | (1L << USER_OP) | (1L << NL))) != 0)) {
					{
					setState(39);
					switch ( getInterpreter().adaptivePredict(_input,5,_ctx) ) {
					case 1:
						{
						setState(36);
						elem();
						}
						break;
					case 2:
						{
						setState(37);
						match(NL);
						}
						break;
					case 3:
						{
						setState(38);
						match(T__0);
						}
						break;
					}
					}
					setState(43);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				curlies--;
				setState(45);
				match(T__33);
				}
				break;
			case T__35:
				enterOuterAlt(_localctx, 4);
				{
				setState(46);
				match(T__35);
				setState(51);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__1) | (1L << T__3) | (1L << T__4) | (1L << T__5) | (1L << T__6) | (1L << T__7) | (1L << T__8) | (1L << T__9) | (1L << T__10) | (1L << T__11) | (1L << T__12) | (1L << T__13) | (1L << T__14) | (1L << T__15) | (1L << T__16) | (1L << T__17) | (1L << T__18) | (1L << T__19) | (1L << T__20) | (1L << T__21) | (1L << T__22) | (1L << T__23) | (1L << T__24) | (1L << T__25) | (1L << T__26) | (1L << T__27) | (1L << T__28) | (1L << T__29) | (1L << T__30) | (1L << T__31) | (1L << T__32) | (1L << T__34) | (1L << T__35) | (1L << T__37) | (1L << T__38) | (1L << T__39) | (1L << T__40) | (1L << T__41) | (1L << T__42) | (1L << T__43) | (1L << T__44) | (1L << T__45) | (1L << T__46) | (1L << T__47) | (1L << T__48) | (1L << T__49) | (1L << T__50) | (1L << T__51) | (1L << T__52) | (1L << T__53) | (1L << HEX) | (1L << INT) | (1L << FLOAT) | (1L << COMPLEX) | (1L << STRING) | (1L << ID) | (1L << USER_OP) | (1L << NL))) != 0)) {
					{
					setState(49);
					switch ( getInterpreter().adaptivePredict(_input,7,_ctx) ) {
					case 1:
						{
						setState(47);
						elem();
						}
						break;
					case 2:
						{
						setState(48);
						eat();
						}
						break;
					}
					}
					setState(53);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(54);
				match(T__36);
				}
				break;
			case T__3:
				enterOuterAlt(_localctx, 5);
				{
				setState(55);
				match(T__3);
				setState(60);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__1) | (1L << T__3) | (1L << T__4) | (1L << T__5) | (1L << T__6) | (1L << T__7) | (1L << T__8) | (1L << T__9) | (1L << T__10) | (1L << T__11) | (1L << T__12) | (1L << T__13) | (1L << T__14) | (1L << T__15) | (1L << T__16) | (1L << T__17) | (1L << T__18) | (1L << T__19) | (1L << T__20) | (1L << T__21) | (1L << T__22) | (1L << T__23) | (1L << T__24) | (1L << T__25) | (1L << T__26) | (1L << T__27) | (1L << T__28) | (1L << T__29) | (1L << T__30) | (1L << T__31) | (1L << T__32) | (1L << T__34) | (1L << T__35) | (1L << T__37) | (1L << T__38) | (1L << T__39) | (1L << T__40) | (1L << T__41) | (1L << T__42) | (1L << T__43) | (1L << T__44) | (1L << T__45) | (1L << T__46) | (1L << T__47) | (1L << T__48) | (1L << T__49) | (1L << T__50) | (1L << T__51) | (1L << T__52) | (1L << T__53) | (1L << HEX) | (1L << INT) | (1L << FLOAT) | (1L << COMPLEX) | (1L << STRING) | (1L << ID) | (1L << USER_OP) | (1L << NL))) != 0)) {
					{
					setState(58);
					switch ( getInterpreter().adaptivePredict(_input,9,_ctx) ) {
					case 1:
						{
						setState(56);
						elem();
						}
						break;
					case 2:
						{
						setState(57);
						eat();
						}
						break;
					}
					}
					setState(62);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(63);
				match(T__2);
				}
				break;
			case T__1:
				enterOuterAlt(_localctx, 6);
				{
				setState(64);
				match(T__1);
				setState(69);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__1) | (1L << T__3) | (1L << T__4) | (1L << T__5) | (1L << T__6) | (1L << T__7) | (1L << T__8) | (1L << T__9) | (1L << T__10) | (1L << T__11) | (1L << T__12) | (1L << T__13) | (1L << T__14) | (1L << T__15) | (1L << T__16) | (1L << T__17) | (1L << T__18) | (1L << T__19) | (1L << T__20) | (1L << T__21) | (1L << T__22) | (1L << T__23) | (1L << T__24) | (1L << T__25) | (1L << T__26) | (1L << T__27) | (1L << T__28) | (1L << T__29) | (1L << T__30) | (1L << T__31) | (1L << T__32) | (1L << T__34) | (1L << T__35) | (1L << T__37) | (1L << T__38) | (1L << T__39) | (1L << T__40) | (1L << T__41) | (1L << T__42) | (1L << T__43) | (1L << T__44) | (1L << T__45) | (1L << T__46) | (1L << T__47) | (1L << T__48) | (1L << T__49) | (1L << T__50) | (1L << T__51) | (1L << T__52) | (1L << T__53) | (1L << HEX) | (1L << INT) | (1L << FLOAT) | (1L << COMPLEX) | (1L << STRING) | (1L << ID) | (1L << USER_OP) | (1L << NL))) != 0)) {
					{
					setState(67);
					switch ( getInterpreter().adaptivePredict(_input,11,_ctx) ) {
					case 1:
						{
						setState(65);
						elem();
						}
						break;
					case 2:
						{
						setState(66);
						eat();
						}
						break;
					}
					}
					setState(71);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(72);
				match(T__2);
				setState(73);
				match(T__2);
				}
				break;
			case T__34:
				enterOuterAlt(_localctx, 7);
				{
				setState(74);
				match(T__34);
				setState(76);
				_la = _input.LA(1);
				if (_la==NL) {
					{
					setState(75);
					eat();
					}
				}

				setState(78);
				match(T__35);
				setState(83);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__1) | (1L << T__3) | (1L << T__4) | (1L << T__5) | (1L << T__6) | (1L << T__7) | (1L << T__8) | (1L << T__9) | (1L << T__10) | (1L << T__11) | (1L << T__12) | (1L << T__13) | (1L << T__14) | (1L << T__15) | (1L << T__16) | (1L << T__17) | (1L << T__18) | (1L << T__19) | (1L << T__20) | (1L << T__21) | (1L << T__22) | (1L << T__23) | (1L << T__24) | (1L << T__25) | (1L << T__26) | (1L << T__27) | (1L << T__28) | (1L << T__29) | (1L << T__30) | (1L << T__31) | (1L << T__32) | (1L << T__34) | (1L << T__35) | (1L << T__37) | (1L << T__38) | (1L << T__39) | (1L << T__40) | (1L << T__41) | (1L << T__42) | (1L << T__43) | (1L << T__44) | (1L << T__45) | (1L << T__46) | (1L << T__47) | (1L << T__48) | (1L << T__49) | (1L << T__50) | (1L << T__51) | (1L << T__52) | (1L << T__53) | (1L << HEX) | (1L << INT) | (1L << FLOAT) | (1L << COMPLEX) | (1L << STRING) | (1L << ID) | (1L << USER_OP) | (1L << NL))) != 0)) {
					{
					setState(81);
					switch ( getInterpreter().adaptivePredict(_input,14,_ctx) ) {
					case 1:
						{
						setState(79);
						elem();
						}
						break;
					case 2:
						{
						setState(80);
						eat();
						}
						break;
					}
					}
					setState(85);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(86);
				match(T__36);
				setState(88);
				switch ( getInterpreter().adaptivePredict(_input,16,_ctx) ) {
				case 1:
					{
					setState(87);
					eat();
					}
					break;
				}
				}
				break;
			case T__39:
				enterOuterAlt(_localctx, 8);
				{
				setState(90);
				match(T__39);
				setState(92);
				_la = _input.LA(1);
				if (_la==NL) {
					{
					setState(91);
					eat();
					}
				}

				setState(94);
				match(T__35);
				setState(99);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__1) | (1L << T__3) | (1L << T__4) | (1L << T__5) | (1L << T__6) | (1L << T__7) | (1L << T__8) | (1L << T__9) | (1L << T__10) | (1L << T__11) | (1L << T__12) | (1L << T__13) | (1L << T__14) | (1L << T__15) | (1L << T__16) | (1L << T__17) | (1L << T__18) | (1L << T__19) | (1L << T__20) | (1L << T__21) | (1L << T__22) | (1L << T__23) | (1L << T__24) | (1L << T__25) | (1L << T__26) | (1L << T__27) | (1L << T__28) | (1L << T__29) | (1L << T__30) | (1L << T__31) | (1L << T__32) | (1L << T__34) | (1L << T__35) | (1L << T__37) | (1L << T__38) | (1L << T__39) | (1L << T__40) | (1L << T__41) | (1L << T__42) | (1L << T__43) | (1L << T__44) | (1L << T__45) | (1L << T__46) | (1L << T__47) | (1L << T__48) | (1L << T__49) | (1L << T__50) | (1L << T__51) | (1L << T__52) | (1L << T__53) | (1L << HEX) | (1L << INT) | (1L << FLOAT) | (1L << COMPLEX) | (1L << STRING) | (1L << ID) | (1L << USER_OP) | (1L << NL))) != 0)) {
					{
					setState(97);
					switch ( getInterpreter().adaptivePredict(_input,18,_ctx) ) {
					case 1:
						{
						setState(95);
						elem();
						}
						break;
					case 2:
						{
						setState(96);
						eat();
						}
						break;
					}
					}
					setState(101);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(102);
				match(T__36);
				setState(104);
				switch ( getInterpreter().adaptivePredict(_input,20,_ctx) ) {
				case 1:
					{
					setState(103);
					eat();
					}
					break;
				}
				}
				break;
			case T__41:
				enterOuterAlt(_localctx, 9);
				{
				setState(106);
				match(T__41);
				setState(108);
				_la = _input.LA(1);
				if (_la==NL) {
					{
					setState(107);
					eat();
					}
				}

				setState(110);
				match(T__35);
				setState(115);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__1) | (1L << T__3) | (1L << T__4) | (1L << T__5) | (1L << T__6) | (1L << T__7) | (1L << T__8) | (1L << T__9) | (1L << T__10) | (1L << T__11) | (1L << T__12) | (1L << T__13) | (1L << T__14) | (1L << T__15) | (1L << T__16) | (1L << T__17) | (1L << T__18) | (1L << T__19) | (1L << T__20) | (1L << T__21) | (1L << T__22) | (1L << T__23) | (1L << T__24) | (1L << T__25) | (1L << T__26) | (1L << T__27) | (1L << T__28) | (1L << T__29) | (1L << T__30) | (1L << T__31) | (1L << T__32) | (1L << T__34) | (1L << T__35) | (1L << T__37) | (1L << T__38) | (1L << T__39) | (1L << T__40) | (1L << T__41) | (1L << T__42) | (1L << T__43) | (1L << T__44) | (1L << T__45) | (1L << T__46) | (1L << T__47) | (1L << T__48) | (1L << T__49) | (1L << T__50) | (1L << T__51) | (1L << T__52) | (1L << T__53) | (1L << HEX) | (1L << INT) | (1L << FLOAT) | (1L << COMPLEX) | (1L << STRING) | (1L << ID) | (1L << USER_OP) | (1L << NL))) != 0)) {
					{
					setState(113);
					switch ( getInterpreter().adaptivePredict(_input,22,_ctx) ) {
					case 1:
						{
						setState(111);
						elem();
						}
						break;
					case 2:
						{
						setState(112);
						eat();
						}
						break;
					}
					}
					setState(117);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(118);
				match(T__36);
				setState(120);
				switch ( getInterpreter().adaptivePredict(_input,24,_ctx) ) {
				case 1:
					{
					setState(119);
					eat();
					}
					break;
				}
				}
				break;
			case T__37:
				enterOuterAlt(_localctx, 10);
				{
				setState(122);
				match(T__37);
				setState(124);
				_la = _input.LA(1);
				if (_la==NL) {
					{
					setState(123);
					eat();
					}
				}

				setState(126);
				match(T__35);
				setState(131);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__1) | (1L << T__3) | (1L << T__4) | (1L << T__5) | (1L << T__6) | (1L << T__7) | (1L << T__8) | (1L << T__9) | (1L << T__10) | (1L << T__11) | (1L << T__12) | (1L << T__13) | (1L << T__14) | (1L << T__15) | (1L << T__16) | (1L << T__17) | (1L << T__18) | (1L << T__19) | (1L << T__20) | (1L << T__21) | (1L << T__22) | (1L << T__23) | (1L << T__24) | (1L << T__25) | (1L << T__26) | (1L << T__27) | (1L << T__28) | (1L << T__29) | (1L << T__30) | (1L << T__31) | (1L << T__32) | (1L << T__34) | (1L << T__35) | (1L << T__37) | (1L << T__38) | (1L << T__39) | (1L << T__40) | (1L << T__41) | (1L << T__42) | (1L << T__43) | (1L << T__44) | (1L << T__45) | (1L << T__46) | (1L << T__47) | (1L << T__48) | (1L << T__49) | (1L << T__50) | (1L << T__51) | (1L << T__52) | (1L << T__53) | (1L << HEX) | (1L << INT) | (1L << FLOAT) | (1L << COMPLEX) | (1L << STRING) | (1L << ID) | (1L << USER_OP) | (1L << NL))) != 0)) {
					{
					setState(129);
					switch ( getInterpreter().adaptivePredict(_input,26,_ctx) ) {
					case 1:
						{
						setState(127);
						elem();
						}
						break;
					case 2:
						{
						setState(128);
						eat();
						}
						break;
					}
					}
					setState(133);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(134);
				match(T__36);
				setState(136);
				switch ( getInterpreter().adaptivePredict(_input,28,_ctx) ) {
				case 1:
					{
					setState(135);
					eat();
					}
					break;
				}
				}
				break;
			case T__38:
			case NL:
				enterOuterAlt(_localctx, 11);
				{
				setState(139);
				_la = _input.LA(1);
				if (_la==NL) {
					{
					setState(138);
					eat();
					}
				}

				setState(141);
				match(T__38);
				setState(143);
				switch ( getInterpreter().adaptivePredict(_input,30,_ctx) ) {
				case 1:
					{
					setState(142);
					eat();
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
		public TerminalNode ID() { return getToken(RFilter.ID, 0); }
		public TerminalNode STRING() { return getToken(RFilter.STRING, 0); }
		public TerminalNode HEX() { return getToken(RFilter.HEX, 0); }
		public TerminalNode INT() { return getToken(RFilter.INT, 0); }
		public TerminalNode FLOAT() { return getToken(RFilter.FLOAT, 0); }
		public TerminalNode COMPLEX() { return getToken(RFilter.COMPLEX, 0); }
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
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__44) | (1L << T__45) | (1L << T__46) | (1L << T__47) | (1L << T__48) | (1L << T__49) | (1L << T__50) | (1L << T__51) | (1L << HEX) | (1L << INT) | (1L << FLOAT) | (1L << COMPLEX) | (1L << STRING) | (1L << ID))) != 0)) ) {
			_errHandler.recoverInline(this);
			} else {
				consume();
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
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__4) | (1L << T__5) | (1L << T__6) | (1L << T__7) | (1L << T__8) | (1L << T__9) | (1L << T__10) | (1L << T__11) | (1L << T__12) | (1L << T__13) | (1L << T__14) | (1L << T__15) | (1L << T__16) | (1L << T__17) | (1L << T__18) | (1L << T__19) | (1L << T__20) | (1L << T__21) | (1L << T__22) | (1L << T__23) | (1L << T__24) | (1L << T__25) | (1L << T__26) | (1L << T__27) | (1L << T__28) | (1L << T__29) | (1L << T__30) | (1L << T__31) | (1L << T__40) | (1L << T__42) | (1L << T__43) | (1L << T__52) | (1L << T__53) | (1L << USER_OP))) != 0)) ) {
			_errHandler.recoverInline(this);
			} else {
				consume();
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

	public static final String _serializedATN =
		"\3\u0430\ud6d1\u8206\uad2d\u4417\uaef1\u8d80\uaadd\3B\u009b\4\2\t\2\4"+
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
		"\3\6\2\2\7\2\4\6\b\n\2\4\4\2/\669>\7\2\7\"++-.\678??\u00c0\2\21\3\2\2"+
		"\2\4\30\3\2\2\2\6\u0094\3\2\2\2\b\u0096\3\2\2\2\n\u0098\3\2\2\2\f\20\5"+
		"\6\4\2\r\20\7@\2\2\16\20\7\3\2\2\17\f\3\2\2\2\17\r\3\2\2\2\17\16\3\2\2"+
		"\2\20\23\3\2\2\2\21\17\3\2\2\2\21\22\3\2\2\2\22\24\3\2\2\2\23\21\3\2\2"+
		"\2\24\25\7\2\2\3\25\3\3\2\2\2\26\27\7@\2\2\27\31\b\3\1\2\30\26\3\2\2\2"+
		"\31\32\3\2\2\2\32\30\3\2\2\2\32\33\3\2\2\2\33\5\3\2\2\2\34\36\5\n\6\2"+
		"\35\37\5\4\3\2\36\35\3\2\2\2\36\37\3\2\2\2\37\u0095\3\2\2\2 \u0095\5\b"+
		"\5\2!#\7#\2\2\"$\5\4\3\2#\"\3\2\2\2#$\3\2\2\2$%\3\2\2\2%+\b\4\1\2&*\5"+
		"\6\4\2\'*\7@\2\2(*\7\3\2\2)&\3\2\2\2)\'\3\2\2\2)(\3\2\2\2*-\3\2\2\2+)"+
		"\3\2\2\2+,\3\2\2\2,.\3\2\2\2-+\3\2\2\2./\b\4\1\2/\u0095\7$\2\2\60\65\7"+
		"&\2\2\61\64\5\6\4\2\62\64\5\4\3\2\63\61\3\2\2\2\63\62\3\2\2\2\64\67\3"+
		"\2\2\2\65\63\3\2\2\2\65\66\3\2\2\2\668\3\2\2\2\67\65\3\2\2\28\u0095\7"+
		"\'\2\29>\7\6\2\2:=\5\6\4\2;=\5\4\3\2<:\3\2\2\2<;\3\2\2\2=@\3\2\2\2><\3"+
		"\2\2\2>?\3\2\2\2?A\3\2\2\2@>\3\2\2\2A\u0095\7\5\2\2BG\7\4\2\2CF\5\6\4"+
		"\2DF\5\4\3\2EC\3\2\2\2ED\3\2\2\2FI\3\2\2\2GE\3\2\2\2GH\3\2\2\2HJ\3\2\2"+
		"\2IG\3\2\2\2JK\7\5\2\2K\u0095\7\5\2\2LN\7%\2\2MO\5\4\3\2NM\3\2\2\2NO\3"+
		"\2\2\2OP\3\2\2\2PU\7&\2\2QT\5\6\4\2RT\5\4\3\2SQ\3\2\2\2SR\3\2\2\2TW\3"+
		"\2\2\2US\3\2\2\2UV\3\2\2\2VX\3\2\2\2WU\3\2\2\2XZ\7\'\2\2Y[\5\4\3\2ZY\3"+
		"\2\2\2Z[\3\2\2\2[\u0095\3\2\2\2\\^\7*\2\2]_\5\4\3\2^]\3\2\2\2^_\3\2\2"+
		"\2_`\3\2\2\2`e\7&\2\2ad\5\6\4\2bd\5\4\3\2ca\3\2\2\2cb\3\2\2\2dg\3\2\2"+
		"\2ec\3\2\2\2ef\3\2\2\2fh\3\2\2\2ge\3\2\2\2hj\7\'\2\2ik\5\4\3\2ji\3\2\2"+
		"\2jk\3\2\2\2k\u0095\3\2\2\2ln\7,\2\2mo\5\4\3\2nm\3\2\2\2no\3\2\2\2op\3"+
		"\2\2\2pu\7&\2\2qt\5\6\4\2rt\5\4\3\2sq\3\2\2\2sr\3\2\2\2tw\3\2\2\2us\3"+
		"\2\2\2uv\3\2\2\2vx\3\2\2\2wu\3\2\2\2xz\7\'\2\2y{\5\4\3\2zy\3\2\2\2z{\3"+
		"\2\2\2{\u0095\3\2\2\2|~\7(\2\2}\177\5\4\3\2~}\3\2\2\2~\177\3\2\2\2\177"+
		"\u0080\3\2\2\2\u0080\u0085\7&\2\2\u0081\u0084\5\6\4\2\u0082\u0084\5\4"+
		"\3\2\u0083\u0081\3\2\2\2\u0083\u0082\3\2\2\2\u0084\u0087\3\2\2\2\u0085"+
		"\u0083\3\2\2\2\u0085\u0086\3\2\2\2\u0086\u0088\3\2\2\2\u0087\u0085\3\2"+
		"\2\2\u0088\u008a\7\'\2\2\u0089\u008b\5\4\3\2\u008a\u0089\3\2\2\2\u008a"+
		"\u008b\3\2\2\2\u008b\u0095\3\2\2\2\u008c\u008e\5\4\3\2\u008d\u008c\3\2"+
		"\2\2\u008d\u008e\3\2\2\2\u008e\u008f\3\2\2\2\u008f\u0091\7)\2\2\u0090"+
		"\u0092\5\4\3\2\u0091\u0090\3\2\2\2\u0091\u0092\3\2\2\2\u0092\u0093\3\2"+
		"\2\2\u0093\u0095\b\4\1\2\u0094\34\3\2\2\2\u0094 \3\2\2\2\u0094!\3\2\2"+
		"\2\u0094\60\3\2\2\2\u00949\3\2\2\2\u0094B\3\2\2\2\u0094L\3\2\2\2\u0094"+
		"\\\3\2\2\2\u0094l\3\2\2\2\u0094|\3\2\2\2\u0094\u008d\3\2\2\2\u0095\7\3"+
		"\2\2\2\u0096\u0097\t\2\2\2\u0097\t\3\2\2\2\u0098\u0099\t\3\2\2\u0099\13"+
		"\3\2\2\2\"\17\21\32\36#)+\63\65<>EGNSUZ^cejnsuz~\u0083\u0085\u008a\u008d"+
		"\u0091\u0094";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}