package io.codemodder.ast;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import java.util.Optional;
import org.javatuples.Pair;
import org.javatuples.Triplet;

/** Find sources of names in JavaParser ASTs. */
final class NameResolver {

  private static Optional<Node> isLocalNameSource(final Node n, final String name) {
    final Optional<Node> maybe =
        ASTs.isExpressionStmtDeclarationOf(n, name).map(Triplet::getValue2);
    // Possible returns:
    return maybe
        .or(() -> ASTs.isResourceOf(n, name).map(Triplet::getValue2))
        .or(() -> ASTs.isForVariableDeclarationOf(n, name).map(Triplet::getValue2))
        .or(() -> ASTs.isForEachVariableDeclarationOf(n, name).map(Triplet::getValue2))
        .or(() -> ASTs.isLambdaExprParameterOf(n, name).map(Pair::getValue1))
        .or(() -> ASTs.isExceptionParameterOf(n, name).map(Pair::getValue1))
        .or(() -> ASTs.isMethodFormalParameterOf(n, name).map(Pair::getValue1))
        .or(() -> ASTs.isMethodTypeParameterOf(n, name).map(Pair::getValue1))
        .or(() -> ASTs.isConstructorFormalParameterOf(n, name).map(Pair::getValue1))
        .or(() -> ASTs.isConstructorTypeParameterOf(n, name).map(Pair::getValue1))
        .or(() -> ASTs.isLocalTypeDeclarationOf(n, name).map(Pair::getValue1))
        .or(() -> ASTs.isLocalRecordDeclarationOf(n, name).map(Pair::getValue1))
        .or(() -> ASTs.isPatternExprDeclarationOf(n, name).map(t -> t));
  }

  private static Optional<Node> findLocalNameSource(Node current, final String name) {
    // Traverse the tree in reverse pre-order until it hits a declaration
    final var it = ASTs.reversePreOrderIterator(current);
    while (!(current instanceof TypeDeclaration) && it.hasNext()) {
      current = it.next();
      final var maybeFound = isLocalNameSource(current, name);
      if (maybeFound.isPresent()) return maybeFound;
    }
    return Optional.empty();
  }

  private static Optional<Node> isFieldOfClass(
      final ClassOrInterfaceDeclaration classDeclaration, final String name) {
    return classDeclaration.getFields().stream()
        .flatMap(field -> ASTs.isFieldDeclarationOf(field, name).stream())
        .findAny()
        .map(Pair::getValue1);
  }

  private static Optional<Node> isNamedMemberOfClass(
      final ClassOrInterfaceDeclaration classDeclaration, final String name) {
    return classDeclaration.getMembers().stream()
        .flatMap(bodyDecl -> ASTs.isNamedMemberOf(bodyDecl, name).stream())
        .findAny()
        .map(n -> (Node) n);
  }

  private static Optional<Node> isNameOfClass(
      final ClassOrInterfaceDeclaration classDeclaration, final String name) {
    if (classDeclaration.getNameAsString().equals(name)) {
      return Optional.of(classDeclaration);
    }
    return Optional.empty();
  }

  private static Optional<Node> findClassLevelNameSource(
      final ClassOrInterfaceDeclaration classDeclaration, final String name) {
    return isFieldOfClass(classDeclaration, name)
        .or(() -> isNamedMemberOfClass(classDeclaration, name))
        .or(() -> ASTs.isClassTypeParameterOf(classDeclaration, name).map(Pair::getValue1))
        .or(() -> isNameOfClass(classDeclaration, name));
  }

  static Optional<Node> resolveSimpleName(final Node start, final String name) {
    // Callable names need more context like signatures to be found. Also, can be overloaded
    Node current = start;
    // Alternate its search from local (i.e. method level) to class level. It may happen because of
    // local type declarations.
    while (current.hasParentNode()) {
      current = current.getParentNode().get();
      // try locally first
      final Optional<Node> maybeDeclaration = findLocalNameSource(current, name);
      if (maybeDeclaration.isPresent()) {
        return maybeDeclaration;
      }
      // No local declaration. Either hit root or a TypeDeclaration after its search
      // TypeDeclaration: ClassOrInterfaceDeclaration, EnumDeclaration, RecordDeclaration
      if (current instanceof ClassOrInterfaceDeclaration) {
        final var classDeclaration = (ClassOrInterfaceDeclaration) current;
        final Optional<Node> maybeClassMember = findClassLevelNameSource(classDeclaration, name);
        if (maybeClassMember.isPresent()) {
          return maybeClassMember;
        }
      }
    }
    // reached CompilationUnit check for top level classes
    final var topLevelTypes = current.findCompilationUnit().get().getTypes();
    final var maybeDecl =
        topLevelTypes.stream().filter(t -> t.getNameAsString().equals(name)).findFirst();
    if (maybeDecl.isPresent()) return maybeDecl.map(n -> n);

    // it's either wildcard imported, inherited, or in the package namespace
    return Optional.empty();
  }

  /** Finds the {@link ClassOrInterfaceDeclaration} that is referenced by a {@link ThisExpr}. */
  static ClassOrInterfaceDeclaration findThisDeclaration(final ThisExpr thisExpr) {
    Node current = thisExpr;
    while (current.hasParentNode() && !(current instanceof ClassOrInterfaceDeclaration)) {
      current = current.getParentNode().get();
    }
    return (ClassOrInterfaceDeclaration) current;
  }

  static Optional<LocalVariableDeclaration> findLocalDeclarationOf(
      final Node start, final String name) {
    final var maybeSource =
        resolveSimpleName(start, name)
            .map(n -> n instanceof VariableDeclarator ? (VariableDeclarator) n : null);
    if (maybeSource.isPresent()) {
      final var vd = maybeSource.get();
      return ASTs.isVariableOfLocalDeclarationStmt(vd)
          .map(
              t ->
                  (LocalVariableDeclaration)
                      new ExpressionStmtVariableDeclaration(
                          t.getValue0(), t.getValue1(), t.getValue2()))
          .or(
              () ->
                  ASTs.isResource(vd)
                      .map(p -> new TryResourceDeclaration(p.getValue0(), p.getValue1(), vd)))
          .or(
              () ->
                  ASTs.isForInitVariable(vd)
                      .map(p -> new ForInitDeclaration(p.getValue0(), p.getValue1(), vd)))
          .or(
              () ->
                  ASTs.isForEachVariable(vd)
                      .map(p -> new ForEachDeclaration(p.getValue0(), p.getValue1(), vd)));
    }

    return Optional.empty();
  }
}
