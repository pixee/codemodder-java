package io.codemodder.remediation.missingsecureflag;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import io.codemodder.CodemodChange;
import io.codemodder.CodemodFileScanningResult;
import io.codemodder.ast.ASTTransforms;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.codetf.FixedFinding;
import io.codemodder.codetf.UnfixedFinding;
import io.codemodder.remediation.LegacyFixCandidate;
import io.codemodder.remediation.LegacyFixCandidateSearchResults;
import io.codemodder.remediation.LegacyFixCandidateSearcher;
import io.codemodder.remediation.RemediationMessages;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;

final class DefaultMissingSecureFlagRemediator implements MissingSecureFlagRemediator {

  @Override
  public <T> CodemodFileScanningResult remediateAll(
      final CompilationUnit cu,
      final String path,
      final DetectorRule detectorRule,
      final List<T> issuesForFile,
      final Function<T, String> getKey,
      final Function<T, Integer> getStartLine,
      final Function<T, Integer> getEndLine,
      final Function<T, Integer> getStartColumn) {

    // this remediator assumes we're pointing to a response.addCookie() call
    LegacyFixCandidateSearcher<T> searcher =
        new LegacyFixCandidateSearcher.Builder<T>()
            .withMethodName("addCookie")
            .withMatcher(mce -> mce.getArguments().size() == 1)
            .build();

    LegacyFixCandidateSearchResults<T> results =
        searcher.search(
            cu,
            path,
            detectorRule,
            issuesForFile,
            getKey,
            getStartLine,
            getEndLine,
            getStartColumn);

    List<CodemodChange> changes = new ArrayList<>();
    List<UnfixedFinding> unfixedFindings = new ArrayList<>();

    for (LegacyFixCandidate<T> result : results.fixCandidates()) {
      MethodCallExpr methodCallExpr = result.call().asMethodCall();
      List<T> issues = result.issues();

      if (methodCallExpr.getScope().isPresent()) {

        // This assumption is a bit strong, but covers the most common cases while avoiding weird
        // ones
        Optional<Statement> maybeStmt =
            methodCallExpr
                .getParentNode()
                .map(p -> p instanceof Statement ? (Statement) p : null)
                .filter(Statement::isExpressionStmt);

        // We want to avoid things like: response.addCookie(new Cookie(...)), so we restrict
        // ourselves
        Optional<Expression> maybeCookieExpression =
            Optional.of(methodCallExpr.getArgument(0))
                .filter(expr -> expr.isNameExpr() || expr.isFieldAccessExpr());

        if (maybeStmt.isPresent() && maybeCookieExpression.isPresent()) {
          final var newStatement =
              new ExpressionStmt(
                  new MethodCallExpr(
                      maybeCookieExpression.get(),
                      "setSecure",
                      new NodeList<>(new BooleanLiteralExpr(true))));

          ASTTransforms.addStatementBeforeStatement(maybeStmt.get(), newStatement);

          FixedFinding fixedFinding = new FixedFinding(getKey.apply(issues.get(0)), detectorRule);
          CodemodChange change =
              CodemodChange.from(
                  result.call().getRange().begin.line, List.of(), List.of(fixedFinding));
          changes.add(change);
        } else {
          List<UnfixedFinding> unfixedFindingsToAdd =
              createUnfixedFindingList(path, detectorRule, getKey, getStartLine, issues);
          unfixedFindings.addAll(unfixedFindingsToAdd);
        }
      } else {
        List<UnfixedFinding> unfixedFindingsToAdd =
            createUnfixedFindingList(path, detectorRule, getKey, getStartLine, issues);
        unfixedFindings.addAll(unfixedFindingsToAdd);
      }
    }
    return CodemodFileScanningResult.from(changes, unfixedFindings);
  }

  private static <T> @NotNull List<UnfixedFinding> createUnfixedFindingList(
      final String path,
      final DetectorRule detectorRule,
      final Function<T, String> getKey,
      final Function<T, Integer> getStartLine,
      final List<T> issues) {
    return issues.stream()
        .map(
            issue ->
                new UnfixedFinding(
                    getKey.apply(issue),
                    detectorRule,
                    path,
                    getStartLine.apply(issue),
                    RemediationMessages.ambiguousCodeShape))
        .toList();
  }
}
