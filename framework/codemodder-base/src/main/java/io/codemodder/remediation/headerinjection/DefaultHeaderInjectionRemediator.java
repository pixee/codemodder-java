package io.codemodder.remediation.headerinjection;

import static io.codemodder.javaparser.JavaParserTransformer.wrap;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import io.codemodder.CodemodChange;
import io.codemodder.CodemodFileScanningResult;
import io.codemodder.DependencyGAV;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.codetf.FixedFinding;
import io.codemodder.remediation.FixCandidate;
import io.codemodder.remediation.FixCandidateSearchResults;
import io.codemodder.remediation.FixCandidateSearcher;
import io.github.pixee.security.Newlines;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

final class DefaultHeaderInjectionRemediator implements HeaderInjectionRemediator {

  private static final Set<String> setHeaderNames = Set.of("setHeader", "addHeader");

  @Override
  public <T> CodemodFileScanningResult remediateAll(
      final CompilationUnit cu,
      final String path,
      final DetectorRule detectorRule,
      final List<T> issuesForFile,
      final Function<T, String> getKey,
      final Function<T, Integer> getLine,
      final Function<T, Integer> getColumn) {

    FixCandidateSearcher<T> searcher =
        new FixCandidateSearcher.Builder<T>()
            .withMatcher(mce -> setHeaderNames.contains(mce.getNameAsString()))
            .withMatcher(mce -> mce.getScope().isPresent())
            .withMatcher(mce -> mce.getArguments().size() == 2)
            .withMatcher(mce -> !mce.getArgument(1).isStringLiteralExpr())
            .build();

    FixCandidateSearchResults<T> results =
        searcher.search(cu, path, detectorRule, issuesForFile, getKey, getLine, getColumn);

    List<CodemodChange> changes = new ArrayList<>();
    for (FixCandidate<T> fixCandidate : results.fixCandidates()) {
      T issue = fixCandidate.issue();
      String findingId = getKey.apply(issue);

      MethodCallExpr setHeaderCall = fixCandidate.methodCall();
      Expression headerValueArgument = setHeaderCall.getArgument(1);
      boolean successfullyChanged =
          wrap(headerValueArgument).withStaticMethod(Newlines.class.getName(), "stripAll", false);
      if (successfullyChanged) {
        changes.add(
            CodemodChange.from(
                setHeaderCall.getRange().get().begin.line,
                List.of(DependencyGAV.JAVA_SECURITY_TOOLKIT),
                new FixedFinding(findingId, detectorRule)));
      }
    }

    return CodemodFileScanningResult.from(changes, results.unfixableFindings());
  }
}
