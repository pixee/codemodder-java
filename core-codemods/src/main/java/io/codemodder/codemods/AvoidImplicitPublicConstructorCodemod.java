package io.codemodder.codemods;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import io.codemodder.*;
import io.codemodder.providers.sonar.ProvidedSonarScan;
import io.codemodder.providers.sonar.RuleIssues;
import io.codemodder.providers.sonar.SonarPluginJavaParserChanger;
import io.codemodder.providers.sonar.api.Issue;
import javax.inject.Inject;

/** A codemod for setting a private constructor to hide implicit public constructor (Sonar) */
@Codemod(
    id = "sonar:java/avoid-implicit-public-constructor-s1118",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW,
    executionPriority = CodemodExecutionPriority.HIGH)
public final class AvoidImplicitPublicConstructorCodemod
    extends SonarPluginJavaParserChanger<Node> {

  @Inject
  public AvoidImplicitPublicConstructorCodemod(
      @ProvidedSonarScan(ruleId = "java:S1118") final RuleIssues issues) {
    super(issues, Node.class, RegionNodeMatcher.MATCHES_START);
  }

  @Override
  public boolean onIssueFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final Node node,
      final Issue issue) {

    return false;
  }
}
