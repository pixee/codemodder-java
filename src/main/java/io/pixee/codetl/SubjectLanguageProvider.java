package io.pixee.codetl;

import io.pixee.ast.Node;
import io.pixee.tools.Checker;
import org.antlr.v4.runtime.tree.ParseTree;

/**
 * This type is responsible for parsing the ANTLR model objects into CodeTL node objects for a given language.
 */
public interface SubjectLanguageProvider {

    Node parseMatchNode(ParseTree tree);

    Node parseReplacementNode(ParseTree tree);

    Iterable<Checker> checkers();
}
