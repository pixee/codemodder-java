package io.codemodder.javaparser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.javaparser.ast.CompilationUnit;
import io.codemodder.SourceDirectory;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class DefaultJavaParserFacadeTest {

  private DefaultJavaParserFacade parser;
  private Path javaFile;

  @BeforeEach
  void setup(@TempDir Path tmpDir) throws IOException {
    this.javaFile = tmpDir.resolve("Foo.java").toAbsolutePath();
    String javaCode =
        """
                package com.acme.util;
                public abstract class Foo {
                    public String a, b, c;
                }
                """;
    Files.writeString(javaFile, javaCode);
    var srcDirs = List.of(SourceDirectory.createDefault(tmpDir, List.of(javaFile.toString())));
    this.parser =
        new DefaultJavaParserFacade(
            () -> {
              try {
                return JavaParserFactory.newFactory().create(srcDirs);
              } catch (IOException e) {
                throw new UncheckedIOException(e);
              }
            });
  }

  @Test
  void it_works_on_good_code() throws IOException {
    CompilationUnit cu = parser.parseJavaFile(javaFile);
    assertThat(cu).isNotNull();
    assertThat(cu.getPackageDeclaration().orElseThrow().getNameAsString())
        .isEqualTo("com.acme.util");
  }

  @Test
  void it_fails_loudly_if_cant_parse() throws IOException {
    Files.writeString(javaFile, "bad code");
    assertThrows(RuntimeException.class, () -> parser.parseJavaFile(javaFile));
  }

  @Test
  void it_fails_loudly_if_bad_file() throws IOException {
    Files.delete(javaFile);
    assertThrows(NoSuchFileException.class, () -> parser.parseJavaFile(javaFile));
  }
}
