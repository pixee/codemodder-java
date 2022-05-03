package io.pixee.codefixer.java;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.visitor.ModifierVisitor;

import java.io.File;

public interface VisitorFactoryNg {

    ModifierVisitor<FileWeavingContext> createVisitor(File javaFile, CompilationUnit cu);

    String ruleId();
}
