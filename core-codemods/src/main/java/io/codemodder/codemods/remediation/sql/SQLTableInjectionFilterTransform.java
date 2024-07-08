package io.codemodder.codemods.remediation.sql;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import io.codemodder.ast.ASTTransforms;
import io.codemodder.ast.LinearizedStringExpression;
import io.codemodder.codemods.SQLParameterizer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/** Wrap table name parameters in SQL queries with an alphanumeric blacklist filter. */
public final class SQLTableInjectionFilterTransform {

  private SQLTableInjectionFilterTransform() {}

  private static boolean isExecuteCall(final MethodCallExpr methodCallExpr) {
    return SQLParameterizer.isParameterizationCandidate(methodCallExpr);
  }

  private static boolean isPrepareStatementCall(final MethodCallExpr methodCallExpr) {
    try {
      final Predicate<MethodCallExpr> isPrepareStatementCallPredicate =
          call -> call.getNameAsString().equals("prepareStatement");

      final Predicate<MethodCallExpr> hasSQLConnectionScope =
          n ->
              n.getScope()
                  .filter(
                      s -> {
                        try {
                          String resolvedType = s.calculateResolvedType().describe();
                          return "java.sql.Connection".equals(resolvedType);
                        } catch (RuntimeException e) {
                          return false;
                        }
                      })
                  .isPresent();

      final Predicate<MethodCallExpr> isFirstArgumentNotSLE =
          n ->
              n.getArguments().getFirst().map(e -> !(e instanceof StringLiteralExpr)).orElse(false);
      // is execute of a statement object whose first argument is not a string?
      return isPrepareStatementCallPredicate
          .and(hasSQLConnectionScope.and(isFirstArgumentNotSLE))
          .test(methodCallExpr);

      // Thrown by the JavaParser Symbol Solver when it can't resolve types
    } catch (RuntimeException e) {
      return false;
    }
  }

  public static boolean matchCall(final MethodCallExpr call) {
    return isPrepareStatementCall(call) || isExecuteCall(call);
  }

  public static boolean fix(final MethodCallExpr call) {
    final var linearized = new LinearizedStringExpression(call.getArgument(0));
    var injections = findTableInjections(linearized);
    injections =
        injections.stream()
            .filter(
                e ->
                    !(e.isMethodCallExpr()
                        && e.asMethodCallExpr().getNameAsString().equals("filterTable")))
            .toList();
    if (!injections.isEmpty()) {
      fix(injections);
      return true;
    }
    return false;
  }

  public static boolean findAndFix(final MethodCallExpr call) {
    if (matchCall(call)) {
      return fix(call);
    }
    return false;
  }

  private static final Pattern regex =
      Pattern.compile(".*from\s+((\\\\)?\")?", Pattern.CASE_INSENSITIVE);

  private static final String filterMethodName = "filterTable";

  private static List<Expression> findTableInjections(final LinearizedStringExpression linearized) {
    final var tableInjections = new ArrayList<Expression>();
    // It takes the next expression after a string that ends in FROM
    // This is not foolproof as join statements may appear after or it may be composed of multiple
    // expression
    // e.g. "SELECT * FROM " + "user_" + tablename + "_secret"
    // Would need to check the grammar and cover all the cases to identify where it ends
    for (var it = linearized.getLinearized().iterator(); it.hasNext(); ) {
      var expr = it.next();
      if (expr.isStringLiteralExpr()) {
        var value = expr.asStringLiteralExpr().getValue();
        if (regex.matcher(value).matches()) {
          if (it.hasNext()) {
            tableInjections.add(it.next());
          }
        }
      }
    }
    // We don't care about static table names...
    tableInjections.removeIf(Expression::isStringLiteralExpr);
    return tableInjections;
  }

  private static void addFilterMethodIfMissing(final ClassOrInterfaceDeclaration classDecl) {
    final String method =
        """
		  void filterTable(final String tablename){
			  Pattern regex = Pattern.compile("[a-zA-Z0-9_]+(.[a-zA-Z0-9_]+)?");
			  if (!regex.matcher(tablename).matches()){
				  throw new SecurityException("Supplied table name contains non-alphanumeric characters");
			  }
		  }
	  """;
    boolean filterMethodPresent =
        classDecl.findAll(MethodDeclaration.class).stream()
            .anyMatch(
                md ->
                    md.getNameAsString().equals(filterMethodName)
                        && md.getParameters().size() == 1
                        && md.getParameters().get(0).getTypeAsString().equals("String"));
    if (!filterMethodPresent) {
      classDecl.addMember(StaticJavaParser.parseMethodDeclaration(method));
    }
    // Add Pattern import
    ASTTransforms.addImportIfMissing(
        classDecl.findCompilationUnit().get(), "java.util.regex.Pattern");
  }

  private static void fix(final List<Expression> injections) {
    injections.forEach(SQLTableInjectionFilterTransform::wrapExpressionWithCall);
    var classDecl = injections.get(0).findAncestor(ClassOrInterfaceDeclaration.class);
    classDecl.ifPresent(SQLTableInjectionFilterTransform::addFilterMethodIfMissing);
  }

  private static void wrapExpressionWithCall(final Expression expr) {
    var newCall = new MethodCallExpr(filterMethodName);
    expr.replace(newCall);
    // maybe use toString instead ?
    newCall.addArgument(new BinaryExpr(expr, new StringLiteralExpr(""), BinaryExpr.Operator.PLUS));
  }
}
