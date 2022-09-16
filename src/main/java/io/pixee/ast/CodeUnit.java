package io.pixee.ast;

import io.pixee.lang.LanguageDescriptor;

/**
 * Represents a unit of code, typically a file. Owns one root node.
 */
public class CodeUnit {

    private LanguageDescriptor language;
    public final Node root;

    public CodeUnit(LanguageDescriptor lang, Node root) {
        language = lang;
        this.root = root;
    }

    public CodeUnit resolve() {
        this.root.resolve(language, null);
        return this;
    }

    @Override
    public String toString() {
        return root.dump("");
    }

}
