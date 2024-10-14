package io.codemodder.remediation.reflectioninjection;

import static io.codemodder.ast.ASTTransforms.addImportIfMissing;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import io.codemodder.CodemodChange;
import io.codemodder.CodemodFileScanningResult;
import io.codemodder.DependencyGAV;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.codetf.FixedFinding;
import io.codemodder.remediation.LegacyFixCandidate;
import io.codemodder.remediation.LegacyFixCandidateSearchResults;
import io.codemodder.remediation.LegacyFixCandidateSearcher;
import io.codemodder.remediation.MethodOrConstructor;
import io.github.pixee.security.Reflection;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

final class DefaultReflectionInjectionRemediator implements ReflectionInjectionRemediator {

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

    LegacyFixCandidateSearcher<T> searcher =
        new LegacyFixCandidateSearcher.Builder<T>()
            .withMatcher(mce -> isClassForNameCall(cu, mce))
            .build();

    LegacyFixCandidateSearchResults<T> results =
        searcher.search(
            cu,
            path,
            detectorRule,
            issuesForFile,
            getKey,
            getStartLine,
            getEndLine,
            getStartColumn);

    List<CodemodChange> changes = new ArrayList<>();

    for (LegacyFixCandidate<T> legacyFixCandidate : results.fixCandidates()) {
      List<T> issues = legacyFixCandidate.issues();
      int line = getStartLine.apply(issues.get(0));

      MethodCallExpr methodCallExpr = legacyFixCandidate.call().asMethodCall();
      replaceMethodCallExpression(cu, methodCallExpr);

      issues.stream()
          .map(getKey)
          .forEach(
              id -> {
                CodemodChange change =
                    CodemodChange.from(
                        line,
                        List.of(DependencyGAV.JAVA_SECURITY_TOOLKIT),
                        new FixedFinding(id, detectorRule));
                changes.add(change);
              });
    }

    return CodemodFileScanningResult.from(changes, results.unfixableFindings());
  }

  /**
   * Updates the scope and name of the method call expression to {@code Reflection.loadAndVerify},
   * and adds the import if missing.
   *
   * @param cu CompilationUnit to update with imports
   * @param methodCallExpr the method call expression to update
   */
  private static void replaceMethodCallExpression(
      final CompilationUnit cu, final MethodCallExpr methodCallExpr) {
    final var name = new NameExpr(Reflection.class.getSimpleName());
    methodCallExpr.setScope(name);
    methodCallExpr.setName("loadAndVerify");
    addImportIfMissing(cu, Reflection.class);
  }

  /**
   * Check if the method call expression is a call to {@code Class.forName(String)}.
   *
   * <p>This is important, because this codemod uses fuzzy region matching, to account for how Sonar
   * reports the region for this finding. Sonar reports the region to be only the "forName" part of
   * the method expression.
   *
   * @param cu CompilationUnit for checking static imports
   * @param methodOrConstructor the node to check
   * @return true if the method call expression is a call to {@code Class.forName(String)}
   */
  private static boolean isClassForNameCall(
      final CompilationUnit cu, final MethodOrConstructor methodOrConstructor) {
    if (!methodOrConstructor.isMethodCall()) {
      return false;
    }
    MethodCallExpr methodCallExpr = methodOrConstructor.asMethodCall();
    final boolean scopeMatches =
        methodCallExpr
            .getScope()
            .map(
                expression -> {
                  if (expression.isNameExpr()) {
                    final var nameExpr = expression.asNameExpr();
                    return nameExpr.getNameAsString().equals("Class");
                  }
                  return false;
                })
            .orElse(
                cu.getImports().stream()
                    .anyMatch(
                        importDeclaration ->
                            importDeclaration.isStatic()
                                && importDeclaration
                                    .getNameAsString()
                                    .equals("java.lang.Class.forName")));
    final var methodNameMatches = methodCallExpr.getNameAsString().equals("forName");
    return scopeMatches && methodNameMatches;
  }
}
