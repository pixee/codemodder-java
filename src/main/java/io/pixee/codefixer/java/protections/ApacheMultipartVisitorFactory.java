package io.pixee.codefixer.java.protections;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import io.pixee.codefixer.java.FileWeavingContext;
import io.pixee.codefixer.java.MethodCallPredicateFactory;
import io.pixee.codefixer.java.MethodCallTransformingModifierVisitor;
import io.pixee.codefixer.java.Transformer;
import io.pixee.codefixer.java.VisitorFactory;
import io.pixee.codefixer.java.Weave;
import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * This type weaves a protection against path traversal attacks on Apache Multipart library by
 * normalizing the filename pulled from a multipart request.
 */
public final class ApacheMultipartVisitorFactory implements VisitorFactory {

  @Override
  public ModifierVisitor<FileWeavingContext> createJavaCodeVisitorFor(
      final File file, final CompilationUnit cu) {
    List<Predicate<MethodCallExpr>> predicates =
        List.of(
            MethodCallPredicateFactory.withName("getName"),
            MethodCallPredicateFactory.withArgumentCount(0),
            MethodCallPredicateFactory.withScopeType(cu, "org.apache.commons.fileupload.FileItem")
                .or(
                    MethodCallPredicateFactory.withScopeType(
                        cu, "org.apache.commons.fileupload.disk.DiskFileItem")),
            MethodCallPredicateFactory.withParentCodeContains("toSimpleFileName").negate(),
            MethodCallPredicateFactory.withArgumentNodeType(0, StringLiteralExpr.class).negate(),
            MethodCallPredicateFactory.withScreamingSnakeCaseVariableNameForArgument(1).negate());

    Transformer<MethodCallExpr, MethodCallExpr> transformer =
        new Transformer<>() {
          @Override
          public TransformationResult<MethodCallExpr> transform(
              final MethodCallExpr methodCallExpr, final FileWeavingContext context) {
            MethodCallExpr safeCall =
                new MethodCallExpr(
                    new NameExpr(io.pixee.security.Filenames.class.getName()),
                    "toSimpleFileName",
                    NodeList.nodeList(methodCallExpr));
            Weave weave =
                Weave.from(
                    methodCallExpr.getRange().get().begin.line,
                    apacheMultipartFilenameSanitizerRuleId);
            return new TransformationResult<>(Optional.of(safeCall), weave);
          }
        };

    return new MethodCallTransformingModifierVisitor(cu, predicates, transformer);
  }

  @Override
  public String ruleId() {
    return apacheMultipartFilenameSanitizerRuleId;
  }

  private static final String apacheMultipartFilenameSanitizerRuleId =
      "pixee:java/sanitize-apache-multipart-filename";
}
