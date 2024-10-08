package io.codemodder.remediation;

import com.github.javaparser.ast.CompilationUnit;
import io.codemodder.codetf.DetectorRule;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

/** Searches for potential fix locations in the source code. */
public interface FixCandidateSearcher<T> {

  /** Searches for potential fix locations in the source code. */
  FixCandidateSearchResults<T> search(
      CompilationUnit cu,
      String path,
      DetectorRule rule,
      List<T> issuesForFile,
      Function<T, String> getKey,
      Function<T, Integer> getStartLine,
      Function<T, Integer> getEndLine,
      Function<T, Integer> getColumn);

  /** Builder for {@link FixCandidateSearcher}. */
  final class Builder<T> {
    private String methodName;
    private final List<Predicate<MethodOrConstructor>> methodMatchers;

    public Builder() {
      this.methodMatchers = new ArrayList<>();
    }

    public Builder<T> withMethodName(final String methodName) {
      this.methodName = Objects.requireNonNull(methodName);
      return this;
    }

    public Builder<T> withMatcher(final Predicate<MethodOrConstructor> methodMatcher) {
      this.methodMatchers.add(Objects.requireNonNull(methodMatcher));
      return this;
    }

    public FixCandidateSearcher<T> build() {
      return new DefaultFixCandidateSearcher<>(methodName, List.copyOf(methodMatchers));
    }
  }
}
