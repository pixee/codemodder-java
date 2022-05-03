package io.pixee.codefixer.java;

import com.github.javaparser.ast.CompilationUnit;

import java.io.File;

public interface VisitorFactoryNg {

    MethodCallTransformingModifierVisitor createVisitor(File javaFile, CompilationUnit cu);

    String ruleId();
}
