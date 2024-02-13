package io.codemodder.codemods;

import static io.codemodder.javaparser.ASTExpectations.expect;

import com.contrastsecurity.sarif.Result;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import io.codemodder.*;
import io.codemodder.providers.sarif.semgrep.ProvidedSemgrepScan;
import java.util.Optional;
import javax.inject.Inject;

@Codemod(
    id =
        "semgrep:java/java.lang.security.audit.overly-permissive-file-permission.overly-permissive-file-permission",
    reviewGuidance = ReviewGuidance.MERGE_AFTER_CURSORY_REVIEW,
    importance = Importance.MEDIUM,
    executionPriority = CodemodExecutionPriority.HIGH)
public final class SemgrepOverlyPermissiveFilePermissionsCodemod
    extends CompositeJavaParserChanger {

  @Inject
  public SemgrepOverlyPermissiveFilePermissionsCodemod(
      @ProvidedSemgrepScan(
              ruleId =
                  "java.lang.security.audit.overly-permissive-file-permission.overly-permissive-file-permission")
          final RuleSarif sarif) {
    super(
        new PermissionAddCallChanger(sarif),
        new FromStringChanger(sarif),
        new InlineFromStringChanger(sarif));
  }

  /**
   * This handles the case where the permission is added inline to {@code setPosixFilePermissions()}
   * like:
   *
   * <p>{@code Files.setPosixFilePermissions(startupScript,
   * PosixFilePermissions.fromString("rwxrwxrwx"));}
   */
  private static final class InlineFromStringChanger
      extends SarifPluginJavaParserChanger<ExpressionStmt> {

    private InlineFromStringChanger(final RuleSarif sarif) {
      super(
          sarif,
          ExpressionStmt.class,
          RegionNodeMatcher.MATCHES_START,
          CodemodReporterStrategy.empty());
    }

    @Override
    public boolean onResultFound(
        final CodemodInvocationContext context,
        final CompilationUnit cu,
        final ExpressionStmt stmt,
        final Result result) {
      Optional<MethodCallExpr> setPosixFilePermissionsRef =
          expect(stmt)
              .toBeExpressionStatement()
              .withMethodCallExpression()
              .withName("setPosixFilePermissions")
              .withArgumentsSize(2)
              .result();
      if (setPosixFilePermissionsRef.isPresent()) {
        MethodCallExpr setPosixFilePermissions = setPosixFilePermissionsRef.get();
        Optional<MethodCallExpr> fromStringRef =
            expect(setPosixFilePermissions.getArgument(1))
                .toBeMethodCallExpression()
                .withName("fromString")
                .withArgumentsSize(1)
                .result();
        if (fromStringRef.isPresent()) {
          return fixFromString(fromStringRef.get());
        }
      }
      return false;
    }
  }

  /**
   * This handles the case where permissions are created from UNIX permission strings like:
   *
   * <p>{@code var p = Permissions.fromString("rwxrwxrwx")}
   */
  private static final class FromStringChanger
      extends SarifPluginJavaParserChanger<ExpressionStmt> {
    private FromStringChanger(final RuleSarif sarif) {
      super(
          sarif,
          ExpressionStmt.class,
          RegionNodeMatcher.MATCHES_START,
          CodemodReporterStrategy.empty());
    }

    @Override
    public boolean onResultFound(
        final CodemodInvocationContext context,
        final CompilationUnit cu,
        final ExpressionStmt fromStringStmt,
        final Result result) {
      Optional<MethodCallExpr> fromStringMethodRef =
          expect(fromStringStmt)
              .toBeExpressionStatement()
              .withSingleVariableDeclarationExpression()
              .toBeInitializedByMethodCall()
              .withArgumentsSize(1)
              .withName("fromString")
              .result();
      if (fromStringMethodRef.isPresent()) {
        return fixFromString(fromStringMethodRef.get());
      }
      return false;
    }
  }

  private static boolean fixFromString(final MethodCallExpr fromStringCall) {
    NodeList<Expression> arguments = fromStringCall.getArguments();
    if (arguments.size() == 1 && arguments.get(0).isStringLiteralExpr()) {
      StringLiteralExpr permissionValue = arguments.get(0).asStringLiteralExpr();
      String previousPermission = permissionValue.getValue();
      if (previousPermission.length() == 9) {
        String newPermission = previousPermission.substring(0, 6) + "---";
        arguments.set(0, new StringLiteralExpr(newPermission));
        return true;
      }
    }
    return false;
  }

  /**
   * This handles the case where the permission is added to a set via {@link
   * java.util.Set#add(Object)} call.
   */
  private static final class PermissionAddCallChanger
      extends SarifPluginJavaParserChanger<MethodCallExpr> {

    private PermissionAddCallChanger(final RuleSarif sarif) {
      super(
          sarif,
          MethodCallExpr.class,
          RegionNodeMatcher.MATCHES_START,
          CodemodReporterStrategy.empty());
    }

    @Override
    public boolean onResultFound(
        final CodemodInvocationContext context,
        final CompilationUnit cu,
        final MethodCallExpr methodCallExpr,
        final Result result) {
      Optional<MethodCallExpr> methodCall =
          expect(methodCallExpr).toBeMethodCallExpression().withArguments().result();

      if (methodCall.isEmpty()) {
        return false;
      }

      MethodCallExpr call = methodCall.get();
      String methodName = call.getNameAsString();
      if ("add".equals(methodName) && call.getArguments().size() == 1) {
        return fixAdd(call);
      }
      return false;
    }

    private boolean fixAdd(final MethodCallExpr call) {
      NodeList<Expression> arguments = call.getArguments();
      Expression permissionArgument = arguments.get(0);
      if (!permissionArgument.isFieldAccessExpr()) {
        return false;
      }
      FieldAccessExpr othersAccess = permissionArgument.asFieldAccessExpr();
      final String newFieldName;
      switch (othersAccess.getNameAsString()) {
        case "OTHERS_READ" -> newFieldName = "GROUP_READ";
        case "OTHERS_WRITE" -> newFieldName = "GROUP_WRITE";
        case "OTHERS_EXECUTE" -> newFieldName = "GROUP_EXECUTE";
        default -> {
          return false;
        }
      }
      othersAccess.setName(newFieldName);
      return true;
    }
  }
}
