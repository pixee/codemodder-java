package io.codemodder.remediation;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import io.codemodder.codetf.DetectorRule;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

/** Searches for potential fix locations in the source code. */
public interface FixCandidateSearcher<T> {

  /**
   * Searches for potential fix locations in the source code.
   *
   * @param cu the compilation unit being analyzed
   * @param path the path of the source file being analyzed
   * @param rule the detector rule for the issues
   * @param issuesForFile the issues to remediate
   * @param getKey strategy to retrieve the key for the issue from {@code T}
   * @param getLine strategy to retrieve the line for the issue described by {@code T}
   * @param getColumn strategy to retrieve the column of the line for the issue described by {@code
   *     T}, or {@code null} if the tool does not provide column information
   */
  FixCandidateSearchResults<T> search(
      CompilationUnit cu,
      String path,
      DetectorRule rule,
      List<T> issuesForFile,
      Function<T, String> getKey,
      ToIntFunction<T> getLine,
      ToIntFunction<T> getColumn);

  default FixCandidateSearchResults<T> search(
      CompilationUnit cu,
      String path,
      DetectorRule rule,
      List<T> issuesForFile,
      Function<T, String> getKey,
      ToIntFunction<T> getLine) {
    return search(cu, path, rule, issuesForFile, getKey, getLine, null);
  }

  /** Builder for {@link FixCandidateSearcher}. */
  final class Builder<T> {
    private String methodName;
    private final List<Predicate<MethodCallExpr>> methodMatchers;

    public Builder() {
      this.methodMatchers = new ArrayList<>();
    }

    public Builder<T> withMethodName(final String methodName) {
      this.methodName = Objects.requireNonNull(methodName);
      return this;
    }

    public Builder<T> withMatcher(final Predicate<MethodCallExpr> methodMatcher) {
      this.methodMatchers.add(Objects.requireNonNull(methodMatcher));
      return this;
    }

    public FixCandidateSearcher<T> build() {
      return new DefaultFixCandidateSearcher<>(methodName, List.copyOf(methodMatchers));
    }
  }
}
