package io.pixee.codetl;

import io.pixee.ast.Node;
import io.pixee.lang.ConceptDescriptor;
import org.antlr.v4.runtime.tree.ParseTree;

/**
 * {@inheritDoc}
 * <p>
 * Responsible for the Java language!
 */
final class JavaLanguageProvider implements SubjectLanguageProvider {

    @Override
    public Node parseMatchNode(final ParseTree node) {
        String languageNodeType = node.getChild(0).getText();
        if ("StaticMethodCall".equals(languageNodeType)) {
            return parseStaticMethodCall(node);
        }
        throw new UnsupportedOperationException();
    }

    /**
     * Turn a <code>StaticMethodCall</code> into a parsed node.
     */
    private StaticMethodCall parseStaticMethodCall(final ParseTree methodCall) {
        return new StaticMethodCall();
    }

    @Override
    public Node parseReplacementNode(final ParseTree node) {
        String languageNodeType = node.getChild(0).getText();
        if ("StaticMethodCall".equals(languageNodeType)) {
            return parseStaticMethodCall(node);
        }
        throw new UnsupportedOperationException();
    }

    private static class StaticMethodCall extends Node {
        private StaticMethodCall() {
            super(new ConceptDescriptor("StaticMethodCall"));
        }
    }
}
