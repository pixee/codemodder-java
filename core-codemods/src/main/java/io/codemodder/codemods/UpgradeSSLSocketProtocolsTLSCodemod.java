package io.codemodder.codemods;

import static io.codemodder.javaparser.JavaParserTransformer.newArray;

import com.contrastsecurity.sarif.Result;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.ArrayCreationExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import io.codemodder.*;
import io.codemodder.providers.sarif.semgrep.SemgrepScan;
import javax.inject.Inject;

/**
 * Targets making sure the protocols set in {@link
 * javax.net.ssl.SSLSocket#setEnabledProtocols(String[])} are safe.
 */
@Codemod(
    id = "pixee:java/upgrade-sslsocket-tls",
    importance = Importance.HIGH,
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW)
public final class UpgradeSSLSocketProtocolsTLSCodemod
    extends SarifPluginJavaParserChanger<MethodCallExpr> {

  @Inject
  public UpgradeSSLSocketProtocolsTLSCodemod(
      @SemgrepScan(ruleId = "upgrade-sslsocket-tls") RuleSarif semgrepSarif) {
    super(semgrepSarif, MethodCallExpr.class);
  }

  @Override
  public boolean onResultFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final MethodCallExpr setEnabledProtocolsCall,
      final Result result) {
    ArrayCreationExpr safeProtocols =
        newArray("String", new StringLiteralExpr(SSLProtocols.safeTlsVersion));
    setEnabledProtocolsCall.setArguments(NodeList.nodeList(safeProtocols));
    return true;
  }
}
