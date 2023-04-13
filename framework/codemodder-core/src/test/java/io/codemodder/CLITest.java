package io.codemodder;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class CLITest {

  private Path workingRepoDir;

  private Path fooJavaFile;

  private Path barJavaFile;

  private List<SourceDirectory> sourceDirectories;

  @BeforeEach
  void setup(final @TempDir Path tmpDir) throws IOException {
    workingRepoDir = tmpDir;
    Path module1JavaDir =
        Files.createDirectories(tmpDir.resolve("module1/src/main/java/com/acme/"));
    Path module2JavaDir =
        Files.createDirectories(tmpDir.resolve("module2/src/main/java/com/acme/util/"));
    fooJavaFile = module1JavaDir.resolve("Foo.java");
    barJavaFile = module2JavaDir.resolve("Bar.java");
    Files.write(
        fooJavaFile,
        "import com.acme.util.Bar; class Foo {private var bar = new Bar();}".getBytes());
    Files.write(barJavaFile, "public class Bar {}".getBytes());
    sourceDirectories =
        List.of(
            SourceDirectory.createDefault(
                tmpDir.resolve("module1/src/main/java").toAbsolutePath().toString(),
                List.of(fooJavaFile.toAbsolutePath().toString())),
            SourceDirectory.createDefault(
                tmpDir.resolve("module2/src/main/java").toAbsolutePath().toString(),
                List.of(barJavaFile.toAbsolutePath().toString())));
  }

  @Test
  void file_finder_works() throws IOException {
    FileFinder finder = new CLI.DefaultFileFinder();

    IncludesExcludes all = IncludesExcludes.any();
    List<Path> files = finder.findFiles(sourceDirectories, all);
    assertThat(files).containsExactly(fooJavaFile, barJavaFile);

    IncludesExcludes onlyFoo =
        IncludesExcludes.withSettings(workingRepoDir.toFile(), List.of("**/Foo.java"), List.of());
    files = finder.findFiles(sourceDirectories, onlyFoo);
    assertThat(files).containsExactly(fooJavaFile);
  }

  @Test
  void javaparser_factory_works() throws IOException {
    JavaParserFactory factory = new CLI.DefaultJavaParserFactory();
    JavaParser javaParser = factory.create(sourceDirectories);
    Optional<CompilationUnit> result = javaParser.parse(fooJavaFile).getResult();
    assertThat(result.isPresent()).isTrue();
    CompilationUnit cu = result.get();
    assertThat(cu.getTypes()).hasSize(1);
    assertThat(cu.getTypes().get(0).getNameAsString()).isEqualTo("Foo");
  }
}
