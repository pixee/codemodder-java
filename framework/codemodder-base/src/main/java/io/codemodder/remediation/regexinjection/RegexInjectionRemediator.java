package io.codemodder.remediation.regexinjection;

import static io.codemodder.javaparser.JavaParserTransformer.wrap;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import io.codemodder.CodemodFileScanningResult;
import io.codemodder.ast.ASTs;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.remediation.*;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;

/** Remediator for Regex Injection vulnerabilities. */
public final class RegexInjectionRemediator<T> implements Remediator<T> {

  private final SearcherStrategyRemediator<T> searchStrategyRemediator;

  public RegexInjectionRemediator() {
    this.searchStrategyRemediator =
        new SearcherStrategyRemediator.Builder<T>()
            .withSearcherStrategyPair(
                new FixCandidateSearcher.Builder<T>()
                    .withMatcher(
                        node ->
                            Optional.of(node)
                                .map(n -> n instanceof Expression e ? e : null)
                                .flatMap(ASTs::isArgumentOfMethodCall)
                                .filter(RegexInjectionRemediator::isCompileCall)
                                .isPresent())
                    .build(),
                new FixPatternCompileStrategy())
            .withSearcherStrategyPair(
                new FixCandidateSearcher.Builder<T>()
                    .withMatcher(
                        node ->
                            Optional.of(node)
                                .map(n -> n instanceof Expression e ? e : null)
                                // Must be the first argument
                                .flatMap(
                                    e ->
                                        ASTs.isArgumentOfMethodCall(e)
                                            .filter(mce -> mce.getArgument(0) == e))
                                .filter(RegexInjectionRemediator::isReplaceFirstCall)
                                .isPresent())
                    .build(),
                new FixStringReplaceFirstStrategy())
            .build();
  }

  /** Fix strategy for {@link Pattern#compile(String)}. */
  private static class FixPatternCompileStrategy implements RemediationStrategy {
    @Override
    public SuccessOrReason fix(final CompilationUnit cu, final Node node) {
      final MethodCallExpr compileCall = ASTs.isArgumentOfMethodCall((Expression) node).get();
      Expression argument = compileCall.getArgument(0);
      wrap(argument).withStaticMethod(Pattern.class.getName(), "quote", false);
      return SuccessOrReason.success();
    }
  }

  /** Check if it's a {@link Pattern#compile(String)} call. */
  private static boolean isCompileCall(final MethodCallExpr methodCallExpr) {
    return "compile".equals(methodCallExpr.getNameAsString())
        && (methodCallExpr.getArguments().size() == 1
            || (methodCallExpr.getArguments().size() == 2
                && !methodCallExpr.getArguments().get(0).isStringLiteralExpr()));
  }

  /** Check if its a {@link String#replaceFirst(String, String)} call. */
  private static boolean isReplaceFirstCall(final MethodCallExpr methodCallExpr) {
    return methodCallExpr.getNameAsString().equals("replaceFirst")
        && methodCallExpr.getArguments().size() == 2
        && !methodCallExpr.getArguments().get(0).isStringLiteralExpr();
  }

  /** Fix strategy for {@link String#replaceFirst(String, String)}. */
  private static class FixStringReplaceFirstStrategy implements RemediationStrategy {
    @Override
    public SuccessOrReason fix(final CompilationUnit cu, final Node node) {
      final MethodCallExpr replaceFirstCall = ASTs.isArgumentOfMethodCall((Expression) node).get();
      Expression argument = replaceFirstCall.getArgument(0);
      wrap(argument).withStaticMethod(Pattern.class.getName(), "quote", false);
      return SuccessOrReason.success();
    }
  }

  @Override
  public CodemodFileScanningResult remediateAll(
      CompilationUnit cu,
      String path,
      DetectorRule detectorRule,
      Collection<T> findingsForPath,
      Function<T, String> findingIdExtractor,
      Function<T, Integer> findingStartLineExtractor,
      Function<T, Optional<Integer>> findingEndLineExtractor,
      Function<T, Optional<Integer>> findingColumnExtractor) {
    return searchStrategyRemediator.remediateAll(
        cu,
        path,
        detectorRule,
        findingsForPath,
        findingIdExtractor,
        findingStartLineExtractor,
        findingEndLineExtractor,
        findingColumnExtractor);
  }
}
