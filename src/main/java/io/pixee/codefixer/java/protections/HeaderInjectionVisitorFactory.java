package io.pixee.codefixer.java.protections;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import io.pixee.security.*;
import io.pixee.codefixer.java.FileWeavingContext;
import io.pixee.codefixer.java.TypeLocator;
import io.pixee.codefixer.java.VisitorFactory;
import io.pixee.codefixer.java.Weave;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * This visitor prevents header injection by making sure any non-constant header values are stripped
 * of newlines.
 */
public final class HeaderInjectionVisitorFactory implements VisitorFactory {

  @Override
  public ModifierVisitor<FileWeavingContext> createJavaCodeVisitorFor(
      final File file, final CompilationUnit cu) {
    //return new HeaderInjectionVisitor(cu);
    throw new UnsupportedOperationException("moved to ng");
  }

    @Override
    public String ruleId() {
        return stripHeaderRuleId;
    }

    private static class HeaderInjectionVisitor extends ModifierVisitor<FileWeavingContext> {
    private final TypeLocator resolver;
    private final List<HeaderSettingDetector> headerProtectors;

    private HeaderInjectionVisitor(final CompilationUnit cu) {
      this.resolver = TypeLocator.createDefault(cu);
      this.headerProtectors = List.of(new J2EEHeaderSettingDetector());
    }

    @Override
    public Visitable visit(final MethodCallExpr methodCallExpr, final FileWeavingContext context) {
      if (context.isLineIncluded(methodCallExpr)) {
        for (HeaderSettingDetector detector : headerProtectors) {
          detector.findHeaderSettingCall(methodCallExpr, resolver, context);
        }
      }
      return super.visit(methodCallExpr, context);
    }
  }

  interface HeaderSettingDetector {
    void findHeaderSettingCall(
        MethodCallExpr expressionStmt, TypeLocator resolver, FileWeavingContext context);
  }

  private static class J2EEHeaderSettingDetector implements HeaderSettingDetector {

    @Override
    public void findHeaderSettingCall(
        final MethodCallExpr methodCallExpr,
        final TypeLocator resolver,
        final FileWeavingContext context) {
      if ("setHeader".equals(methodCallExpr.getNameAsString())
          && methodCallExpr.getArguments().size() == 2) {
        Optional<Expression> scope = methodCallExpr.getScope();
        if (scope.isPresent()) {
          final Expression expression = scope.get();
          final String typeName = resolver.locateType(expression);
          if ("javax.servlet.http.HttpServletResponse".equals(typeName)) {
            final Expression argument = methodCallExpr.getArgument(1);
            if (!(argument instanceof StringLiteralExpr)) {
              if (argument.isNameExpr() && !looksLikeConstant(argument.toString())) {
                final MethodCallExpr stripNewlinesCall =
                    new MethodCallExpr(callbackClass, "stripNewlines");
                stripNewlinesCall.setArguments(NodeList.nodeList(argument));
                methodCallExpr.setArguments(
                    NodeList.nodeList(methodCallExpr.getArgument(0), stripNewlinesCall));
                context.addWeave(
                    Weave.from(methodCallExpr.getRange().get().begin.line, stripHeaderRuleId));
              }
            }
          }
        }
      }
    }

    private boolean looksLikeConstant(final String variableName) {
      return commonConstantPattern.matcher(variableName).matches();
    }
  }

  private static final Pattern commonConstantPattern = Pattern.compile("[A-Z_]{2,}");
  private static final NameExpr callbackClass = new NameExpr(HttpHeader.class.getName());
  private static final String stripHeaderRuleId = "pixee:java/strip-http-header";
}
