package io.codemodder.remediation;

import com.github.javaparser.Position;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.codetf.UnfixedFinding;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import org.jetbrains.annotations.VisibleForTesting;

final class DefaultLegacyFixCandidateSearcher<T> implements LegacyFixCandidateSearcher<T> {

  private final List<Predicate<MethodOrConstructor>> matchers;
  private final String methodName;

  DefaultLegacyFixCandidateSearcher(
      final String methodName, final List<Predicate<MethodOrConstructor>> matchers) {
    this.methodName = methodName;
    this.matchers = matchers;
  }

  @Override
  public LegacyFixCandidateSearchResults<T> search(
      final CompilationUnit cu,
      final String path,
      final DetectorRule rule,
      final List<T> issuesForFile,
      final Function<T, String> getKey,
      final Function<T, Integer> getStartLine,
      final Function<T, Integer> getEndLine,
      final Function<T, Integer> getColumn) {

    List<UnfixedFinding> unfixedFindings = new ArrayList<>();

    List<MethodOrConstructor> calls =
        cu.findAll(Node.class).stream()
            // limit to just the interesting nodes for us
            .filter(n -> n instanceof MethodCallExpr || n instanceof ObjectCreationExpr)
            // turn them into our convenience type
            .map(MethodOrConstructor::new)
            // don't find calls we may have added -- you can pick these out by them not having a
            // range yet
            .filter(MethodOrConstructor::hasRange)
            // filter by method name if one is provided in the search
            .filter(mce -> methodName == null || mce.isMethodCallWithName(methodName))
            // filter by matchers
            .filter(mce -> matchers.stream().allMatch(m -> m.test(mce)))
            .toList();

    Map<MethodOrConstructor, List<T>> fixCandidateToIssueMapping = new HashMap<>();

    for (T issue : issuesForFile) {
      String findingId = getKey.apply(issue);
      int issueStartLine = getStartLine.apply(issue);
      Integer issueEndLine = getEndLine.apply(issue);
      Integer column = getColumn.apply(issue);
      List<MethodOrConstructor> callsForIssue =
          calls.stream()
              .filter(MethodOrConstructor::hasRange)
              .filter(
                  mce -> {
                    int callStartLine = mce.getRange().begin.line;
                    return matches(issueStartLine, issueEndLine, callStartLine);
                  })
              .toList();
      if (callsForIssue.size() > 1 && column != null) {
        callsForIssue =
            callsForIssue.stream()
                .filter(mce -> mce.getRange().contains(new Position(issueStartLine, column)))
                .toList();
      }
      if (callsForIssue.isEmpty()) {
        unfixedFindings.add(
            new UnfixedFinding(
                findingId, rule, path, issueStartLine, RemediationMessages.noCallsAtThatLocation));
        continue;
      } else if (callsForIssue.size() > 1) {
        unfixedFindings.add(
            new UnfixedFinding(
                findingId, rule, path, issueStartLine, RemediationMessages.multipleCallsFound));
        continue;
      }
      MethodOrConstructor call = callsForIssue.get(0);
      fixCandidateToIssueMapping.computeIfAbsent(call, k -> new ArrayList<>()).add(issue);
    }

    List<LegacyFixCandidate<T>> legacyFixCandidates =
        fixCandidateToIssueMapping.entrySet().stream()
            .map(entry -> new LegacyFixCandidate<>(entry.getKey(), entry.getValue()))
            .toList();

    return new LegacyFixCandidateSearchResults<T>() {
      @Override
      public List<UnfixedFinding> unfixableFindings() {
        return unfixedFindings;
      }

      @Override
      public List<LegacyFixCandidate<T>> fixCandidates() {
        return legacyFixCandidates;
      }
    };
  }

  @VisibleForTesting
  static boolean matches(
      final int issueStartLine, final Integer issueEndLine, final int startCallLine) {
    // if the issue spans multiple lines, the call must be within that range
    if (issueEndLine != null) {
      return isInBetween(startCallLine, issueStartLine, issueEndLine);
    }
    // if the issue is on a single line, the call must be on that line
    return startCallLine == issueStartLine;
  }

  private static boolean isInBetween(int number, int lowerBound, int upperBound) {
    return number >= lowerBound && number <= upperBound;
  }
}
