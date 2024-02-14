package io.codemodder.javaparser;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import io.codemodder.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class JavaParserCodemodRunnerTest {

  private static final class UpdatesMethodNamesParserChanger extends JavaParserChanger {
    private UpdatesMethodNamesParserChanger() {
      super(new EmptyReporter());
    }

    @Override
    public List<CodemodChange> visit(CodemodInvocationContext context, CompilationUnit cu) {
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
  }

  private static final class UpdatesClassNamesParserChanger extends JavaParserChanger {
    private UpdatesClassNamesParserChanger() {
      super(new EmptyReporter());
    }

    @Override
    public List<CodemodChange> visit(CodemodInvocationContext context, CompilationUnit cu) {
      final List<CodemodChange> codemodChanges = new ArrayList<>();
      cu.findAll(ClassOrInterfaceDeclaration.class).stream()
          .forEach(
              c -> {
                c.setName("B");
                codemodChanges.add(CodemodChange.from(c.getBegin().get().line));
              });
      return codemodChanges;
    }
  }

  /** A {@link JavaParserChanger} that updates all method names to "newMethodName". */
  private final JavaParserChanger updatesAllMethodNamesChanger =
      new UpdatesMethodNamesParserChanger();

  private JavaParserCodemodRunner runner;
  private Path tmpDir;
  private List<CodemodChange> expectedCodemodChanges;

  @BeforeEach
  void setup(@TempDir Path tmpDir) {
    this.runner =
        new JavaParserCodemodRunner(
            JavaParserFacade.from(JavaParser::new),
            updatesAllMethodNamesChanger,
            EncodingDetector.create());
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

    CodemodChange change1 = CodemodChange.from(4, DependencyGAV.JAVA_SECURITY_TOOLKIT);
    CodemodChange change2 = CodemodChange.from(5, DependencyGAV.JAVA_SECURITY_TOOLKIT);
    this.expectedCodemodChanges = List.of(change1, change2);

    Path javaFile = tmpDir.resolve("Foo.java");
    Files.write(javaFile, javaCode.getBytes());
    CodemodInvocationContext context = mock(CodemodInvocationContext.class);
    when(context.codemodId()).thenReturn("my-codemod-id");
    when(context.lineIncludesExcludes()).thenReturn(new LineIncludesExcludes.MatchesEverything());
    when(context.path()).thenReturn(javaFile);
    when(context.contents()).thenReturn(javaCode);
    CodeDirectory dir = mock(CodeDirectory.class);
    when(dir.asPath()).thenReturn(tmpDir);
    when(context.codeDirectory()).thenReturn(dir);
    List<CodemodChange> changes = runner.run(context);
    assertThat(changes, hasItems(expectedCodemodChanges.toArray(CodemodChange[]::new)));
    assertThat(Files.readString(javaFile), is(updatedCode));
  }

  @Test
  void it_composes_successfully() throws IOException {
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
            + "class B {\n"
            + "  void foo() {\n"
            + "    newMethodName();\n"
            + "    newMethodName();\n"
            + "  }\n"
            + "}\n";

    Path javaFile = tmpDir.resolve("Foo.java");
    Files.write(javaFile, javaCode.getBytes());
    CodemodInvocationContext context = mock(CodemodInvocationContext.class);
    when(context.codemodId()).thenReturn("pixee-test:java/my-composite");
    when(context.lineIncludesExcludes()).thenReturn(new LineIncludesExcludes.MatchesEverything());
    when(context.path()).thenReturn(javaFile);
    when(context.contents()).thenReturn(javaCode);
    CodeDirectory dir = mock(CodeDirectory.class);
    when(dir.asPath()).thenReturn(tmpDir);
    when(context.codeDirectory()).thenReturn(dir);

    this.runner =
        new JavaParserCodemodRunner(
            JavaParserFacade.from(JavaParser::new),
            new RunsBothCodemod(
                new UpdatesMethodNamesParserChanger(),
                new UpdatesClassNamesParserChanger(),
                new EmptyReporter()),
            EncodingDetector.create());
    List<CodemodChange> changes = runner.run(context);

    CodemodChange change1 = CodemodChange.from(2);
    CodemodChange change2 = CodemodChange.from(4, DependencyGAV.JAVA_SECURITY_TOOLKIT);
    CodemodChange change3 = CodemodChange.from(5, DependencyGAV.JAVA_SECURITY_TOOLKIT);

    this.expectedCodemodChanges = List.of(change1, change2, change3);
    assertThat(changes, hasItems(expectedCodemodChanges.toArray(CodemodChange[]::new)));
    assertThat(Files.readString(javaFile), is(updatedCode));
  }

  @Codemod(
      reviewGuidance = ReviewGuidance.MERGE_AFTER_REVIEW,
      importance = Importance.LOW,
      id = "pixee-test:java/my-composite")
  private static class RunsBothCodemod extends CompositeJavaParserChanger {
    @Inject
    public RunsBothCodemod(
        UpdatesMethodNamesParserChanger codemod1,
        UpdatesClassNamesParserChanger codemod2,
        CodemodReporterStrategy reporter) {
      super(reporter, codemod1, codemod2);
    }
  }
}
