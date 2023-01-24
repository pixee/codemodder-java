package io.openpixee.java.plugins.codeql;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import java.util.Optional;

public final class JDBCResourceLeakFixer {

  public static boolean isFixable(MethodCallExpr mce) {
    // Assumptions/Properties from CodeQL:
    // It is not a try resource, no close() is called within its scope (even if assigned).
    // There does not exists a "root" expression that is closed.
    // It does not escape its context: assigned to a field or returned.

    // We test for the following property: It has no descendent resource r that is: (1) not closed,
    // and (2) escapes mce's scope.

    return false;
  }

  /** Fixes the leak of {@code mce} and returns its line */
  public static int fix(MethodCallExpr mce) {
    int originalLine = mce.getRange().get().begin.line;
    // Actual fix here
    return originalLine;
  }

  public static Optional<Integer> checkAndFix(MethodCallExpr mce) {
    if (isFixable(mce)) return Optional.of(fix(mce));
    return Optional.empty();
  }

  public static void main(String[] args) {
    String code = "";
    final var combinedTypeSolver = new CombinedTypeSolver();
    StaticJavaParser.getParserConfiguration()
        .setSymbolResolver(new JavaSymbolSolver(combinedTypeSolver));
    var cu = StaticJavaParser.parse(code);
    LexicalPreservingPrinter.setup(cu);
    System.out.println(LexicalPreservingPrinter.print(cu));
  }
}
