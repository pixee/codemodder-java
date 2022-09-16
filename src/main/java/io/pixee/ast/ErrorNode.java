package io.pixee.ast;

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
