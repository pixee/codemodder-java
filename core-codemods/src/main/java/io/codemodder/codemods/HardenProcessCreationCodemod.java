package io.codemodder.codemods;

import com.contrastsecurity.sarif.Result;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.SimpleName;
import io.codemodder.*;
import io.codemodder.ast.ASTTransforms;
import io.codemodder.providers.sarif.semgrep.SemgrepScan;
import io.github.pixee.security.SystemCommand;
import java.util.List;
import javax.inject.Inject;

/** Harden new process creation. */
@Codemod(
    id = "pixee:java/harden-process-creation",
    importance = Importance.HIGH,
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW)
public final class HardenProcessCreationCodemod
    extends SarifPluginJavaParserChanger<MethodCallExpr> {

  @Inject
  public HardenProcessCreationCodemod(
      @SemgrepScan(ruleId = "harden-process-creation") final RuleSarif sarif) {
    super(sarif, MethodCallExpr.class);
  }

  @Override
  public boolean onResultFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final MethodCallExpr methodCallExpr,
      final Result result) {

    final List<FieldDeclaration> fieldDeclarations = cu.findAll(FieldDeclaration.class);
    final List<FieldDeclaration> constantFieldDeclarations =
        getConstantFieldDeclarations(fieldDeclarations);
    final List<SimpleName> constantNames = extractVariableNames(constantFieldDeclarations);

    final List<Expression> allArgumentsExpressions =
        ArgumentExpressionExtractor.extractExpressions(methodCallExpr);

    final List<Expression> onlyVariableExpressions =
        allArgumentsExpressions.stream()
            .filter(Expression::isNameExpr)
            .filter(
                expression -> {
                  final NameExpr nameExpr = (NameExpr) expression;
                  return !constantNames.contains(nameExpr.getName());
                })
            .toList();

    final boolean containsOnlyConstantsAndHardcodedValues = onlyVariableExpressions.isEmpty();

    if (containsOnlyConstantsAndHardcodedValues) {
      return false;
    }

    Node parent = methodCallExpr.getParentNode().get();
    Expression scope = methodCallExpr.getScope().get();
    ASTTransforms.addImportIfMissing(cu, SystemCommand.class);
    NameExpr callbackClass = new NameExpr(SystemCommand.class.getSimpleName());
    MethodCallExpr safeExpression = new MethodCallExpr(callbackClass, "runCommand");
    NodeList<Expression> nodeList = new NodeList<>();
    nodeList.add(scope);
    nodeList.addAll(methodCallExpr.getArguments());
    safeExpression.setArguments(nodeList);

    parent.replace(methodCallExpr, safeExpression);
    return true;
  }

  @Override
  public List<DependencyGAV> dependenciesRequired() {
    return List.of(DependencyGAV.JAVA_SECURITY_TOOLKIT);
  }

  private List<SimpleName> extractVariableNames(List<FieldDeclaration> fieldDeclarations) {
    return fieldDeclarations.stream()
        .flatMap(fieldDeclaration -> fieldDeclaration.getVariables().stream())
        .map(VariableDeclarator::getName)
        .toList();
  }

  private List<FieldDeclaration> getConstantFieldDeclarations(
      final List<FieldDeclaration> fieldDeclarations) {

    return fieldDeclarations.stream()
        .filter(FieldDeclaration::isFinal) // Check if the fieldDeclaration has the 'final' modifier
        .filter(
            fieldDeclaration ->
                fieldDeclaration.getVariables().stream()
                    .allMatch(
                        variable ->
                            variable
                                .getInitializer()
                                .isPresent())) // Check if all variables have initializers
        .toList();
  }
}
