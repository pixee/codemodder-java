package io.pixee.engine;

import io.pixee.ast.*;
import io.pixee.codetl.CodeTLRuleDefinition;

import java.util.ArrayList;
import java.util.Collection;
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
        return new PatternMatch(pattern, candidate, matchNode(pattern,candidate));
    }

    protected boolean matchNode(Node pattern, Node candidate) {
        if (pattern.concept != candidate.concept) return false;
        for (Child patternChild: pattern.children()) {
            Collection<Child> candChildren = candidate.childrenFor(patternChild.role());
            if (candChildren.isEmpty()) return false;
            if (!candChildren.stream().anyMatch(it -> matchChild(patternChild, it))) {
                return false;
            }
        }
        return true;
    }

    protected boolean matchChild(Child pattern, Child candidate) {
        if (pattern.role() != candidate.role()) return false;
        return matchData(pattern.data(), candidate.data());
    }

    private boolean matchData(Data pattern, Data candidate) {
        if (pattern instanceof Node pattNode && candidate instanceof Node candNode) {
            return matchNode(pattNode, candNode);
        }
        if (pattern instanceof Value pattVal && candidate instanceof Value candVal) {
            return pattVal.equals(candVal);
        }
        return false;
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
