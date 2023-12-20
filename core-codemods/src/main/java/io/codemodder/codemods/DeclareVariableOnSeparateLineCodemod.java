package io.codemodder.codemods;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithVariables;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.Type;
import io.codemodder.Codemod;
import io.codemodder.CodemodExecutionPriority;
import io.codemodder.CodemodInvocationContext;
import io.codemodder.ReviewGuidance;
import io.codemodder.providers.sonar.ProvidedSonarScan;
import io.codemodder.providers.sonar.RuleIssues;
import io.codemodder.providers.sonar.SonarPluginJavaParserChanger;
import io.codemodder.providers.sonar.api.Issue;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** A codemod for declaring a variable on a separate line. */
@Codemod(
    id = "sonar:java/declare-variable-on-separate-line-s1659",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW,
    executionPriority = CodemodExecutionPriority.HIGH)
public final class DeclareVariableOnSeparateLineCodemod extends SonarPluginJavaParserChanger<VariableDeclarator> {

  @Inject
  public DeclareVariableOnSeparateLineCodemod(
      @ProvidedSonarScan(ruleId = "java:S1659") final RuleIssues issues) {
    super(issues, VariableDeclarator.class);
  }

  @Override
  public boolean onIssueFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final VariableDeclarator variableDeclarator,
      final Issue issue) {

      final Optional<Node> parentOptional = variableDeclarator.getParentNode();

      if(parentOptional.isEmpty()){
          return false;
      }

      final NodeWithVariables<?> parentNode = (NodeWithVariables<?>) parentOptional.get();

      final List<VariableDeclarator> inlineVariables = parentNode.getVariables().stream().toList();

      final boolean isFieldDeclaration = parentNode instanceof FieldDeclaration;

      final NodeList<Modifier> modifiers =  isFieldDeclaration ? ((FieldDeclaration) parentNode).getModifiers() : ((VariableDeclarationExpr) parentNode).getModifiers();

      final NodeList<AnnotationExpr> annotations = isFieldDeclaration ? ((FieldDeclaration) parentNode).getAnnotations() : null;

      parentNode.setVariables( new NodeList<>(inlineVariables.get(0)) );

      final List<Node> nodesToAdd = new ArrayList<>();
      for(int i = 1; i<inlineVariables.size(); i++){

          if(isFieldDeclaration){
              final FieldDeclaration fieldDeclaration = new FieldDeclaration(modifiers, annotations,  new NodeList<>(inlineVariables.get(i)));
              nodesToAdd.add(fieldDeclaration);
          } else {
              final VariableDeclarationExpr variableDeclarationExpr = new VariableDeclarationExpr(modifiers, new NodeList<>(inlineVariables.get(i)));
              final ExpressionStmt expressionStmt = new ExpressionStmt(variableDeclarationExpr);
              nodesToAdd.add(expressionStmt);
          }

      }

      if(isFieldDeclaration){
          final Optional<Node> classOrInterfaceDeclarationOptional = ((FieldDeclaration) parentNode).getParentNode();

          if (classOrInterfaceDeclarationOptional.isPresent() && classOrInterfaceDeclarationOptional.get() instanceof ClassOrInterfaceDeclaration classOrInterfaceDeclaration) {

              final List<BodyDeclaration<?>> originalMembers= classOrInterfaceDeclaration.getMembers().stream().toList();

              final int index = originalMembers.indexOf(parentNode);

              final List<BodyDeclaration<?>> membersBefore = new ArrayList<>();
              final List<BodyDeclaration<?>> membersAfter = new ArrayList<>();

              for (int i = 0; i < originalMembers.size(); i++){
                  if(i <= index){
                      membersBefore.add(originalMembers.get(i));
                  } else {
                      membersAfter.add(originalMembers.get(i));
                  }
              }

              final List<BodyDeclaration<?>> allMembers = new ArrayList<>(membersBefore);
              nodesToAdd.forEach( node -> allMembers.add((BodyDeclaration<?>) node));
              allMembers.addAll(membersAfter);


              classOrInterfaceDeclaration.setMembers(new NodeList<>( allMembers));
          }
      } else {
          final Optional<Node> expressionStmtOptional = ((VariableDeclarationExpr) parentNode).getParentNode();
          if (expressionStmtOptional.isPresent() && expressionStmtOptional.get() instanceof ExpressionStmt expressionStmt) {
              final Optional<Node> blockStmtOptional = expressionStmt.getParentNode();
              if(blockStmtOptional.isPresent() && blockStmtOptional.get() instanceof BlockStmt blockStmt){
                  final List<Statement> originalStmts = blockStmt.getStatements().stream().toList();
                  final int index = originalStmts.indexOf(expressionStmt);

                  final List<Statement> stmtsBefore = new ArrayList<>();
                  final List<Statement> stmtsAfter = new ArrayList<>();

                  for(int i = 0; i < originalStmts.size() ; i++){
                      if(i <= index){
                          stmtsBefore.add(originalStmts.get(i));
                      } else {
                          stmtsAfter.add(originalStmts.get(i));
                      }
                  }

                  final List<Statement> allStmts = new ArrayList<>(stmtsBefore);
                  nodesToAdd.forEach( node -> allStmts.add((Statement) node));
                  allStmts.addAll(stmtsAfter);

                  blockStmt.setStatements(new NodeList<>(allStmts));
              }
          }
      }

      return true;
  }


}
