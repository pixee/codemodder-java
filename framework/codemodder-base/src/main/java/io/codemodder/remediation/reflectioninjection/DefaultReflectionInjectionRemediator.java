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
import io.codemodder.remediation.FixCandidate;
import io.codemodder.remediation.FixCandidateSearchResults;
import io.codemodder.remediation.FixCandidateSearcher;
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
      final Function<T, Integer> getLine,
      final Function<T, Integer> getColumn) {

    FixCandidateSearcher<T> searcher =
        new FixCandidateSearcher.Builder<T>()
            .withMatcher(mce -> isClassForNameCall(cu, mce))
            .build();

    FixCandidateSearchResults<T> results =
        searcher.search(cu, path, detectorRule, issuesForFile, getKey, getLine, getColumn);
    List<CodemodChange> changes = new ArrayList<>();

    for (FixCandidate<T> fixCandidate : results.fixCandidates()) {
      T issue = fixCandidate.issue();
      String id = getKey.apply(issue);
      int line = getLine.apply(issue);
      MethodCallExpr methodCallExpr = fixCandidate.methodCall();
      replaceMethodCallExpression(cu, methodCallExpr);
      CodemodChange change =
          CodemodChange.from(
              line,
              List.of(DependencyGAV.JAVA_SECURITY_TOOLKIT),
              new FixedFinding(id, detectorRule));
      changes.add(change);
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
   * @param methodCallExpr the method call expression to check
   * @return true if the method call expression is a call to {@code Class.forName(String)}
   */
  private static boolean isClassForNameCall(
      final CompilationUnit cu, final MethodCallExpr methodCallExpr) {
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
