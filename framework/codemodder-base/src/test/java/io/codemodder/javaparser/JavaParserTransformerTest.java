package io.codemodder.javaparser;

import static io.codemodder.ast.ASTTransforms.removeImportIfUnused;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Test aspects of {@link io.codemodder.javaparser.JavaParserTransformer}. */
final class JavaParserTransformerTest {

  private static Stream<Arguments> removeImportCases() {
    return Stream.of(
        Arguments.of(
            """
                package com.acme;

                import java.io.File;

                public class Foo {
                    public void bar() {
                        String a = File.separator;
                    }
                }
                """),
        Arguments.of(
            """
                package com.acme;

                import java.io.File;

                public class Foo {
                    public void bar() {
                        var a = new File("foo");
                    }
                }
                """),
        Arguments.of(
            """
                package com.acme;

                import java.io.File;

                public class Foo {
                    public void bar() {
                        File a = createFile("foo");
                    }
                }
                """),
        Arguments.of(
            """
                package com.acme;

                import java.io.File;

                public class Foo {
                    public File bar() {
                        return null;
                    }
                }
                """),
        Arguments.of(
            """
                package com.acme;

                import java.io.File;

                public class Foo {
                    public String bar() {
                        return File.class.getName();
                    }
                }
                """),
        Arguments.of(
            """
            package com.acme;

            import java.io.File;

            public class Foo<File> {
                public String bar() {
                    return "bar";
                }
            }
            """),
        Arguments.of(
            """
            package com.acme;

            import java.io.File;

            public class Foo extends File {
                public String bar() {
                    return "bar";
                }
            }
            """));
  }

  @ParameterizedTest
  @MethodSource("removeImportCases")
  void it_doesnt_remove_used_imports(final String code) {
    CompilationUnit cu = StaticJavaParser.parse(code);
    LexicalPreservingPrinter.setup(cu);
    removeImportIfUnused(cu, "java.io.File");
    assertThat(LexicalPreservingPrinter.print(cu)).isEqualTo(code);
  }

  @Test
  void it_removes_when_possible() {
    String code =
        """
                package com.acme;

                import java.io.File;

                public class Foo {
                    public String bar() {
                        return "bar";
                    }
                }
                """;

    String afterCode =
        """
                package com.acme;

                public class Foo {
                    public String bar() {
                        return "bar";
                    }
                }
                """;
    CompilationUnit cu = StaticJavaParser.parse(code);
    LexicalPreservingPrinter.setup(cu);
    removeImportIfUnused(cu, "java.io.File");
    assertThat(LexicalPreservingPrinter.print(cu)).isEqualTo(afterCode);
  }
}
