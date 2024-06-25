package io.codemodder.ast;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.TryStmt;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Contains the expressions and statements that span the scope of a local declaration. See <a
 * href="https://docs.oracle.com/javase/specs/jls/se19/html/jls-6.html#jls-6.3">Java Language
 * Specification - Section 6.3 </a> for how the scope of local declarations is defined. The
 * nodelists are ordered and the nodes in {@code expressions} happens before the ones in {@code
 * statements}.
 */
public final class LocalScope {

  NodeList<Expression> expressions;
  NodeList<Statement> statements;

  private LocalScope(NodeList<Expression> expressions, NodeList<Statement> statements) {
    this.expressions = expressions;
    this.statements = statements;
  }

  /** Calculates the scope of a local declaration in a {@link TryStmt}. */
  public static LocalScope fromTryResource(TryStmt stmt, VariableDeclarator vd) {
    var vde = (VariableDeclarationExpr) vd.getParentNode().get();
    var resources = stmt.getResources();
    var expressions = new NodeList<Expression>();
    expressions.setParentNode(stmt);
    resources.stream().skip(resources.indexOf(vde) + 1).forEach(expressions::add);
    return new LocalScope(expressions, stmt.getTryBlock().getStatements());
  }

  /** Calculates the scope of a local declaration in a {@link ExpressionStmt}. */
  public static LocalScope fromLocalDeclaration(ExpressionStmt stmt, VariableDeclarator vd) {
    var expressions = new NodeList<Expression>();
    // We expect a VariableDeclarationExpr in the stmt, it may contain multiple declarations
    var vde = (VariableDeclarationExpr) vd.getParentNode().get();
    var vdIndex = vde.getVariables().indexOf(vd);
    vde.getVariables().stream()
        .skip(vdIndex + 1)
        .map(VariableDeclarator::getInitializer)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .forEach(expressions::add);

    // Local variable declarations are always contained in a block statement.
    var block = (BlockStmt) stmt.getParentNode().get();
    var statements = new NodeList<Statement>();
    statements.setParentNode(block);
    block.getStatements().stream()
        .skip(block.getStatements().indexOf(stmt) + 1)
        .forEach(statements::add);
    return new LocalScope(expressions, statements);
  }

  /** Calculates the scope of a local declaration in a {@link ForEachStmt}. */
  public static LocalScope fromForEachDeclaration(ForEachStmt stmt) {
    var expressions = new NodeList<Expression>();
    expressions.setParentNode(stmt);
    var body = stmt.getBody();
    NodeList<Statement> statements;
    if (body instanceof BlockStmt) {
      statements = ((BlockStmt) body).getStatements();
    } else {
      statements = new NodeList<>();
      statements.setParentNode(stmt);
    }
    return new LocalScope(expressions, statements);
  }

  /** Calculates the scope of a local declaration in a {@link ForStmt}. */
  public static LocalScope fromForDeclaration(ForStmt stmt, VariableDeclarator vd) {
    var expressions = new NodeList<Expression>();
    var vde = (VariableDeclarationExpr) stmt.getInitialization().get(0);
    // finds vd in for init and adds any initialization for
    int vdIndex =
        IntStream.range(0, vde.getVariables().size())
            .filter(i -> vde.getVariable(i).equals(vd))
            .findFirst()
            .getAsInt();
    vde.getVariables().stream()
        .skip(vdIndex + 1)
        .forEach(vdInVDE -> vdInVDE.getInitializer().ifPresent(expressions::add));

    if (stmt.getCompare().isPresent()) expressions.add(stmt.getCompare().get());
    expressions.addAll(stmt.getUpdate());
    var body = stmt.getBody();
    NodeList<Statement> statements;
    if (body instanceof BlockStmt) {
      statements = ((BlockStmt) body).getStatements();
    } else {
      statements = new NodeList<>();
      statements.setParentNode(stmt);
    }
    return new LocalScope(expressions, statements);
  }

  /** Calculates the scope of a local declaration in a {@link Parameter}. */
  public static LocalScope fromParameter(Parameter parameter) {
    // Always possible
    var parent = parameter.getParentNode().get();
    NodeList<Statement> statements = new NodeList<>();
    var expressions = new NodeList<Expression>();
    if (parent instanceof LambdaExpr) {
      var allStatements = Stream.of(((LambdaExpr) parent).getBody());
      allStatements
          .flatMap(s -> s.isBlockStmt() ? s.asBlockStmt().getStatements().stream() : Stream.of(s))
          .forEach(statements::add);
    }
    if (parent instanceof MethodDeclaration) {
      var maybeBody = ((MethodDeclaration) parent).getBody();
      statements = maybeBody.map(bs -> bs.getStatements()).orElse(statements);
    }
    if (parent instanceof CatchClause) {
      statements = ((CatchClause) parent).getBody().getStatements();
    }
    if (parent instanceof ConstructorDeclaration) {
      statements = ((ConstructorDeclaration) parent).getBody().getStatements();
    }
    return new LocalScope(expressions, statements);
  }

  /** Calculates the scope of a Assignment {@link AssignExpr}. This function is currently incomplete as it only works if the AssignExpr is contained in an ExpressionStmt.*/
  public static LocalScope fromAssignExpression(AssignExpr aexpr) {
    var expressions = new NodeList<Expression>();
    var statements = new NodeList<Statement>();
    var maybeStmt = ASTs.isExpressionStmtExpr(aexpr);
    if (maybeStmt.isPresent()){
	    var block = (BlockStmt) maybeStmt.get().getParentNode().get();
	    statements.setParentNode(block);
	    block.getStatements().stream()
		.skip(block.getStatements().indexOf(maybeStmt.get()) + 1)
		.forEach(statements::add);
    }
    return new LocalScope(expressions, statements);
  }

  public NodeList<Expression> getExpressions() {
    return expressions;
  }

  public NodeList<Statement> getStatements() {
    return statements;
  }

  public Stream<Node> stream() {
    return Stream.concat(expressions.stream(), statements.stream());
  }

  /** Returns true if and only if {@code n} is contained in {@code scope} */
  public boolean inScope(Node n) {
    // Always true for LocalScope
    var scopeStatementsRoot = statements.getParentNode().get();
    for (var e : expressions) if (n.equals(e) || e.isAncestorOf(n)) return true;
    for (var s : statements) if (n.equals(s) || s.isAncestorOf(n)) return true;
    return false;
  }
}
