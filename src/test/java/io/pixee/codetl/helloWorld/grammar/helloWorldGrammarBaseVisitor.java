// Generated from /Users/markusvoelter/Documents/projects/pixee/codetl/src/test/java/io/pixee/codetl/helloWorld/grammar/helloWorldGrammar.g4 by ANTLR 4.10.1
package io.pixee.codetl.helloWorld.grammar;
import org.antlr.v4.runtime.tree.AbstractParseTreeVisitor;

/**
 * This class provides an empty implementation of {@link helloWorldGrammarVisitor},
 * which can be extended to create a visitor which only needs to handle a subset
 * of the available methods.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public class helloWorldGrammarBaseVisitor<T> extends AbstractParseTreeVisitor<T> implements helloWorldGrammarVisitor<T> {
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	@Override public T visitProgram(helloWorldGrammarParser.ProgramContext ctx) { return visitChildren(ctx); }
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	@Override public T visitVariable(helloWorldGrammarParser.VariableContext ctx) { return visitChildren(ctx); }
	/**
	 * {@inheritDoc}
	 *
	 * <p>The default implementation returns the result of calling
	 * {@link #visitChildren} on {@code ctx}.</p>
	 */
	@Override public T visitNumlit(helloWorldGrammarParser.NumlitContext ctx) { return visitChildren(ctx); }
}