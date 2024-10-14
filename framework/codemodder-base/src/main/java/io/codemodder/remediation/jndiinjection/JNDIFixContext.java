package io.codemodder.remediation.jndiinjection;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;
import io.codemodder.Either;
import java.util.Optional;

/** Contains all the relevant nodes for applying a fix to a JNDI vulnerability */
record JNDIFixContext(
    MethodCallExpr lookupCall,
    ClassOrInterfaceDeclaration parentClass,
    NameExpr contextNameVariable,
    BlockStmt blockStmt,
    Integer index) {

  /**
   * Test if a given node containing a JNDI vulnerability has all the relevant information for a
   * fix.
   *
   * @param node
   * @return Either the context with all the nodes for a fix, or a reason for failure.
   */
  static Either<JNDIFixContext, String> fromNode(final Node node) {
    Optional<MethodCallExpr> maybeLookupCall =
        Optional.of(node).map(n -> n instanceof MethodCallExpr ? (MethodCallExpr) n : null);

    if (maybeLookupCall.isEmpty()) {
      return Either.right("Node is not a method call");
    }
    var lookupCall = maybeLookupCall.get();

    // get the parent method of the lookup() call
    Optional<MethodDeclaration> maybeParentMethod =
        lookupCall.findAncestor(MethodDeclaration.class);
    if (maybeParentMethod.isEmpty()) {
      return Either.right("No method found around lookup call");
    }
    var parentMethod = maybeParentMethod.get();

    // confirm it's a concrete type -- can't add validation method to
    Optional<ClassOrInterfaceDeclaration> maybeParentClass =
        parentMethod.findAncestor(ClassOrInterfaceDeclaration.class);
    if (maybeParentClass.filter(c -> c.isInterface()).isPresent()) {
      return Either.right("Cannot add validation method to interface");
    }
    var parentClass = maybeParentClass.get();

    Optional<Statement> maybeLookupStatement = lookupCall.findAncestor(Statement.class);
    if (maybeLookupStatement.isEmpty()) {
      return Either.right("No statement found around lookup call");
    }
    var lookupStatement = maybeLookupStatement.get();

    // validate the shape of code around the lookup call to make sure it's safe to add the call and
    // method
    Optional<NameExpr> maybeContextNameVariable =
        lookupCall.getArguments().stream()
            .findFirst()
            .map(arg -> arg.isNameExpr() ? arg.asNameExpr() : null);
    if (maybeContextNameVariable.isEmpty()) {
      return Either.right("No context name variable found for the first argument");
    }
    var contextNameVariable = maybeContextNameVariable.get();

    Optional<Node> lookupParentNode = lookupStatement.getParentNode();
    if (lookupParentNode.isEmpty()) {
      return Either.right("No parent node found around lookup call");
    }

    if (!(lookupParentNode.get() instanceof BlockStmt blockStmt)) {
      return Either.right("No block statement found around lookup call");
    }

    // add the validation call to the block statement
    int index = blockStmt.getStatements().indexOf(lookupStatement);

    // All the tests were successful, return the context.
    return Either.left(
        new JNDIFixContext(lookupCall, parentClass, contextNameVariable, blockStmt, index));
  }
}
