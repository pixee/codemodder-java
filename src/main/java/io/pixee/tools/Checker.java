package io.pixee.tools;

import io.pixee.ast.Node;

/**
 * Interfaces for any kind of checker that produces messages
 */
public interface Checker {

    Iterable<Message> execute(Node n, boolean isUsedAsPattern);

}
