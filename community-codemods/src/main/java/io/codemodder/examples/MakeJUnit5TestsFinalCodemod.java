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

/** A codemod that adds final modifiers to JUnit 5 tests. */
@Codemod(
    id = "codemodder:java/make-junit5-tests-final",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW,
    importance = Importance.LOW)
public final class MakeJUnit5TestsFinalCodemod
    extends SarifPluginJavaParserChanger<ClassOrInterfaceDeclaration> {

  private static final String DETECTION_RULE =
      """
            rules:
              - id: find-nonfinal-junit-class-modifiers
                patterns:
                  - pattern: class $CLASS { ... }
                  - pattern-not: final class $CLASS { ... }
                  - pattern-not: abstract class $CLASS { ... }
                  - metavariable-regex:
                      metavariable: $CLASS
                      regex: .*Test
                  - pattern-inside: |
                      ...
                      import org.junit.jupiter.api.Test;
                      ...
            """;

  @Inject
  public MakeJUnit5TestsFinalCodemod(@SemgrepScan(yaml = DETECTION_RULE) final RuleSarif sarif) {
    super(sarif, ClassOrInterfaceDeclaration.class, CodemodReporterStrategy.empty());
  }

  @Override
  public boolean onResultFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final ClassOrInterfaceDeclaration typeDefinition,
      final Result result) {
    typeDefinition.getModifiers().add(Modifier.finalModifier());
    return true;
  }

  @Override
  public String getSummary() {
    return "Tests made final!";
  }

  @Override
  public String getDescription() {
    return "JUnit 5 tests should be final!";
  }

  @Override
  public List<CodeTFReference> getReferences() {
    return List.of(
        new CodeTFReference(
            "https://github.com/HugoMatilla/Effective-JAVA-Summary#17-design-and-document-for-inheritance-or-else-prohibit-it",
            "Effective Java (Chapter 17: Design and document for inheritance or else prohibit it)"));
  }

  @Override
  public String getIndividualChangeDescription(final Path filePath, final CodemodChange change) {
    return "Made class final";
  }
}
