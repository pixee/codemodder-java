package io.openpixee.java;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

final class IncludesExcludesTest {

  @Test
  void it_handles_globs_and_negates() {
    IncludesExcludes includesExcludes =
        IncludesExcludes.withSettings(
            new File("."), List.of("*.java", "**/*.java"), List.of("**/test/**/*.java"));

    assertThat(includesExcludes.shouldInspect(new File("src/main/java/Foo.java")), is(true));
    assertThat(includesExcludes.shouldInspect(new File("src/test/java/Foo.java")), is(false));
  }

  @Test
  void it_handles_lines() {
    IncludesExcludes includesExcludes =
        IncludesExcludes.withSettings(
            new File("."), List.of("src/main/java/Foo.java:16"), Collections.emptyList());
    var file = new File("src/main/java/Foo.java");
    assertThat(includesExcludes.shouldInspect(file), is(true));
    LineIncludesExcludes fileIncludesExcludes = includesExcludes.getIncludesExcludesForFile(file);
    assertThat(fileIncludesExcludes.matches(1), is(false));
    assertThat(fileIncludesExcludes.matches(15), is(false));
    assertThat(fileIncludesExcludes.matches(16), is(true));
    assertThat(fileIncludesExcludes.matches(17), is(false));
    assertThat(fileIncludesExcludes.matches(Integer.MAX_VALUE), is(false));
  }

  @Test
  void it_handles_conflicting_include_and_excludes() {
    IncludesExcludes includesExcludes =
        IncludesExcludes.withSettings(
            new File("."), List.of("*.java", "**/*.java"), List.of("src/main/java/Foo.java"));
    assertThat(includesExcludes.shouldInspect(new File("src/main/java/Foo.java")), is(false));
  }

  @Test
  void it_handles_conflicting_include_and_excludes_line() {
    IncludesExcludes includesExcludes =
        IncludesExcludes.withSettings(
            new File("."), List.of("*.java", "**/*.java"), List.of("src/main/java/Foo.java:16"));
    File file = new File("src/main/java/Foo.java");
    assertThat(includesExcludes.shouldInspect(file), is(true));
    LineIncludesExcludes fileIncludesExcludes = includesExcludes.getIncludesExcludesForFile(file);
    assertThat(fileIncludesExcludes.matches(15), is(true));
    assertThat(fileIncludesExcludes.matches(16), is(false));
    assertThat(fileIncludesExcludes.matches(17), is(true));
  }

  @Test
  void it_throws_on_null_includes_or_excludes() {
    assertThrows(
        NullPointerException.class,
        () -> {
          IncludesExcludes.withSettings(
              new File("."), null, List.of("src/main/java/com/acme/Foo.java:17"));
        });

    assertThrows(
        NullPointerException.class,
        () -> {
          IncludesExcludes.withSettings(
              new File("."), List.of("src/main/java/com/acme/Foo.java:17"), null);
        });
  }

  @Test
  void it_throws_on_illegal_combination() {
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          IncludesExcludes includesExcludes =
              IncludesExcludes.withSettings(
                  new File("."),
                  List.of("src/main/java/com/acme/Foo.java:16"),
                  List.of("src/main/java/com/acme/Foo.java:17"));
          includesExcludes.getIncludesExcludesForFile(new File("src/main/java/com/acme/Foo.java"));
        });
  }
}
