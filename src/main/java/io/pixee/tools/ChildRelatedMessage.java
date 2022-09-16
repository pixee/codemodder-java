package io.pixee.tools;

import io.pixee.ast.Child;
import io.pixee.ast.Node;

public class ChildRelatedMessage implements Message {

    private Child child;
    private String text;

    public ChildRelatedMessage(Child c, String text) {
        child = c;
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }
}
