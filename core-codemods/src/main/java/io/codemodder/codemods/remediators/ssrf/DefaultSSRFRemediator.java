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
import io.github.pixee.security.HostValidator;
import io.github.pixee.security.Urls;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

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

    List<CodemodChange> changes = new ArrayList<>();

    for (FixCandidate<T> candidate : results.fixCandidates()) {
      ObjectCreationExpr call = (ObjectCreationExpr) candidate.call().asNode();
      List<T> issues = candidate.issues();
      harden(cu, call);
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
    }

    List<UnfixedFinding> unfixedFindings = new ArrayList<>(results.unfixableFindings());
    return CodemodFileScanningResult.from(changes, unfixedFindings);
  }

  private void harden(final CompilationUnit cu, final ObjectCreationExpr newUrlCall) {
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
    addImportIfMissing(cu, Urls.class.getName());
    addImportIfMissing(cu, HostValidator.class.getName());
    FieldAccessExpr httpProtocolsExpr = new FieldAccessExpr();
    httpProtocolsExpr.setScope(new NameExpr(Urls.class.getSimpleName()));
    httpProtocolsExpr.setName("HTTP_PROTOCOLS");

    FieldAccessExpr denyCommonTargetsExpr = new FieldAccessExpr();

    denyCommonTargetsExpr.setScope(new NameExpr(HostValidator.class.getSimpleName()));
    denyCommonTargetsExpr.setName("DENY_COMMON_INFRASTRUCTURE_TARGETS");

    NodeList<Expression> newArguments = new NodeList<>();
    newArguments.addAll(arguments); // first are all the arguments they were passing to "new URL"
    newArguments.add(httpProtocolsExpr); // load the protocols they're allowed
    newArguments.add(denyCommonTargetsExpr); // load the host validator
    MethodCallExpr safeCall =
        new MethodCallExpr(new NameExpr(Urls.class.getSimpleName()), "create", newArguments);
    newUrlCall.replace(safeCall);
  }
}
