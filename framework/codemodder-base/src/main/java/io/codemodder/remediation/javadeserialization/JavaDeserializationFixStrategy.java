package io.codemodder.remediation.javadeserialization;

import static io.codemodder.javaparser.JavaParserTransformer.replace;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import io.codemodder.DependencyGAV;
import io.codemodder.Either;
import io.codemodder.ast.ASTs;
import io.codemodder.ast.LocalDeclaration;
import io.codemodder.remediation.*;
import io.github.pixee.security.ObjectInputFilters;
import java.util.List;
import java.util.Optional;

public final class JavaDeserializationFixStrategy implements RemediationStrategy {

  /**
   * Tries to find the construction of the scope of a call.
   *
   * @return
   */
  private Either<ObjectCreationExpr, String> findConstructor(final MethodCallExpr call) {

    var maybeCallScope = call.getScope().map(s -> s instanceof NameExpr ? s.asNameExpr() : null);
    if (maybeCallScope.isEmpty()) {
      return Either.right("Unexpected shape");
    }
    NameExpr callScope = maybeCallScope.get();

    Optional<LocalDeclaration> declaration =
        ASTs.findEarliestLocalDeclarationOf(callScope.getName());
    if (declaration.isEmpty()) {
      return Either.right("No declaration found");
    }

    LocalDeclaration localDeclaration = declaration.get();
    Node varDeclarationAndExpr = localDeclaration.getDeclaration();
    if (varDeclarationAndExpr instanceof VariableDeclarator varDec) {
      Optional<Expression> initializer = varDec.getInitializer();
      if (initializer.isEmpty()) {
        return Either.right("No initializer found");
      }

      Expression expression = initializer.get();
      if (expression instanceof ObjectCreationExpr objCreation) {
        return Either.left(expression.asObjectCreationExpr());
      }
    } else {
      return Either.right("Unexpected declaration type");
    }
    return Either.right("Failed to find constructor for associated call");
  }

  @Override
  public SuccessOrReason fix(final CompilationUnit cu, final Node node) {

    Optional<Either<MethodCallExpr, ObjectCreationExpr>> maybeCallOrConstructor =
        Optional.<Either<MethodCallExpr, ObjectCreationExpr>>empty()
            .or(
                () ->
                    node instanceof MethodCallExpr
                        ? Optional.of(Either.left((MethodCallExpr) node))
                        : Optional.empty())
            .or(
                () ->
                    node instanceof ObjectCreationExpr
                        ? Optional.of(Either.right((ObjectCreationExpr) node))
                        : Optional.empty());
    if (maybeCallOrConstructor.isEmpty()) {
      return SuccessOrReason.reason("Not a call or constructor");
    }

    Either<ObjectCreationExpr, String> maybeConstructor =
        maybeCallOrConstructor
            .get()
            // If it is a call, we're pointing to the readObject(), so we must work backwards to
            // find the
            // declaration of the ObjectInputStream
            .ifLeftOrElseGet(this::findConstructor, Either::left);

    // afailed to find the construction
    if (maybeConstructor.isRight()) {
      return SuccessOrReason.reason(maybeConstructor.getRight());
    }

    fixObjectInputStreamCreation(maybeConstructor.getLeft());
    return SuccessOrReason.success(List.of(DependencyGAV.JAVA_SECURITY_TOOLKIT));
  }

  private void fixObjectInputStreamCreation(final ObjectCreationExpr objCreation) {
    replace(objCreation)
        .withStaticMethod(ObjectInputFilters.class.getName(), "createSafeObjectInputStream")
        .withStaticImport()
        .withSameArguments();
  }
}
