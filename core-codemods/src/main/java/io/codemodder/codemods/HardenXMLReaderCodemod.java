package io.codemodder.codemods;

import com.contrastsecurity.sarif.Result;
import com.contrastsecurity.sarif.SarifSchema210;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import io.codemodder.*;
import io.codemodder.javaparser.ChangesResult;
import io.codemodder.providers.sarif.semgrep.SemgrepRunner;
import io.codemodder.providers.sarif.semgrep.SemgrepScan;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Disables external entity resolution in {@link org.xml.sax.XMLReader} use. This codemod takes a
 * different approach than similarly-purposed {@link HardenXMLInputFactoryCodemod}. It attempts to
 * inline the necessary calls to {@link org.xml.sax.XMLReader#setFeature(String, boolean)} to
 * disable external entities. It must be somewhat clever about this in case one is already present,
 * only presenting the one that's needed. We could do this inline with JavaParser inspection but it
 * is more robust to use Semgrep to determine which settings are needed.
 */
@Codemod(
    id = "pixee:java/harden-xmlreader",
    importance = Importance.HIGH,
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW)
public final class HardenXMLReaderCodemod extends SarifPluginJavaParserChanger<MethodCallExpr> {

  private final SemgrepRunner semgrepRunner;

  @Inject
  public HardenXMLReaderCodemod(@SemgrepScan(ruleId = "harden-xmlreader") final RuleSarif sarif) {
    super(sarif, MethodCallExpr.class, RegionNodeMatcher.MATCHES_START);
    this.semgrepRunner = SemgrepRunner.createDefault();
  }

  @Override
  public ChangesResult onResultFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final MethodCallExpr parseCall, // parse is a vulnerable parse() call on an XMLReader
      final Result result) {

    // job #1 -- check what setFeatures() are called to see which we don't need to add
    SettingsNeededToInject required;

    try {
      required = gatherRequiredSettings(context, parseCall);
    } catch (IOException e) {
      log.warn("issue running semgrep to figure out needed calls", e);
      return ChangesResult.noChanges;
    }

    Optional<Expression> scope = parseCall.getScope();
    if (scope.isEmpty()) {
      return ChangesResult.noChanges;
    }

    // add the required reader settings to prevent external entity resolution
    Expression reader = scope.get();
    List<Statement> statements = new ArrayList<>();
    if (required.externalGeneralEntities) {
      MethodCallExpr setFeature = new MethodCallExpr(reader, "setFeature");
      setFeature.addArgument(
          new StringLiteralExpr("http://xml.org/sax/features/external-general-entities"));
      setFeature.addArgument(new BooleanLiteralExpr(false));
      statements.add(new ExpressionStmt(setFeature));
    }
    if (required.externalParameterEntities) {
      MethodCallExpr setFeature = new MethodCallExpr(reader, "setFeature");
      setFeature.addArgument(
          new StringLiteralExpr("http://xml.org/sax/features/external-parameter-entities"));
      setFeature.addArgument(new BooleanLiteralExpr(false));
      statements.add(new ExpressionStmt(setFeature));
    }

    Optional<Statement> parseStatementRef = parseCall.findAncestor(Statement.class);

    if (parseStatementRef.isEmpty()) {
      return ChangesResult.noChanges;
    }
    Statement parseStatement = parseStatementRef.get();
    Optional<BlockStmt> blockStmt = parseStatement.findAncestor(BlockStmt.class);
    if (blockStmt.isEmpty()) {
      return ChangesResult.noChanges;
    }

    NodeList<Statement> existingStatements = blockStmt.get().getStatements();
    int parseStatementIndex = existingStatements.indexOf(parseStatement);
    existingStatements.addAll(parseStatementIndex, statements);
    return ChangesResult.changesApplied;
  }

  private SettingsNeededToInject gatherRequiredSettings(
      final CodemodInvocationContext context, final MethodCallExpr parseCall) throws IOException {
    Path needsBothRuleFile = createRuleFile("harden-xmlreader-needs-both.yaml");
    Path needsGeneralRuleFile = createRuleFile("harden-xmlreader-just-needs-general.yaml");
    Path needsParameterRuleFile = createRuleFile("harden-xmlreader-just-needs-parameter.yaml");

    try {
      return getSettingsNeededToInject(
          context, parseCall, needsBothRuleFile, needsGeneralRuleFile, needsParameterRuleFile);
    } finally {
      needsBothRuleFile.toFile().delete();
      needsGeneralRuleFile.toFile().delete();
      needsParameterRuleFile.toFile().delete();
    }
  }

  private SettingsNeededToInject getSettingsNeededToInject(
      final CodemodInvocationContext context,
      final MethodCallExpr parseCall,
      final Path needsBothRuleFile,
      final Path needsGeneralRuleFile,
      final Path needsParameterRuleFile)
      throws IOException {
    Path codeDir = context.codeDirectory().asPath();
    List<String> codeFilePath = List.of(context.path().toString());

    // find all .parse() calls that require both external calls -- probably the most common case
    SarifSchema210 sarif =
        semgrepRunner.run(List.of(needsBothRuleFile), codeDir, codeFilePath, List.of());

    needsBothRuleFile.toFile().delete();

    if (hasParseCallInResults(sarif, parseCall)) {
      return new SettingsNeededToInject(true, true);
    }

    // find all .parse() calls that require only external-general restrictions
    sarif = semgrepRunner.run(List.of(needsGeneralRuleFile), codeDir, codeFilePath, List.of());

    needsGeneralRuleFile.toFile().delete();

    if (hasParseCallInResults(sarif, parseCall)) {
      return new SettingsNeededToInject(true, false);
    }

    // find all .parse() calls that require one external-parameter restrictions
    sarif = semgrepRunner.run(List.of(needsParameterRuleFile), codeDir, codeFilePath, List.of());

    needsParameterRuleFile.toFile().delete();

    if (hasParseCallInResults(sarif, parseCall)) {
      return new SettingsNeededToInject(false, true);
    }

    // just return both and "fail closed" to make sure we send a fix, even if it's somehow overkill
    log.warn(
        "We matched the parse call but can't determine which settings are needed. Defaulting to both. Are all the YAML patterns aligned?");
    return new SettingsNeededToInject(true, true);
  }

  private Path createRuleFile(final String ruleFileName) throws IOException {
    // copy the classpath entry to disk so semgrep can read it
    Path tempFile = Files.createTempFile("xxe", ".yaml");
    tempFile.toFile().deleteOnExit();
    String classpathPath = "io/codemodder/codemods/" + ruleFileName;
    try (var is = getClass().getClassLoader().getResourceAsStream(classpathPath)) {
      Files.copy(is, tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }
    return tempFile;
  }

  private boolean hasParseCallInResults(
      final SarifSchema210 sarif, final MethodCallExpr parseCall) {
    return sarif.getRuns().get(0).getResults().stream()
        .map(r -> r.getLocations().get(0))
        .anyMatch(
            l ->
                l.getPhysicalLocation().getRegion().getStartLine()
                    == parseCall.getRange().get().begin.line);
  }

  /** Models which settings need to be injected. */
  record SettingsNeededToInject(
      boolean externalGeneralEntities, boolean externalParameterEntities) {}

  private static final Logger log = LoggerFactory.getLogger(HardenXMLReaderCodemod.class);
}
