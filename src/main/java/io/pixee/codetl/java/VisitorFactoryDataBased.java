package io.pixee.codetl.java;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import io.pixee.codefixer.java.FileWeavingContext;
import io.pixee.codefixer.java.ObjectCreationTransformingModifierVisitor;
import io.pixee.codefixer.java.VisitorFactory;

import java.io.File;

public class VisitorFactoryDataBased implements VisitorFactory {
  private final VisitorFactoryData<ObjectCreationExpr> data;

  public VisitorFactoryDataBased() {
    this(new VisitorFactoryData<>());
  }

  public VisitorFactoryDataBased(VisitorFactoryData<ObjectCreationExpr> data) {
    this.data = data;
  }

  public VisitorFactoryData<ObjectCreationExpr> getData() {
    return data;
  }

  @Override
  public ModifierVisitor<FileWeavingContext> createJavaCodeVisitorFor(
      File file, CompilationUnit cu) {
    return new ObjectCreationTransformingModifierVisitor(
        cu, data.getPredicates(), data.getCreationExpr());
  }

  @Override
  public String ruleId() {
    // TODO: where does the rule id come from? DSL?
    return "abc";
  }
}
