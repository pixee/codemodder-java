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
import io.codemodder.remediation.MethodOrConstructor;
import io.github.pixee.security.ObjectInputFilters;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;

final class DefaultJavaDeserializationRemediator implements JavaDeserializationRemediator {

  @Override
  public <T> CodemodFileScanningResult remediateAll(
      final CompilationUnit cu,
      final String path,
      final DetectorRule detectorRule,
      final List<T> issuesForFile,
      final Function<T, String> getKey,
      final Function<T, Integer> getStartLine,
      final Function<T, Integer> getEndLine,
      final Function<T, Integer> getStartColumn) {

    FixCandidateSearcher<T> searcher =
        new FixCandidateSearcher.Builder<T>()
            .withMethodName("readObject")
            .withMatcher(MethodOrConstructor::isMethodCallWithScope)
            .withMatcher(mce -> mce.getArguments().isEmpty())
            .build();

    // search for readObject() calls on those lines, assuming the tool points there
    FixCandidateSearchResults<T> results =
        searcher.search(
            cu,
            path,
            detectorRule,
            issuesForFile,
            getKey,
            getStartLine,
            getEndLine,
            getStartColumn);

    if (results.fixCandidates().isEmpty()) {
      // try searching for matching ObjectInputStream creation objects, maybe that's where they're
      // pointing
      searcher =
          new FixCandidateSearcher.Builder<T>()
              .withMatcher(mc -> mc.isConstructorForType("ObjectInputStream"))
              .build();

      results =
          searcher.search(
              cu,
              path,
              detectorRule,
              issuesForFile,
              getKey,
              getStartLine,
              getEndLine,
              getStartColumn);
    }

    List<CodemodChange> changes = new ArrayList<>();
    List<UnfixedFinding> unfixedFindings = new ArrayList<>();

    for (FixCandidate<T> fixCandidate : results.fixCandidates()) {
      List<T> issues = fixCandidate.issues();
      MethodOrConstructor call = fixCandidate.call();

      if (call.isConstructor()) {
        // we're pointing to the readObject(), fix and move on
        fixObjectInputStreamCreation((ObjectCreationExpr) call.asNode());
        CodemodChange change = buildFixChange(detectorRule, getKey, getStartLine, issues);
        changes.add(change);
        continue;
      }

      // we're pointing to the readObject(), so we must work backwards to find the declaration of
      // the ObjectInputStream
      MethodCallExpr mce = (MethodCallExpr) call.asNode();
      Expression callScope = mce.getScope().get();
      if (!callScope.isNameExpr()) {
        // can't fix these
        issues.stream()
            .map(
                i ->
                    new UnfixedFinding(
                        getKey.apply(i),
                        detectorRule,
                        path,
                        getStartLine.apply(i),
                        "Unexpected shape"))
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
                        getStartLine.apply(i),
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
                          getStartLine.apply(i),
                          "No initializer found"))
              .forEach(unfixedFindings::add);
          continue;
        }

        Expression expression = initializer.get();
        if (expression instanceof ObjectCreationExpr objCreation) {
          fixObjectInputStreamCreation(objCreation);
          CodemodChange change = buildFixChange(detectorRule, getKey, getStartLine, issues);
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
                        getStartLine.apply(i),
                        "Unexpected declaration type"))
            .forEach(unfixedFindings::add);
      }
    }

    unfixedFindings.addAll(results.unfixableFindings());
    return CodemodFileScanningResult.from(changes, unfixedFindings);
  }

  /**
   * Build a {@link io.codemodder.CodemodChange} for this code change that fixes the given issues.
   */
  private static <T> @NotNull CodemodChange buildFixChange(
      final DetectorRule detectorRule,
      final Function<T, String> getKey,
      final Function<T, Integer> getLine,
      final List<T> issues) {
    return CodemodChange.from(
        getLine.apply(issues.get(0)),
        List.of(DependencyGAV.JAVA_SECURITY_TOOLKIT),
        issues.stream().map(i -> new FixedFinding(getKey.apply(i), detectorRule)).toList());
  }

  private void fixObjectInputStreamCreation(final ObjectCreationExpr objCreation) {
    replace(objCreation)
        .withStaticMethod(ObjectInputFilters.class.getName(), "createSafeObjectInputStream")
        .withStaticImport()
        .withSameArguments();
  }
}
