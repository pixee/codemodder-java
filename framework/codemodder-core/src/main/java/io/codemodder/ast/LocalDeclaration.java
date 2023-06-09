package io.codemodder.ast;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * A local declaration. Either a local variable declaration, a formal parameter, or an exception
 * parameter.
 */
public interface LocalDeclaration {

  /** Returns the name used in this declaration as a String. */
  String getName();

  /** The node where the declaration occurs. */
  Node getDeclaration();

  /** The scope of this declaration. */
  LocalScope getScope();

  /** Verifies if a given {@link NameExpr} is a reference to this variable/parameter. */
  default boolean isReference(final NameExpr reference) {
    return reference.getNameAsString().equals(getName())
        && ASTs.findNonCallableSimpleNameSource(reference.getName())
            .filter(m -> m == getDeclaration())
            .isPresent();
  }

  /** Finds all references to this variable/parameter in its scope. */
  default Stream<NameExpr> findAllReferences() {
    return getScope().stream()
        .flatMap(n -> n.findAll(NameExpr.class).stream())
        .filter(this::isReference);
  }

  /** Finds all method calls for the variable/parameter in this declaration. */
  default Stream<MethodCallExpr> findAllMethodCalls() {
    Predicate<MethodCallExpr> isScopeInMethodCall =
        mce ->
            mce.getScope().filter(s -> s.isNameExpr() && isReference(s.asNameExpr())).isPresent();

    return getScope().stream()
        .flatMap(n -> n.findAll(MethodCallExpr.class, isScopeInMethodCall).stream());
  }
}
