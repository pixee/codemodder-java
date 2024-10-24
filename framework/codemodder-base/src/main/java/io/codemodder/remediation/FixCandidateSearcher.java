package io.codemodder.remediation;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import io.codemodder.codetf.DetectorRule;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

/** Searches for potential fix locations in the source code. */
public interface FixCandidateSearcher<T> {

  /**
   * Searches the AST for nodes associated with the given issues.
   *
   * @param cu
   * @param path
   * @param rule
   * @param issuesForFile
   * @param getKey A function that extracts the key for T.
   * @param getStartLine A function that extracts start line information from T. Always required.
   * @param getEndLine A function that extracts end line information from T. May not be available
   * @param getColumn A function that extracts column information from T. May not be available
   * @return
   */
  FixCandidateSearchResults<T> search(
      CompilationUnit cu,
      String path,
      DetectorRule rule,
      List<T> issuesForFile,
      Function<T, String> getKey,
      Function<T, Integer> getStartLine,
      Function<T, Optional<Integer>> getEndLine,
      Function<T, Optional<Integer>> getColumn);

  /** Builder for {@link FixCandidateSearcher}. */
  final class Builder<T> {
    private final List<Predicate<Node>> matchers;
    private NodePositionMatcher nodePositionMatcher;

    public Builder() {
      this.matchers = new ArrayList<>();
      this.nodePositionMatcher = new DefaultNodePositionMatcher();
    }

    public Builder<T> withMatcher(final Predicate<Node> matcher) {
      this.matchers.add(Objects.requireNonNull(matcher));
      return this;
    }

    public Builder<T> withNodePositionMatcher(final NodePositionMatcher nodePositionMatcher) {
      this.nodePositionMatcher = nodePositionMatcher;
      return this;
    }

    public FixCandidateSearcher<T> build() {
      return new DefaultFixCandidateSearcher<>(List.copyOf(matchers), nodePositionMatcher);
    }
  }
}
