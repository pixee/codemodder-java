package io.pixee.dsl.java;

// Generated from DSL.g4 by ANTLR 4.10.1
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class DSLParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.10.1", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, T__2=3, T__3=4, MATCH=5, REPLACE=6, WITH=7, COLON=8, EQ=9, 
		INSERT=10, INTO=11, DATA=12, FLOW=13, BEFORE=14, AFTER=15, AND=16, OR=17, 
		WHERE=18, CURLY_BRACKET_OPEN=19, CURLY_BRACKET_CLOSE=20, InsertIntoDataFlow=21, 
		Identifier=22, RuleIdentifier=23, Variable=24, RuleId=25, WHITESPACE=26;
	public static final int
		RULE_type = 0, RULE_rule_id = 1, RULE_match_replace = 2, RULE_match = 3, 
		RULE_replace = 4, RULE_clause = 5, RULE_where = 6, RULE_start = 7;
	private static String[] makeRuleNames() {
		return new String[] {
			"type", "rule_id", "match_replace", "match", "replace", "clause", "where", 
			"start"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "'.'", "'rule'", "'('", "')'", "'match'", "'replace'", "'with'", 
			"':'", "'='", "'insert'", "'into'", "'data'", "'flow'", "'before'", "'after'", 
			"'and'", "'or'", "'where'", "'{'", "'}'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, null, null, null, null, "MATCH", "REPLACE", "WITH", "COLON", "EQ", 
			"INSERT", "INTO", "DATA", "FLOW", "BEFORE", "AFTER", "AND", "OR", "WHERE", 
			"CURLY_BRACKET_OPEN", "CURLY_BRACKET_CLOSE", "InsertIntoDataFlow", "Identifier", 
			"RuleIdentifier", "Variable", "RuleId", "WHITESPACE"
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
	public String getGrammarFileName() { return "DSL.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public DSLParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	public static class TypeContext extends ParserRuleContext {
		public List<TerminalNode> Identifier() { return getTokens(DSLParser.Identifier); }
		public TerminalNode Identifier(int i) {
			return getToken(DSLParser.Identifier, i);
		}
		public TypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_type; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DSLListener ) ((DSLListener)listener).enterType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DSLListener ) ((DSLListener)listener).exitType(this);
		}
	}

	public final TypeContext type() throws RecognitionException {
		TypeContext _localctx = new TypeContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_type);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(16);
			match(Identifier);
			setState(21);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__0) {
				{
				{
				setState(17);
				match(T__0);
				setState(18);
				match(Identifier);
				}
				}
				setState(23);
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

	public static class Rule_idContext extends ParserRuleContext {
		public Token id;
		public TerminalNode RuleId() { return getToken(DSLParser.RuleId, 0); }
		public Rule_idContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_rule_id; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DSLListener ) ((DSLListener)listener).enterRule_id(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DSLListener ) ((DSLListener)listener).exitRule_id(this);
		}
	}

	public final Rule_idContext rule_id() throws RecognitionException {
		Rule_idContext _localctx = new Rule_idContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_rule_id);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(24);
			match(T__1);
			setState(25);
			((Rule_idContext)_localctx).id = match(RuleId);
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

	public static class Match_replaceContext extends ParserRuleContext {
		public Token id;
		public Token Identifier;
		public List<Token> name = new ArrayList<Token>();
		public TypeContext type;
		public List<TypeContext> value = new ArrayList<TypeContext>();
		public TerminalNode CURLY_BRACKET_OPEN() { return getToken(DSLParser.CURLY_BRACKET_OPEN, 0); }
		public List<TerminalNode> EQ() { return getTokens(DSLParser.EQ); }
		public TerminalNode EQ(int i) {
			return getToken(DSLParser.EQ, i);
		}
		public TerminalNode CURLY_BRACKET_CLOSE() { return getToken(DSLParser.CURLY_BRACKET_CLOSE, 0); }
		public List<TerminalNode> Identifier() { return getTokens(DSLParser.Identifier); }
		public TerminalNode Identifier(int i) {
			return getToken(DSLParser.Identifier, i);
		}
		public List<TypeContext> type() {
			return getRuleContexts(TypeContext.class);
		}
		public TypeContext type(int i) {
			return getRuleContext(TypeContext.class,i);
		}
		public Match_replaceContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_match_replace; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DSLListener ) ((DSLListener)listener).enterMatch_replace(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DSLListener ) ((DSLListener)listener).exitMatch_replace(this);
		}
	}

	public final Match_replaceContext match_replace() throws RecognitionException {
		Match_replaceContext _localctx = new Match_replaceContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_match_replace);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(27);
			((Match_replaceContext)_localctx).id = match(Identifier);
			setState(28);
			match(CURLY_BRACKET_OPEN);
			setState(29);
			((Match_replaceContext)_localctx).Identifier = match(Identifier);
			((Match_replaceContext)_localctx).name.add(((Match_replaceContext)_localctx).Identifier);
			setState(30);
			match(EQ);
			setState(31);
			((Match_replaceContext)_localctx).type = type();
			((Match_replaceContext)_localctx).value.add(((Match_replaceContext)_localctx).type);
			setState(37);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==Identifier) {
				{
				{
				setState(32);
				((Match_replaceContext)_localctx).Identifier = match(Identifier);
				((Match_replaceContext)_localctx).name.add(((Match_replaceContext)_localctx).Identifier);
				setState(33);
				match(EQ);
				setState(34);
				((Match_replaceContext)_localctx).type = type();
				((Match_replaceContext)_localctx).value.add(((Match_replaceContext)_localctx).type);
				}
				}
				setState(39);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(40);
			match(CURLY_BRACKET_CLOSE);
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

	public static class MatchContext extends ParserRuleContext {
		public TerminalNode MATCH() { return getToken(DSLParser.MATCH, 0); }
		public Match_replaceContext match_replace() {
			return getRuleContext(Match_replaceContext.class,0);
		}
		public MatchContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_match; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DSLListener ) ((DSLListener)listener).enterMatch(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DSLListener ) ((DSLListener)listener).exitMatch(this);
		}
	}

	public final MatchContext match() throws RecognitionException {
		MatchContext _localctx = new MatchContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_match);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(42);
			match(MATCH);
			setState(43);
			match_replace();
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

	public static class ReplaceContext extends ParserRuleContext {
		public Token var;
		public TerminalNode REPLACE() { return getToken(DSLParser.REPLACE, 0); }
		public Match_replaceContext match_replace() {
			return getRuleContext(Match_replaceContext.class,0);
		}
		public TerminalNode WITH() { return getToken(DSLParser.WITH, 0); }
		public TerminalNode Variable() { return getToken(DSLParser.Variable, 0); }
		public ReplaceContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_replace; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DSLListener ) ((DSLListener)listener).enterReplace(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DSLListener ) ((DSLListener)listener).exitReplace(this);
		}
	}

	public final ReplaceContext replace() throws RecognitionException {
		ReplaceContext _localctx = new ReplaceContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_replace);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(45);
			match(REPLACE);
			setState(48);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==Variable) {
				{
				setState(46);
				((ReplaceContext)_localctx).var = match(Variable);
				setState(47);
				match(WITH);
				}
			}

			setState(50);
			match_replace();
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

	public static class ClauseContext extends ParserRuleContext {
		public Token Identifier;
		public List<Token> var = new ArrayList<Token>();
		public List<Token> method = new ArrayList<Token>();
		public List<Token> target = new ArrayList<Token>();
		public List<TerminalNode> Identifier() { return getTokens(DSLParser.Identifier); }
		public TerminalNode Identifier(int i) {
			return getToken(DSLParser.Identifier, i);
		}
		public ClauseContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_clause; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DSLListener ) ((DSLListener)listener).enterClause(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DSLListener ) ((DSLListener)listener).exitClause(this);
		}
	}

	public final ClauseContext clause() throws RecognitionException {
		ClauseContext _localctx = new ClauseContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_clause);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(52);
			((ClauseContext)_localctx).Identifier = match(Identifier);
			((ClauseContext)_localctx).var.add(((ClauseContext)_localctx).Identifier);
			setState(53);
			match(T__0);
			setState(54);
			((ClauseContext)_localctx).Identifier = match(Identifier);
			((ClauseContext)_localctx).method.add(((ClauseContext)_localctx).Identifier);
			setState(55);
			match(T__2);
			setState(56);
			((ClauseContext)_localctx).Identifier = match(Identifier);
			((ClauseContext)_localctx).target.add(((ClauseContext)_localctx).Identifier);
			setState(57);
			match(T__3);
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

	public static class WhereContext extends ParserRuleContext {
		public TerminalNode WHERE() { return getToken(DSLParser.WHERE, 0); }
		public List<ClauseContext> clause() {
			return getRuleContexts(ClauseContext.class);
		}
		public ClauseContext clause(int i) {
			return getRuleContext(ClauseContext.class,i);
		}
		public List<TerminalNode> AND() { return getTokens(DSLParser.AND); }
		public TerminalNode AND(int i) {
			return getToken(DSLParser.AND, i);
		}
		public List<TerminalNode> OR() { return getTokens(DSLParser.OR); }
		public TerminalNode OR(int i) {
			return getToken(DSLParser.OR, i);
		}
		public WhereContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_where; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DSLListener ) ((DSLListener)listener).enterWhere(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DSLListener ) ((DSLListener)listener).exitWhere(this);
		}
	}

	public final WhereContext where() throws RecognitionException {
		WhereContext _localctx = new WhereContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_where);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(59);
			match(WHERE);
			setState(60);
			clause();
			setState(65);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==AND || _la==OR) {
				{
				{
				setState(61);
				_la = _input.LA(1);
				if ( !(_la==AND || _la==OR) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(62);
				clause();
				}
				}
				setState(67);
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

	public static class StartContext extends ParserRuleContext {
		public Rule_idContext rule_id() {
			return getRuleContext(Rule_idContext.class,0);
		}
		public MatchContext match() {
			return getRuleContext(MatchContext.class,0);
		}
		public ReplaceContext replace() {
			return getRuleContext(ReplaceContext.class,0);
		}
		public WhereContext where() {
			return getRuleContext(WhereContext.class,0);
		}
		public StartContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_start; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof DSLListener ) ((DSLListener)listener).enterStart(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof DSLListener ) ((DSLListener)listener).exitStart(this);
		}
	}

	public final StartContext start() throws RecognitionException {
		StartContext _localctx = new StartContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_start);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(68);
			rule_id();
			setState(69);
			match();
			setState(71);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WHERE) {
				{
				setState(70);
				where();
				}
			}

			setState(73);
			replace();
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
		"\u0004\u0001\u001aL\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001\u0002"+
		"\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004\u0007\u0004\u0002"+
		"\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002\u0007\u0007\u0007\u0001"+
		"\u0000\u0001\u0000\u0001\u0000\u0005\u0000\u0014\b\u0000\n\u0000\f\u0000"+
		"\u0017\t\u0000\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0002\u0001\u0002"+
		"\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002"+
		"\u0005\u0002$\b\u0002\n\u0002\f\u0002\'\t\u0002\u0001\u0002\u0001\u0002"+
		"\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0004\u0001\u0004\u0001\u0004"+
		"\u0003\u00041\b\u0004\u0001\u0004\u0001\u0004\u0001\u0005\u0001\u0005"+
		"\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0006"+
		"\u0001\u0006\u0001\u0006\u0001\u0006\u0005\u0006@\b\u0006\n\u0006\f\u0006"+
		"C\t\u0006\u0001\u0007\u0001\u0007\u0001\u0007\u0003\u0007H\b\u0007\u0001"+
		"\u0007\u0001\u0007\u0001\u0007\u0000\u0000\b\u0000\u0002\u0004\u0006\b"+
		"\n\f\u000e\u0000\u0001\u0001\u0000\u0010\u0011H\u0000\u0010\u0001\u0000"+
		"\u0000\u0000\u0002\u0018\u0001\u0000\u0000\u0000\u0004\u001b\u0001\u0000"+
		"\u0000\u0000\u0006*\u0001\u0000\u0000\u0000\b-\u0001\u0000\u0000\u0000"+
		"\n4\u0001\u0000\u0000\u0000\f;\u0001\u0000\u0000\u0000\u000eD\u0001\u0000"+
		"\u0000\u0000\u0010\u0015\u0005\u0016\u0000\u0000\u0011\u0012\u0005\u0001"+
		"\u0000\u0000\u0012\u0014\u0005\u0016\u0000\u0000\u0013\u0011\u0001\u0000"+
		"\u0000\u0000\u0014\u0017\u0001\u0000\u0000\u0000\u0015\u0013\u0001\u0000"+
		"\u0000\u0000\u0015\u0016\u0001\u0000\u0000\u0000\u0016\u0001\u0001\u0000"+
		"\u0000\u0000\u0017\u0015\u0001\u0000\u0000\u0000\u0018\u0019\u0005\u0002"+
		"\u0000\u0000\u0019\u001a\u0005\u0019\u0000\u0000\u001a\u0003\u0001\u0000"+
		"\u0000\u0000\u001b\u001c\u0005\u0016\u0000\u0000\u001c\u001d\u0005\u0013"+
		"\u0000\u0000\u001d\u001e\u0005\u0016\u0000\u0000\u001e\u001f\u0005\t\u0000"+
		"\u0000\u001f%\u0003\u0000\u0000\u0000 !\u0005\u0016\u0000\u0000!\"\u0005"+
		"\t\u0000\u0000\"$\u0003\u0000\u0000\u0000# \u0001\u0000\u0000\u0000$\'"+
		"\u0001\u0000\u0000\u0000%#\u0001\u0000\u0000\u0000%&\u0001\u0000\u0000"+
		"\u0000&(\u0001\u0000\u0000\u0000\'%\u0001\u0000\u0000\u0000()\u0005\u0014"+
		"\u0000\u0000)\u0005\u0001\u0000\u0000\u0000*+\u0005\u0005\u0000\u0000"+
		"+,\u0003\u0004\u0002\u0000,\u0007\u0001\u0000\u0000\u0000-0\u0005\u0006"+
		"\u0000\u0000./\u0005\u0018\u0000\u0000/1\u0005\u0007\u0000\u00000.\u0001"+
		"\u0000\u0000\u000001\u0001\u0000\u0000\u000012\u0001\u0000\u0000\u0000"+
		"23\u0003\u0004\u0002\u00003\t\u0001\u0000\u0000\u000045\u0005\u0016\u0000"+
		"\u000056\u0005\u0001\u0000\u000067\u0005\u0016\u0000\u000078\u0005\u0003"+
		"\u0000\u000089\u0005\u0016\u0000\u00009:\u0005\u0004\u0000\u0000:\u000b"+
		"\u0001\u0000\u0000\u0000;<\u0005\u0012\u0000\u0000<A\u0003\n\u0005\u0000"+
		"=>\u0007\u0000\u0000\u0000>@\u0003\n\u0005\u0000?=\u0001\u0000\u0000\u0000"+
		"@C\u0001\u0000\u0000\u0000A?\u0001\u0000\u0000\u0000AB\u0001\u0000\u0000"+
		"\u0000B\r\u0001\u0000\u0000\u0000CA\u0001\u0000\u0000\u0000DE\u0003\u0002"+
		"\u0001\u0000EG\u0003\u0006\u0003\u0000FH\u0003\f\u0006\u0000GF\u0001\u0000"+
		"\u0000\u0000GH\u0001\u0000\u0000\u0000HI\u0001\u0000\u0000\u0000IJ\u0003"+
		"\b\u0004\u0000J\u000f\u0001\u0000\u0000\u0000\u0005\u0015%0AG";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}