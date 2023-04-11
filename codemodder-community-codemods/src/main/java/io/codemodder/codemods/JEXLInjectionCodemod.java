package io.codemodder.codemods;

import com.contrastsecurity.sarif.Result;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.Expression;
import io.codemodder.Codemod;
import io.codemodder.CodemodInvocationContext;
import io.codemodder.RegionExtractor;
import io.codemodder.ReviewGuidance;
import io.codemodder.RuleSarif;
import io.codemodder.SarifPluginJavaParserChanger;
import io.codemodder.providers.sarif.codeql.CodeQLScan;
import javax.inject.Inject;

@Codemod(
    id = "codeql:java/jexl-expression-injection",
    author = "andre.silva@pixee.ai",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW)
public class JEXLInjectionCodemod extends SarifPluginJavaParserChanger<Expression> {

  @Inject
  public JEXLInjectionCodemod(
      @CodeQLScan(ruleId = "java/jexl-expression-injection") final RuleSarif sarif) {
    super(sarif, Expression.class, RegionExtractor.FROM_FIRST_LOCATION);
  }

  @Override
  public boolean onResultFound(
      CodemodInvocationContext context, CompilationUnit cu, Expression node, Result result) {
    System.out.println("JEXL Injection Node: " + node);
    return false;
  }
}
