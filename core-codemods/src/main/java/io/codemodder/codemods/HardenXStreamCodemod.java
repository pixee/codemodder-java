package io.codemodder.codemods;

import com.contrastsecurity.sarif.Result;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.zafarkhaja.semver.Version;
import io.codemodder.*;
import io.codemodder.ast.ASTTransforms;
import io.codemodder.javaparser.ChangesResult;
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
  public ChangesResult onResultFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final VariableDeclarator newXStreamVariable,
      final Result result) {

    final Optional<Statement> existingStatementOptional =
        newXStreamVariable.findAncestor(Statement.class);
    if (existingStatementOptional.isEmpty()) {
      return ChangesResult.noChanges;
    }
    final Statement existingStatement = existingStatementOptional.get();

    final String nameAsString = newXStreamVariable.getNameAsString();

    if (canUseDenyTypesByWildcard(context)) {
      final Statement fixStatement =
          StaticJavaParser.parseStatement(
              "UnwantedTypes.dangerousClassNameTokens().forEach( token -> { "
                  + nameAsString
                  + ".denyTypesByWildcard(new String[] { \"*\" + token + \"*\" });});");

      ASTTransforms.addStatementAfterStatement(existingStatement, fixStatement);
      ASTTransforms.addImportIfMissing(cu, "io.github.pixee.security.UnwantedTypes");
      return ChangesResult.changesAppliedWith(List.of(DependencyGAV.JAVA_SECURITY_TOOLKIT));
    }

    final Statement fixStatement = buildFixStatement(nameAsString);
    ASTTransforms.addStatementAfterStatement(existingStatement, fixStatement);
    ASTTransforms.addImportIfMissing(cu, "io.github.pixee.security.xstream.HardeningConverter");
    return ChangesResult.changesAppliedWith(List.of(JAVA_SECURITY_TOOLKIT_XSTREAM));
  }

  private boolean canUseDenyTypesByWildcard(final CodemodInvocationContext context) {
    final Optional<DependencyGAV> xstreamDependencyOptional = getXstreamDependency(context);

    if (xstreamDependencyOptional.isEmpty()) {
      return false;
    }

    try {
      final Version xtreamDependencyVersion =
          Version.valueOf(xstreamDependencyOptional.get().version());
      final Version comparableVersion = Version.valueOf("1.4.8");
      return xtreamDependencyVersion.greaterThanOrEqualTo(comparableVersion);
    } catch (final Exception e) {
      return false;
    }
  }

  private static Statement buildFixStatement(final String variableName) {
    final ExpressionStmt newStatement = new ExpressionStmt();
    final ObjectCreationExpr hardeningConverter = new ObjectCreationExpr();
    hardeningConverter.setType(new ClassOrInterfaceType("HardeningConverter"));
    final MethodCallExpr registerConverterCall =
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

  private static final DependencyGAV JAVA_SECURITY_TOOLKIT_XSTREAM =
      DependencyGAV.createDefault(
          "io.github.pixee",
          "java-security-toolkit-xstream",
          "1.0.2",
          "This library holds security APIs for hardening XStream operations.",
          DependencyLicenses.MIT,
          "https://github.com/pixee/java-security-toolkit-xstream",
          true);
}
