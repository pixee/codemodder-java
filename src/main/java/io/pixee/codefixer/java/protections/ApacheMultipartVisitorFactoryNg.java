package io.pixee.codefixer.java.protections;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import io.pixee.codefixer.java.FileWeavingContext;
import io.pixee.codefixer.java.MethodCallTransformingModifierVisitor;
import io.pixee.codefixer.java.NodePredicateFactory;
import io.pixee.codefixer.java.Transformer;
import io.pixee.codefixer.java.VisitorFactory;
import io.pixee.codefixer.java.Weave;

import java.io.File;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * This type weaves a protection against path traversal attacks on Apache Multipart library by
 * normalizing the filename pulled from a multipart request.
 */
public final class ApacheMultipartVisitorFactoryNg implements VisitorFactory {

  @Override
  public ModifierVisitor<FileWeavingContext> createJavaCodeVisitorFor(
      final File file, final CompilationUnit cu) {
    Set<Predicate<MethodCallExpr>> predicates = Set.of(
            NodePredicateFactory.withMethodName("getName"),
            NodePredicateFactory.withArgumentCount(0),
            NodePredicateFactory.withScopeType(cu, "org.apache.commons.fileupload.FileItem").or(NodePredicateFactory.withScopeType(cu, "org.apache.commons.fileupload.disk.DiskFileItem")),
            NodePredicateFactory.withParentCodeContains("toSimpleFileName").negate(),

            NodePredicateFactory.withArgumentNodeType(0, StringLiteralExpr.class).negate(),
            NodePredicateFactory.withScreamingSnakeCaseVariableNameForArgument(1).negate()
    );

    Transformer<MethodCallExpr> transformer = new Transformer<>() {
      @Override
      public TransformationResult<MethodCallExpr> transform(final MethodCallExpr methodCallExpr, final FileWeavingContext context) {
        MethodCallExpr safeCall =
                new MethodCallExpr(
                        new NameExpr(io.pixee.security.SafeIO.class.getName()),
                        "toSimpleFileName",
                        NodeList.nodeList(methodCallExpr));
        Weave weave =
                Weave.from(methodCallExpr.getRange().get().begin.line, apacheMultipartFilenameSanitizerRuleId);
        return new TransformationResult<>(Optional.of(safeCall), weave);
      }
    };

    return new MethodCallTransformingModifierVisitor(cu, predicates, transformer);
  }

  @Override
  public String ruleId() {
      return apacheMultipartFilenameSanitizerRuleId;
  }

  private static final String apacheMultipartFilenameSanitizerRuleId = "pixee:java/apache-multipart-filename-sanitizer";
}
