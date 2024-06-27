package io.codemodder.remediation.jndiinjection;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;
import io.codemodder.CodemodChange;
import io.codemodder.CodemodFileScanningResult;
import io.codemodder.DependencyGAV;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.codetf.FixedFinding;
import io.codemodder.codetf.UnfixedFinding;
import io.codemodder.remediation.FixCandidate;
import io.codemodder.remediation.FixCandidateSearchResults;
import io.codemodder.remediation.FixCandidateSearcher;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Function;
import java.util.function.ToIntFunction;

final class DefaultJNDIInjectionRemediator implements JNDIInjectionRemediator {

  private final JNDIFixStrategy fixStrategy;

  DefaultJNDIInjectionRemediator() {
    this(new ReplaceLimitedLookupStrategy());
  }

  DefaultJNDIInjectionRemediator(final JNDIFixStrategy fixStrategy) {
    this.fixStrategy = Objects.requireNonNull(fixStrategy);
  }

  @Override
  public <T> CodemodFileScanningResult remediateAll(
      final CompilationUnit cu,
      final String path,
      final DetectorRule detectorRule,
      final List<T> issuesForFile,
      final Function<T, String> getKey,
      final ToIntFunction<T> getLine,
      final Function<T, OptionalInt> getColumn) {

    FixCandidateSearcher<T> searcher =
        new FixCandidateSearcher.Builder<T>()
            .withMethodName("lookup")
            .withMatcher(mce -> mce.getScope().isPresent())
            .withMatcher(mce -> mce.getArguments().size() == 1)
            .withMatcher(mce -> mce.getArgument(0).isNameExpr())
            .build();

    // find all the potential lookup() calls
    FixCandidateSearchResults<T> results =
        searcher.search(cu, path, detectorRule, issuesForFile, getKey, getLine, getColumn);

    List<UnfixedFinding> unfixedFindings = new ArrayList<>();
    List<CodemodChange> changes = new ArrayList<>();

    for (FixCandidate<T> fixCandidate : results.fixCandidates()) {
      List<T> issues = fixCandidate.issues();
      int line = getLine.applyAsInt(issues.get(0));

      MethodCallExpr lookupCall = fixCandidate.methodCall();
      // get the parent method of the lookup() call
      Optional<MethodDeclaration> parentMethod = lookupCall.findAncestor(MethodDeclaration.class);
      if (parentMethod.isEmpty()) {
        issues.stream()
            .map(getKey)
            .map(
                findingId ->
                    new UnfixedFinding(
                        findingId, detectorRule, path, line, "No method found around lookup call"))
            .forEach(unfixedFindings::add);
        continue;
      }

      // confirm its a concrete type -- can't add validation method to
      ClassOrInterfaceDeclaration parentClass =
          parentMethod.get().findAncestor(ClassOrInterfaceDeclaration.class).get();
      if (parentClass.isInterface()) {
        issues.stream()
            .map(getKey)
            .map(
                findingId ->
                    new UnfixedFinding(
                        findingId,
                        detectorRule,
                        path,
                        line,
                        "Cannot add validation method to interface"))
            .forEach(unfixedFindings::add);
        continue;
      }

      Optional<Statement> lookupStatement = lookupCall.findAncestor(Statement.class);
      if (lookupStatement.isEmpty()) {
        issues.stream()
            .map(getKey)
            .map(
                findingId ->
                    new UnfixedFinding(
                        findingId,
                        detectorRule,
                        path,
                        line,
                        "No statement found around lookup call"))
            .forEach(unfixedFindings::add);
        continue;
      }

      // validate the shape of code around the lookup call to make sure its safe to add the call and
      // method
      NameExpr contextNameVariable = lookupCall.getArgument(0).asNameExpr();

      Optional<Node> lookupParentNode = lookupStatement.get().getParentNode();
      if (lookupParentNode.isEmpty()) {
        issues.stream()
            .map(getKey)
            .map(
                findingId ->
                    new UnfixedFinding(
                        findingId,
                        detectorRule,
                        path,
                        line,
                        "No parent node found around lookup call"))
            .forEach(unfixedFindings::add);
        continue;
      }

      if (!(lookupParentNode.get() instanceof BlockStmt blockStmt)) {
        issues.stream()
            .map(getKey)
            .map(
                findingId ->
                    new UnfixedFinding(
                        findingId,
                        detectorRule,
                        path,
                        line,
                        "No block statement found around lookup call"))
            .forEach(unfixedFindings::add);
        continue;
      }

      // add the validation call to the block statement
      int index = blockStmt.getStatements().indexOf(lookupStatement.get());
      List<DependencyGAV> deps =
          fixStrategy.fix(cu, parentClass, lookupCall, contextNameVariable, blockStmt, index);

      List<FixedFinding> fixedFindings =
          issues.stream()
              .map(getKey)
              .map(findingId -> new FixedFinding(findingId, detectorRule))
              .toList();

      changes.add(CodemodChange.from(line, deps, fixedFindings));
    }

    List<UnfixedFinding> allUnfixedFindings = new ArrayList<>(results.unfixableFindings());
    allUnfixedFindings.addAll(unfixedFindings);

    return CodemodFileScanningResult.from(changes, allUnfixedFindings);
  }
}
