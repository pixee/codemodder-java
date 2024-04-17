package io.codemodder.codemods;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.BinaryExpr.Operator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import io.codemodder.*;
import io.codemodder.ast.ASTTransforms;
import io.codemodder.javaparser.JavaParserChanger;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/** Parameterize possible injections for Hibernate queries. */
@Codemod(
    id = "pixee:java/hql-parameterizer",
    importance = Importance.HIGH,
    reviewGuidance = ReviewGuidance.MERGE_AFTER_REVIEW)
public final class HQLParameterizationCodemod extends JavaParserChanger {

  private Optional<CodemodChange> onNodeFound(
      final CodemodInvocationContext context,
      final MethodCallExpr methodCallExpr,
      final CompilationUnit cu) {
    if (isQueryCreation(methodCallExpr)) {
      final var queryParameterizer = new QueryParameterizer(methodCallExpr.getArgument(0));
      if (!queryParameterizer.getInjections().isEmpty()) {
        fix(methodCallExpr, queryParameterizer);
        var maybeMethodDecl = methodCallExpr.findAncestor(CallableDeclaration.class);
        // Cleanup, removes empty string concatenations and unused variables
        maybeMethodDecl.ifPresent(cd -> ASTTransforms.removeEmptyStringConcatenation(cd));
        // Remove potential unused variables left after transform
        maybeMethodDecl.ifPresent(md -> ASTTransforms.removeUnusedLocalVariables(md));
        return Optional.of(CodemodChange.from(methodCallExpr.getRange().get().begin.line));
      }
    }
    return Optional.empty();
  }

  @Override
  public CodemodFileScanningResult visit(
      final CodemodInvocationContext context, final CompilationUnit cu) {
    List<CodemodChange> changes =
        cu.findAll(MethodCallExpr.class).stream()
            .flatMap(mce -> onNodeFound(context, mce, cu).stream())
            .collect(Collectors.toList());
    return CodemodFileScanningResult.withOnlyChanges(changes);
  }

  private static final String queryParameterNamePrefix = ":parameter";

  private boolean isQueryCreation(final MethodCallExpr methodCallExpr) {
    final Predicate<MethodCallExpr> isQueryCall =
        mce ->
            mce.getNameAsString().equals("createQuery")
                || mce.getNameAsString().equals("createNativeQuery");
    // TODO Removed for now as it's failing to properly resolve Session type here, look for
    // solutions
    final Predicate<MethodCallExpr> isQueryFactory =
        mce ->
            mce.getScope()
                .filter(s -> s.calculateResolvedType().describe().equals("org.hibernate.Session"))
                .isPresent();

    // return isQueryCall.and(isQueryFactory).test(methodCallExpr);
    return isQueryCall.test(methodCallExpr);
  }

  private List<Expression> fixInjections(
      final List<Deque<Expression>> injections, Map<Expression, Expression> resolvedMap) {
    final List<Expression> combinedExpressions = new ArrayList<>();
    int count = 0;
    for (final var injection : injections) {
      // fix start
      final var start = injection.removeFirst();
      final var startString = start.asStringLiteralExpr().getValue();
      final var builder = new StringBuilder(startString);
      final int lastQuoteIndex = startString.lastIndexOf('\'') + 1;
      final var prepend = startString.substring(lastQuoteIndex);
      builder.replace(lastQuoteIndex - 1, startString.length(), queryParameterNamePrefix + count);
      start.asStringLiteralExpr().setValue(builder.toString());

      // fix end
      final var end = injection.removeLast();
      final var endString = end.asStringLiteralExpr().getValue();
      final int firstQuoteIndex = endString.indexOf('\'');
      final var newEnd = end.asStringLiteralExpr().getValue().substring(firstQuoteIndex + 1);
      final var append = endString.substring(0, firstQuoteIndex);
      end.asStringLiteralExpr().setValue(newEnd);

      // build expression for parameters
      var combined = combineExpressions(injection, resolvedMap);
      // add the suffix of start
      if (prepend != "") {
        final var newCombined =
            new BinaryExpr(new StringLiteralExpr(prepend), combined, Operator.PLUS);
        combined = newCombined;
      }
      // add the prefix of end
      if (append != "") {
        final var newCombined =
            new BinaryExpr(combined, new StringLiteralExpr(append), Operator.PLUS);
        combined = newCombined;
      }
      combinedExpressions.add(combined);
      count++;
    }
    return combinedExpressions;
  }

  private Expression combineExpressions(
      final Deque<Expression> injectionExpressions, Map<Expression, Expression> resolutionMap) {
    final var it = injectionExpressions.iterator();
    Expression combined = it.next();
    boolean atLeastOneString = false;
    try {
      atLeastOneString = combined.calculateResolvedType().describe().equals("java.lang.String");
    } catch (final Exception ignored) {
    }
    unresolve(combined, resolutionMap).replace(new StringLiteralExpr(""));

    while (it.hasNext()) {
      final var expr = it.next();
      try {
        if (!atLeastOneString
            && expr.calculateResolvedType().describe().equals("java.lang.String")) {

          atLeastOneString = true;
        }
      } catch (final Exception ignored) {
      }
      unresolve(expr, resolutionMap).replace(new StringLiteralExpr(""));
      combined = new BinaryExpr(combined, expr, Operator.PLUS);
    }
    if (atLeastOneString) return combined;
    else return new BinaryExpr(combined, new StringLiteralExpr(""), Operator.PLUS);
  }

  private Expression unresolve(
      final Expression expr, final Map<Expression, Expression> resolutionMap) {
    Expression unresolved = expr;
    while (resolutionMap.get(unresolved) != null) {
      unresolved = resolutionMap.get(unresolved);
    }
    return unresolved;
  }

  private void fix(final MethodCallExpr queryCall, final QueryParameterizer queryParameterizer) {

    final var injections = queryParameterizer.getInjections();
    final var combinedExpressions =
        fixInjections(
            injections, queryParameterizer.getLinearizedQuery().getResolvedExpressionsMap());

    // query.setParameter() for each injection
    var call = queryCall;
    for (int i = 0; i < combinedExpressions.size(); i++) {
      final var newCall = new MethodCallExpr();
      call.replace(newCall);
      newCall.setScope(call);
      newCall.setName("setParameter");
      newCall.setArguments(
          new NodeList<>(
              new StringLiteralExpr(queryParameterNamePrefix + i), combinedExpressions.get(i)));
      call = newCall;
    }
    queryCall.setArgument(0, queryParameterizer.getRoot());
  }
}
