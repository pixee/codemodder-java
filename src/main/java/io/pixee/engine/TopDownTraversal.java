package io.pixee.engine;

import io.pixee.ast.Node;

import java.util.ArrayList;
import java.util.List;

public class TopDownTraversal {

    private List<Node> nodes = new ArrayList<Node>();

    public TopDownTraversal(Node node) {
        collect(node);
    }

    public Iterable<Node> nodes() {
        return nodes;
    }

    private void collect(Node node) {
        nodes.add(node);
        node.childNodes().stream().forEach(it -> collect(it));
    }

}
