package io.codemodder.remediation.jndiinjection;

import static io.codemodder.javaparser.JavaParserTransformer.wrap;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import io.codemodder.DependencyGAV;
import io.codemodder.remediation.RemediationStrategy;
import io.codemodder.remediation.SuccessOrReason;
import io.github.pixee.security.JNDI;
import java.util.List;

/**
 * Remediates JNDI injection vulnerabilities. It does this by weaving in a check to limit what JNDI
 * resources are available, and users can add more.
 *
 * <p>Inspiration for this came from logback:
 * https://github.com/qos-ch/logback/blob/979d76f3f2847f1c129bcc6295e69187d02e472c/logback-core/src/main/java/ch/qos/logback/core/util/JNDIUtil.java#L54
 */
public final class ReplaceLimitedLookupStrategy implements RemediationStrategy {

  @Override
  public SuccessOrReason fix(final CompilationUnit cu, final Node node) {
    var contextOrReason = JNDIFixContext.fromNode(node);

    if (contextOrReason.isRight()) {
      return SuccessOrReason.fromFailure(contextOrReason.getRight());
    }

    var context = contextOrReason.getLeft();

    String className = JNDI.class.getName();
    String methodName = "limitedContext";

    // rather than insert all new nodes, we'll replace the ones in place
    // ctx.lookup(foo) -> JNDI.limitedContext(ctx).lookup(foo)

    // this is the JNDI Context object
    Expression jndiContext = context.lookupCall().getScope().get();

    // the new scope is the static call
    wrap(jndiContext).withStaticMethod(className, methodName, false);

    return SuccessOrReason.fromSuccess(List.of(DependencyGAV.JAVA_SECURITY_TOOLKIT));
  }
}
