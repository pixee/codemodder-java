package io.codemodder.codemods;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.BinaryExpr.Operator;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import io.codemodder.*;
import io.codemodder.ast.ASTs;
import io.codemodder.javaparser.JavaParserChanger;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.javatuples.Pair;

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
        return Optional.of(CodemodChange.from(methodCallExpr.getRange().get().begin.line));
      }
    }
    return Optional.empty();
  }

  @Override
  public List<CodemodChange> visit(
      final CodemodInvocationContext context, final CompilationUnit cu) {
    return cu.findAll(MethodCallExpr.class).stream()
        .flatMap(mce -> onNodeFound(context, mce, cu).stream())
        .collect(Collectors.toList());
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

  /** Removes an expression from an expression subtree. */
  private Expression collapse(final Expression e, final Expression root) {
    final var p = e.getParentNode().get();
    if (p instanceof BinaryExpr) {
      if (e.equals(((BinaryExpr) p).getLeft())) {
        final var child = ((BinaryExpr) p).getRight();
        if (p.equals(root)) {
          return child;
        } else {
          p.replace(child);
          return root;
        }
      }
      if (e.equals(((BinaryExpr) p).getRight())) {
        final var child = ((BinaryExpr) p).getLeft();
        if (p.equals(root)) {
          return child;
        } else {
          p.replace(child);
          return root;
        }
      }
    } else if (p instanceof EnclosedExpr) {
      return collapse((Expression) p, root);
    }
    e.remove();
    return root;
  }

  private Pair<List<Expression>, Expression> fixInjections(
      final List<Deque<Expression>> injections, Expression root) {
    final List<Expression> combinedExpressions = new ArrayList<>();
    int count = 0;
    for (final var injection : injections) {
      final var start = injection.removeFirst();
      final var startString = start.asStringLiteralExpr().getValue();
      final var builder = new StringBuilder(startString);
      builder.replace(
          startString.length() - 1, startString.length(), queryParameterNamePrefix + count);
      start.asStringLiteralExpr().setValue(builder.toString());

      final var end = injection.removeLast();
      final var newEnd = end.asStringLiteralExpr().getValue().substring(1);
      if (newEnd.equals("")) {
        root = collapse(end, root);
      } else {
        end.asStringLiteralExpr().setValue(newEnd);
      }
      final var pair = combineExpressions(injection, root);
      combinedExpressions.add(pair.getValue0());
      root = pair.getValue1();
      count++;
    }
    return new Pair<>(combinedExpressions, root);
  }

  private Pair<Expression, Expression> combineExpressions(
      final Deque<Expression> injectionExpressions, Expression root) {
    final var it = injectionExpressions.iterator();
    Expression combined = it.next();
    boolean atLeastOneString = false;
    try {
      atLeastOneString = combined.calculateResolvedType().describe().equals("java.lang.String");
    } catch (final Exception ignored) {
    }
    root = collapse(combined, root);

    while (it.hasNext()) {
      final var expr = it.next();
      try {
        if (!atLeastOneString
            && expr.calculateResolvedType().describe().equals("java.lang.String")) {

          atLeastOneString = true;
        }
      } catch (final Exception ignored) {
      }
      root = collapse(expr, root);
      combined = new BinaryExpr(combined, expr, Operator.PLUS);
    }
    if (atLeastOneString) return new Pair<>(combined, root);
    else
      return new Pair<>(new BinaryExpr(combined, new StringLiteralExpr(""), Operator.PLUS), root);
  }

  private void fix(final MethodCallExpr queryCall, final QueryParameterizer queryParameterizer) {

    final var injections = queryParameterizer.getInjections();
    var root = queryParameterizer.getRoot();
    final var pair = fixInjections(injections, root);
    root = pair.getValue1();
    final var combinedExpressions = pair.getValue0();

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

    // Deleting some expressions may result in some String declarations with no initializer
    // We delete those.
    var allEmptyLVD =
        queryParameterizer.getStringDeclarations().stream()
            .filter(lvd -> lvd.getVariableDeclarator().getInitializer().isEmpty())
            .collect(Collectors.toSet());
    var newEmptyLVDs = allEmptyLVD;
    while (!newEmptyLVDs.isEmpty()) {
      for (var lvd : newEmptyLVDs) {
        for (final var ref : ASTs.findAllReferences(lvd)) {
          root = collapse(ref, root);
        }
        lvd.getVariableDeclarationExpr().removeForced();
      }
      newEmptyLVDs =
          queryParameterizer.getStringDeclarations().stream()
              .filter(lvd -> lvd.getVariableDeclarator().getInitializer().isEmpty())
              .collect(Collectors.toSet());
      newEmptyLVDs.removeAll(allEmptyLVD);
      allEmptyLVD.addAll(newEmptyLVDs);
    }

    queryCall.setArgument(0, root);
  }
}
