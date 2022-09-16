package io.pixee.ast;

import io.pixee.lang.PropertyDescriptor;

/**
 * A special kind of child we insert into the tree if we cannot construct
 * it because of a misfit with the language definition
 */
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
