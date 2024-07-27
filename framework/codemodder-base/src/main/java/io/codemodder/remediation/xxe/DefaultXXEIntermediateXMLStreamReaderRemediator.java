package io.codemodder.remediation.xxe;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.Statement;
import io.codemodder.CodemodChange;
import io.codemodder.CodemodFileScanningResult;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.codetf.FixedFinding;
import io.codemodder.codetf.UnfixedFinding;
import io.codemodder.remediation.FixCandidate;
import io.codemodder.remediation.FixCandidateSearchResults;
import io.codemodder.remediation.FixCandidateSearcher;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

final class DefaultXXEIntermediateXMLStreamReaderRemediator
    implements XXEIntermediateXMLStreamReaderRemediator {

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
            .withMethodName("createXMLStreamReader")
            .withMatcher(mce -> mce.getScope().isPresent())
            .withMatcher(mce -> mce.getArguments().isNonEmpty())
            .build();

    FixCandidateSearchResults<T> results =
        searcher.search(cu, path, detectorRule, issuesForFile, getKey, getLine, getColumn);

    List<CodemodChange> changes = new ArrayList<>();
    List<UnfixedFinding> unfixedFindings = new ArrayList<>();

    for (FixCandidate<T> fixCandidate : results.fixCandidates()) {
      List<T> issues = fixCandidate.issues();

      // get the xmlstreamreader scope variable
      MethodCallExpr createXMLStreamReaderCall = fixCandidate.methodCall();
      Expression xmlStreamReaderScope = createXMLStreamReaderCall.getScope().get();
      // make sure its a variable
      if (!xmlStreamReaderScope.isNameExpr()) {
        issues.stream()
            .map(
                issue ->
                    new UnfixedFinding(
                        getKey.apply(issue),
                        detectorRule,
                        path,
                        getLine.apply(issue),
                        "Could not find the XMLStreamReader variable"))
            .forEach(unfixedFindings::add);
        continue;
      }
      // get the variable
      NameExpr xmlStreamReaderVariable = xmlStreamReaderScope.asNameExpr();
      // get the JavaParser statement that contains the create call
      Optional<Statement> ancestorStatement =
          createXMLStreamReaderCall.findAncestor(Statement.class);
      if (ancestorStatement.isEmpty()) {
        issues.stream()
            .map(
                issue ->
                    new UnfixedFinding(
                        getKey.apply(issue),
                        detectorRule,
                        path,
                        getLine.apply(issue),
                        "Could not find the statement containing the XMLStreamReader creation"))
            .forEach(unfixedFindings::add);
        continue;
      }
      Statement stmt = ancestorStatement.get();
      XMLFeatures.addXMLInputFactoryDisablingStatement(xmlStreamReaderVariable, stmt, true);
      issues.stream()
          .map(
              issue ->
                  CodemodChange.from(
                      getLine.apply(issue), new FixedFinding(getKey.apply(issue), detectorRule)))
          .forEach(changes::add);
    }

    unfixedFindings.addAll(results.unfixableFindings());
    return CodemodFileScanningResult.from(changes, unfixedFindings);
  }
}
