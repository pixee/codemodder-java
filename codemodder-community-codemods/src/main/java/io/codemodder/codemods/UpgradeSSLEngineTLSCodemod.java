package io.codemodder.codemods;

import com.contrastsecurity.sarif.Result;
import com.github.javaparser.ast.ArrayCreationLevel;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.ArrayCreationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import io.codemodder.*;
import io.codemodder.providers.sarif.semgrep.SemgrepScan;
import javax.inject.Inject;

/**
 * Targets making sure the protocols set in {@link
 * javax.net.ssl.SSLEngine#setEnabledProtocols(String[])} are safe.
 */
@Codemod(
    id = "pixee:java/upgrade-sslengine-tls",
    author = "arshan@pixee.ai",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW)
public final class UpgradeSSLEngineTLSCodemod extends SarifPluginJavaParserChanger<MethodCallExpr> {

  @Inject
  public UpgradeSSLEngineTLSCodemod(
      @SemgrepScan(ruleId = "upgrade-sslengine-tls") RuleSarif semgrepSarif) {
    super(semgrepSarif, MethodCallExpr.class);
  }

  @Override
  public boolean onResultFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final MethodCallExpr setEnabledProtocolsCall,
      final Result result) {

    final ArrayCreationExpr safeArgument =
        new ArrayCreationExpr(new ClassOrInterfaceType("String"));
    safeArgument.setLevels(NodeList.nodeList(new ArrayCreationLevel()));
    safeArgument.setInitializer(
        new ArrayInitializerExpr(
            NodeList.nodeList(new StringLiteralExpr(SSLProtocols.safeTlsVersion))));
    setEnabledProtocolsCall.setArguments(NodeList.nodeList(safeArgument));
    return true;
  }
}
