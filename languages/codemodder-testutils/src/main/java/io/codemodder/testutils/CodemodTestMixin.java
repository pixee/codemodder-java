package io.codemodder.testutils;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import io.codemodder.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The basic tests for codemods. */
public interface CodemodTestMixin {

  @Test
  default void it_verifies_codemod(@TempDir final Path tmpDir) throws IOException {
    Metadata metadata = getClass().getAnnotation(Metadata.class);
    if (metadata == null) {
      throw new IllegalArgumentException("CodemodTest must be annotated with @Metadata");
    }

    Class<? extends Changer> codemod = metadata.codemodType();
    Path testResourceDir = Path.of(metadata.testResourceDir());

    List<DependencyGAV> dependencies =
        Arrays.asList(metadata.dependencies()).stream()
            .map(
                dependency -> {
                  String[] tokens = dependency.split(":");
                  return DependencyGAV.createDefault(tokens[0], tokens[1], tokens[2]);
                })
            .collect(Collectors.toUnmodifiableList());

    Path testDir = Path.of("src/test/resources/" + testResourceDir);
    verifyCodemod(codemod, tmpDir, testDir, dependencies);
  }

  private CompilationUnit parseJavaFile(final Path javaFile) throws IOException {
    JavaParser javaParser = new JavaParser();

    final CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
    combinedTypeSolver.add(new ReflectionTypeSolver());
    javaParser.getParserConfiguration().setSymbolResolver(new JavaSymbolSolver(combinedTypeSolver));

    final ParseResult<CompilationUnit> result = javaParser.parse(Files.newInputStream(javaFile));
    if (!result.isSuccessful()) {
      throw new IllegalArgumentException("couldn't parse file: " + javaFile);
    }
    final CompilationUnit cu = result.getResult().orElseThrow();
    LexicalPreservingPrinter.setup(cu);
    return cu;
  }

  private void verifyCodemod(
      final Class<? extends Changer> codemod,
      final Path tmpDir,
      final Path testResourceDir,
      final List<DependencyGAV> dependencies)
      throws IOException {

    // create a copy of the test file in the temp directory to serve as our "repository"
    Path before = testResourceDir.resolve("Test.java.before");
    Path after = testResourceDir.resolve("Test.java.after");
    Path pathToJavaFile = tmpDir.resolve("Test.java");
    Files.copy(before, pathToJavaFile);

    // run the codemod
    CodemodInvoker invoker = new CodemodInvoker(List.of(codemod), tmpDir);
    CompilationUnit cu = parseJavaFile(pathToJavaFile);

    FileWeavingContext context =
        FileWeavingContext.createDefault(pathToJavaFile.toFile(), IncludesExcludes.any());
    invoker.execute(pathToJavaFile, cu, context);

    // make sure the file is transformed to the expected output
    String transformedJavaCode = LexicalPreservingPrinter.print(cu);
    assertThat(transformedJavaCode, equalTo(Files.readString(after)));

    // make sure the dependencies are added
    List<Weave> weaves = context.weaves();
    List<DependencyGAV> dependenciesNeeded = weaves.get(0).getDependenciesNeeded();
    assertThat(dependenciesNeeded, hasItems(dependencies.toArray(new DependencyGAV[0])));
  }
}
