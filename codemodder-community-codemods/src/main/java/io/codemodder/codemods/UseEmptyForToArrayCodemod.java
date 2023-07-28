package io.codemodder.codemods;

import com.contrastsecurity.sarif.Result;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.type.Type;
import io.codemodder.*;
import io.codemodder.providers.sarif.pmd.PmdScan;
import javax.inject.Inject;

/**
 * A codemod that forces a zero-sized array to pass to {@link
 * java.util.Collection#toArray(Object[])} since that is <a
 * href="https://shipilev.net/blog/2016/arrays-wisdom-ancients/">preferable for performance</a>.
 */
@Codemod(
    id = "pixee:java/use-empty-for-toarray",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW)
public final class UseEmptyForToArrayCodemod extends SarifPluginJavaParserChanger<MethodCallExpr> {

  @Inject
  public UseEmptyForToArrayCodemod(
      @PmdScan(ruleId = "category/java/performance.xml/OptimizableToArrayCall")
          final RuleSarif ruleSarif) {
    super(ruleSarif, MethodCallExpr.class, RegionNodeMatcher.MATCHES_START);
  }

  @Override
  public boolean onResultFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final MethodCallExpr toArrayCall,
      final Result result) {
    ArrayCreationExpr toArrayArg = toArrayCall.getArgument(0).asArrayCreationExpr();
    Type elementType = toArrayArg.getElementType();
    if (!toArrayArg.getInitializer().isPresent()) {
      return false;
    }
    ArrayInitializerExpr array = toArrayArg.getInitializer().get();
    return true;
  }
}
