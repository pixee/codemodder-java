package io.codemodder.codemods;

import com.contrastsecurity.sarif.Result;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.ArrayCreationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.UnknownType;
import io.codemodder.*;
import io.codemodder.ast.ASTTransforms;
import io.codemodder.providers.sarif.semgrep.SemgrepScan;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;

/** Adds gadget filtering logic to XStream deserialization. */
@Codemod(
    id = "pixee:java/harden-xstream",
    importance = Importance.HIGH,
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW)
public final class HardenXStreamCodemod extends SarifPluginJavaParserChanger<VariableDeclarator> {

  @Inject
  public HardenXStreamCodemod(@SemgrepScan(ruleId = "harden-xstream") final RuleSarif sarif) {
    super(sarif, VariableDeclarator.class, RegionNodeMatcher.MATCHES_START);
  }

  @Override
  public boolean onResultFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final VariableDeclarator newXStreamVariable,
      final Result result) {

    Optional<DependencyGAV> xstreamDependencyOptional = getXstreamDependency(context);

    useDenyTypes =
        xstreamDependencyOptional.isPresent()
            && isGreaterThanOrEqualTo(xstreamDependencyOptional.get().version(), "1.4.8");

    String nameAsString = newXStreamVariable.getNameAsString();

    if (useDenyTypes) {
      Statement fixStatement = buildDenyTypesByWildcardStatement(nameAsString);
      Statement existingStatement = newXStreamVariable.findAncestor(Statement.class).get();
      ASTTransforms.addStatementAfterStatement(existingStatement, fixStatement);
      ASTTransforms.addImportIfMissing(cu, "io.github.pixee.security.UnwantedTypes");
    } else {
      Statement fixStatement = buildFixStatement(nameAsString);
      Statement existingStatement = newXStreamVariable.findAncestor(Statement.class).get();
      ASTTransforms.addStatementAfterStatement(existingStatement, fixStatement);
      ASTTransforms.addImportIfMissing(cu, "io.github.pixee.security.xstream.HardeningConverter");
    }

    return true;
  }

  private Statement buildDenyTypesByWildcardStatement(final String variableName) {

    // represents: "*" + token + "*"
    final BinaryExpr stringTokenExpression =
        new BinaryExpr(
            new BinaryExpr(
                new StringLiteralExpr("*"), new NameExpr("token"), BinaryExpr.Operator.PLUS),
            new StringLiteralExpr("*"),
            BinaryExpr.Operator.PLUS);

    // represents: new String[] { "*" + token + "*" }
    final ArrayCreationExpr stringArray =
        new ArrayCreationExpr(
            new ArrayType(new ClassOrInterfaceType("String")),
            NodeList.nodeList(),
            new ArrayInitializerExpr(NodeList.nodeList(stringTokenExpression)));

    // represents: xstream.denyTypesByWildcard(new String[] { "*" + token + "*" });
    MethodCallExpr registerConverterCall = new MethodCallExpr("denyTypesByWildcard", stringArray);
    ExpressionStmt newStatement = new ExpressionStmt();
    newStatement.setExpression(registerConverterCall);
    registerConverterCall.setScope(new NameExpr(variableName));

    // represents: UnwantedTypes.dangerousClassNameTokens()
    final MethodCallExpr unwantedTypesDangerousClassNameTokensMethod =
        new MethodCallExpr(new NameExpr("UnwantedTypes"), "dangerousClassNameTokens");

    // represents UnwantedTypes.dangerousClassNameTokens().forEach()
    MethodCallExpr forEachMethodCall =
        new MethodCallExpr(unwantedTypesDangerousClassNameTokensMethod, "forEach");

    /*
    Represents:
    UnwantedTypes.dangerousClassNameTokens().forEach( token -> {
            xstream.denyTypesByWildcard(new String[] { "*" + token + "*" });
        });
     */
    forEachMethodCall.addArgument(
        new LambdaExpr(
            new Parameter(new UnknownType(), "token"),
            new BlockStmt(NodeList.nodeList(newStatement))));

    ExpressionStmt newStatement2 = new ExpressionStmt();
    newStatement2.setExpression(forEachMethodCall);

    return newStatement2;
  }

  private static Statement buildFixStatement(final String variableName) {
    ExpressionStmt newStatement = new ExpressionStmt();
    ObjectCreationExpr hardeningConverter = new ObjectCreationExpr();
    hardeningConverter.setType(new ClassOrInterfaceType("HardeningConverter"));
    MethodCallExpr registerConverterCall =
        new MethodCallExpr("registerConverter", hardeningConverter);
    newStatement.setExpression(registerConverterCall);
    registerConverterCall.setScope(new NameExpr(variableName));
    return newStatement;
  }

  private Optional<DependencyGAV> getXstreamDependency(final CodemodInvocationContext context) {
    return context.dependencies().stream()
        .filter(
            dependency ->
                "com.thoughtworks.xstream".equals(dependency.group())
                    && "xstream".equals(dependency.artifact()))
        .findFirst();
  }

  private static boolean isGreaterThanOrEqualTo(String version, String compareToVersion) {
    String[] versionParts = version.split("\\.");
    String[] compareToVersionParts = compareToVersion.split("\\.");

    // Compare major version
    int majorVersionComparison =
        Integer.compare(
            Integer.parseInt(versionParts[0]), Integer.parseInt(compareToVersionParts[0]));
    if (majorVersionComparison > 0) {
      return true; // Current version is greater
    } else if (majorVersionComparison < 0) {
      return false; // Current version is less
    }

    // Compare minor version
    int minorVersionComparison =
        Integer.compare(
            Integer.parseInt(versionParts[1]), Integer.parseInt(compareToVersionParts[1]));
    if (minorVersionComparison > 0) {
      return true; // Current version is greater
    } else if (minorVersionComparison < 0) {
      return false; // Current version is less
    }

    // Compare patch version
    int patchVersionComparison =
        Integer.compare(
            Integer.parseInt(versionParts[2]), Integer.parseInt(compareToVersionParts[2]));
    return patchVersionComparison >= 0;
  }

  @Override
  public List<DependencyGAV> dependenciesRequired() {

    return useDenyTypes
        ? List.of(DependencyGAV.JAVA_SECURITY_TOOLKIT)
        : List.of(JAVA_SECURITY_TOOLKIT_XSTREAM);
  }

  private static final DependencyGAV JAVA_SECURITY_TOOLKIT_XSTREAM =
      DependencyGAV.createDefault(
          "io.github.pixee",
          "java-security-toolkit-xstream",
          "1.0.2",
          "This library holds security APIs for hardening XStream operations.",
          DependencyLicenses.MIT,
          "https://github.com/pixee/java-security-toolkit-xstream",
          true);

  private static boolean useDenyTypes;
}
