package io.codemodder.codemods.remediators.ssrf;

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
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.javatuples.Pair;

public final class WhitelistSSRFRemediator implements SSRFRemediator {

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

    // new URL(url) case
    FixCandidateSearcher<T> urlSearcher =
        new FixCandidateSearcher.Builder<T>()
            .withMatcher(mce -> mce.isConstructorForType("URL"))
            .withMatcher(mce -> !mce.getArguments().isEmpty())
            .build();

    // ...

    // RestTemplate().exchange(url,...)
    FixCandidateSearcher<T> rtSearcher =
        new FixCandidateSearcher.Builder<T>()
            // is method with name
            .withMatcher(mce -> mce.isMethodCallWithName("exchange"))
            // has RestTemplate as scope
            .withMatcher(MethodOrConstructor::isMethodCallWithScope)
            // .withMatcher(mce -> mce.asMethodCall().getScope().filter(s ->
            // (("org.springframework.web.client" +
            //        ".RestTemplate").equals(s.calculateResolvedType().describe()))).isPresent())
            .build();

    var pairResult =
        searchAndFix(
            rtSearcher,
            (cunit, moc) -> hardenRT(cunit, moc.asMethodCall()),
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

  static final String defaultMethodName = "filterURL";

  private static String generateFilterMethodName(final ClassOrInterfaceDeclaration classDecl) {
    var methodNames =
        classDecl.getMethods().stream()
            .map(CallableDeclaration::getNameAsString)
            .filter(s -> s.startsWith(defaultMethodName))
            .sorted()
            .collect(Collectors.toCollection(ArrayList::new));
    if (methodNames.isEmpty()) {
      return defaultMethodName;
    }
    String number = methodNames.get(methodNames.size() - 1).substring(defaultMethodName.length());
    if (number.isBlank()) {
      return defaultMethodName + "_1";
    }
    int num = (new SecureRandom()).nextInt();
    try {
      num = Integer.parseInt(number.substring(1)) + 1;
    } catch (NumberFormatException e) {
    }
    return defaultMethodName + "_" + num;
  }

  private static void addFilterMethod(
      final ClassOrInterfaceDeclaration classDecl, final String newMethodName) {
    final String method =
        """
                String %s(final String url){
                    var allowedHosts = List.of("");
                    if (!allowedHosts.contains(url)){
                        throw new SecurityException("Supplied URL is not allowed.");
                    }
                    return url;
                }
            """
            .formatted(newMethodName);
    classDecl.addMember(StaticJavaParser.parseMethodDeclaration(method));
  }

  private boolean hardenRT(final CompilationUnit cu, final MethodCallExpr call) {
    var maybeFirstArg =
        call.getArguments().stream()
            .findFirst()
            .filter(
                arg ->
                    !(arg.isMethodCallExpr()
                        && arg.asMethodCallExpr().getNameAsString().startsWith(defaultMethodName)));
    if (maybeFirstArg.isPresent()) {
      var maybeClassDecl = call.findAncestor(ClassOrInterfaceDeclaration.class);
      if (maybeClassDecl.isPresent()) {
        var newMethodName = generateFilterMethodName(maybeClassDecl.get());
        addFilterMethod(maybeClassDecl.get(), newMethodName);
        var wrappedArg = new MethodCallExpr(newMethodName, maybeFirstArg.get().clone());
        maybeFirstArg.get().replace(wrappedArg);
        return true;
      }
    }
    return false;
  }
}
