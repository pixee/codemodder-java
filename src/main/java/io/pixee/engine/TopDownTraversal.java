package io.pixee.engine;

import io.pixee.ast.Node;

import java.util.ArrayList;
import java.util.List;

/**
 * provides a top-down traversal/iterator through an AST
 */
public class TopDownTraversal {

    private final List<Node> nodes = new ArrayList<Node>();

    public TopDownTraversal(Node node) {
        collect(node);
    }

    public Iterable<Node> nodes() {
        return nodes;
    }

    private void collect(Node node) {
        nodes.add(node);
        node.childNodes().forEach(this::collect);
    }

}
