// Generated from DSL.g4 by ANTLR 4.10.1
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link DSLParser}.
 */
public interface DSLListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link DSLParser#type}.
	 * @param ctx the parse tree
	 */
	void enterType(DSLParser.TypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link DSLParser#type}.
	 * @param ctx the parse tree
	 */
	void exitType(DSLParser.TypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link DSLParser#rule_id}.
	 * @param ctx the parse tree
	 */
	void enterRule_id(DSLParser.Rule_idContext ctx);
	/**
	 * Exit a parse tree produced by {@link DSLParser#rule_id}.
	 * @param ctx the parse tree
	 */
	void exitRule_id(DSLParser.Rule_idContext ctx);
	/**
	 * Enter a parse tree produced by {@link DSLParser#match_replace}.
	 * @param ctx the parse tree
	 */
	void enterMatch_replace(DSLParser.Match_replaceContext ctx);
	/**
	 * Exit a parse tree produced by {@link DSLParser#match_replace}.
	 * @param ctx the parse tree
	 */
	void exitMatch_replace(DSLParser.Match_replaceContext ctx);
	/**
	 * Enter a parse tree produced by {@link DSLParser#match}.
	 * @param ctx the parse tree
	 */
	void enterMatch(DSLParser.MatchContext ctx);
	/**
	 * Exit a parse tree produced by {@link DSLParser#match}.
	 * @param ctx the parse tree
	 */
	void exitMatch(DSLParser.MatchContext ctx);
	/**
	 * Enter a parse tree produced by {@link DSLParser#replace}.
	 * @param ctx the parse tree
	 */
	void enterReplace(DSLParser.ReplaceContext ctx);
	/**
	 * Exit a parse tree produced by {@link DSLParser#replace}.
	 * @param ctx the parse tree
	 */
	void exitReplace(DSLParser.ReplaceContext ctx);
	/**
	 * Enter a parse tree produced by {@link DSLParser#clause}.
	 * @param ctx the parse tree
	 */
	void enterClause(DSLParser.ClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link DSLParser#clause}.
	 * @param ctx the parse tree
	 */
	void exitClause(DSLParser.ClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link DSLParser#where}.
	 * @param ctx the parse tree
	 */
	void enterWhere(DSLParser.WhereContext ctx);
	/**
	 * Exit a parse tree produced by {@link DSLParser#where}.
	 * @param ctx the parse tree
	 */
	void exitWhere(DSLParser.WhereContext ctx);
	/**
	 * Enter a parse tree produced by {@link DSLParser#start}.
	 * @param ctx the parse tree
	 */
	void enterStart(DSLParser.StartContext ctx);
	/**
	 * Exit a parse tree produced by {@link DSLParser#start}.
	 * @param ctx the parse tree
	 */
	void exitStart(DSLParser.StartContext ctx);
}