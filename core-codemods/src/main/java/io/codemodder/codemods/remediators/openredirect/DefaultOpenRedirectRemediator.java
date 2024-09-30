package io.codemodder.codemods.remediators.openredirect;

import static io.codemodder.ast.ASTTransforms.addImportIfMissing;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.*;
import io.codemodder.CodemodChange;
import io.codemodder.CodemodFileScanningResult;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.codetf.FixedFinding;
import io.codemodder.codetf.UnfixedFinding;
import io.codemodder.remediation.FixCandidate;
import io.codemodder.remediation.FixCandidateSearchResults;
import io.codemodder.remediation.FixCandidateSearcher;
import io.codemodder.remediation.MethodOrConstructor;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.javatuples.Pair;

final class DefaultOpenRedirectRemediator implements OpenRedirectRemediator {

  private static final String DEFAULT_METHOD_NAME = "whitelistedRedirect";

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

    List<CodemodChange> changes = new ArrayList<>();
    List<UnfixedFinding> unfixedFindings = new ArrayList<>();

    // Match method calls
    // ois.readObject()
    FixCandidateSearcher<T> searcher =
        new FixCandidateSearcher.Builder<T>()
            .withMatcher(MethodOrConstructor::isMethodCallWithScope)
            .withMatcher(moc -> moc.isMethodCallWithName("sendRedirect"))
            .build();

    var pairResult =
        searchAndFix(
            searcher,
            (cunit, moc) -> harden(cunit, moc.asMethodCall()),
            cu,
            path,
            detectorRule,
            issuesForFile,
            getKey,
            getStartLine,
            getEndLine,
            getStartColumn);
    changes.addAll(pairResult.getValue0());
    unfixedFindings.addAll(pairResult.getValue1());

    return CodemodFileScanningResult.from(changes, unfixedFindings);
  }

  private static String generateFilterMethodName(final ClassOrInterfaceDeclaration classDecl) {
    var methodNames =
        classDecl.getMethods().stream()
            .map(CallableDeclaration::getNameAsString)
            .filter(s -> s.startsWith(DEFAULT_METHOD_NAME))
            .sorted()
            .collect(Collectors.toCollection(ArrayList::new));
    if (methodNames.isEmpty()) {
      return DEFAULT_METHOD_NAME;
    }
    String number = methodNames.get(methodNames.size() - 1).substring(DEFAULT_METHOD_NAME.length());
    if (number.isBlank()) {
      return DEFAULT_METHOD_NAME + "_1";
    }
    int num;
    try {
      num = Integer.parseInt(number.substring(1)) + 1;
    } catch (NumberFormatException e) {
      return DEFAULT_METHOD_NAME;
    }
    return DEFAULT_METHOD_NAME + "_" + num;
  }

  private static void addFilterMethod(
      final ClassOrInterfaceDeclaration classDecl, final String newMethodName) {
    final String method =
        """
                    void %s(final HttpServletResponse resp, final String url){
                        var allowedHosts = List.of("");
                        if (!allowedHosts.contains(url)){
                            throw new SecurityException("Supplied URL is not allowed.");
                        }
                        resp.sendRedirect(url);
                    }
                """
            .formatted(newMethodName);
    classDecl.addMember(StaticJavaParser.parseMethodDeclaration(method));
  }

  private boolean harden(final CompilationUnit cu, final MethodCallExpr call) {
    var maybeFirstArg =
        call.getArguments().stream()
            .findFirst()
            .filter(
                arg ->
                    !(arg.isMethodCallExpr()
                        && arg.asMethodCallExpr()
                            .getNameAsString()
                            .startsWith(DEFAULT_METHOD_NAME)));
    if (maybeFirstArg.isPresent()) {
      var maybeClassDecl = call.findAncestor(ClassOrInterfaceDeclaration.class);
      if (maybeClassDecl.isPresent()) {
        var newMethodName = generateFilterMethodName(maybeClassDecl.get());
        addFilterMethod(maybeClassDecl.get(), newMethodName);
        var newCall =
            new MethodCallExpr(newMethodName, call.getScope().get(), maybeFirstArg.get().clone());
        call.replace(newCall);
        addImportIfMissing(cu, SecurityException.class);
        return true;
      }
    }
    return false;
  }

  private <T> Pair<List<CodemodChange>, List<UnfixedFinding>> searchAndFix(
      final FixCandidateSearcher<T> searcher,
      final BiPredicate<CompilationUnit, MethodOrConstructor> fixer,
      final CompilationUnit cu,
      final String path,
      final DetectorRule detectorRule,
      final List<T> issuesForFile,
      final Function<T, String> getKey,
      final Function<T, Integer> getStartLine,
      final Function<T, Integer> getEndLine,
      final Function<T, Integer> getStartColumn) {
    List<CodemodChange> changes = new ArrayList<>();
    List<UnfixedFinding> unfixedFindings = new ArrayList<>();

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

    for (FixCandidate<T> candidate : results.fixCandidates()) {
      MethodOrConstructor call = candidate.call();
      List<T> issues = candidate.issues();
      if (fixer.test(cu, call)) {
        List<FixedFinding> fixedFindings =
            issues.stream()
                .map(issue -> new FixedFinding(getKey.apply(issue), detectorRule))
                .toList();
        CodemodChange change =
            CodemodChange.from(getStartLine.apply(issues.get(0)), List.of(), fixedFindings);
        changes.add(change);
      } else {
        issues.forEach(
            issue -> {
              final String id = getKey.apply(issue);
              final UnfixedFinding unfixableFinding =
                  new UnfixedFinding(
                      id,
                      detectorRule,
                      path,
                      getStartLine.apply(issues.get(0)),
                      "State changing effects possible or unrecognized code shape");
              unfixedFindings.add(unfixableFinding);
            });
      }
    }
    return Pair.with(changes, unfixedFindings);
  }
}
