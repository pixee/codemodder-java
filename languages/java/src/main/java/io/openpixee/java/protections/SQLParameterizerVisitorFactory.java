package io.openpixee.java.protections;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import io.codemodder.FileWeavingContext;
import io.codemodder.Weave;
import io.openpixee.java.VisitorFactory;
import io.openpixee.jdbcparameterizer.SQLParameterizer;
import java.io.File;

/** Protects against user data SQL injections by parameterizing the query call. */
public final class SQLParameterizerVisitorFactory implements VisitorFactory {

  private static class SQLParameterizerVisitor extends ModifierVisitor<FileWeavingContext> {

    private final CompilationUnit cu;

    private SQLParameterizerVisitor(CompilationUnit cu) {
      this.cu = cu;
    }

    @Override
    public Visitable visit(final MethodCallExpr methodCallExpr, final FileWeavingContext context) {
      if (SQLParameterizer.isParameterizationCandidate(methodCallExpr)) {
        var fixer = new SQLParameterizer(this.cu);
        var maybeChanges =
            fixer.parameterizeStatement(methodCallExpr, methodCallExpr.getArgument(0));
        if (maybeChanges.isLeft()) {
          for (var c : maybeChanges.getLeft()) {
            // Create Weave based on Change
            context.addWeave(Weave.from(c.getLine(), sqlParameterizerRuleId));
          }
          return methodCallExpr;
        }
      }
      return super.visit(methodCallExpr, context);
    }
  }

  @Override
  public ModifierVisitor<FileWeavingContext> createJavaCodeVisitorFor(
      final File file, final CompilationUnit cu) {

    return new SQLParameterizerVisitor(cu);
  }

  @Override
  public String ruleId() {
    return sqlParameterizerRuleId;
  }

  // 1 MB
  private static final String sqlParameterizerRuleId = "pixee:java/sql-parameterizer";
}
