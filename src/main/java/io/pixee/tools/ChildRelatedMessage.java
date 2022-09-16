package io.pixee.tools;

import io.pixee.ast.Child;
import io.pixee.ast.Node;

/**
 * A message that relates to a Child
 */
public class ChildRelatedMessage implements Message {

    private final Child child;
    private final String text;

    public ChildRelatedMessage(Child c, String text) {
        child = c;
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }
}
