package io.openpixee.java.protections;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import io.codemodder.FileWeavingContext;
import io.openpixee.java.DoNothingVisitor;
import io.openpixee.java.VisitorFactory;
import java.io.File;
import java.io.IOException;
import org.junit.jupiter.api.Test;

final class WeakPRNGTest {

  @Test
  void it_replaces_insecure_random() throws IOException {

    // we use a DoNothingVisitor here because this should be provided by the new codemod
    WeavingTests.assertJavaWeaveWorkedAndWontReweave(
        "src/test/java/com/acme/testcode/RandomVulnerability.java",
        new VisitorFactory() {
          @Override
          public ModifierVisitor<FileWeavingContext> createJavaCodeVisitorFor(
              final File file, final CompilationUnit cu) {
            return new DoNothingVisitor();
          }

          @Override
          public String ruleId() {
            return "pixee:java/unused";
          }
        });
  }
}
