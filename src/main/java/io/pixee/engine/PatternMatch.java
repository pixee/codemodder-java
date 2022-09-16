package io.pixee.engine;

import io.pixee.ast.Node;

/**
 * Represents a successful or non-succesful match of a pattern
 * against a node in the AST
 */
public class PatternMatch {

    public final Node pattern;
    public final Node matchedNode;
    public final boolean hasMatched;

    public PatternMatch(Node pattern, Node matchedNode, boolean hasMatched) {
        this.pattern = pattern;
        this.matchedNode = matchedNode;
        this.hasMatched = hasMatched;
    }

    public boolean hasMatched() {
        return hasMatched;
    }
}
