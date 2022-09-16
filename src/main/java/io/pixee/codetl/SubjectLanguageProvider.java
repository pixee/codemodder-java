package io.pixee.codetl;

import io.pixee.ast.Node;
import org.antlr.v4.runtime.tree.ParseTree;

/**
 * This type is responsible for parsing the ANTLR model objects into CodeTL node objects.
 */
interface SubjectLanguageProvider {

    Node parseMatchNode(ParseTree node);

    Node parseReplacementNode(ParseTree node);
}
