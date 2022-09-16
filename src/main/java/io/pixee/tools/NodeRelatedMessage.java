package io.pixee.tools;

import io.pixee.ast.Node;

public class NodeRelatedMessage implements Message {

    private Node node;
    private String text;

    public NodeRelatedMessage(Node n, String text) {
        node = n;
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }
}
