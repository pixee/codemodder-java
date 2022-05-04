package io.pixee.codefixer.java;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.MethodCallExpr;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

final class ParentCodeContainsPredicate implements Predicate<MethodCallExpr> {

  private final String searchStr;

  ParentCodeContainsPredicate(final String searchStr) {
    this.searchStr = Objects.requireNonNull(searchStr);
  }

  @Override
  public boolean test(final MethodCallExpr methodCallExpr) {
    Optional<Node> parentNode = methodCallExpr.getParentNode();
    if (parentNode.isPresent()) {
      return parentNode.get().toString().contains(searchStr);
    }
    return false;
  }
}
