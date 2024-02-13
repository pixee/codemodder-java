package io.codemodder.codemods;

import static io.codemodder.ast.ASTTransforms.addImportIfMissing;

import com.contrastsecurity.sarif.Result;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import io.codemodder.*;
import io.codemodder.providers.sarif.semgrep.SemgrepScan;
import java.security.SecureRandom;
import javax.inject.Inject;

/** Turns {@link java.util.Random} into {@link java.security.SecureRandom}. */
@Codemod(
    id = "pixee:java/secure-random",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW,
    importance = Importance.MEDIUM,
    executionPriority = CodemodExecutionPriority.LOW)
public final class SecureRandomCodemod extends SarifPluginJavaParserChanger<ObjectCreationExpr> {

  private static final String DETECTION_RULE =
      """
      rules:
        - id: secure-random
          pattern: new Random()
      """;

  @Inject
  public SecureRandomCodemod(@SemgrepScan(yaml = DETECTION_RULE) final RuleSarif sarif) {
    super(sarif, ObjectCreationExpr.class);
  }

  @Override
  public boolean onResultFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final ObjectCreationExpr objectCreationExpr,
      final Result result) {
    objectCreationExpr.setType("SecureRandom");
    addImportIfMissing(cu, SecureRandom.class.getName());
    return true;
  }
}
