package io.codemodder.codemods;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import io.codemodder.*;
import io.codemodder.providers.sarif.semgrep.SemgrepScan;
import javax.inject.Inject;

@Codemod(
    id = "pixee:java/replace-generics-in-var",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW)
public final class ReplaceGenericsInVarCodemod
    extends SarifPluginJavaParserChanger<VariableDeclarationExpr> {

  @Inject
  public ReplaceGenericsInVarCodemod(
      @SemgrepScan(ruleId = "replace-generics-in-var") final RuleSarif sarif) {
    super(sarif, VariableDeclarationExpr.class);
  }

  @Override
  public boolean onResultFound(
      CodemodInvocationContext context,
      CompilationUnit cu,
      VariableDeclarationExpr variableDeclarationExpr,
      com.contrastsecurity.sarif.Result result) {

    for (VariableDeclarator variableDeclarator : variableDeclarationExpr.getVariables()) {
      variableDeclarator
          .getInitializer()
          .ifPresent(
              initializer -> {
                /*
                 When removing the type from the initializer, the diamond operators were removed as well. This logic
                 corrects that problem by treating the initializer as a string and uses a regex to remove specific
                 content from the diamond operators.
                */
                String initializerCode = initializer.toString();

                // Remove type arguments from the initializer, need to keep diamond operator
                String cleanedInitializerCode = initializerCode.replaceAll("<.*?>", "<>");
                // Fixes bad output instances with nested types
                String checkInitializerCode = cleanedInitializerCode.replaceAll("<>>", "<>");

                // Create a new StringLiteralExpr with the cleaned code
                Expression cleanedInitializer = new StringLiteralExpr(checkInitializerCode);

                // Remove wrapped quotes
                String formatted =
                    cleanedInitializer
                        .toString()
                        .substring(1, cleanedInitializer.toString().length() - 1);

                variableDeclarator.setInitializer(formatted);
              });
    }

    return true;
  }
}
