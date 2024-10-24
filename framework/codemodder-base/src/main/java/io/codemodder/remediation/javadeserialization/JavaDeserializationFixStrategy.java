package io.codemodder.remediation.javadeserialization;

import static io.codemodder.javaparser.JavaParserTransformer.replace;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import io.codemodder.DependencyGAV;
import io.codemodder.Either;
import io.codemodder.ast.ASTs;
import io.codemodder.ast.LocalDeclaration;
import io.codemodder.remediation.*;
import io.github.pixee.security.ObjectInputFilters;
import java.util.List;
import java.util.Optional;

/** Default strategy to hardens deserialization vulnerabilities. */
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
        return Either.left(objCreation);
      }
    } else {
      return Either.right("Unexpected declaration type");
    }
    return Either.right("Failed to find constructor for associated call");
  }

  @Override
  public SuccessOrReason fix(final CompilationUnit cu, final Node node) {
    // We know that the target must exist and be an OCE from the match
    final Node consideredNode =
        node instanceof VariableDeclarationExpr vd
            ? vd.getVariable(0).getInitializer().get().asObjectCreationExpr()
            : node;
    Optional<Either<MethodCallExpr, ObjectCreationExpr>> maybeCallOrConstructor =
        Optional.<Either<MethodCallExpr, ObjectCreationExpr>>empty()
            .or(
                () ->
                    consideredNode instanceof MethodCallExpr
                        ? Optional.of(Either.left((MethodCallExpr) consideredNode))
                        : Optional.empty())
            .or(
                () ->
                    consideredNode instanceof ObjectCreationExpr
                        ? Optional.of(Either.right((ObjectCreationExpr) consideredNode))
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

    // failed to find the construction
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

  /**
   * Match code shape for AssignExpr case
   *
   * @param node
   * @return
   */
  public static boolean match(final VariableDeclarationExpr node) {
    return Optional.of(node)
        .flatMap(vde -> vde.getVariables().getFirst())
        .flatMap(VariableDeclarator::getInitializer)
        .map(e -> e.isObjectCreationExpr() ? e.asObjectCreationExpr() : null)
        .filter(JavaDeserializationFixStrategy::match)
        .isPresent();
  }

  /**
   * Match code shape for ObjectCreationExpr case
   *
   * @param node
   * @return
   */
  public static boolean match(final ObjectCreationExpr node) {
    return Optional.of(node)
        .map(n -> n instanceof ObjectCreationExpr ? (ObjectCreationExpr) n : null)
        .filter(oce -> "ObjectInputStream".equals(oce.getTypeAsString()))
        .isPresent();
  }

  /**
   * Match code shape for MethodCallExpr case
   *
   * @param node
   * @return
   */
  public static boolean match(final MethodCallExpr node) {
    return Optional.of(node)
        .map(n -> n instanceof MethodCallExpr ? (MethodCallExpr) n : null)
        .filter(mce -> mce.getNameAsString().equals("readObject"))
        .filter(mce -> mce.getArguments().isEmpty())
        .isPresent();
  }

  /**
   * Default matching
   *
   * @param node
   * @return
   */
  public static boolean match(final Node node) {
    if (node instanceof MethodCallExpr mce) {
      return match(mce);
    } else if (node instanceof ObjectCreationExpr oce) {
      return match(oce);
    }
    return false;
  }
}
