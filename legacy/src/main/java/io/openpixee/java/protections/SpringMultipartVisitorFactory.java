package io.openpixee.java.protections;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import io.codemodder.DependencyGAV;
import io.codemodder.FileWeavingContext;
import io.codemodder.Weave;
import io.codemodder.ast.ASTTransforms;
import io.github.pixee.security.Filenames;
import io.openpixee.java.MethodCallPredicateFactory;
import io.openpixee.java.MethodCallTransformingModifierVisitor;
import io.openpixee.java.Transformer;
import io.openpixee.java.VisitorFactory;
import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * This type weaves a protection against path traversal attacks on Spring multipart code by
 * normalizing the filename pulled from a multipart request.
 */
public final class SpringMultipartVisitorFactory implements VisitorFactory {

  @Override
  public ModifierVisitor<FileWeavingContext> createJavaCodeVisitorFor(
      final File file, final CompilationUnit cu) {
    List<Predicate<MethodCallExpr>> predicates =
        List.of(
            MethodCallPredicateFactory.withName("getOriginalFilename"),
            MethodCallPredicateFactory.withArgumentCount(0),
            MethodCallPredicateFactory.withScopeType(
                cu, "org.springframework.web.multipart.MultipartFile"),
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
                    new NameExpr(Filenames.class.getSimpleName()),
                    "toSimpleFileName",
                    NodeList.nodeList(methodCallExpr));
            ASTTransforms.addImportIfMissing(cu, Filenames.class);
            Weave weave =
                Weave.from(
                    methodCallExpr.getRange().get().begin.line,
                    springMultipartFilenameSanitizerRuleId,
                    DependencyGAV.JAVA_SECURITY_TOOLKIT);
            return new TransformationResult<>(Optional.of(safeCall), weave);
          }
        };

    return new MethodCallTransformingModifierVisitor(cu, predicates, transformer);
  }

  @Override
  public String ruleId() {
    return springMultipartFilenameSanitizerRuleId;
  }

  private static final String springMultipartFilenameSanitizerRuleId =
      "pixee:java/sanitize-spring-multipart-filename";
}
