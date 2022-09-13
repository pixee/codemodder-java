package io.pixee.codetl.java;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import io.pixee.codefixer.java.FileWeavingContext;
import io.pixee.codefixer.java.MethodCallTransformingModifierVisitor;
import io.pixee.codefixer.java.ObjectCreationTransformingModifierVisitor;
import io.pixee.codefixer.java.Transformer;
import io.pixee.codefixer.java.VisitorFactory;

import java.io.File;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DSLFactoryDataBased implements VisitorFactory {
    private final DSLFactoryData data;

    public DSLFactoryDataBased() {
        this(new DSLFactoryData());
    }

    public DSLFactoryDataBased(DSLFactoryData data) {
        this.data = data;
    }

    public DSLFactoryData getData() {
        return data;
    }

    @Override
    public ModifierVisitor<FileWeavingContext> createJavaCodeVisitorFor(
            File file, CompilationUnit cu) {

        Stream<? extends Predicate<?>> predicatesStream = data
                .getPredicates()
                .stream()
                .map(f -> f.apply(cu));
        switch (data.getTransformationType()) {
            case OBJECT:
                return new ObjectCreationTransformingModifierVisitor(
                        cu,
                        predicatesStream
                                .map(p -> (Predicate<ObjectCreationExpr>) p)
                                .collect(Collectors.toList()),
                        (Transformer<ObjectCreationExpr, ObjectCreationExpr>) data.getTransformer()
                );
            case METHOD:
                return new MethodCallTransformingModifierVisitor(cu,
                        predicatesStream
                                .map(p -> (Predicate<MethodCallExpr>) p)
                                .collect(Collectors.toList()),
                        (Transformer<MethodCallExpr, MethodCallExpr>) data.getTransformer()
                );
        }
        throw new IllegalStateException("Don't know how to create visitor");
    }

    @Override
    public String ruleId() {
        return getData().getRuleId();
    }
}
