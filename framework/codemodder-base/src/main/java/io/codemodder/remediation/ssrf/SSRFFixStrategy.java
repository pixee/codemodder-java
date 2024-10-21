package io.codemodder.remediation.ssrf;

import static io.codemodder.ast.ASTTransforms.addImportIfMissing;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.*;
import io.codemodder.DependencyGAV;
import io.codemodder.remediation.RemediationStrategy;
import io.codemodder.remediation.SuccessOrReason;
import io.github.pixee.security.HostValidator;
import io.github.pixee.security.Urls;
import java.util.List;

/** Default strategy to fix SSRF issues. */
public final class SSRFFixStrategy implements RemediationStrategy {

  @Override
  public SuccessOrReason fix(final CompilationUnit cu, final Node node) {
    if (node instanceof MethodCallExpr mce) {
      return hardenRT(cu, mce);

    } else if (node instanceof ObjectCreationExpr oce) {
      return harden(cu, oce);
    }
    return SuccessOrReason.reason("Not a method call or constructor");
  }

  /**
   * Fixes SSRF issues originating from URL(...) constructors
   *
   * @param cu
   * @param newUrlCall
   * @return
   */
  private SuccessOrReason harden(final CompilationUnit cu, final ObjectCreationExpr newUrlCall) {
    NodeList<Expression> arguments = newUrlCall.getArguments();

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
    MethodCallExpr safeCall = wrapInUrlsCreate(cu, arguments);
    newUrlCall.replace(safeCall);
    return SuccessOrReason.success(List.of(DependencyGAV.JAVA_SECURITY_TOOLKIT));
  }

  private MethodCallExpr wrapInUrlsCreate(
      final CompilationUnit cu, final NodeList<Expression> arguments) {
    addImportIfMissing(cu, Urls.class.getName());
    addImportIfMissing(cu, HostValidator.class.getName());

    FieldAccessExpr httpProtocolsExpr = new FieldAccessExpr();
    httpProtocolsExpr.setScope(new NameExpr(Urls.class.getSimpleName()));
    httpProtocolsExpr.setName("HTTP_PROTOCOLS");

    FieldAccessExpr denyCommonTargetsExpr = new FieldAccessExpr();
    denyCommonTargetsExpr.setScope(new NameExpr(HostValidator.class.getSimpleName()));
    denyCommonTargetsExpr.setName("DENY_COMMON_INFRASTRUCTURE_TARGETS");

    NodeList<Expression> newArguments = new NodeList<>();
    newArguments.addAll(arguments); // add expression
    newArguments.add(httpProtocolsExpr); // load the protocols they're allowed
    newArguments.add(denyCommonTargetsExpr); // load the host validator

    return new MethodCallExpr(new NameExpr(Urls.class.getSimpleName()), "create", newArguments);
  }

  /**
   * Fixes SRRF issues originating from RestTemplate.exchange() calls.
   *
   * @param cu
   * @param call
   * @return
   */
  private SuccessOrReason hardenRT(final CompilationUnit cu, final MethodCallExpr call) {
    var maybeFirstArg = call.getArguments().stream().findFirst();
    if (maybeFirstArg.isPresent()) {
      var wrappedArg =
          new MethodCallExpr(
              wrapInUrlsCreate(cu, new NodeList<>(maybeFirstArg.get().clone())), "toString");
      maybeFirstArg.get().replace(wrappedArg);
      return SuccessOrReason.success(List.of(DependencyGAV.JAVA_SECURITY_TOOLKIT));
    }
    return SuccessOrReason.reason("Could not find first argument");
  }
}
