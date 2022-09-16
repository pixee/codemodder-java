package io.pixee.ast;

import io.pixee.lang.PropertyDescriptor;

public class ErrorChild extends Child {

    private String problem;

    public ErrorChild(String problem) {
        super((PropertyDescriptor) null, null);
        this.problem = problem;
    }

    public String problem() {
        return problem;
    }

    @Override
    public String toString() {
        return "child/error:" + problem;
    }
}
