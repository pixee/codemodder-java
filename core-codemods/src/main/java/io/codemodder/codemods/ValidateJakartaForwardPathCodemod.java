package io.codemodder.codemods;

import static io.codemodder.javaparser.JavaParserTransformer.wrap;

import com.contrastsecurity.sarif.Result;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.Expression;
import io.codemodder.*;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.javaparser.ChangesResult;
import io.codemodder.providers.sarif.semgrep.SemgrepScan;
import java.util.List;
import javax.inject.Inject;

/**
 * Makes sure that internal Jakarta forwards don't go to places they shouldn't (e.g.,
 * /WEB-INF/web.xml.)
 */
@Codemod(
    id = "pixee:java/validate-jakarta-forward-path",
    importance = Importance.HIGH,
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW)
public final class ValidateJakartaForwardPathCodemod
    extends SarifPluginJavaParserChanger<Expression> implements FixOnlyCodeChanger {

  @Inject
  public ValidateJakartaForwardPathCodemod(
      @SemgrepScan(ruleId = "validate-jakarta-forward-path") final RuleSarif sarif) {
    super(sarif, Expression.class);
  }

  @Override
  public ChangesResult onResultFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final Expression path,
      final Result result) {
    return wrap(path)
            .withStaticMethod(
                "io.github.pixee.security.jakarta.PathValidator", "validateDispatcherPath", true)
        ? ChangesResult.changesAppliedWith(List.of(DependencyGAV.JAVA_SECURITY_TOOLKIT))
        : ChangesResult.noChanges;
  }

  @Override
  public String vendorName() {
    return "Semgrep";
  }

  @Override
  public DetectorRule getDetectorRule() {
    return new DetectorRule(
        "validate-jakarta-forward-path",
        "Introduce protections against user-controlled internal request forwarding",
        "https://cheatsheetseries.owasp.org/cheatsheets/Unvalidated_Redirects_and_Forwards_Cheat_Sheet.html#dangerous-forward-example");
  }
}
