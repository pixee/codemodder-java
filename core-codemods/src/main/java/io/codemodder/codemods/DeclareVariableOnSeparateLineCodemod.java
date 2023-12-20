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
import io.codemodder.Codemod;
import io.codemodder.CodemodExecutionPriority;
import io.codemodder.CodemodInvocationContext;
import io.codemodder.ReviewGuidance;
import io.codemodder.providers.sonar.ProvidedSonarScan;
import io.codemodder.providers.sonar.RuleIssues;
import io.codemodder.providers.sonar.SonarPluginJavaParserChanger;
import io.codemodder.providers.sonar.api.Issue;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;

/** A codemod for declaring a variable on a separate line. */
@Codemod(
    id = "sonar:java/declare-variable-on-separate-line-s1659",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW,
    executionPriority = CodemodExecutionPriority.HIGH)
public final class DeclareVariableOnSeparateLineCodemod
    extends SonarPluginJavaParserChanger<VariableDeclarator> {

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

        if (parentOptional.isEmpty()) {
            return false;
        }

        final NodeWithVariables<?> parentNode = (NodeWithVariables<?>) parentOptional.get();

        final boolean isFieldDeclaration = parentNode instanceof FieldDeclaration;

        final List<VariableDeclarator> inlineVariables = parentNode.getVariables().stream().toList();
        final NodeList<Modifier> modifiers = getModifiers(parentNode, isFieldDeclaration);
        final NodeList<AnnotationExpr> annotations = isFieldDeclaration ? ((FieldDeclaration) parentNode).getAnnotations() : null;

        parentNode.setVariables(new NodeList<>(inlineVariables.get(0)));

        final List<Node> nodesToAdd = createNodesToAdd(isFieldDeclaration, inlineVariables, modifiers, annotations);

        if (isFieldDeclaration) {
            handleFieldDeclaration(parentNode, nodesToAdd);
        } else {
            handleVariableDeclarationExpr(parentNode, nodesToAdd);
        }

        return true;
    }

    private NodeList<Modifier> getModifiers(NodeWithVariables<?> parentNode, boolean isFieldDeclaration) {
        return isFieldDeclaration ?
                ((FieldDeclaration) parentNode).getModifiers() :
                ((VariableDeclarationExpr) parentNode).getModifiers();
    }

    private List<Node> createNodesToAdd(boolean isFieldDeclaration, List<VariableDeclarator> inlineVariables,
                                        NodeList<Modifier> modifiers, NodeList<AnnotationExpr> annotations) {
        List<Node> nodesToAdd = new ArrayList<>();
        for (int i = 1; i < inlineVariables.size(); i++) {
            if (isFieldDeclaration) {
                final FieldDeclaration fieldDeclaration =
                        new FieldDeclaration(modifiers, annotations, new NodeList<>(inlineVariables.get(i)));
                nodesToAdd.add(fieldDeclaration);
            } else {
                final VariableDeclarationExpr variableDeclarationExpr =
                        new VariableDeclarationExpr(modifiers, new NodeList<>(inlineVariables.get(i)));
                final ExpressionStmt expressionStmt = new ExpressionStmt(variableDeclarationExpr);
                nodesToAdd.add(expressionStmt);
            }
        }
        return nodesToAdd;
    }

    private void handleFieldDeclaration(NodeWithVariables<?> parentNode, List<Node> nodesToAdd) {
        Optional<Node> classOrInterfaceDeclarationOptional = ((FieldDeclaration) parentNode).getParentNode();
        if (classOrInterfaceDeclarationOptional.isPresent() &&
                classOrInterfaceDeclarationOptional.get() instanceof ClassOrInterfaceDeclaration classOrInterfaceDeclaration) {

            int index = classOrInterfaceDeclaration.getMembers().indexOf(parentNode);
            List<BodyDeclaration<?>> originalMembers = classOrInterfaceDeclaration.getMembers().stream().toList();

            List<BodyDeclaration<?>> membersBefore = originalMembers.subList(0, index + 1);
            List<BodyDeclaration<?>> membersAfter = originalMembers.subList(index + 1, originalMembers.size());

            List<BodyDeclaration<?>> allMembers = new ArrayList<>(membersBefore);
            nodesToAdd.forEach(node -> allMembers.add((BodyDeclaration<?>) node));
            allMembers.addAll(membersAfter);

            classOrInterfaceDeclaration.setMembers(new NodeList<>(allMembers));
        }
    }

    private void handleVariableDeclarationExpr(NodeWithVariables<?> parentNode, List<Node> nodesToAdd) {
        Optional<Node> expressionStmtOptional = ((VariableDeclarationExpr) parentNode).getParentNode();
        if (expressionStmtOptional.isPresent() && expressionStmtOptional.get() instanceof ExpressionStmt expressionStmt) {
            Optional<Node> blockStmtOptional = expressionStmt.getParentNode();
            if (blockStmtOptional.isPresent() && blockStmtOptional.get() instanceof BlockStmt blockStmt) {

                int index = blockStmt.getStatements().indexOf(expressionStmt);
                List<Statement> originalStmts = blockStmt.getStatements().stream().toList();

                List<Statement> stmtsBefore = originalStmts.subList(0, index + 1);
                List<Statement> stmtsAfter = originalStmts.subList(index + 1, originalStmts.size());

                List<Statement> allStmts = new ArrayList<>(stmtsBefore);
                nodesToAdd.forEach(node -> allStmts.add((Statement) node));
                allStmts.addAll(stmtsAfter);

                blockStmt.setStatements(new NodeList<>(allStmts));
            }
        }
    }

}
