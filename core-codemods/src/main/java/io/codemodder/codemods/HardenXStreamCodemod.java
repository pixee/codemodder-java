package io.codemodder.codemods;

import com.contrastsecurity.sarif.Result;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
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

      Optional<DependencyGAV>  xstreamDependencyOptional = getXstreamDependency(context) ;

      boolean useDenyTypes = xstreamDependencyOptional.isPresent() && isGreaterThanOrEqualTo(xstreamDependencyOptional.get().version(), "1.4.8");

    String nameAsString = newXStreamVariable.getNameAsString();
    Statement fixStatement = buildFixStatement(nameAsString);
    Statement existingStatement = newXStreamVariable.findAncestor(Statement.class).get();
    ASTTransforms.addStatementAfterStatement(existingStatement, fixStatement);
    ASTTransforms.addImportIfMissing(cu, "io.github.pixee.security.xstream.HardeningConverter");
    return true;
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

  private Optional<DependencyGAV> getXstreamDependency(final CodemodInvocationContext context){
      return context.dependencies().stream()
              .filter(dependency -> dependency.group().equals("com.thoughtworks.xstream") && dependency.artifact().equals("xstream"))
              .findFirst();
  }

    private static boolean isGreaterThanOrEqualTo(String version, String compareToVersion) {
        String[] versionParts = version.split("\\.");
        String[] compareToVersionParts = compareToVersion.split("\\.");

        // Compare major version
        int majorVersionComparison = Integer.compare(Integer.parseInt(versionParts[0]), Integer.parseInt(compareToVersionParts[0]));
        if (majorVersionComparison > 0) {
            return true; // Current version is greater
        } else if (majorVersionComparison < 0) {
            return false; // Current version is less
        }

        // Compare minor version
        int minorVersionComparison = Integer.compare(Integer.parseInt(versionParts[1]), Integer.parseInt(compareToVersionParts[1]));
        if (minorVersionComparison > 0) {
            return true; // Current version is greater
        } else if (minorVersionComparison < 0) {
            return false; // Current version is less
        }

        // Compare patch version
        int patchVersionComparison = Integer.compare(Integer.parseInt(versionParts[2]), Integer.parseInt(compareToVersionParts[2]));
        return patchVersionComparison >= 0;
    }

  @Override
  public List<DependencyGAV> dependenciesRequired() {
    return List.of(JAVA_SECURITY_TOOLKIT_XSTREAM);
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
