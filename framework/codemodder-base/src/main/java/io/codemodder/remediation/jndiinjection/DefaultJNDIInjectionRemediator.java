package io.codemodder.remediation.jndiinjection;

import static io.codemodder.ast.ASTTransforms.addImportIfMissing;
import static io.codemodder.remediation.RemediationMessages.multipleCallsFound;
import static io.codemodder.remediation.RemediationMessages.noCallsAtThatLocation;

import com.github.javaparser.Position;
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

    List<UnfixedFinding> unfixedFindings = new ArrayList<>();
    List<CodemodChange> changes = new ArrayList<>();

    List<MethodCallExpr> lookupCalls =
        cu.findAll(MethodCallExpr.class).stream()
            .filter(mce -> mce.getNameAsString().equals("lookup"))
            .filter(mce -> mce.getScope().isPresent() && mce.getScope().get().isNameExpr())
            .filter(mce -> mce.getArguments().size() == 1)
            .filter(mce -> mce.getArguments().get(0).isNameExpr())
            .toList();

    for (T issue : issuesForFile) {
      String key = getKey.apply(issue);
      int line = getLine.apply(issue);
      Integer column = getColumn.apply(issue);

      List<MethodCallExpr> candidateLookupCalls =
          lookupCalls.stream().filter(mce -> mce.getRange().get().begin.line == line).toList();

      if (candidateLookupCalls.size() > 1 && column != null) {
        candidateLookupCalls =
            candidateLookupCalls.stream()
                .filter(mce -> mce.getRange().get().contains(new Position(line, column)))
                .toList();
      }

      if (candidateLookupCalls.isEmpty()) {
        UnfixedFinding unfixedFinding =
            new UnfixedFinding(key, detectorRule, path, line, noCallsAtThatLocation);
        unfixedFindings.add(unfixedFinding);
        continue;
      } else if (candidateLookupCalls.size() > 1) {
        UnfixedFinding unfixedFinding =
            new UnfixedFinding(key, detectorRule, path, line, multipleCallsFound);
        unfixedFindings.add(unfixedFinding);
        continue;
      }

      MethodCallExpr lookupCall = candidateLookupCalls.get(0);

      // get the parent method of the lookup() call
      Optional<MethodDeclaration> parentMethod = lookupCall.findAncestor(MethodDeclaration.class);
      if (parentMethod.isEmpty()) {
        UnfixedFinding unfixedFinding =
            new UnfixedFinding(key, detectorRule, path, line, "No method found around lookup call");
        unfixedFindings.add(unfixedFinding);
        continue;
      }

      // confirm its a concrete type -- can't add validation method to
      ClassOrInterfaceDeclaration parentClass =
          parentMethod.get().findAncestor(ClassOrInterfaceDeclaration.class).get();
      if (parentClass.isInterface()) {
        UnfixedFinding unfixedFinding =
            new UnfixedFinding(
                key, detectorRule, path, line, "Cannot add validation method to interface");
        unfixedFindings.add(unfixedFinding);
        continue;
      }

      Optional<Statement> lookupStatement = lookupCall.findAncestor(Statement.class);
      if (lookupStatement.isEmpty()) {
        UnfixedFinding unfixedFinding =
            new UnfixedFinding(
                key, detectorRule, path, line, "No statement found around lookup call");
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
                key, detectorRule, path, line, "No parent node found around lookup call");
        unfixedFindings.add(unfixedFinding);
        continue;
      }

      if (!(lookupParentNode.get() instanceof BlockStmt)) {
        UnfixedFinding unfixedFinding =
            new UnfixedFinding(
                key, detectorRule, path, line, "No block statement found around lookup call");
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

      changes.add(CodemodChange.from(line, new FixedFinding(key, detectorRule)));
    }

    return CodemodFileScanningResult.from(changes, unfixedFindings);
  }

  private static final String validateResourceMethodName = "validateResourceName";
}
