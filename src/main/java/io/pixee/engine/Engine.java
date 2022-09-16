package io.pixee.engine;

import io.pixee.ast.CodeUnit;
import io.pixee.ast.Node;
import io.pixee.codetl.CodeTLRuleDefinition;

import java.util.ArrayList;
import java.util.List;

/**
 * Runs a set of transformations against a code unit
 */
public class Engine {

    private final List<CodeTLRuleDefinition> rules = new ArrayList<>();

    public void registerRule(CodeTLRuleDefinition rule) {
        this.rules.add(rule);
    }

    protected PatternMatch match(Node pattern, Node candidate) {
        return new PatternMatch(pattern, candidate, pattern.concept == candidate.concept);
    }

    protected void performReplacement(Node matched, Node replacement) {
        matched.parent().replaceChild(matched, replacement);
    }


    public void transformWithRules(CodeUnit code) {
        for (Node n: new TopDownTraversal(code.root).nodes()) {
            for (CodeTLRuleDefinition r: rules) {
                PatternMatch match = match(r.getNodeToMatch(), n);
                if (match.hasMatched()) {
                    performReplacement(match.matchedNode, r.getReplacementNode().copy());
                }
            }
        }
    }


}
