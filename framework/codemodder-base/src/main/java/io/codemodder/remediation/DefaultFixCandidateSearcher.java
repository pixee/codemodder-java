package io.codemodder.remediation;

import com.github.javaparser.Position;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.codetf.UnfixedFinding;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

final class DefaultFixCandidateSearcher<T> implements FixCandidateSearcher<T> {

  private final List<Predicate<MethodCallExpr>> matchers;
  private final String methodName;

  DefaultFixCandidateSearcher(
      final String methodName, final List<Predicate<MethodCallExpr>> matchers) {
    this.methodName = methodName;
    this.matchers = matchers;
  }

  @Override
  public FixCandidateSearchResults<T> search(
      final CompilationUnit cu,
      final String path,
      final DetectorRule rule,
      final List<T> issuesForFile,
      final Function<T, String> getKey,
      final ToIntFunction<T> getLine,
      final ToIntFunction<T> getColumn) {

    List<UnfixedFinding> unfixedFindings = new ArrayList<>();
    List<MethodCallExpr> calls =
        cu.findAll(MethodCallExpr.class).stream()
            .filter(
                mce ->
                    mce.getRange()
                        .isPresent()) // don't find calls we may have added -- you can pick these
            // out by them not having a range yet
            .filter(mce -> methodName == null || methodName.equals(mce.getNameAsString()))
            .filter(mce -> matchers.stream().allMatch(m -> m.test(mce)))
            .toList();

    Map<MethodCallExpr, List<T>> fixCandidateToIssueMapping = new HashMap<>();

    for (T issue : issuesForFile) {
      String findingId = getKey.apply(issue);
      int line = getLine.applyAsInt(issue);
      final OptionalInt column =
          getColumn == null ? OptionalInt.empty() : OptionalInt.of(getColumn.applyAsInt(issue));
      List<MethodCallExpr> callsForIssue =
          calls.stream()
              .filter(mce -> mce.getRange().isPresent())
              .filter(mce -> mce.getRange().get().begin.line == line)
              .toList();
      if (callsForIssue.size() > 1 && column.isPresent()) {
        callsForIssue =
            callsForIssue.stream()
                .filter(mce -> mce.getRange().get().contains(new Position(line, column.getAsInt())))
                .toList();
      }
      if (callsForIssue.isEmpty()) {
        unfixedFindings.add(
            new UnfixedFinding(
                findingId, rule, path, line, RemediationMessages.noCallsAtThatLocation));
        continue;
      } else if (callsForIssue.size() > 1) {
        unfixedFindings.add(
            new UnfixedFinding(
                findingId, rule, path, line, RemediationMessages.multipleCallsFound));
        continue;
      }
      MethodCallExpr call = callsForIssue.get(0);
      fixCandidateToIssueMapping.computeIfAbsent(call, k -> new ArrayList<>()).add(issue);
    }

    List<FixCandidate<T>> fixCandidates =
        fixCandidateToIssueMapping.entrySet().stream()
            .map(entry -> new FixCandidate<>(entry.getKey(), entry.getValue()))
            .toList();

    return new FixCandidateSearchResults<T>() {
      @Override
      public List<UnfixedFinding> unfixableFindings() {
        return unfixedFindings;
      }

      @Override
      public List<FixCandidate<T>> fixCandidates() {
        return fixCandidates;
      }
    };
  }
}
