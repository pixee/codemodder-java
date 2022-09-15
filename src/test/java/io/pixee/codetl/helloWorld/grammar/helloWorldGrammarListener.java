// Generated from /Users/markusvoelter/Documents/projects/pixee/codetl/src/test/java/io/pixee/codetl/helloWorld/grammar/helloWorldGrammar.g4 by ANTLR 4.10.1
package io.pixee.codetl.helloWorld.grammar;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link helloWorldGrammarParser}.
 */
public interface helloWorldGrammarListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link helloWorldGrammarParser#program}.
	 * @param ctx the parse tree
	 */
	void enterProgram(helloWorldGrammarParser.ProgramContext ctx);
	/**
	 * Exit a parse tree produced by {@link helloWorldGrammarParser#program}.
	 * @param ctx the parse tree
	 */
	void exitProgram(helloWorldGrammarParser.ProgramContext ctx);
	/**
	 * Enter a parse tree produced by {@link helloWorldGrammarParser#variable}.
	 * @param ctx the parse tree
	 */
	void enterVariable(helloWorldGrammarParser.VariableContext ctx);
	/**
	 * Exit a parse tree produced by {@link helloWorldGrammarParser#variable}.
	 * @param ctx the parse tree
	 */
	void exitVariable(helloWorldGrammarParser.VariableContext ctx);
	/**
	 * Enter a parse tree produced by {@link helloWorldGrammarParser#numlit}.
	 * @param ctx the parse tree
	 */
	void enterNumlit(helloWorldGrammarParser.NumlitContext ctx);
	/**
	 * Exit a parse tree produced by {@link helloWorldGrammarParser#numlit}.
	 * @param ctx the parse tree
	 */
	void exitNumlit(helloWorldGrammarParser.NumlitContext ctx);
}