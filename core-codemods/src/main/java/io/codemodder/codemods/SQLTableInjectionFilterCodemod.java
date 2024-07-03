package io.codemodder.codemods;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import io.codemodder.*;
import io.codemodder.ast.LinearizedStringExpression;
import io.codemodder.javaparser.JavaParserChanger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/** Parameterizes SQL statements in the JDBC API. */
@Codemod(
    id = "pixee:java/sql-table-injection-filter",
    importance = Importance.HIGH,
    reviewGuidance = ReviewGuidance.MERGE_AFTER_REVIEW)
public final class SQLTableInjectionFilterCodemod extends JavaParserChanger {

  private Optional<CodemodChange> onNodeFound(final MethodCallExpr methodCallExpr) {
    if (findAndFix(methodCallExpr)) {
      return Optional.of(CodemodChange.from(methodCallExpr.getBegin().get().line));
    }
    return Optional.empty();
  }

  @Override
  public CodemodFileScanningResult visit(
      final CodemodInvocationContext context, final CompilationUnit cu) {
    List<CodemodChange> changes =
        cu.findAll(MethodCallExpr.class).stream()
            .flatMap(mce -> onNodeFound(mce).stream())
            .toList();
    return CodemodFileScanningResult.withOnlyChanges(changes);
  }

  /**
   * Checks if the {@link MethodCallExpr} is of one of the execute calls of {@link
   * java.sql.Statement} whose argument is not a {@link String} literal.
   */
  static boolean isExecuteCall(final MethodCallExpr methodCallExpr) {
    try {
      final Predicate<MethodCallExpr> isExecute = SQLParameterizer::isSupportedJdbcMethodCall;

      final Predicate<MethodCallExpr> hasScopeSQLStatement =
          n ->
              n.getScope()
                  .filter(
                      s -> {
                        try {
                          String resolvedType = s.calculateResolvedType().describe();
                          return "java.sql.Statement".equals(resolvedType);
                        } catch (RuntimeException e) {
                          return false;
                        }
                      })
                  .isPresent();

      final Predicate<MethodCallExpr> isFirstArgumentNotSLE =
          n ->
              n.getArguments().getFirst().map(e -> !(e instanceof StringLiteralExpr)).orElse(false);
      // is execute of an statement object whose first argument is not a string?
      if (isExecute.and(hasScopeSQLStatement.and(isFirstArgumentNotSLE)).test(methodCallExpr)) {
        return true;
      }
      return false;

      // Thrown by the JavaParser Symbol Solver when it can't resolve types
    } catch (RuntimeException e) {
      return false;
    }
  }

  private static boolean isPrepareStatementCall(final MethodCallExpr methodCallExpr) {
    try {
      final Predicate<MethodCallExpr> isPrepareStatementCall =
          call -> call.getNameAsString() == "prepareStatement";

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
      // is execute of an statement object whose first argument is not a string?
      if (isPrepareStatementCall
          .and(hasSQLConnectionScope.and(isFirstArgumentNotSLE))
          .test(methodCallExpr)) {
        return true;
      }
      return false;

      // Thrown by the JavaParser Symbol Solver when it can't resolve types
    } catch (RuntimeException e) {
      return false;
    }
  }

  public static boolean findAndFix(final MethodCallExpr call) {
    if (isPrepareStatementCall(call) || isExecuteCall(call)) {
      var linearized = new LinearizedStringExpression(call.getArgument(0));
      fix(findTableInjections(linearized));
      return true;
    }
    return false;
  }

  private static Pattern regex = Pattern.compile(".*from\s+\"?", Pattern.CASE_INSENSITIVE);

  private static String filterMethodName = "filterTable";

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
    tableInjections.removeIf(e -> e.isStringLiteralExpr());
    return tableInjections;
  }

  private static void addFilterMethodIfMissing(final ClassOrInterfaceDeclaration classDecl) {
    // TODO allow dots in table names for schema.tablename case
    final String method =
        """
		  void filterTable(final String tablename){
			  var regex = Pattern.compile("[a-zA-Z0-9]+(.[a-zA-Z0-9]+)?");
			  if (!regex.matcher(tablename).matches()){
				  throw new RuntimeException("Table name with no alphanumeric characters");
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
    // TODO add Pattern, RuntimeException imports

  }

  private static void fix(final List<Expression> injections) {
    injections.forEach(SQLTableInjectionFilterCodemod::wrapExpressionWithCall);
    var classDecl = injections.get(0).findAncestor(ClassOrInterfaceDeclaration.class);
    classDecl.ifPresent(SQLTableInjectionFilterCodemod::addFilterMethodIfMissing);
  }

  private static void wrapExpressionWithCall(final Expression expr) {
    var newCall = new MethodCallExpr(filterMethodName);
    expr.replace(newCall);
    // maybe use toString instead ?
    newCall.addArgument(new BinaryExpr(expr, new StringLiteralExpr(""), BinaryExpr.Operator.PLUS));
  }
}
