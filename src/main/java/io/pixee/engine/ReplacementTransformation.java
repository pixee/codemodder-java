package io.pixee.engine;

import io.pixee.ast.Node;

public class ReplacementTransformation extends Transformation {

    private final Node replacement;

    public ReplacementTransformation(Node pattern, Node replacement) {
        super(pattern);
        this.replacement = replacement;
    }

    @Override
    void perform(PatternMatch match) {
        Node matched = match.matchedNode;
        matched.parent().replaceChild(matched, replacement);
    }
}
