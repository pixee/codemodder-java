package io.openpixee.java.protections;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import io.codemodder.FileWeavingContext;
import io.codemodder.Weave;
import io.openpixee.java.MethodCallPredicateFactory;
import io.openpixee.java.MethodCallTransformingModifierVisitor;
import io.openpixee.java.Transformer;
import io.openpixee.java.VisitorFactory;
import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Targets making sure the protocols set in {@link javax.net.ssl.SSLContext#getInstance(String)} are
 * safe.
 */
public final class SSLContextGetInstanceVisitorFactory implements VisitorFactory {

  @Override
  public ModifierVisitor<FileWeavingContext> createJavaCodeVisitorFor(
      final File file, final CompilationUnit cu) {

    Predicate<MethodCallExpr> isUnsafeArgument =
        new Predicate<MethodCallExpr>() {
          @Override
          public boolean test(final MethodCallExpr methodCallExpr) {
            StringLiteralExpr argument = methodCallExpr.getArgument(0).asStringLiteralExpr();
            return SSLProtocols.isUnsafeStringLiteral.test(argument);
          }
        };

    Predicate<MethodCallExpr> isUnsafeVariable =
        new Predicate<MethodCallExpr>() {
          @Override
          public boolean test(final MethodCallExpr methodCallExpr) {
            NameExpr variableName = methodCallExpr.getArgument(0).asNameExpr();
            return SSLProtocols.isUnsafeStringVariable.test(variableName);
          }
        };

    List<Predicate<MethodCallExpr>> predicates =
        List.of(
            MethodCallPredicateFactory.withName("getInstance"),
            MethodCallPredicateFactory.withArgumentCount(1),
            MethodCallPredicateFactory.withScopeType(cu, "javax.net.ssl.SSLContext")
                .or(MethodCallPredicateFactory.withScopeType(cu, "SSLContext")),
            (MethodCallPredicateFactory.withArgumentNodeType(0, StringLiteralExpr.class)
                    .and(isUnsafeArgument))
                .or(
                    MethodCallPredicateFactory.withArgumentNodeType(0, NameExpr.class)
                        .and(isUnsafeVariable)));

    Transformer<MethodCallExpr, MethodCallExpr> transformer =
        new Transformer<>() {
          @Override
          public TransformationResult<MethodCallExpr> transform(
              final MethodCallExpr methodCallExpr, final FileWeavingContext context) {
            final StringLiteralExpr safeArgument = new StringLiteralExpr(safeTlsVersion);
            methodCallExpr.setArguments(NodeList.nodeList(safeArgument));
            Weave weave =
                Weave.from(methodCallExpr.getRange().get().begin.line, tlsVersionUpgradeRuleId);
            return new TransformationResult<>(Optional.empty(), weave);
          }
        };

    return new MethodCallTransformingModifierVisitor(cu, predicates, transformer);
  }

  @Override
  public String ruleId() {
    return tlsVersionUpgradeRuleId;
  }

  private static final String tlsVersionUpgradeRuleId = "pixee:java/upgrade-sslcontext-tls";
  private static final String safeTlsVersion = "TLSv1.2";
}
