package io.codemodder.remediation.predictableseed;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.LiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import io.codemodder.CodemodChange;
import io.codemodder.CodemodFileScanningResult;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.codetf.FixedFinding;
import io.codemodder.remediation.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/** Remediator for predictable seed weaknesses. */
public final class PredictableSeedRemediator<T> implements Remediator<T> {

  @Override
  public CodemodFileScanningResult remediateAll(
      final CompilationUnit cu,
      final String path,
      final DetectorRule detectorRule,
      final Collection<T> findingsForPath,
      final Function<T, String> findingIdExtractor,
      final Function<T, Integer> findingStartLineExtractor,
      final Function<T, Optional<Integer>> findingEndLineExtractor,
      final Function<T, Optional<Integer>> findingStartColumnExtractor) {

    FixCandidateSearcher<T> searcher =
        new FixCandidateSearcher.Builder<T>()
            .withMatcher(
                n ->
                    Optional.of(n)
                        .map(MethodOrConstructor::new)
                        .filter(mce -> mce.isMethodCallWithName("setSeed"))
                        .filter(mce -> mce.asNode().hasScope())
                        .filter(mce -> mce.getArguments().size() == 1)
                        // technically, we don't need this, just to prevent a silly tool from
                        // reporting on hardcoded data
                        .filter(mce -> !(mce.getArguments().get(0) instanceof LiteralExpr))
                        .isPresent())
            .build();

    FixCandidateSearchResults<T> candidateSearchResults =
        searcher.search(
            cu,
            path,
            detectorRule,
            List.copyOf(findingsForPath),
            findingIdExtractor,
            findingStartLineExtractor,
            findingEndLineExtractor,
            findingStartColumnExtractor);

    List<CodemodChange> changes = new ArrayList<>();
    for (FixCandidate<T> fixCandidate : candidateSearchResults.fixCandidates()) {
      MethodCallExpr setSeedCall = (MethodCallExpr) fixCandidate.node();
      MethodCallExpr safeExpression =
          new MethodCallExpr(new NameExpr(System.class.getSimpleName()), "currentTimeMillis");
      NodeList<Expression> arguments = setSeedCall.getArguments();
      arguments.set(0, safeExpression);

      // should only ever be one, but just in case
      List<FixedFinding> fixedFindings =
          fixCandidate.issues().stream()
              .map(issue -> new FixedFinding(findingIdExtractor.apply(issue), detectorRule))
              .toList();

      int reportedLine = setSeedCall.getRange().get().begin.line;
      changes.add(CodemodChange.from(reportedLine, List.of(), fixedFindings));
    }

    return CodemodFileScanningResult.from(changes, candidateSearchResults.unfixableFindings());
  }
}
