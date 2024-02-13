package io.codemodder.codemods;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.SimpleName;
import io.codemodder.*;
import io.codemodder.providers.sonar.ProvidedSonarScan;
import io.codemodder.providers.sonar.RuleIssues;
import io.codemodder.providers.sonar.SonarPluginJavaParserChanger;
import io.codemodder.providers.sonar.api.Issue;
import java.util.Optional;
import javax.inject.Inject;

/** A codemod for removing unused private methods. */
@Codemod(
    id = "sonar:java/remove-unused-private-method-s1144",
    reviewGuidance = ReviewGuidance.MERGE_AFTER_REVIEW,
    importance = Importance.HIGH,
    executionPriority = CodemodExecutionPriority.HIGH)
public final class RemoveUnusedPrivateMethodCodemod
    extends SonarPluginJavaParserChanger<SimpleName> {

  @Inject
  public RemoveUnusedPrivateMethodCodemod(
      @ProvidedSonarScan(ruleId = "java:S1144") final RuleIssues issues) {
    super(issues, SimpleName.class);
  }

  @Override
  public boolean onIssueFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final SimpleName node,
      final Issue issue) {

    final Optional<Node> methodDeclarationOptional = node.getParentNode();

    if (methodDeclarationOptional.isEmpty()) {
      return false;
    }

    final MethodDeclaration methodDeclaration = (MethodDeclaration) methodDeclarationOptional.get();

    methodDeclaration.removeForced();

    return true;
  }
}
