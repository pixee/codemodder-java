package io.pixee.tools;

import io.pixee.ast.Node;

public interface Checker {

    Iterable<Message> run(Node n);

}
