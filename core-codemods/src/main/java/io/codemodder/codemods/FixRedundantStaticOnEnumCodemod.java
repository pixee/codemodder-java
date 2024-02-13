package io.codemodder.codemods;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.EnumDeclaration;
import io.codemodder.*;
import io.codemodder.providers.sonar.ProvidedSonarScan;
import io.codemodder.providers.sonar.RuleIssues;
import io.codemodder.providers.sonar.SonarPluginJavaParserChanger;
import io.codemodder.providers.sonar.api.Issue;
import javax.inject.Inject;

/** A codemod for automatically removing redundant static flags on nested enums. */
@Codemod(
    id = "sonar:java/remove-redundant-static-s2786",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW,
    importance = Importance.LOW,
    executionPriority = CodemodExecutionPriority.HIGH)
public final class FixRedundantStaticOnEnumCodemod
    extends SonarPluginJavaParserChanger<EnumDeclaration> {

  @Inject
  public FixRedundantStaticOnEnumCodemod(
      @ProvidedSonarScan(ruleId = "java:S2786") final RuleIssues issues) {
    super(issues, EnumDeclaration.class);
  }

  @Override
  public boolean onIssueFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final EnumDeclaration enumDecl,
      final Issue issue) {
    if (enumDecl.isStatic()) {
      enumDecl.setStatic(false);
      return true;
    }
    return false;
  }
}
