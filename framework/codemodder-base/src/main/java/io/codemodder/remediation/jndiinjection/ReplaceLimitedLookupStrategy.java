package io.codemodder.remediation.jndiinjection;

import static io.codemodder.javaparser.JavaParserTransformer.wrap;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import io.codemodder.DependencyGAV;
import io.github.pixee.security.JNDI;
import java.util.List;

/** Replaces the call with a limited call. */
final class ReplaceLimitedLookupStrategy implements JNDIFixStrategy {

  @Override
  public List<DependencyGAV> fix(
      final CompilationUnit cu,
      final ClassOrInterfaceDeclaration parentClass,
      final MethodCallExpr lookupCall,
      final NameExpr contextNameVariable,
      final BlockStmt blockStmt,
      final int index) {

    String className = JNDI.class.getName();
    String methodName = "limitedContext";

    // rather than insert all new nodes, we'll replace the ones in place
    // ctx.lookup(foo) -> JNDI.limitedContext(ctx).lookup(foo)

    // this is the JNDI Context object
    Expression jndiContext = lookupCall.getScope().get();

    // the new scope is the static call
    wrap(jndiContext).withStaticMethod(className, methodName, false);

    return List.of(DependencyGAV.JAVA_SECURITY_TOOLKIT);
  }
}
