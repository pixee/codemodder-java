package io.openpixee.java.protections;

import com.github.javaparser.ast.ArrayCreationLevel;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.ArrayCreationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import io.codemodder.Weave;
import io.openpixee.java.FileWeavingContext;
import io.openpixee.java.MethodCallPredicateFactory;
import io.openpixee.java.MethodCallTransformingModifierVisitor;
import io.openpixee.java.Transformer;
import io.openpixee.java.VisitorFactory;
import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * This visitor prevents the use of SSL protocols that are considered unsafe by modern standards.
 */
abstract class SSLProtocolsVisitorFactory implements VisitorFactory {

  private final String methodName;
  private final String typeName;
  private final String fullyQualifiedTypeName;

  SSLProtocolsVisitorFactory(
      final String methodName, final String typeName, final String fullyQualifiedTypeName) {
    this.methodName = Objects.requireNonNull(methodName);
    this.typeName = Objects.requireNonNull(typeName);
    this.fullyQualifiedTypeName = Objects.requireNonNull(fullyQualifiedTypeName);
  }

  @Override
  public ModifierVisitor<FileWeavingContext> createJavaCodeVisitorFor(
      final File file, final CompilationUnit cu) {

    List<Predicate<MethodCallExpr>> predicates =
        List.of(
            MethodCallPredicateFactory.withName(methodName),
            MethodCallPredicateFactory.withArgumentCount(1),
            MethodCallPredicateFactory.withScopeType(cu, typeName)
                .or(MethodCallPredicateFactory.withScopeType(cu, fullyQualifiedTypeName)),
            (MethodCallPredicateFactory.withArgumentNodeType(0, ArrayCreationExpr.class)
                    .and(SSLProtocols.hasUnsafeArrayArgument))
                .or(
                    MethodCallPredicateFactory.withArgumentNodeType(0, NameExpr.class)
                        .and(SSLProtocols.hasUnsafeArrayArgumentVariable)));

    Transformer<MethodCallExpr, MethodCallExpr> transformer =
        new Transformer<>() {
          @Override
          public TransformationResult<MethodCallExpr> transform(
              final MethodCallExpr methodCallExpr, final FileWeavingContext context) {
            final ArrayCreationExpr safeArgument =
                new ArrayCreationExpr(new ClassOrInterfaceType("String"));
            safeArgument.setLevels(NodeList.nodeList(new ArrayCreationLevel()));
            safeArgument.setInitializer(
                new ArrayInitializerExpr(
                    NodeList.nodeList(new StringLiteralExpr(SSLProtocols.safeTlsVersion))));
            methodCallExpr.setArguments(NodeList.nodeList(safeArgument));
            Weave weave = Weave.from(methodCallExpr.getRange().get().begin.line, ruleId());
            return new TransformationResult<>(Optional.empty(), weave);
          }
        };

    return new MethodCallTransformingModifierVisitor(cu, predicates, transformer);
  }
}
