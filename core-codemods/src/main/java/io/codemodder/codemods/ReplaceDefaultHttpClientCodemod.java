package io.codemodder.codemods;

import static io.codemodder.ast.ASTTransforms.addImportIfMissing;
import static io.codemodder.javaparser.JavaParserTransformer.replace;

import com.contrastsecurity.sarif.Result;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import io.codemodder.*;
import io.codemodder.providers.sarif.semgrep.SemgrepScan;
import javax.inject.Inject;

/**
 * This codemod replaces instances of org.apache.http.impl.client.DefaultHttpClient with
 * HttpClientBuilder.create().useSystemProperties().build().
 */
@Codemod(
    id = "pixee:java/replace-apache-defaulthttpclient",
    importance = Importance.MEDIUM,
    reviewGuidance = ReviewGuidance.MERGE_AFTER_CURSORY_REVIEW)
public final class ReplaceDefaultHttpClientCodemod
    extends SarifPluginJavaParserChanger<ObjectCreationExpr> {

  private static final String RULE =
      """
          rules:
            - id: replace-apache-defaulthttpclient
              patterns:
                - pattern: new org.apache.http.impl.client.DefaultHttpClient()
          """;

  @Inject
  public ReplaceDefaultHttpClientCodemod(@SemgrepScan(yaml = RULE) final RuleSarif sarif) {
    super(sarif, ObjectCreationExpr.class);
  }

  @Override
  public boolean onResultFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final ObjectCreationExpr objectCreationExpr,
      final Result result) {
    Expression expression =
        StaticJavaParser.parseExpression(
            "HttpClientBuilder.create().useSystemProperties().build()");
    replace(objectCreationExpr).withExpression(expression);
    addImportIfMissing(cu, "org.apache.http.impl.client.HttpClientBuilder");
    return true;
  }
}
