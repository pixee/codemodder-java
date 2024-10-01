package io.codemodder.codemods.remediators.ssrf;

import static io.codemodder.ast.ASTTransforms.addImportIfMissing;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import io.codemodder.CodemodChange;
import io.codemodder.CodemodFileScanningResult;
import io.codemodder.DependencyGAV;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.codetf.FixedFinding;
import io.codemodder.codetf.UnfixedFinding;
import io.codemodder.remediation.FixCandidate;
import io.codemodder.remediation.FixCandidateSearchResults;
import io.codemodder.remediation.FixCandidateSearcher;
import io.codemodder.remediation.MethodOrConstructor;
import io.github.pixee.security.HostValidator;
import io.github.pixee.security.Urls;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Function;
import org.javatuples.Pair;

final class DefaultSSRFRemediator implements SSRFRemediator {

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

    // search for new URL() calls on those lines, assuming the tool points there -- there are plenty
    // of more signatures to chase down
    FixCandidateSearcher<T> searcher =
        new FixCandidateSearcher.Builder<T>()
            .withMatcher(mce -> mce.isConstructorForType("URL"))
            .withMatcher(mce -> !mce.getArguments().isEmpty())
            .build();

    // RestTemplate().exchange(url,...)
    FixCandidateSearcher<T> rtSearcher =
        new FixCandidateSearcher.Builder<T>()
            // is method with name
            .withMatcher(mce -> mce.isMethodCallWithName("exchange"))
            // has RestTemplate as scope
            .withMatcher(MethodOrConstructor::isMethodCallWithScope)
            // The type check below doesn't work
            // .withMatcher(mce -> mce.asMethodCall().getScope().filter(s ->
            // (("org.springframework.web.client" +
            //        ".RestTemplate").equals(s.calculateResolvedType().describe()))).isPresent())
            .build();

    List<CodemodChange> changes = new ArrayList<>();
    List<UnfixedFinding> unfixedFindings = new ArrayList<>();

    var pairResult =
        searchAndFix(
            searcher,
            (cunit, moc) -> harden(cunit, moc.asObjectCreationExpr()),
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

    var pairResultRT =
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
    changes.addAll(pairResultRT.getValue0());
    unfixedFindings.addAll(pairResultRT.getValue1());

    return CodemodFileScanningResult.from(changes, unfixedFindings);
  }

  private boolean harden(final CompilationUnit cu, final ObjectCreationExpr newUrlCall) {
    NodeList<Expression> arguments = newUrlCall.getArguments();

    /*
     * We need to replace:
     *
     * URL u = new URL(foo)
     *
     * With:
     *
     * import io.github.pixee.security.Urls;
     * ...
     * URL u = Urls.create(foo, io.github.pixee.security.Urls.HTTP_PROTOCOLS, io.github.pixee.security.HostValidator.ALLOW_ALL)
     */
    MethodCallExpr safeCall = wrapInUrlsCreate(cu, arguments);
    newUrlCall.replace(safeCall);
    return true;
  }

  private MethodCallExpr wrapInUrlsCreate(
      final CompilationUnit cu, final NodeList<Expression> arguments) {
    addImportIfMissing(cu, Urls.class.getName());
    addImportIfMissing(cu, HostValidator.class.getName());

    FieldAccessExpr httpProtocolsExpr = new FieldAccessExpr();
    httpProtocolsExpr.setScope(new NameExpr(Urls.class.getSimpleName()));
    httpProtocolsExpr.setName("HTTP_PROTOCOLS");

    FieldAccessExpr denyCommonTargetsExpr = new FieldAccessExpr();
    denyCommonTargetsExpr.setScope(new NameExpr(HostValidator.class.getSimpleName()));
    denyCommonTargetsExpr.setName("DENY_COMMON_INFRASTRUCTURE_TARGETS");

    NodeList<Expression> newArguments = new NodeList<>();
    newArguments.addAll(arguments); // add expression
    newArguments.add(httpProtocolsExpr); // load the protocols they're allowed
    newArguments.add(denyCommonTargetsExpr); // load the host validator

    return new MethodCallExpr(new NameExpr(Urls.class.getSimpleName()), "create", newArguments);
  }

  private boolean hardenRT(final CompilationUnit cu, final MethodCallExpr call) {
    var maybeFirstArg = call.getArguments().stream().findFirst();
    if (maybeFirstArg.isPresent()) {
      var wrappedArg =
          new MethodCallExpr(
              wrapInUrlsCreate(cu, new NodeList<>(maybeFirstArg.get().clone())), "toString");
      maybeFirstArg.get().replace(wrappedArg);
      return true;
    }
    return false;
  }

  /**
   * Returns a list of changes and unfixed findings for a pair of searcher, that gather relevant
   * issues, and a fixer predicate, that returns true if the change is successful.
   */
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
            CodemodChange.from(
                getStartLine.apply(issues.get(0)),
                List.of(DependencyGAV.JAVA_SECURITY_TOOLKIT),
                fixedFindings);
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
