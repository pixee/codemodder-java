package io.pixee.dsl.java;

// Generated from DSL.g4 by ANTLR 4.7.2
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class DSLLexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.7.2", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, T__2=3, T__3=4, MATCH=5, REPLACE=6, WITH=7, COLON=8, EQ=9, 
		INSERT=10, INTO=11, DATA=12, FLOW=13, BEFORE=14, AFTER=15, AND=16, OR=17, 
		WHERE=18, CURLY_BRACKET_OPEN=19, CURLY_BRACKET_CLOSE=20, InsertIntoDataFlow=21, 
		Identifier=22, RuleIdentifier=23, Variable=24, RuleId=25, WHITESPACE=26;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	private static String[] makeRuleNames() {
		return new String[] {
			"T__0", "T__1", "T__2", "T__3", "MATCH", "REPLACE", "WITH", "COLON", 
			"EQ", "INSERT", "INTO", "DATA", "FLOW", "BEFORE", "AFTER", "AND", "OR", 
			"WHERE", "CURLY_BRACKET_OPEN", "CURLY_BRACKET_CLOSE", "InsertIntoDataFlow", 
			"Identifier", "RuleIdentifier", "VALID_ID_START", "VALID_ID_CHAR", "VALID_RULE_ID_CHAR", 
			"Variable", "RuleId", "WHITESPACE"
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


	public DSLLexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "DSL.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public String[] getChannelNames() { return channelNames; }

	@Override
	public String[] getModeNames() { return modeNames; }

	@Override
	public ATN getATN() { return _ATN; }

	public static final String _serializedATN =
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2\34\u00c4\b\1\4\2"+
		"\t\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4"+
		"\13\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22"+
		"\t\22\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31"+
		"\t\31\4\32\t\32\4\33\t\33\4\34\t\34\4\35\t\35\4\36\t\36\3\2\3\2\3\3\3"+
		"\3\3\3\3\3\3\3\3\4\3\4\3\5\3\5\3\6\3\6\3\6\3\6\3\6\3\6\3\7\3\7\3\7\3\7"+
		"\3\7\3\7\3\7\3\7\3\b\3\b\3\b\3\b\3\b\3\t\3\t\3\n\3\n\3\13\3\13\3\13\3"+
		"\13\3\13\3\13\3\13\3\f\3\f\3\f\3\f\3\f\3\r\3\r\3\r\3\r\3\r\3\16\3\16\3"+
		"\16\3\16\3\16\3\17\3\17\3\17\3\17\3\17\3\17\3\17\3\20\3\20\3\20\3\20\3"+
		"\20\3\20\3\21\3\21\3\21\3\21\3\22\3\22\3\22\3\23\3\23\3\23\3\23\3\23\3"+
		"\23\3\24\3\24\3\25\3\25\3\26\3\26\3\26\3\26\3\26\3\26\5\26\u009a\n\26"+
		"\3\27\3\27\7\27\u009e\n\27\f\27\16\27\u00a1\13\27\3\30\3\30\7\30\u00a5"+
		"\n\30\f\30\16\30\u00a8\13\30\3\31\5\31\u00ab\n\31\3\32\3\32\5\32\u00af"+
		"\n\32\3\33\3\33\5\33\u00b3\n\33\3\34\3\34\3\34\3\35\3\35\3\35\3\35\3\35"+
		"\3\35\3\36\6\36\u00bf\n\36\r\36\16\36\u00c0\3\36\3\36\2\2\37\3\3\5\4\7"+
		"\5\t\6\13\7\r\b\17\t\21\n\23\13\25\f\27\r\31\16\33\17\35\20\37\21!\22"+
		"#\23%\24\'\25)\26+\27-\30/\31\61\2\63\2\65\2\67\329\33;\34\3\2\5\5\2C"+
		"\\aac|\4\2//\62;\5\2\13\f\17\17\"\"\2\u00c6\2\3\3\2\2\2\2\5\3\2\2\2\2"+
		"\7\3\2\2\2\2\t\3\2\2\2\2\13\3\2\2\2\2\r\3\2\2\2\2\17\3\2\2\2\2\21\3\2"+
		"\2\2\2\23\3\2\2\2\2\25\3\2\2\2\2\27\3\2\2\2\2\31\3\2\2\2\2\33\3\2\2\2"+
		"\2\35\3\2\2\2\2\37\3\2\2\2\2!\3\2\2\2\2#\3\2\2\2\2%\3\2\2\2\2\'\3\2\2"+
		"\2\2)\3\2\2\2\2+\3\2\2\2\2-\3\2\2\2\2/\3\2\2\2\2\67\3\2\2\2\29\3\2\2\2"+
		"\2;\3\2\2\2\3=\3\2\2\2\5?\3\2\2\2\7D\3\2\2\2\tF\3\2\2\2\13H\3\2\2\2\r"+
		"N\3\2\2\2\17V\3\2\2\2\21[\3\2\2\2\23]\3\2\2\2\25_\3\2\2\2\27f\3\2\2\2"+
		"\31k\3\2\2\2\33p\3\2\2\2\35u\3\2\2\2\37|\3\2\2\2!\u0082\3\2\2\2#\u0086"+
		"\3\2\2\2%\u0089\3\2\2\2\'\u008f\3\2\2\2)\u0091\3\2\2\2+\u0093\3\2\2\2"+
		"-\u009b\3\2\2\2/\u00a2\3\2\2\2\61\u00aa\3\2\2\2\63\u00ae\3\2\2\2\65\u00b2"+
		"\3\2\2\2\67\u00b4\3\2\2\29\u00b7\3\2\2\2;\u00be\3\2\2\2=>\7\60\2\2>\4"+
		"\3\2\2\2?@\7t\2\2@A\7w\2\2AB\7n\2\2BC\7g\2\2C\6\3\2\2\2DE\7*\2\2E\b\3"+
		"\2\2\2FG\7+\2\2G\n\3\2\2\2HI\7o\2\2IJ\7c\2\2JK\7v\2\2KL\7e\2\2LM\7j\2"+
		"\2M\f\3\2\2\2NO\7t\2\2OP\7g\2\2PQ\7r\2\2QR\7n\2\2RS\7c\2\2ST\7e\2\2TU"+
		"\7g\2\2U\16\3\2\2\2VW\7y\2\2WX\7k\2\2XY\7v\2\2YZ\7j\2\2Z\20\3\2\2\2[\\"+
		"\7<\2\2\\\22\3\2\2\2]^\7?\2\2^\24\3\2\2\2_`\7k\2\2`a\7p\2\2ab\7u\2\2b"+
		"c\7g\2\2cd\7t\2\2de\7v\2\2e\26\3\2\2\2fg\7k\2\2gh\7p\2\2hi\7v\2\2ij\7"+
		"q\2\2j\30\3\2\2\2kl\7f\2\2lm\7c\2\2mn\7v\2\2no\7c\2\2o\32\3\2\2\2pq\7"+
		"h\2\2qr\7n\2\2rs\7q\2\2st\7y\2\2t\34\3\2\2\2uv\7d\2\2vw\7g\2\2wx\7h\2"+
		"\2xy\7q\2\2yz\7t\2\2z{\7g\2\2{\36\3\2\2\2|}\7c\2\2}~\7h\2\2~\177\7v\2"+
		"\2\177\u0080\7g\2\2\u0080\u0081\7t\2\2\u0081 \3\2\2\2\u0082\u0083\7c\2"+
		"\2\u0083\u0084\7p\2\2\u0084\u0085\7f\2\2\u0085\"\3\2\2\2\u0086\u0087\7"+
		"q\2\2\u0087\u0088\7t\2\2\u0088$\3\2\2\2\u0089\u008a\7y\2\2\u008a\u008b"+
		"\7j\2\2\u008b\u008c\7g\2\2\u008c\u008d\7t\2\2\u008d\u008e\7g\2\2\u008e"+
		"&\3\2\2\2\u008f\u0090\7}\2\2\u0090(\3\2\2\2\u0091\u0092\7\177\2\2\u0092"+
		"*\3\2\2\2\u0093\u0094\5\25\13\2\u0094\u0095\5\27\f\2\u0095\u0096\5\31"+
		"\r\2\u0096\u0099\5\33\16\2\u0097\u009a\5\35\17\2\u0098\u009a\5\37\20\2"+
		"\u0099\u0097\3\2\2\2\u0099\u0098\3\2\2\2\u009a,\3\2\2\2\u009b\u009f\5"+
		"\61\31\2\u009c\u009e\5\63\32\2\u009d\u009c\3\2\2\2\u009e\u00a1\3\2\2\2"+
		"\u009f\u009d\3\2\2\2\u009f\u00a0\3\2\2\2\u00a0.\3\2\2\2\u00a1\u009f\3"+
		"\2\2\2\u00a2\u00a6\5\61\31\2\u00a3\u00a5\5\65\33\2\u00a4\u00a3\3\2\2\2"+
		"\u00a5\u00a8\3\2\2\2\u00a6\u00a4\3\2\2\2\u00a6\u00a7\3\2\2\2\u00a7\60"+
		"\3\2\2\2\u00a8\u00a6\3\2\2\2\u00a9\u00ab\t\2\2\2\u00aa\u00a9\3\2\2\2\u00ab"+
		"\62\3\2\2\2\u00ac\u00af\5\61\31\2\u00ad\u00af\4\62;\2\u00ae\u00ac\3\2"+
		"\2\2\u00ae\u00ad\3\2\2\2\u00af\64\3\2\2\2\u00b0\u00b3\5\61\31\2\u00b1"+
		"\u00b3\t\3\2\2\u00b2\u00b0\3\2\2\2\u00b2\u00b1\3\2\2\2\u00b3\66\3\2\2"+
		"\2\u00b4\u00b5\7&\2\2\u00b5\u00b6\5-\27\2\u00b68\3\2\2\2\u00b7\u00b8\5"+
		"/\30\2\u00b8\u00b9\7<\2\2\u00b9\u00ba\5/\30\2\u00ba\u00bb\7\61\2\2\u00bb"+
		"\u00bc\5/\30\2\u00bc:\3\2\2\2\u00bd\u00bf\t\4\2\2\u00be\u00bd\3\2\2\2"+
		"\u00bf\u00c0\3\2\2\2\u00c0\u00be\3\2\2\2\u00c0\u00c1\3\2\2\2\u00c1\u00c2"+
		"\3\2\2\2\u00c2\u00c3\b\36\2\2\u00c3<\3\2\2\2\n\2\u0099\u009f\u00a6\u00aa"+
		"\u00ae\u00b2\u00c0\3\b\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}