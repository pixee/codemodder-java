package io.pixee.engine;

import io.pixee.ast.CodeUnit;
import io.pixee.ast.Node;

import java.util.ArrayList;
import java.util.List;

public class Engine {

    private final List<Transformation> transformations = new ArrayList<Transformation>();

    public void registerTransformation(Transformation tx) {
        this.transformations.add(tx);
    }

    public void transform(CodeUnit code) {
        for (Node n: new TopDownTraversal(code.root).nodes()) {
            for (Transformation tf: transformations) {
                PatternMatch match = tf.match(n);
                if (match.hasMatched()) {
                    tf.perform(match);
                }
            }
        }
    }


}
