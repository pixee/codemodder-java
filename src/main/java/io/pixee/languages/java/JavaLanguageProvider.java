package io.pixee.languages.java;

import io.pixee.ast.Node;
import io.pixee.codetl.SubjectLanguageProvider;
import io.pixee.lang.ConceptDescriptor;
import io.pixee.tools.Checker;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.ArrayList;

/**
 * {@inheritDoc}
 * MV: I think we don't need this
 * <p>
 * Responsible for the Java language!
 */
public final class JavaLanguageProvider implements SubjectLanguageProvider {

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

    @Override
    public Iterable<Checker> checkers() {
        return new ArrayList<>();
    }

    private static class StaticMethodCall extends Node {
        private StaticMethodCall() {
            super(new ConceptDescriptor("StaticMethodCall"));
        }
    }
}
