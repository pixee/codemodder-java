package io.codemodder.remediation.errorexposure;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import io.codemodder.CodemodFileScanningResult;
import io.codemodder.ast.ASTs;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.javaparser.ChangesResult;
import io.codemodder.remediation.*;
import io.codemodder.remediation.headerinjection.HeaderInjectionFixStrategy;
import javassist.expr.MethodCall;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * Removes exposure from error messages.
 */
public final class ErrorMessageExposureFixStrategy extends MatchAndFixStrategy {

    private static List<String> printErrorMethods = List.of("printStackTrace");
    private static List<String> printMethods = List.of("println", "print", "sendError");

    /**
     * Test if the node is an expression that is the argument of a method call
     * @param node
     * @return
     */
    @Override
    public boolean match(final Node node) {
        return Optional.empty()
                // is an argument of a call e.g. (response.sendError(418,<expression>))
                .or(() -> Optional.of(node).map(n -> n instanceof Expression? (Expression) n : null).flatMap(ASTs::isArgumentOfMethodCall).filter(mce -> printMethods.contains(mce.getNameAsString())))
                // is itself a method call that send errors: e.g. err.printStackTrace()
                .or(() -> Optional.of(node).map(n -> n instanceof Expression? (Expression) n : null).flatMap(e -> e.toMethodCallExpr()).filter(mce -> printErrorMethods.contains(mce.getNameAsString())))
                .isPresent()
                ;
    }

    @Override
    public SuccessOrReason fix(final CompilationUnit cu, final Node node) {
        // we know from the match that this is true
        Expression expr = (Expression) node;
        // find encompassing statement
        Optional<Statement> maybeStmt = Optional.<MethodCallExpr>empty()
                // grab the relevant method call from the two cases
                .or(() -> Optional.of(node).map(n -> n instanceof Expression? (Expression) n : null).flatMap(ASTs::isArgumentOfMethodCall).filter(mce -> printMethods.contains(mce.getNameAsString())))
                // is itself a method call that send errors: e.g. err.printStackTrace()
                .or(() -> Optional.of(node).map(n -> n instanceof Expression? (Expression) n : null).flatMap(e -> e.toMethodCallExpr()).filter(mce -> printErrorMethods.contains(mce.getNameAsString())))
                // check if the method call is in a statement by itself
                .flatMap(mce -> mce.getParentNode())
                .map(p -> p instanceof ExpressionStmt? (ExpressionStmt) p : null);
        // Remove it
        if (maybeStmt.isPresent()){
            maybeStmt.get().remove();
            return SuccessOrReason.success();
        }
        return SuccessOrReason.reason("The call is not a statement");
    }
}
