package io.codemodder.codemods;

import com.contrastsecurity.sarif.Result;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import io.codemodder.*;
import io.codemodder.ast.ASTTransforms;
import io.codemodder.providers.sarif.semgrep.SemgrepScan;
import io.github.pixee.security.SystemCommand;
import java.util.List;
import javax.inject.Inject;

/** Harden SnakeYAML . */
@Codemod(
    id = "pixee:java/harden-snakeyaml-construction",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW)
public final class HardenSnakeYamlCreationCodemod
    extends SarifPluginJavaParserChanger<MethodCallExpr> {

  private static final String RULE = """
          """;

  @Inject
  public HardenSnakeYamlCreationCodemod(
      @SemgrepScan(ruleId = "harden-snakeyaml-construction") final RuleSarif sarif) {
    super(sarif, MethodCallExpr.class);
  }

  @Override
  public boolean onResultFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final MethodCallExpr methodCallExpr,
      final Result result) {
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
}
