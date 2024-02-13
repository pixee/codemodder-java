package io.codemodder.codemods;

import com.contrastsecurity.sarif.Result;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import io.codemodder.*;
import io.codemodder.providers.sarif.semgrep.SemgrepScan;
import javax.inject.Inject;

/**
 * Targets making sure the protocols set in {@link javax.net.ssl.SSLContext#getInstance(String)} are
 * safe.
 */
@Codemod(
    id = "pixee:java/upgrade-sslcontext-tls",
    importance = Importance.HIGH,
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW)
public final class UpgradeSSLContextTLSCodemod
    extends SarifPluginJavaParserChanger<MethodCallExpr> {

  @Inject
  public UpgradeSSLContextTLSCodemod(
      @SemgrepScan(ruleId = "upgrade-sslcontext-tls") RuleSarif semgrepSarif) {
    super(semgrepSarif, MethodCallExpr.class);
  }

  @Override
  public boolean onResultFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final MethodCallExpr getInstanceCall,
      final Result result) {

    getInstanceCall.setArguments(
        NodeList.nodeList(new StringLiteralExpr(SSLProtocols.safeTlsVersion)));
    return true;
  }
}
