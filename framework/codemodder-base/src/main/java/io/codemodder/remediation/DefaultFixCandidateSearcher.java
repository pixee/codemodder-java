package io.codemodder.remediation;

import com.github.javaparser.Position;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.codetf.UnfixedFinding;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * Maps issues of type T to relevant nodes in the AST. Relevant nodes are decided with matchers.
 *
 * @param <T>
 */
final class DefaultFixCandidateSearcher<T> implements FixCandidateSearcher<T> {

  private final List<Predicate<Node>> matchers;

  DefaultFixCandidateSearcher(final List<Predicate<Node>> matchers) {
    this.matchers = matchers;
  }

  @Override
  public FixCandidateSearchResults<T> search(
      final CompilationUnit cu,
      final String path,
      final DetectorRule rule,
      final List<T> issuesForFile,
      final Function<T, String> getKey,
      final Function<T, Integer> getStartLine,
      final Function<T, Optional<Integer>> getEndLine,
      final Function<T, Optional<Integer>> getColumn) {

    List<UnfixedFinding> unfixedFindings = new ArrayList<>();

    List<Node> nodes =
        cu.findAll(Node.class).stream()
            // filter by matchers
            .filter(n -> matchers.stream().allMatch(m -> m.test(n)))
            .toList();

    Map<Node, List<T>> fixCandidateToIssueMapping = new HashMap<>();

    for (T issue : issuesForFile) {
      String findingId = getKey.apply(issue);
      int issueStartLine = getStartLine.apply(issue);
      Optional<Integer> maybeEndLine = getEndLine.apply(issue);
      Optional<Integer> maybeColumn = getColumn.apply(issue);
      List<Node> nodesForIssue =
          nodes.stream()
              .filter(
                  n -> {
                    int nodeStartLine = n.getRange().orElseThrow().begin.line;
                    // if issue end line info is present, match the node line with the issue range
                    return maybeEndLine
                        .map(issueEndLine -> matches(issueStartLine, nodeStartLine, issueEndLine))
                        .orElse(matches(issueStartLine, nodeStartLine));
                  })
              // if column info is present, check if the column is contained in the node's range
              .filter(
                  n ->
                      maybeColumn
                          .map(
                              column ->
                                  n.getRange()
                                      .orElseThrow()
                                      .contains(new Position(issueStartLine, column)))
                          .orElse(true))
              .toList();
      if (nodesForIssue.isEmpty()) {
        unfixedFindings.add(
            new UnfixedFinding(
                findingId, rule, path, issueStartLine, RemediationMessages.noNodesAtThatLocation));
        continue;
      } else if (nodesForIssue.size() > 1) {
        unfixedFindings.add(
            new UnfixedFinding(
                findingId, rule, path, issueStartLine, RemediationMessages.multipleNodesFound));
        continue;
      }
      Node node = nodesForIssue.get(0);
      fixCandidateToIssueMapping.computeIfAbsent(node, k -> new ArrayList<>()).add(issue);
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

  @VisibleForTesting
  static boolean matches(
      final int issueStartLine, final int startNodeLine, final int issueEndLine) {
    // if the issue spans multiple lines, the node must be within that range
    return matches(issueStartLine, startNodeLine)
        && isInBetween(startNodeLine, issueStartLine, issueEndLine);
  }

  static boolean matches(final int issueStartLine, final int startNodeLine) {
    // if the issue is on a single line, the node must be on that line
    return startNodeLine == issueStartLine;
  }

  private static boolean isInBetween(int number, int lowerBound, int upperBound) {
    return number >= lowerBound && number <= upperBound;
  }
}
