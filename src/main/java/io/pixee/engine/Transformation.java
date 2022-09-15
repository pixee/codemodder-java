package io.pixee.engine;

import io.pixee.ast.Node;

public abstract class Transformation {

    private final Node pattern;

    public Transformation(Node pattern) {
        this.pattern = pattern;
    }

    public PatternMatch match(Node n) {
        return new PatternMatch(pattern, n, pattern.concept == n.concept);
    }

    abstract void perform(PatternMatch match);

}
