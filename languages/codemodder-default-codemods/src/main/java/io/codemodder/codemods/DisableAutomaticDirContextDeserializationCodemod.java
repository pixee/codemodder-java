package io.codemodder.codemods;

import com.contrastsecurity.sarif.Result;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import io.codemodder.Codemod;
import io.codemodder.CodemodInvocationContext;
import io.codemodder.RegionExtractor;
import io.codemodder.ReviewGuidance;
import io.codemodder.RuleSarif;
import io.codemodder.ast.ASTTransforms;
import io.codemodder.ast.ASTs;
import io.codemodder.providers.sarif.semgrep.SemgrepJavaParserChanger;
import io.codemodder.providers.sarif.semgrep.SemgrepScan;
import java.util.stream.IntStream;
import javax.inject.Inject;

/** Disables automatic return of objects in {@link javax.naming.DirContext#search}. */
@Codemod(
    id = "pixee:java/disable-dircontext-deserialization",
    author = "andre.silva@pixee.ai",
    reviewGuidance = ReviewGuidance.MERGE_AFTER_CURSORY_REVIEW)
public final class DisableAutomaticDirContextDeserializationCodemod
    extends SemgrepJavaParserChanger<MethodCallExpr> {

  @Inject
  public DisableAutomaticDirContextDeserializationCodemod(
      @SemgrepScan(ruleId = "disable-dircontext-deserialization") final RuleSarif sarif) {
    super(sarif, MethodCallExpr.class, RegionExtractor.FROM_FIRST_LOCATION);
  }

  private static ObjectCreationExpr createNewControlsObject(final Expression searchControlsArg) {
    final var newControls = new ObjectCreationExpr();
    newControls.setType("SearchControls");
    newControls.addArgument(new MethodCallExpr(searchControlsArg, "getSearchScope"));
    newControls.addArgument(new MethodCallExpr(searchControlsArg, "getCountLimit"));
    newControls.addArgument(new MethodCallExpr(searchControlsArg, "getTimeLimit"));
    newControls.addArgument(new MethodCallExpr(searchControlsArg, "getReturningAttributes"));
    newControls.addArgument(new BooleanLiteralExpr(false));
    newControls.addArgument(new MethodCallExpr(searchControlsArg, "getDerefLinkFlag"));
    return newControls;
  }

  /**
   * Replaces the argument with a constructor call for SearchControls object. The Arguments of the
   * call are derived from the expression.
   */
  private static void applyTransform(
      MethodCallExpr searchCall, Expression searchControlsArg, int scaIndex) {
    searchCall.setArgument(scaIndex, createNewControlsObject(searchControlsArg));
    ASTTransforms.addImportIfMissing(
        searchCall.findCompilationUnit().get(), "javax.naming.directory.SearchControls");
  }

  /**
   * Find the source of the name and check if it is a object creation of a SearchControls object
   * with the returning object flag set to false.
   */
  private static boolean hasFalseReturnObjectFlag(NameExpr sNameExpr) {
    // find the source of name and check if it creates a SearchControls object
    var maybeObjCreation =
        ASTs.findNonCallableSimpleNameSource(sNameExpr.getName())
            .map(n -> n instanceof VariableDeclarator ? (VariableDeclarator) n : null)
            .flatMap(vd -> vd.getInitializer())
            .map(expr -> expr.isObjectCreationExpr() ? expr.asObjectCreationExpr() : null)
            .filter(
                oce ->
                    oce.getType()
                        .resolve()
                        .describe()
                        .equals("javax.naming.directory.SearchControls"));
    if (maybeObjCreation.isPresent()) {
      // check if the flag is set to false
      if (maybeObjCreation.get().getArgument(4).equals(new BooleanLiteralExpr(false))) {
        return true;
      }
    }
    return false;
  }

  private static boolean nameExprCase(
      final NameExpr searchControlsArg, final MethodCallExpr methodCallExpr, final int scaIndex) {
    if (!hasFalseReturnObjectFlag(searchControlsArg)) {
      applyTransform(methodCallExpr, searchControlsArg, scaIndex);
      return true;
    } else {
      return false;
    }
  }

  private static boolean objCreationExprCase(
      final ObjectCreationExpr searchControlsArg,
      final MethodCallExpr methodCallExpr,
      final int scaIndex) {
    if (searchControlsArg
        .asObjectCreationExpr()
        .getArgument(4)
        .equals(new BooleanLiteralExpr(true))) {
      searchControlsArg.asObjectCreationExpr().setArgument(4, new BooleanLiteralExpr(false));
      return true;
    } else {
      return false;
    }
  }

  @Override
  public boolean onSemgrepResultFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final MethodCallExpr methodCallExpr,
      final Result result) {

    // find index of Search Controls argument
    final var maybeIndex =
        IntStream.range(0, methodCallExpr.getArguments().size())
            .filter(
                i ->
                    methodCallExpr
                        .getArgument(i)
                        .calculateResolvedType()
                        .describe()
                        .equals("javax.naming.directory.SearchControls"))
            .findFirst();
    if (maybeIndex.isPresent()) {
      final Expression searchControlsArg = methodCallExpr.getArgument(maybeIndex.getAsInt());
      // just change the argument if needed
      if (searchControlsArg instanceof ObjectCreationExpr) {
        return objCreationExprCase(
            searchControlsArg.asObjectCreationExpr(), methodCallExpr, maybeIndex.getAsInt());
      }
      if (searchControlsArg instanceof NameExpr) {
        // replace the argument with a newly created instance of SearchControls
        return nameExprCase(searchControlsArg.asNameExpr(), methodCallExpr, maybeIndex.getAsInt());
      }
      // other may have problems with regards to side-effects of earlier expressions in the call
    } else {
      throw new IllegalArgumentException("Call has no SearchControls argument");
    }
    return false;
  }
}
