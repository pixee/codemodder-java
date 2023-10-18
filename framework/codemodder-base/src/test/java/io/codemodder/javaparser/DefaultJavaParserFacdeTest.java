package io.codemodder.javaparser;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javaparser.ast.CompilationUnit;
import io.codemodder.SourceDirectory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class DefaultJavaParserFacdeTest {

  private DefaultJavaParserFacde cache;
  private Path javaFile;
  private String javaCode;

  @BeforeEach
  void setup(@TempDir Path tmpDir) throws IOException {
    this.javaFile = tmpDir.resolve("Foo.java").toAbsolutePath();
    this.javaCode =
        """
                package com.acme.util;
                public abstract class Foo {
                    public String a, b, c;
                }
                """;
    Files.writeString(javaFile, javaCode);
    var srcDirs = List.of(SourceDirectory.createDefault(tmpDir, List.of(javaFile.toString())));
    this.cache = new DefaultJavaParserFacde(JavaParserFactory.newFactory().create(srcDirs));
  }

  @Test
  void it_caches() {
    CompilationUnit cu = cache.parseJavaFile(javaFile, javaCode);
    assertThat(cu).isNotNull();
    assertThat(cu.getPackageDeclaration().orElseThrow().getNameAsString())
        .isEqualTo("com.acme.util");

    CompilationUnit cachedCu = cache.parseJavaFile(javaFile, "invalid java here");
    assertThat(cachedCu).isSameAs(cu);
    assertThat(cachedCu.getPackageDeclaration().orElseThrow().getNameAsString())
        .isEqualTo("com.acme.util");
  }
}
