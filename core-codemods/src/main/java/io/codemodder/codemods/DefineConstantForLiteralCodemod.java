package io.codemodder.codemods;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import io.codemodder.*;
import io.codemodder.providers.sonar.ProvidedSonarScan;
import io.codemodder.providers.sonar.RuleIssues;
import io.codemodder.providers.sonar.SonarPluginJavaParserChanger;
import io.codemodder.providers.sonar.api.Issue;

import javax.inject.Inject;
import java.util.Optional;

/** A codemod for creating a constant for a literal string that is duplicated n times. */
@Codemod(
    id = "sonar:java/define-constant-for-duplicate-literal-s1192",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW,
    executionPriority = CodemodExecutionPriority.HIGH)
public final class DefineConstantForLiteralCodemod extends SonarPluginJavaParserChanger<StringLiteralExpr> {

  @Inject
  public DefineConstantForLiteralCodemod(
      @ProvidedSonarScan(ruleId = "java:S1192") final RuleIssues issues) {
    super(issues, StringLiteralExpr.class, RegionNodeMatcher.MATCHES_START);
  }

  @Override
  public boolean onIssueFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final StringLiteralExpr stringLiteralExpr,
      final Issue issue) {


        if(defineConstant(stringLiteralExpr)){

            return true;
        }

        return false;
  }

  private boolean defineConstant(
          final StringLiteralExpr stringLiteralExpr){
      final Optional<ClassOrInterfaceDeclaration> classOrInterfaceDeclarationOptional = stringLiteralExpr.findAncestor(ClassOrInterfaceDeclaration.class);

      if(classOrInterfaceDeclarationOptional.isPresent()){
          final String constantName = stringLiteralExpr.getValue().toUpperCase().concat("_CONSTANT");

          NodeList<Modifier> modifiers = NodeList.nodeList(Modifier.privateModifier(), Modifier.staticModifier(), Modifier.finalModifier());

          Type type = new ClassOrInterfaceType(null, "String");

          // Creating the VariableDeclarationExpr
          VariableDeclarator variableDeclarator = new VariableDeclarator();
          variableDeclarator.setInitializer(new StringLiteralExpr(stringLiteralExpr.getValue()));
          variableDeclarator.setType(type);
          variableDeclarator.setName(constantName);


          // Creating the FieldDeclaration
          FieldDeclaration fieldDeclaration = new FieldDeclaration(modifiers, variableDeclarator);

          final ClassOrInterfaceDeclaration classOrInterfaceDeclaration = classOrInterfaceDeclarationOptional.get();
          NodeList<BodyDeclaration<?>> members = classOrInterfaceDeclaration.getMembers();

          // Add field declaration to the top of the members of the class
          members.addFirst(fieldDeclaration);

          return true;

      }

      return false;
  }
}
