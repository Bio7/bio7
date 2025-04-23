// Generated from RFilter.g4 by ANTLR 4.13.1
package com.eco.bio7.reditor.antlr;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast", "CheckReturnValue"})
public class RFilter extends Parser {
	static { RuntimeMetaData.checkVersion("4.13.1", RuntimeMetaData.VERSION); }

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
		T__52=53, T__53=54, RAW_STRING=55, PIPE=56, PIPEBIND=57, PLACEHOLDER=58, 
		HEX=59, INT=60, FLOAT=61, COMPLEX=62, STRING=63, ID=64, USER_OP=65, POWER_OP=66, 
		NL=67, WS=68, UNKNOWN=69;
	public static final int
		RULE_stream = 0, RULE_eat = 1, RULE_elem = 2, RULE_atom = 3, RULE_op = 4;
	private static String[] makeRuleNames() {
		return new String[] {
			"stream", "eat", "elem", "atom", "op"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "';'", "'[['", "']'", "'['", "'::'", "':::'", "'$'", "'@'", "'-'", 
			"'+'", "':'", "'*'", "'/'", "'>'", "'>='", "'<'", "'<='", "'=='", "'!='", 
			"'!'", "'&'", "'&&'", "'|'", "'||'", "'~'", "'<-'", "'<<-'", "'='", "'->'", 
			"'->>'", "':='", "'{'", "'}'", "'function'", "'\\'", "'('", "')'", "'if'", 
			"'else'", "'for'", "'in'", "'while'", "'repeat'", "'?'", "'next'", "'break'", 
			"'NULL'", "'NA'", "'Inf'", "'NaN'", "'TRUE'", "'FALSE'", "','", "'...'", 
			null, "'|>'", "'=>'", "'_'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, "RAW_STRING", "PIPE", "PIPEBIND", 
			"PLACEHOLDER", "HEX", "INT", "FLOAT", "COMPLEX", "STRING", "ID", "USER_OP", 
			"POWER_OP", "NL", "WS", "UNKNOWN"
		};
	}
	private static final String[] _SYMBOLIC_NAMES = makeSymbolicNames();
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

	@SuppressWarnings("CheckReturnValue")
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
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & -324259319199563786L) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & 15L) != 0)) {
				{
				setState(13);
				_errHandler.sync(this);
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

	@SuppressWarnings("CheckReturnValue")
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

	@SuppressWarnings("CheckReturnValue")
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
			_errHandler.sync(this);
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
			case T__40:
			case T__42:
			case T__43:
			case T__52:
			case T__53:
			case PIPE:
			case PIPEBIND:
			case USER_OP:
			case POWER_OP:
				enterOuterAlt(_localctx, 1);
				{
				setState(26);
				op();
				setState(28);
				_errHandler.sync(this);
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
			case T__31:
				enterOuterAlt(_localctx, 3);
				{
				setState(31);
				match(T__31);
				setState(33);
				_errHandler.sync(this);
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
				while ((((_la) & ~0x3f) == 0 && ((1L << _la) & -324259319199563786L) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & 15L) != 0)) {
					{
					setState(39);
					_errHandler.sync(this);
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
				match(T__32);
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
				while ((((_la) & ~0x3f) == 0 && ((1L << _la) & -324259319199563788L) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & 15L) != 0)) {
					{
					setState(49);
					_errHandler.sync(this);
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
				while ((((_la) & ~0x3f) == 0 && ((1L << _la) & -324259319199563788L) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & 15L) != 0)) {
					{
					setState(58);
					_errHandler.sync(this);
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
				while ((((_la) & ~0x3f) == 0 && ((1L << _la) & -324259319199563788L) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & 15L) != 0)) {
					{
					setState(67);
					_errHandler.sync(this);
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
			case T__33:
			case T__34:
				enterOuterAlt(_localctx, 7);
				{
				setState(74);
				_la = _input.LA(1);
				if ( !(_la==T__33 || _la==T__34) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(76);
				_errHandler.sync(this);
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
				while ((((_la) & ~0x3f) == 0 && ((1L << _la) & -324259319199563788L) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & 15L) != 0)) {
					{
					setState(81);
					_errHandler.sync(this);
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
				_errHandler.sync(this);
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
				_errHandler.sync(this);
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
				while ((((_la) & ~0x3f) == 0 && ((1L << _la) & -324259319199563788L) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & 15L) != 0)) {
					{
					setState(97);
					_errHandler.sync(this);
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
				_errHandler.sync(this);
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
				_errHandler.sync(this);
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
				while ((((_la) & ~0x3f) == 0 && ((1L << _la) & -324259319199563788L) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & 15L) != 0)) {
					{
					setState(113);
					_errHandler.sync(this);
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
				_errHandler.sync(this);
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
				_errHandler.sync(this);
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
				while ((((_la) & ~0x3f) == 0 && ((1L << _la) & -324259319199563788L) != 0) || ((((_la - 64)) & ~0x3f) == 0 && ((1L << (_la - 64)) & 15L) != 0)) {
					{
					setState(129);
					_errHandler.sync(this);
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
				_errHandler.sync(this);
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
				_errHandler.sync(this);
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
				_errHandler.sync(this);
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

	@SuppressWarnings("CheckReturnValue")
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
			if ( !(((((_la - 45)) & ~0x3f) == 0 && ((1L << (_la - 45)) & 1032447L) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
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

	@SuppressWarnings("CheckReturnValue")
	public static class OpContext extends ParserRuleContext {
		public TerminalNode USER_OP() { return getToken(RFilter.USER_OP, 0); }
		public TerminalNode POWER_OP() { return getToken(RFilter.POWER_OP, 0); }
		public TerminalNode PIPE() { return getToken(RFilter.PIPE, 0); }
		public TerminalNode PIPEBIND() { return getToken(RFilter.PIPEBIND, 0); }
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
			if ( !(((((_la - 5)) & ~0x3f) == 0 && ((1L << (_la - 5)) & 3466365231679143935L) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
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
		"\u0004\u0001E\u0099\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001\u0002"+
		"\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004\u0007\u0004\u0001"+
		"\u0000\u0001\u0000\u0001\u0000\u0005\u0000\u000e\b\u0000\n\u0000\f\u0000"+
		"\u0011\t\u0000\u0001\u0000\u0001\u0000\u0001\u0001\u0001\u0001\u0004\u0001"+
		"\u0017\b\u0001\u000b\u0001\f\u0001\u0018\u0001\u0002\u0001\u0002\u0003"+
		"\u0002\u001d\b\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0003\u0002\""+
		"\b\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0005\u0002("+
		"\b\u0002\n\u0002\f\u0002+\t\u0002\u0001\u0002\u0001\u0002\u0001\u0002"+
		"\u0001\u0002\u0001\u0002\u0005\u00022\b\u0002\n\u0002\f\u00025\t\u0002"+
		"\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0005\u0002;\b\u0002"+
		"\n\u0002\f\u0002>\t\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002"+
		"\u0005\u0002D\b\u0002\n\u0002\f\u0002G\t\u0002\u0001\u0002\u0001\u0002"+
		"\u0001\u0002\u0001\u0002\u0003\u0002M\b\u0002\u0001\u0002\u0001\u0002"+
		"\u0001\u0002\u0005\u0002R\b\u0002\n\u0002\f\u0002U\t\u0002\u0001\u0002"+
		"\u0001\u0002\u0003\u0002Y\b\u0002\u0001\u0002\u0001\u0002\u0003\u0002"+
		"]\b\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0005\u0002b\b\u0002\n\u0002"+
		"\f\u0002e\t\u0002\u0001\u0002\u0001\u0002\u0003\u0002i\b\u0002\u0001\u0002"+
		"\u0001\u0002\u0003\u0002m\b\u0002\u0001\u0002\u0001\u0002\u0001\u0002"+
		"\u0005\u0002r\b\u0002\n\u0002\f\u0002u\t\u0002\u0001\u0002\u0001\u0002"+
		"\u0003\u0002y\b\u0002\u0001\u0002\u0001\u0002\u0003\u0002}\b\u0002\u0001"+
		"\u0002\u0001\u0002\u0001\u0002\u0005\u0002\u0082\b\u0002\n\u0002\f\u0002"+
		"\u0085\t\u0002\u0001\u0002\u0001\u0002\u0003\u0002\u0089\b\u0002\u0001"+
		"\u0002\u0003\u0002\u008c\b\u0002\u0001\u0002\u0001\u0002\u0003\u0002\u0090"+
		"\b\u0002\u0001\u0002\u0003\u0002\u0093\b\u0002\u0001\u0003\u0001\u0003"+
		"\u0001\u0004\u0001\u0004\u0001\u0004\u0000\u0000\u0005\u0000\u0002\u0004"+
		"\u0006\b\u0000\u0003\u0001\u0000\"#\u0002\u0000-4;@\u0006\u0000\u0005"+
		"\u001f))+,5689AB\u00be\u0000\u000f\u0001\u0000\u0000\u0000\u0002\u0016"+
		"\u0001\u0000\u0000\u0000\u0004\u0092\u0001\u0000\u0000\u0000\u0006\u0094"+
		"\u0001\u0000\u0000\u0000\b\u0096\u0001\u0000\u0000\u0000\n\u000e\u0003"+
		"\u0004\u0002\u0000\u000b\u000e\u0005C\u0000\u0000\f\u000e\u0005\u0001"+
		"\u0000\u0000\r\n\u0001\u0000\u0000\u0000\r\u000b\u0001\u0000\u0000\u0000"+
		"\r\f\u0001\u0000\u0000\u0000\u000e\u0011\u0001\u0000\u0000\u0000\u000f"+
		"\r\u0001\u0000\u0000\u0000\u000f\u0010\u0001\u0000\u0000\u0000\u0010\u0012"+
		"\u0001\u0000\u0000\u0000\u0011\u000f\u0001\u0000\u0000\u0000\u0012\u0013"+
		"\u0005\u0000\u0000\u0001\u0013\u0001\u0001\u0000\u0000\u0000\u0014\u0015"+
		"\u0005C\u0000\u0000\u0015\u0017\u0006\u0001\uffff\uffff\u0000\u0016\u0014"+
		"\u0001\u0000\u0000\u0000\u0017\u0018\u0001\u0000\u0000\u0000\u0018\u0016"+
		"\u0001\u0000\u0000\u0000\u0018\u0019\u0001\u0000\u0000\u0000\u0019\u0003"+
		"\u0001\u0000\u0000\u0000\u001a\u001c\u0003\b\u0004\u0000\u001b\u001d\u0003"+
		"\u0002\u0001\u0000\u001c\u001b\u0001\u0000\u0000\u0000\u001c\u001d\u0001"+
		"\u0000\u0000\u0000\u001d\u0093\u0001\u0000\u0000\u0000\u001e\u0093\u0003"+
		"\u0006\u0003\u0000\u001f!\u0005 \u0000\u0000 \"\u0003\u0002\u0001\u0000"+
		"! \u0001\u0000\u0000\u0000!\"\u0001\u0000\u0000\u0000\"#\u0001\u0000\u0000"+
		"\u0000#)\u0006\u0002\uffff\uffff\u0000$(\u0003\u0004\u0002\u0000%(\u0005"+
		"C\u0000\u0000&(\u0005\u0001\u0000\u0000\'$\u0001\u0000\u0000\u0000\'%"+
		"\u0001\u0000\u0000\u0000\'&\u0001\u0000\u0000\u0000(+\u0001\u0000\u0000"+
		"\u0000)\'\u0001\u0000\u0000\u0000)*\u0001\u0000\u0000\u0000*,\u0001\u0000"+
		"\u0000\u0000+)\u0001\u0000\u0000\u0000,-\u0006\u0002\uffff\uffff\u0000"+
		"-\u0093\u0005!\u0000\u0000.3\u0005$\u0000\u0000/2\u0003\u0004\u0002\u0000"+
		"02\u0003\u0002\u0001\u00001/\u0001\u0000\u0000\u000010\u0001\u0000\u0000"+
		"\u000025\u0001\u0000\u0000\u000031\u0001\u0000\u0000\u000034\u0001\u0000"+
		"\u0000\u000046\u0001\u0000\u0000\u000053\u0001\u0000\u0000\u00006\u0093"+
		"\u0005%\u0000\u00007<\u0005\u0004\u0000\u00008;\u0003\u0004\u0002\u0000"+
		"9;\u0003\u0002\u0001\u0000:8\u0001\u0000\u0000\u0000:9\u0001\u0000\u0000"+
		"\u0000;>\u0001\u0000\u0000\u0000<:\u0001\u0000\u0000\u0000<=\u0001\u0000"+
		"\u0000\u0000=?\u0001\u0000\u0000\u0000><\u0001\u0000\u0000\u0000?\u0093"+
		"\u0005\u0003\u0000\u0000@E\u0005\u0002\u0000\u0000AD\u0003\u0004\u0002"+
		"\u0000BD\u0003\u0002\u0001\u0000CA\u0001\u0000\u0000\u0000CB\u0001\u0000"+
		"\u0000\u0000DG\u0001\u0000\u0000\u0000EC\u0001\u0000\u0000\u0000EF\u0001"+
		"\u0000\u0000\u0000FH\u0001\u0000\u0000\u0000GE\u0001\u0000\u0000\u0000"+
		"HI\u0005\u0003\u0000\u0000I\u0093\u0005\u0003\u0000\u0000JL\u0007\u0000"+
		"\u0000\u0000KM\u0003\u0002\u0001\u0000LK\u0001\u0000\u0000\u0000LM\u0001"+
		"\u0000\u0000\u0000MN\u0001\u0000\u0000\u0000NS\u0005$\u0000\u0000OR\u0003"+
		"\u0004\u0002\u0000PR\u0003\u0002\u0001\u0000QO\u0001\u0000\u0000\u0000"+
		"QP\u0001\u0000\u0000\u0000RU\u0001\u0000\u0000\u0000SQ\u0001\u0000\u0000"+
		"\u0000ST\u0001\u0000\u0000\u0000TV\u0001\u0000\u0000\u0000US\u0001\u0000"+
		"\u0000\u0000VX\u0005%\u0000\u0000WY\u0003\u0002\u0001\u0000XW\u0001\u0000"+
		"\u0000\u0000XY\u0001\u0000\u0000\u0000Y\u0093\u0001\u0000\u0000\u0000"+
		"Z\\\u0005(\u0000\u0000[]\u0003\u0002\u0001\u0000\\[\u0001\u0000\u0000"+
		"\u0000\\]\u0001\u0000\u0000\u0000]^\u0001\u0000\u0000\u0000^c\u0005$\u0000"+
		"\u0000_b\u0003\u0004\u0002\u0000`b\u0003\u0002\u0001\u0000a_\u0001\u0000"+
		"\u0000\u0000a`\u0001\u0000\u0000\u0000be\u0001\u0000\u0000\u0000ca\u0001"+
		"\u0000\u0000\u0000cd\u0001\u0000\u0000\u0000df\u0001\u0000\u0000\u0000"+
		"ec\u0001\u0000\u0000\u0000fh\u0005%\u0000\u0000gi\u0003\u0002\u0001\u0000"+
		"hg\u0001\u0000\u0000\u0000hi\u0001\u0000\u0000\u0000i\u0093\u0001\u0000"+
		"\u0000\u0000jl\u0005*\u0000\u0000km\u0003\u0002\u0001\u0000lk\u0001\u0000"+
		"\u0000\u0000lm\u0001\u0000\u0000\u0000mn\u0001\u0000\u0000\u0000ns\u0005"+
		"$\u0000\u0000or\u0003\u0004\u0002\u0000pr\u0003\u0002\u0001\u0000qo\u0001"+
		"\u0000\u0000\u0000qp\u0001\u0000\u0000\u0000ru\u0001\u0000\u0000\u0000"+
		"sq\u0001\u0000\u0000\u0000st\u0001\u0000\u0000\u0000tv\u0001\u0000\u0000"+
		"\u0000us\u0001\u0000\u0000\u0000vx\u0005%\u0000\u0000wy\u0003\u0002\u0001"+
		"\u0000xw\u0001\u0000\u0000\u0000xy\u0001\u0000\u0000\u0000y\u0093\u0001"+
		"\u0000\u0000\u0000z|\u0005&\u0000\u0000{}\u0003\u0002\u0001\u0000|{\u0001"+
		"\u0000\u0000\u0000|}\u0001\u0000\u0000\u0000}~\u0001\u0000\u0000\u0000"+
		"~\u0083\u0005$\u0000\u0000\u007f\u0082\u0003\u0004\u0002\u0000\u0080\u0082"+
		"\u0003\u0002\u0001\u0000\u0081\u007f\u0001\u0000\u0000\u0000\u0081\u0080"+
		"\u0001\u0000\u0000\u0000\u0082\u0085\u0001\u0000\u0000\u0000\u0083\u0081"+
		"\u0001\u0000\u0000\u0000\u0083\u0084\u0001\u0000\u0000\u0000\u0084\u0086"+
		"\u0001\u0000\u0000\u0000\u0085\u0083\u0001\u0000\u0000\u0000\u0086\u0088"+
		"\u0005%\u0000\u0000\u0087\u0089\u0003\u0002\u0001\u0000\u0088\u0087\u0001"+
		"\u0000\u0000\u0000\u0088\u0089\u0001\u0000\u0000\u0000\u0089\u0093\u0001"+
		"\u0000\u0000\u0000\u008a\u008c\u0003\u0002\u0001\u0000\u008b\u008a\u0001"+
		"\u0000\u0000\u0000\u008b\u008c\u0001\u0000\u0000\u0000\u008c\u008d\u0001"+
		"\u0000\u0000\u0000\u008d\u008f\u0005\'\u0000\u0000\u008e\u0090\u0003\u0002"+
		"\u0001\u0000\u008f\u008e\u0001\u0000\u0000\u0000\u008f\u0090\u0001\u0000"+
		"\u0000\u0000\u0090\u0091\u0001\u0000\u0000\u0000\u0091\u0093\u0006\u0002"+
		"\uffff\uffff\u0000\u0092\u001a\u0001\u0000\u0000\u0000\u0092\u001e\u0001"+
		"\u0000\u0000\u0000\u0092\u001f\u0001\u0000\u0000\u0000\u0092.\u0001\u0000"+
		"\u0000\u0000\u00927\u0001\u0000\u0000\u0000\u0092@\u0001\u0000\u0000\u0000"+
		"\u0092J\u0001\u0000\u0000\u0000\u0092Z\u0001\u0000\u0000\u0000\u0092j"+
		"\u0001\u0000\u0000\u0000\u0092z\u0001\u0000\u0000\u0000\u0092\u008b\u0001"+
		"\u0000\u0000\u0000\u0093\u0005\u0001\u0000\u0000\u0000\u0094\u0095\u0007"+
		"\u0001\u0000\u0000\u0095\u0007\u0001\u0000\u0000\u0000\u0096\u0097\u0007"+
		"\u0002\u0000\u0000\u0097\t\u0001\u0000\u0000\u0000 \r\u000f\u0018\u001c"+
		"!\')13:<CELQSX\\achlqsx|\u0081\u0083\u0088\u008b\u008f\u0092";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}