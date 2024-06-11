package io.codemodder.codemods;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.SimpleName;
import io.codemodder.*;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.javaparser.ChangesResult;
import io.codemodder.providers.sonar.ProvidedSonarScan;
import io.codemodder.providers.sonar.RuleFinding;
import io.codemodder.providers.sonar.SonarFindingType;
import io.codemodder.providers.sonar.SonarPluginJavaParserChanger;
import io.codemodder.sonar.model.Issue;
import java.util.Optional;
import javax.inject.Inject;

import io.codemodder.sonar.model.SonarFinding;

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
      @ProvidedSonarScan(type = SonarFindingType.ISSUE, ruleId = "java:S1144")
          final RuleFinding issues) {
    super(issues, SimpleName.class);
  }

  @Override
  public ChangesResult onFindingFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final SimpleName node,
      final SonarFinding sonarFinding) {

    final Optional<Node> methodDeclarationOptional = node.getParentNode();

    if (methodDeclarationOptional.isEmpty()) {
      return ChangesResult.noChanges;
    }

    final MethodDeclaration methodDeclaration = (MethodDeclaration) methodDeclarationOptional.get();

    methodDeclaration.removeForced();

    return ChangesResult.changesApplied;
  }

  @Override
  public DetectorRule detectorRule() {
    return new DetectorRule(
        "java:S1144",
        "Unused private methods should be removed",
        "https://rules.sonarsource.com/java/RSPEC-1144");
  }
}
