package io.codemodder.remediation.sqlinjection;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import io.codemodder.Either;
import io.codemodder.ast.ASTs;
import io.codemodder.ast.LocalVariableDeclaration;
import io.codemodder.remediation.RemediationStrategy;
import io.codemodder.remediation.SuccessOrReason;
import java.util.Optional;

/** Composes several transformations related to SQL injections. */
public final class SQLInjectionFixComposer implements RemediationStrategy {

  public SQLInjectionFixComposer() {}

  /**
   * Checks if the given binaryExpr ends up as an argument for a SQL execute method that can be
   * fixed.
   *
   * @param binaryExpr
   * @return An Optional containing the execute call if successful
   */
  private Optional<MethodCallExpr> flowsIntoExecuteCall(final BinaryExpr binaryExpr) {
    // Is argument of a method call
    var maybeDirectArgumentOfCall =
        ASTs.isArgumentOfMethodCall(binaryExpr).filter(SQLInjectionFixComposer::match);

    // or it is initialization of a variable that flows into an execute call
    return maybeDirectArgumentOfCall.or(() -> isIndirectArgumentOfCall(binaryExpr));
  }

  /**
   * Test if the expr is initialized into a variable that flows into a call
   *
   * @param expr
   * @return
   */
  private Optional<MethodCallExpr> isIndirectArgumentOfCall(final Expression expr) {
    return ASTs.isInitExpr(expr)
        .flatMap(LocalVariableDeclaration::fromVariableDeclarator)
        .flatMap(
            lvd ->
                lvd.findAllReferences()
                    .flatMap(ne -> ASTs.isArgumentOfMethodCall(ne).stream())
                    .filter(SQLInjectionFixComposer::match)
                    .findFirst());
  }

  /**
   * Given a node, checks if it is a {@link MethodCallExpr} related to executing JDBC API SQL
   * queries (i.e. prepareStatement(), executeQuery(), etc.), or a {@link BinaryExpr} that flows
   * into one, parameterize data injections or add a validation step for structural injections.
   */
  public SuccessOrReason fix(final CompilationUnit cu, final Node node) {

    // Is a binary expr or method call expr?
    Optional<Either<MethodCallExpr, BinaryExpr>> morb;
    if (node instanceof MethodCallExpr m) {
      morb = Optional.of(Either.left(m));
    } else if (node instanceof BinaryExpr b) {
      morb = Optional.of(Either.right(b));
    } else {
      morb = Optional.empty();
    }
    if (morb.isEmpty()) {
      return SuccessOrReason.reason("Neither a binary expression or method call");
    }
    // If binary expr, try to find if it flows into a function as argument
    // map the left into optional for type consistency
    Optional<MethodCallExpr> maybeCall =
        morb.flatMap(e -> e.ifLeftOrElseGet(Optional::of, this::flowsIntoExecuteCall));
    if (maybeCall.isEmpty()) {
      return SuccessOrReason.reason(
          "Could not find any execute call that the binary expr flows into");
    }

    MethodCallExpr methodCallExpr = maybeCall.get();

    // First, check if any data injection fixes apply
    var maybeFixed = new SQLParameterizer(methodCallExpr, cu).checkAndFix();
    if (maybeFixed.isPresent()) {
      // If yes, execute cleanup steps and check if any table injection remains.
      SQLParameterizerWithCleanup.cleanup(maybeFixed.get());
      SQLTableInjectionFilterTransform.findAndFix(maybeFixed.get());
      return SuccessOrReason.success();
      // If not, try the table injection only
    } else {
      return SQLTableInjectionFilterTransform.findAndFix(methodCallExpr)
          ? SuccessOrReason.success()
          : SuccessOrReason.reason("Could not fix injection");
    }
  }

  /**
   * Check if the node is a JDBC API query method that is a target of a SQL injection
   * transformation.
   */
  public static boolean match(final Node node) {
    var maybeMethodCall =
        Optional.of(node)
            .map(n -> n instanceof MethodCallExpr ? (MethodCallExpr) n : null)
            .filter(
                n ->
                    SQLParameterizer.isSupportedJdbcMethodCall(n)
                        || SQLTableInjectionFilterTransform.matchCall(n));
    return maybeMethodCall.isPresent();
  }
}
