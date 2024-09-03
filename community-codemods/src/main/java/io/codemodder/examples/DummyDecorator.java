package io.codemodder.examples;

import static io.codemodder.ast.ASTTransforms.addImportIfMissing;

import com.contrastsecurity.sarif.Result;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import io.codemodder.*;
import io.codemodder.javaparser.ChangesResult;
import io.codemodder.providers.sarif.semgrep.SemgrepScan;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;

/** Adds a dummy decorator. */
@Codemod(
    id = "acme:java/dummy-decorator",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW,
    importance = Importance.MEDIUM,
    executionPriority = CodemodExecutionPriority.LOW)
public final class DummyDecorator
    extends SarifPluginJavaParserChanger<ClassOrInterfaceDeclaration> {

  private static final String DETECTION_RULE =
      """
            rules:
              - id: dummy-decorator
                pattern: public class $CLAZZ implements Serializable
            """;

  /** This reporter tells our users who receive these changes about the change. */
  private static class DummyDecoratorReporter implements CodemodReporterStrategy {
    @Override
    public String getSummary() {
      return "Added decorator";
    }

    @Override
    public String getDescription() {
      return "description here";
    }

    @Override
    public String getChange(final Path path, final CodemodChange change) {
      return "change here";
    }

    @Override
    public List<String> getReferences() {
      return List.of("https://com.acme/dummy-decorator");
    }
  }

  @Inject
  public DummyDecorator(@SemgrepScan(yaml = DETECTION_RULE) final RuleSarif sarif) {
    // javaparser returns the range including everything from beginning of class to end of class,
    // since that's the
    // top level class AST node. we just want the class declaration part, so we are okay to match
    // semgrep on just
    // the line start
    super(
        sarif,
        ClassOrInterfaceDeclaration.class,
        RegionNodeMatcher.MATCHES_LINE,
        new DummyDecoratorReporter());
  }

  @Override
  public ChangesResult onResultFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final ClassOrInterfaceDeclaration classOrInterfaceDeclaration,
      final Result result) {
    Optional<AnnotationExpr> dummyAnnotation =
        classOrInterfaceDeclaration.getAnnotationByName("DummyAnnotation");
    if (dummyAnnotation.isEmpty()) {
      classOrInterfaceDeclaration.addAnnotation("DummyAnnotation");
      addImportIfMissing(cu, "com.acme.DummyAnnotation");
      return ChangesResult.changesApplied;
    }
    return ChangesResult.noChanges;
  }
}
