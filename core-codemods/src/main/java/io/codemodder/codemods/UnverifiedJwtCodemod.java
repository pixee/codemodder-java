package io.codemodder.codemods;

import com.contrastsecurity.sarif.Result;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import io.codemodder.*;
import io.codemodder.ast.ASTTransforms;
import io.codemodder.ast.ASTs;
import io.codemodder.ast.LocalVariableDeclaration;
import io.codemodder.providers.sarif.codeql.ProvidedCodeQLScan;
import javax.inject.Inject;

/** Fixes issues reported under the id "missing-jwt-signature-check". */
@Codemod(
    id = "codeql:java/missing-jwt-signature-check",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW,
    importance = Importance.MEDIUM,
    executionPriority = CodemodExecutionPriority.HIGH)
public class UnverifiedJwtCodemod extends SarifPluginJavaParserChanger<Expression> {

  @Inject
  public UnverifiedJwtCodemod(
      @ProvidedCodeQLScan(ruleId = "java/missing-jwt-signature-check") final RuleSarif sarif) {
    super(sarif, Expression.class, SourceCodeRegionExtractor.FROM_SARIF_FIRST_LOCATION);
  }

  private boolean fix(MethodCallExpr parseCall) {
    parseCall.setName("parseClaimsJws");
    return true;
  }

  @Override
  public boolean onResultFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final Expression expression,
      final Result result) {
    // The result points to the expression that calls parse methods:
    // parse(token), parse(token,handler), parseClaimsJwt(token), parsePlaintextJwt(token)
    var maybeParseCall = ASTs.isScopeInMethodCall(expression);

    // parseClaimsJws returns a different type than the other parse methods
    // Changing types may bring compilation errors in certain situations

    // The call itself is the scope in another methodcall, e.g <expr>.parse(token).getBody()
    if (maybeParseCall.flatMap(mce -> ASTs.isScopeInMethodCall(mce)).isPresent()) {
      return fix(maybeParseCall.get());
    }

    // If the call is the init expression of a local variable
    var maybeLocalDeclaration =
        maybeParseCall
            .flatMap(mce -> ASTs.isInitExpr(mce))
            .flatMap(vd -> LocalVariableDeclaration.fromVariableDeclarator(vd));
    if (maybeLocalDeclaration.isPresent()) {
      var parseCall = maybeParseCall.get();
      var localDeclaration = maybeLocalDeclaration.get();
      var lvdType = localDeclaration.getVariableDeclarator().getType();

      // Having trouble with JavaParser type resolution here
      // It is Jwt<A,B> type, Jws is actually a subtype, but you can't cast because of type
      // parameters
      // So, if we're using raw type, we just change the type
      if (!lvdType.isVarType() && !lvdType.toString().contains("<")) {
        localDeclaration.getVariableDeclarator().setType("Jws<Claims>");
        ASTTransforms.addImportIfMissing(cu, "io.jsonwebtoken.Jws");
        ASTTransforms.addImportIfMissing(cu, "io.jsonwebtoken.Claims");
        return fix(parseCall);
      }

      // If not, we check if all uses are just methodcalls, meaning the type isn't being used in any
      // meaningful way
      var allNameExpr =
          localDeclaration.getScope().stream()
              .flatMap(
                  n ->
                      n
                          .findAll(
                              NameExpr.class,
                              ne -> ne.getNameAsString().equals(localDeclaration.getName()))
                          .stream());

      // if the only uses is being scope of a method calls, then we can change it
      if (allNameExpr.allMatch(ne -> ASTs.isScopeInMethodCall(ne).isPresent())) {
        localDeclaration.getVariableDeclarator().setType("Jws<Claims>");
        ASTTransforms.addImportIfMissing(cu, "io.jsonwebtoken.Jws");
        ASTTransforms.addImportIfMissing(cu, "io.jsonwebtoken.Claims");
        return fix(parseCall);
      }
    }

    return false;
  }
}
