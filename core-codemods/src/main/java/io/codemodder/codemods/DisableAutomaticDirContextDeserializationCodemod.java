package io.codemodder.codemods;

import com.contrastsecurity.sarif.Result;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import io.codemodder.*;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.javaparser.ChangesResult;
import io.codemodder.providers.sarif.semgrep.SemgrepScan;
import javax.inject.Inject;

/** Disables automatic return of objects in {@code javax.naming.DirContext#search}. */
@Codemod(
    id = "pixee:java/disable-dircontext-deserialization",
    importance = Importance.HIGH,
    reviewGuidance = ReviewGuidance.MERGE_AFTER_CURSORY_REVIEW)
public final class DisableAutomaticDirContextDeserializationCodemod
    extends SarifPluginJavaParserChanger<ObjectCreationExpr> implements FixOnlyCodeChanger {

  @Inject
  public DisableAutomaticDirContextDeserializationCodemod(
      @SemgrepScan(ruleId = "disable-dircontext-deserialization") final RuleSarif sarif) {
    super(sarif, ObjectCreationExpr.class, SourceCodeRegionExtractor.FROM_SARIF_FIRST_LOCATION);
  }

  @Override
  public ChangesResult onResultFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final ObjectCreationExpr objectCreationExpr,
      final Result result) {
    objectCreationExpr.setArgument(4, new BooleanLiteralExpr(false));
    return ChangesResult.changesApplied;
  }

  @Override
  public String vendorName() {
    return "Semgrep";
  }

  @Override
  public DetectorRule getDetectorRule() {
    return new DetectorRule(
        "disable-dircontext-deserialization",
        "Harden LDAP call against deserialization attacks",
        "https://www.blackhat.com/docs/us-16/materials/us-16-Munoz-A-Journey-From-JNDI-LDAP-Manipulation-To-RCE.pdf");
  }
}
