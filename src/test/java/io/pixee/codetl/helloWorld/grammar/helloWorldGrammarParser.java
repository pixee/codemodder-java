// Generated from /Users/markusvoelter/Documents/projects/pixee/codetl/src/test/java/io/pixee/codetl/helloWorld/grammar/helloWorldGrammar.g4 by ANTLR 4.10.1
package io.pixee.codetl.helloWorld.grammar;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class helloWorldGrammarParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.10.1", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, WS=2, NEWLINE=3, VAR=4, ID=5, NUMBER=6;
	public static final int
		RULE_program = 0, RULE_variable = 1, RULE_numlit = 2;
	private static String[] makeRuleNames() {
		return new String[] {
			"program", "variable", "numlit"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "'='", null, null, "'var'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, null, "WS", "NEWLINE", "VAR", "ID", "NUMBER"
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
	public String getGrammarFileName() { return "helloWorldGrammar.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public helloWorldGrammarParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	public static class ProgramContext extends ParserRuleContext {
		public TerminalNode EOF() { return getToken(helloWorldGrammarParser.EOF, 0); }
		public List<VariableContext> variable() {
			return getRuleContexts(VariableContext.class);
		}
		public VariableContext variable(int i) {
			return getRuleContext(VariableContext.class,i);
		}
		public ProgramContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_program; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof helloWorldGrammarListener ) ((helloWorldGrammarListener)listener).enterProgram(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof helloWorldGrammarListener ) ((helloWorldGrammarListener)listener).exitProgram(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof helloWorldGrammarVisitor ) return ((helloWorldGrammarVisitor<? extends T>)visitor).visitProgram(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ProgramContext program() throws RecognitionException {
		ProgramContext _localctx = new ProgramContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_program);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(9);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==VAR) {
				{
				{
				setState(6);
				variable();
				}
				}
				setState(11);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(12);
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

	public static class VariableContext extends ParserRuleContext {
		public TerminalNode VAR() { return getToken(helloWorldGrammarParser.VAR, 0); }
		public TerminalNode ID() { return getToken(helloWorldGrammarParser.ID, 0); }
		public NumlitContext numlit() {
			return getRuleContext(NumlitContext.class,0);
		}
		public TerminalNode NEWLINE() { return getToken(helloWorldGrammarParser.NEWLINE, 0); }
		public VariableContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_variable; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof helloWorldGrammarListener ) ((helloWorldGrammarListener)listener).enterVariable(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof helloWorldGrammarListener ) ((helloWorldGrammarListener)listener).exitVariable(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof helloWorldGrammarVisitor ) return ((helloWorldGrammarVisitor<? extends T>)visitor).visitVariable(this);
			else return visitor.visitChildren(this);
		}
	}

	public final VariableContext variable() throws RecognitionException {
		VariableContext _localctx = new VariableContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_variable);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(14);
			match(VAR);
			setState(15);
			match(ID);
			setState(16);
			match(T__0);
			setState(17);
			numlit();
			setState(18);
			match(NEWLINE);
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

	public static class NumlitContext extends ParserRuleContext {
		public TerminalNode NUMBER() { return getToken(helloWorldGrammarParser.NUMBER, 0); }
		public NumlitContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_numlit; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof helloWorldGrammarListener ) ((helloWorldGrammarListener)listener).enterNumlit(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof helloWorldGrammarListener ) ((helloWorldGrammarListener)listener).exitNumlit(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof helloWorldGrammarVisitor ) return ((helloWorldGrammarVisitor<? extends T>)visitor).visitNumlit(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NumlitContext numlit() throws RecognitionException {
		NumlitContext _localctx = new NumlitContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_numlit);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(20);
			match(NUMBER);
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
		"\u0004\u0001\u0006\u0017\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001"+
		"\u0002\u0002\u0007\u0002\u0001\u0000\u0005\u0000\b\b\u0000\n\u0000\f\u0000"+
		"\u000b\t\u0000\u0001\u0000\u0001\u0000\u0001\u0001\u0001\u0001\u0001\u0001"+
		"\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0002\u0001\u0002\u0001\u0002"+
		"\u0000\u0000\u0003\u0000\u0002\u0004\u0000\u0000\u0014\u0000\t\u0001\u0000"+
		"\u0000\u0000\u0002\u000e\u0001\u0000\u0000\u0000\u0004\u0014\u0001\u0000"+
		"\u0000\u0000\u0006\b\u0003\u0002\u0001\u0000\u0007\u0006\u0001\u0000\u0000"+
		"\u0000\b\u000b\u0001\u0000\u0000\u0000\t\u0007\u0001\u0000\u0000\u0000"+
		"\t\n\u0001\u0000\u0000\u0000\n\f\u0001\u0000\u0000\u0000\u000b\t\u0001"+
		"\u0000\u0000\u0000\f\r\u0005\u0000\u0000\u0001\r\u0001\u0001\u0000\u0000"+
		"\u0000\u000e\u000f\u0005\u0004\u0000\u0000\u000f\u0010\u0005\u0005\u0000"+
		"\u0000\u0010\u0011\u0005\u0001\u0000\u0000\u0011\u0012\u0003\u0004\u0002"+
		"\u0000\u0012\u0013\u0005\u0003\u0000\u0000\u0013\u0003\u0001\u0000\u0000"+
		"\u0000\u0014\u0015\u0005\u0006\u0000\u0000\u0015\u0005\u0001\u0000\u0000"+
		"\u0000\u0001\t";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}