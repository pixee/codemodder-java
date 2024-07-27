package io.codemodder.remediation.javadeserialization;

import static io.codemodder.javaparser.JavaParserTransformer.replace;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import io.codemodder.CodemodChange;
import io.codemodder.CodemodFileScanningResult;
import io.codemodder.DependencyGAV;
import io.codemodder.ast.ASTs;
import io.codemodder.ast.LocalDeclaration;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.codetf.FixedFinding;
import io.codemodder.codetf.UnfixedFinding;
import io.codemodder.remediation.FixCandidate;
import io.codemodder.remediation.FixCandidateSearchResults;
import io.codemodder.remediation.FixCandidateSearcher;
import io.github.pixee.security.ObjectInputFilters;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

final class DefaultJavaDeserializationRemediator implements JavaDeserializationRemediator {

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
            .withMethodName("readObject")
            .withMatcher(mce -> mce.getScope().isPresent())
            .withMatcher(mce -> mce.getArguments().isEmpty())
            .build();

    FixCandidateSearchResults<T> results =
        searcher.search(cu, path, detectorRule, issuesForFile, getKey, getLine, getColumn);

    List<CodemodChange> changes = new ArrayList<>();
    List<UnfixedFinding> unfixedFindings = new ArrayList<>();
    for (FixCandidate<T> fixCandidate : results.fixCandidates()) {
      List<T> issues = fixCandidate.issues();
      MethodCallExpr call = fixCandidate.methodCall();
      // get the declaration of the ObjectInputStream
      Expression callScope = call.getScope().get();
      if (!callScope.isNameExpr()) {
        // can't fix these
        issues.stream()
            .map(
                i ->
                    new UnfixedFinding(
                        getKey.apply(i), detectorRule, path, getLine.apply(i), "Unexpected shape"))
            .forEach(unfixedFindings::add);
        continue;
      }

      Optional<LocalDeclaration> declaration =
          ASTs.findEarliestLocalDeclarationOf(callScope.asNameExpr().getName());
      if (declaration.isEmpty()) {
        issues.stream()
            .map(
                i ->
                    new UnfixedFinding(
                        getKey.apply(i),
                        detectorRule,
                        path,
                        getLine.apply(i),
                        "No declaration found"))
            .forEach(unfixedFindings::add);
        continue;
      }

      LocalDeclaration localDeclaration = declaration.get();
      Node varDeclarationAndExpr = localDeclaration.getDeclaration();
      if (varDeclarationAndExpr instanceof VariableDeclarator varDec) {
        Optional<Expression> initializer = varDec.getInitializer();
        if (initializer.isEmpty()) {
          issues.stream()
              .map(
                  i ->
                      new UnfixedFinding(
                          getKey.apply(i),
                          detectorRule,
                          path,
                          getLine.apply(i),
                          "No initializer found"))
              .forEach(unfixedFindings::add);
          continue;
        }

        Expression expression = initializer.get();
        if (expression instanceof ObjectCreationExpr objCreation) {
          fixObjectInputStreamCreation(objCreation);
          CodemodChange change =
              CodemodChange.from(
                  getLine.apply(issues.get(0)),
                  List.of(DependencyGAV.JAVA_SECURITY_TOOLKIT),
                  issues.stream()
                      .map(i -> new FixedFinding(getKey.apply(i), detectorRule))
                      .toList());
          changes.add(change);
        }
      } else {
        issues.stream()
            .map(
                i ->
                    new UnfixedFinding(
                        getKey.apply(i),
                        detectorRule,
                        path,
                        getLine.apply(i),
                        "Unexpected declaration type"))
            .forEach(unfixedFindings::add);
      }
    }

    unfixedFindings.addAll(results.unfixableFindings());
    return CodemodFileScanningResult.from(changes, unfixedFindings);
  }

  private void fixObjectInputStreamCreation(final ObjectCreationExpr objCreation) {
    replace(objCreation)
        .withStaticMethod(ObjectInputFilters.class.getName(), "createSafeObjectInputStream")
        .withStaticImport()
        .withSameArguments();
  }
}
