package io.pixee.tools;

import io.pixee.ast.Node;

/**
 * A message that relates to a node
 */
public class NodeRelatedMessage implements Message {

    private final Node node;
    private final String text;

    public NodeRelatedMessage(Node n, String text) {
        node = n;
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }
}
