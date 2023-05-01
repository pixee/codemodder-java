package io.codemodder.javaparser;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import io.codemodder.*;
import io.codemodder.codetf.CodeTFReference;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class JavaParserCodemodRunnerTest {

  /** A {@link JavaParserChanger} that updates all method names to "newMethodName". */
  private final JavaParserChanger updatesAllMethodNamesChanger =
      new JavaParserChanger() {
        @Override
        public List<CodemodChange> visit(CodemodInvocationContext context, CompilationUnit cu) {
          LexicalPreservingPrinter.setup(cu);
          final List<CodemodChange> codemodChanges = new ArrayList<>();
          cu.findAll(MethodCallExpr.class).stream()
              .forEach(
                  mce -> {
                    mce.setName("newMethodName");
                    codemodChanges.add(
                        CodemodChange.from(
                            mce.getBegin().get().line, DependencyGAV.JAVA_SECURITY_TOOLKIT));
                  });
          return codemodChanges;
        }

        @Override
        public String getSummary() {
          return "my summary";
        }

        @Override
        public String getDescription() {
          return "my description";
        }

        @Override
        public List<CodeTFReference> getReferences() {
          return List.of(new CodeTFReference("my reference", "my reference description"));
        }
      };

  private JavaParserCodemodRunner runner;
  private Path tmpDir;
  private List<CodemodChange> expectedCodemodChanges;

  @BeforeEach
  void setup(@TempDir Path tmpDir) {
    CodemodChange change1 = CodemodChange.from(4, DependencyGAV.JAVA_SECURITY_TOOLKIT);
    CodemodChange change2 = CodemodChange.from(5, DependencyGAV.JAVA_SECURITY_TOOLKIT);
    this.expectedCodemodChanges = List.of(change1, change2);
    this.runner =
        new JavaParserCodemodRunner(
            new JavaParser(), updatesAllMethodNamesChanger, EncodingDetector.create());
    this.tmpDir = tmpDir;
  }

  @Test
  void it_returns_fixed_file() throws IOException {
    String javaCode =
        "import thing;\n"
            + "class A {\n"
            + "  void foo() {\n"
            + "    bar();\n"
            + "    bar();\n"
            + "  }\n"
            + "}\n";
    String updatedCode =
        "import thing;\n"
            + "class A {\n"
            + "  void foo() {\n"
            + "    newMethodName();\n"
            + "    newMethodName();\n"
            + "  }\n"
            + "}\n";

    Path javaFile = tmpDir.resolve("Foo.java");
    Files.write(javaFile, javaCode.getBytes());
    CodemodInvocationContext context = mock(CodemodInvocationContext.class);
    when(context.codemodId()).thenReturn("my-codemod-id");
    when(context.lineIncludesExcludes()).thenReturn(new LineIncludesExcludes.MatchesEverything());
    when(context.path()).thenReturn(javaFile);
    CodeDirectory dir = mock(CodeDirectory.class);
    when(dir.asPath()).thenReturn(tmpDir);
    when(context.codeDirectory()).thenReturn(dir);
    List<CodemodChange> changes = runner.run(context);
    assertThat(changes, hasItems(expectedCodemodChanges.toArray(CodemodChange[]::new)));
    assertThat(Files.readString(javaFile), is(updatedCode));
  }
}
