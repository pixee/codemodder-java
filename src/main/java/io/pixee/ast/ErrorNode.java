package io.pixee.ast;

/**
 * A special kind of node we insert into the tree if we cannot construct
 * it because of a misfit with the language definition
 */
public class ErrorNode extends Node {

    private String problem;

    public ErrorNode(String problem) {
        super(null);
        this.problem = problem;
    }

    public String problem() {
        return problem;
    }

    @Override
    public String toString() {
        return "node/error:" + problem;
    }
}
