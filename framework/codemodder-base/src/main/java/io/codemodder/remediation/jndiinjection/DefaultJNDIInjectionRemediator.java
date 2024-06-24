package io.codemodder.remediation.jndiinjection;

import static io.codemodder.ast.ASTTransforms.addImportIfMissing;

import com.github.javaparser.StaticJavaParser;
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
import io.codemodder.codetf.DetectorRule;
import io.codemodder.codetf.FixedFinding;
import io.codemodder.codetf.UnfixedFinding;
import io.codemodder.remediation.FixCandidate;
import io.codemodder.remediation.FixCandidateSearchResults;
import io.codemodder.remediation.FixCandidateSearcher;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

final class DefaultJNDIInjectionRemediator implements JNDIInjectionRemediator {

  private final MethodDeclaration fixMethod;

  DefaultJNDIInjectionRemediator() {
    String fixMethodCode =
        """
                private static void validateResourceName(final String name) {
                    if (name != null) {
                      Set<String> illegalNames = Set.of("ldap://", "rmi://", "dns://", "java:");
                      String canonicalName = name.toLowerCase().trim();
                      if (illegalNames.stream().anyMatch(canonicalName::startsWith)) {
                        throw new SecurityException("Illegal JNDI resource name: " + name);
                      }
                    }
                }
                """;

    this.fixMethod = StaticJavaParser.parseMethodDeclaration(fixMethodCode);
  }

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
      T issue = fixCandidate.issue();
      String findingId = getKey.apply(issue);
      int line = getLine.apply(issue);

      MethodCallExpr lookupCall = fixCandidate.methodCall();
      // get the parent method of the lookup() call
      Optional<MethodDeclaration> parentMethod = lookupCall.findAncestor(MethodDeclaration.class);
      if (parentMethod.isEmpty()) {
        UnfixedFinding unfixedFinding =
            new UnfixedFinding(
                findingId, detectorRule, path, line, "No method found around lookup call");
        unfixedFindings.add(unfixedFinding);
        continue;
      }

      // confirm its a concrete type -- can't add validation method to
      ClassOrInterfaceDeclaration parentClass =
          parentMethod.get().findAncestor(ClassOrInterfaceDeclaration.class).get();
      if (parentClass.isInterface()) {
        UnfixedFinding unfixedFinding =
            new UnfixedFinding(
                findingId, detectorRule, path, line, "Cannot add validation method to interface");
        unfixedFindings.add(unfixedFinding);
        continue;
      }

      Optional<Statement> lookupStatement = lookupCall.findAncestor(Statement.class);
      if (lookupStatement.isEmpty()) {
        UnfixedFinding unfixedFinding =
            new UnfixedFinding(
                findingId, detectorRule, path, line, "No statement found around lookup call");
        unfixedFindings.add(unfixedFinding);
        continue;
      }

      // validate the shape of code around the lookup call to make sure its safe to add the call and
      // method
      NameExpr contextNameVariable = lookupCall.getArgument(0).asNameExpr();
      MethodCallExpr validationCall = new MethodCallExpr(null, validateResourceMethodName);
      validationCall.addArgument(contextNameVariable);

      Optional<Node> lookupParentNode = lookupStatement.get().getParentNode();
      if (lookupParentNode.isEmpty()) {
        UnfixedFinding unfixedFinding =
            new UnfixedFinding(
                findingId, detectorRule, path, line, "No parent node found around lookup call");
        unfixedFindings.add(unfixedFinding);
        continue;
      }

      if (!(lookupParentNode.get() instanceof BlockStmt)) {
        UnfixedFinding unfixedFinding =
            new UnfixedFinding(
                findingId, detectorRule, path, line, "No block statement found around lookup call");
        unfixedFindings.add(unfixedFinding);
        continue;
      }

      // add the validation call to the block statement
      BlockStmt blockStmt = (BlockStmt) lookupParentNode.get();
      int index = blockStmt.getStatements().indexOf(lookupStatement.get());
      blockStmt.addStatement(index, validationCall);

      // add the validation method if it's not already present
      boolean alreadyHasResourceValidationCallPresent =
          parentClass.findAll(MethodDeclaration.class).stream()
              .anyMatch(
                  md ->
                      md.getNameAsString().equals(validateResourceMethodName)
                          && md.getParameters().size() == 1
                          && md.getParameters().get(0).getTypeAsString().equals("String"));

      if (!alreadyHasResourceValidationCallPresent) {
        parentClass.addMember(fixMethod);
        addImportIfMissing(cu, Set.class);
      }

      changes.add(CodemodChange.from(line, new FixedFinding(findingId, detectorRule)));
    }

    List<UnfixedFinding> allUnfixedFindings = new ArrayList<>(results.unfixableFindings());
    allUnfixedFindings.addAll(unfixedFindings);

    return CodemodFileScanningResult.from(changes, allUnfixedFindings);
  }

  private static final String validateResourceMethodName = "validateResourceName";
}
