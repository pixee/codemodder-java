package io.codemodder.codemods;

import static io.codemodder.ast.ASTTransforms.addImportIfMissing;

import com.contrastsecurity.sarif.Result;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.*;
import io.codemodder.*;
import io.codemodder.providers.sarif.semgrep.SemgrepScan;
import io.github.pixee.security.HostValidator;
import io.github.pixee.security.Urls;
import java.util.List;
import javax.inject.Inject;

@Codemod(
    id = "pixee:java/sandbox-url-creation",
    importance = Importance.HIGH,
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW)
public final class SSRFCodemod extends SarifPluginJavaParserChanger<ObjectCreationExpr> {

  @Inject
  public SSRFCodemod(@SemgrepScan(ruleId = "sandbox-url-creation") RuleSarif semgrepSarif) {
    super(semgrepSarif, ObjectCreationExpr.class);
  }

  @Override
  public boolean onResultFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final ObjectCreationExpr n,
      final Result result) {
    NodeList<Expression> arguments = n.getArguments();

    /*
     * We need to replace:
     *
     * URL u = new URL(foo)
     *
     * With:
     *
     * import io.github.pixee.security.Urls;
     * ...
     * URL u = Urls.create(foo, io.github.pixee.security.Urls.HTTP_PROTOCOLS, io.github.pixee.security.HostValidator.ALLOW_ALL)
     */
    addImportIfMissing(cu, Urls.class.getName());
    addImportIfMissing(cu, HostValidator.class.getName());
    FieldAccessExpr httpProtocolsExpr = new FieldAccessExpr();
    httpProtocolsExpr.setScope(new NameExpr(Urls.class.getSimpleName()));
    httpProtocolsExpr.setName("HTTP_PROTOCOLS");

    FieldAccessExpr denyCommonTargetsExpr = new FieldAccessExpr();

    denyCommonTargetsExpr.setScope(new NameExpr(HostValidator.class.getSimpleName()));
    denyCommonTargetsExpr.setName("DENY_COMMON_INFRASTRUCTURE_TARGETS");

    NodeList<Expression> newArguments = new NodeList<>();
    newArguments.addAll(arguments); // first are all the arguments they were passing to "new URL"
    newArguments.add(httpProtocolsExpr); // load the protocols they're allowed
    newArguments.add(denyCommonTargetsExpr); // load the host validator
    MethodCallExpr safeCall =
        new MethodCallExpr(
            new NameExpr(io.github.pixee.security.Urls.class.getSimpleName()),
            "create",
            newArguments);
    n.replace(safeCall);

    return true;
  }

  @Override
  public List<DependencyGAV> dependenciesRequired() {
    return List.of(DependencyGAV.JAVA_SECURITY_TOOLKIT);
  }
}
