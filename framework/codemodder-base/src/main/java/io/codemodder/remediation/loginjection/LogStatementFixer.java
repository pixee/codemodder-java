package io.codemodder.remediation.loginjection;

import static io.codemodder.javaparser.JavaParserTransformer.wrap;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.resolution.types.ResolvedType;
import io.codemodder.DependencyGAV;
import io.codemodder.ast.ASTs;
import io.codemodder.ast.LocalDeclaration;
import io.codemodder.remediation.RemediationStrategy;
import io.codemodder.remediation.SuccessOrReason;
import io.github.pixee.security.Newlines;
import java.util.List;
import java.util.Optional;

/**
 * The shapes of code we want to be able to fix:
 *
 * <pre>
 * log.info("User with id: " + userId + " has been created");
 * logger.error("User with id: " + userId + " has been created", ex);
 * log.warn(msg);
 * </pre>
 */
final class LogStatementFixer implements RemediationStrategy {

  @Override
  public SuccessOrReason fix(final CompilationUnit compilationUnit, final Node node) {
    MethodCallExpr logCall = (MethodCallExpr) node;
    NodeList<Expression> arguments = logCall.getArguments();
    return fixArguments(arguments);
  }

  private SuccessOrReason fixArguments(final NodeList<Expression> arguments) {

    // first and only is NameExpr (not an exception) (args == 1), (args can be 2 if exception)
    if ((arguments.size() == 1 && isNameOrMethodExpr(arguments.get(0)))
        || (arguments.size() == 2
            && isNameOrMethodExpr(arguments.get(0))
            && isException(arguments.get(1)))) {
      Expression argument = arguments.get(0);
      wrapWithNewlineSanitizer(argument);
      return SuccessOrReason.success(List.of(DependencyGAV.JAVA_SECURITY_TOOLKIT));
    }

    // first is string literal and second is NameExpr (args can be 3 if not exception)
    if ((arguments.size() == 2
            && arguments.get(0).isStringLiteralExpr()
            && isNameOrMethodExpr(arguments.get(1)))
        || (arguments.size() == 3
            && arguments.get(0).isStringLiteralExpr()
            && isNameOrMethodExpr(arguments.get(1))
            && isException(arguments.get(2)))) {
      Expression argument = arguments.get(1);
      wrapWithNewlineSanitizer(argument);
      return SuccessOrReason.success(List.of(DependencyGAV.JAVA_SECURITY_TOOLKIT));
    }

    // first is BinaryExpr with NameExpr in it (args == 2) (args can be 3 if last is exception)
    if ((arguments.size() == 2 && arguments.get(0).isBinaryExpr())
        || (arguments.size() == 3
            && arguments.get(0).isBinaryExpr()
            && isException(arguments.get(2)))) {
      BinaryExpr binaryExpr = arguments.get(0).asBinaryExpr();
      Optional<Expression> expressionToWrap = findExpressionToWrap(binaryExpr);
      if (expressionToWrap.isPresent()) {
        wrapWithNewlineSanitizer(expressionToWrap.get());
        return SuccessOrReason.success(List.of(DependencyGAV.JAVA_SECURITY_TOOLKIT));
      }
    }

    // first and only argument is a binary expression with a call to be able to wrap in it
    if (arguments.size() == 1 && arguments.get(0).isBinaryExpr()) {
      BinaryExpr binaryExpr = arguments.get(0).asBinaryExpr();
      Optional<Expression> expressionToWrap = findExpressionToWrap(binaryExpr);
      if (expressionToWrap.isPresent()) {
        wrapWithNewlineSanitizer(expressionToWrap.get());
        return SuccessOrReason.success(List.of(DependencyGAV.JAVA_SECURITY_TOOLKIT));
      }
    }

    return SuccessOrReason.reason("Unfixable log call shape");
  }

  private static boolean isNameOrMethodExpr(final Expression expression) {
    return expression.isNameExpr() || expression.isMethodCallExpr();
  }

  private boolean isException(final Expression expression) {
    if (expression.isNameExpr()) {
      try {
        ResolvedType type = expression.calculateResolvedType();
        String typeName = type.describe();
        return isExceptionTypeName(typeName);
      } catch (Exception e) {
        Optional<LocalDeclaration> declarationRef =
            ASTs.findEarliestLocalDeclarationOf(expression, "ex");
        if (declarationRef.isPresent()) {
          LocalDeclaration localDeclaration = declarationRef.get();
          Node declaration = localDeclaration.getDeclaration();
          // handle if its a parameter or a local variable
          if (declaration instanceof Parameter param) {
            String typeAsString = param.getTypeAsString();
            return isExceptionTypeName(typeAsString);
          } else if (declaration instanceof VariableDeclarator var) {
            String typeAsString = var.getTypeAsString();
            return isExceptionTypeName(typeAsString);
          }
        }
        Optional<Node> nameSourceNodeRef = ASTs.findNonCallableSimpleNameSource(expression, "e");
        if (nameSourceNodeRef.isPresent()) {
          Node declaration = nameSourceNodeRef.get();
          // handle if its a parameter or a local variable
          if (declaration instanceof Parameter param) {
            String typeAsString = param.getTypeAsString();
            return isExceptionTypeName(typeAsString);
          } else if (declaration instanceof VariableDeclarator var) {
            String typeAsString = var.getTypeAsString();
            return isExceptionTypeName(typeAsString);
          }
        }
      }
    }
    return false;
  }

  private static boolean isExceptionTypeName(final String typeName) {
    return typeName.endsWith("Exception") || typeName.endsWith("Throwable");
  }

  /**
   * We handle 4 expression code shapes. <code>
   *     print(user.getName());
   *     print("Hello, " + user.getName());
   *     print(user.getName() + ", hello!");
   *     print("Hello, " + user.getName() + ", hello!");
   * </code>
   *
   * <p>Note that we should only handle, for the tougher cases, string literals in combination with
   * the given expression. Note any other combination of expressions.
   */
  private static Optional<Expression> findExpressionToWrap(final Expression argument) {

    if (argument.isNameExpr()) {
      return Optional.of(argument);
    } else if (argument.isBinaryExpr()) {
      BinaryExpr binaryExpr = argument.asBinaryExpr();
      if (binaryExpr.getLeft().isBinaryExpr() && binaryExpr.getRight().isStringLiteralExpr()) {
        BinaryExpr leftBinaryExpr = binaryExpr.getLeft().asBinaryExpr();
        if (leftBinaryExpr.getLeft().isStringLiteralExpr()
            && !leftBinaryExpr.getRight().isStringLiteralExpr()) {
          return Optional.of(leftBinaryExpr.getRight());
        }
      } else if (binaryExpr.getLeft().isStringLiteralExpr()
          && binaryExpr.getRight().isStringLiteralExpr()) {
        return Optional.empty();
      } else if (binaryExpr.getLeft().isStringLiteralExpr()) {
        return Optional.of(binaryExpr.getRight());
      } else if (binaryExpr.getRight().isStringLiteralExpr()) {
        return Optional.of(binaryExpr.getLeft());
      }
    }
    return Optional.empty();
  }

  private static void wrapWithNewlineSanitizer(final Expression expression) {
    wrap(expression).withStaticMethod(Newlines.class.getName(), "stripAll", true);
  }
}
