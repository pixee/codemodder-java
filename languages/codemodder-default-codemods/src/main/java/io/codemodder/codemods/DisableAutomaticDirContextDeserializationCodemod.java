package io.codemodder.codemods;

import com.contrastsecurity.sarif.Result;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import io.codemodder.Codemod;
import io.codemodder.CodemodInvocationContext;
import io.codemodder.RegionExtractor;
import io.codemodder.ReviewGuidance;
import io.codemodder.RuleSarif;
import io.codemodder.providers.sarif.semgrep.SemgrepJavaParserChanger;
import io.codemodder.providers.sarif.semgrep.SemgrepScan;
import javax.inject.Inject;

/** Disables automatic return of objects in {@link javax.naming.DirContext#search}. */
@Codemod(
    id = "pixee:java/disable-dircontext-deserialization",
    author = "andre.silva@pixee.ai",
    reviewGuidance = ReviewGuidance.MERGE_AFTER_CURSORY_REVIEW)
public final class DisableAutomaticDirContextDeserializationCodemod
    extends SemgrepJavaParserChanger<ObjectCreationExpr> {

  @Inject
  public DisableAutomaticDirContextDeserializationCodemod(
      @SemgrepScan(ruleId = "disable-dircontext-deserialization") final RuleSarif sarif) {
    super(sarif, ObjectCreationExpr.class, RegionExtractor.FROM_FIRST_LOCATION);
  }

  @Override
  public boolean onSemgrepResultFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final ObjectCreationExpr objectCreationExpr,
      final Result result) {
    if (objectCreationExpr.getArgument(4).equals(new BooleanLiteralExpr(true))) {
      objectCreationExpr.setArgument(4, new BooleanLiteralExpr(false));
      return true;
    }
    return false;
  }
}
