package io.codemodder.codemods.sonar;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.expr.SimpleName;
import io.codemodder.*;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.javaparser.ChangesResult;
import io.codemodder.providers.sonar.ProvidedSonarScan;
import io.codemodder.providers.sonar.RuleIssue;
import io.codemodder.providers.sonar.SonarPluginJavaParserChanger;
import io.codemodder.sonar.model.Issue;
import java.util.Optional;
import javax.inject.Inject;

/** A codemod for setting a private constructor to hide implicit public constructor (Sonar) */
@Codemod(
    id = "sonar:java/avoid-implicit-public-constructor-s1118",
    reviewGuidance = ReviewGuidance.MERGE_AFTER_REVIEW,
    importance = Importance.LOW,
    executionPriority = CodemodExecutionPriority.HIGH)
public final class AvoidImplicitPublicConstructorCodemod
    extends SonarPluginJavaParserChanger<SimpleName, Issue> {

  @Inject
  public AvoidImplicitPublicConstructorCodemod(
      @ProvidedSonarScan(ruleId = "java:S1118") final RuleIssue issues) {
    super(issues, SimpleName.class);
  }

  @Override
  public ChangesResult onFindingFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final SimpleName simpleName,
      final Issue issue) {

    final Optional<Node> classOptional = simpleName.getParentNode();

    if (classOptional.isPresent()
        && classOptional.get() instanceof ClassOrInterfaceDeclaration classNode) {
      // Create a constructor
      final ConstructorDeclaration constructor = new ConstructorDeclaration();
      constructor.setName(classNode.getName());
      constructor.setModifiers(Modifier.privateModifier().getKeyword());

      // Add the constructor at the beginning of the class node's members
      NodeList<BodyDeclaration<?>> members = classNode.getMembers();
      members.add(0, constructor);

      // Update the class node's members
      classNode.setMembers(members);
      return ChangesResult.changesApplied;
    }

    return ChangesResult.noChanges;
  }

  @Override
  public DetectorRule detectorRule() {
    return new DetectorRule(
        "java:S1118",
        "Utility classes should not have public constructors",
        "https://rules.sonarsource.com/java/RSPEC-1118/");
  }
}
