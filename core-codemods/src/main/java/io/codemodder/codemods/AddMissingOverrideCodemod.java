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

/** A codemod for automatically fixing missing @Override annotations. */
@Codemod(
    id = "sonar:java/add-missing-override-s1161",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW,
    importance = Importance.LOW,
    executionPriority = CodemodExecutionPriority.HIGH)
public final class AddMissingOverrideCodemod extends SonarPluginJavaParserChanger<SimpleName> {

  @Inject
  public AddMissingOverrideCodemod(
      @ProvidedSonarScan(ruleId = "java:S1161") final RuleIssues issues) {
    super(issues, SimpleName.class);
  }

  @Override
  public boolean onIssueFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final SimpleName methodName,
      final Issue issue) {

    Optional<Node> parentNodeRef = methodName.getParentNode();
    if (parentNodeRef.isPresent()) {
      Node parentNode = parentNodeRef.get();
      if (parentNode instanceof MethodDeclaration method) {
        method.addAnnotation(Override.class);
        return true;
      }
    }
    return true;
  }
}
