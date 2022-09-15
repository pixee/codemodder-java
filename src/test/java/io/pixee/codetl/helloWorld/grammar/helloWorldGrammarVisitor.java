// Generated from /Users/markusvoelter/Documents/projects/pixee/codetl/src/test/java/io/pixee/codetl/helloWorld/grammar/helloWorldGrammar.g4 by ANTLR 4.10.1
package io.pixee.codetl.helloWorld.grammar;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link helloWorldGrammarParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface helloWorldGrammarVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link helloWorldGrammarParser#program}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProgram(helloWorldGrammarParser.ProgramContext ctx);
	/**
	 * Visit a parse tree produced by {@link helloWorldGrammarParser#variable}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariable(helloWorldGrammarParser.VariableContext ctx);
	/**
	 * Visit a parse tree produced by {@link helloWorldGrammarParser#numlit}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNumlit(helloWorldGrammarParser.NumlitContext ctx);
}