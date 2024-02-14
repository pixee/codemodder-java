package io.codemodder.examples;

import com.contrastsecurity.sarif.Result;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import io.codemodder.*;
import io.codemodder.codetf.CodeTFReference;
import io.codemodder.providers.sarif.semgrep.SemgrepScan;
import java.nio.file.Path;
import java.util.List;
import javax.inject.Inject;

/** A codemod that makes JUnit 5 tests package private. */
@Codemod(
    id = "codemodder:java/make-junit5-tests-package-private",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW,
    importance = Importance.LOW)
public final class MakeJUnit5TestsPackagePrivateCodemod
    extends SarifPluginJavaParserChanger<ClassOrInterfaceDeclaration> {

  private static final String DETECTION_RULE =
      """
            rules:
              - id: find-public-junit-class-modifiers
                patterns:
                  - pattern: public class $CLASS { ... }
                  - metavariable-regex:
                      metavariable: $CLASS
                      regex: .*Test
                  - pattern-inside: |
                      ...
                      import org.junit.jupiter.api.Test;
                      ...
            """;

  @Inject
  public MakeJUnit5TestsPackagePrivateCodemod(
      @SemgrepScan(yaml = DETECTION_RULE) final RuleSarif sarif) {
    super(sarif, ClassOrInterfaceDeclaration.class, CodemodReporterStrategy.empty());
  }

  @Override
  public boolean onResultFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final ClassOrInterfaceDeclaration typeDefinition,
      final Result result) {
    typeDefinition.getModifiers().removeIf(modifier -> modifier.equals(Modifier.publicModifier()));
    return true;
  }

  @Override
  public String getSummary() {
    return "Tests made package-private!";
  }

  @Override
  public String getDescription() {
    return "JUnit 5 tests should be package-private!";
  }

  @Override
  public List<CodeTFReference> getReferences() {
    return List.of(
        new CodeTFReference(
            "https://github.com/junit-team/junit5/issues/679", "JUnit 5 Issue #679"));
  }

  @Override
  public String getIndividualChangeDescription(final Path filePath, final CodemodChange change) {
    return "Made class package private";
  }
}
