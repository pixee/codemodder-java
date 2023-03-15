package io.openpixee.java;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import io.codemodder.IncludesExcludes;
import java.io.File;
import org.junit.jupiter.api.Test;

final class PathMatcherTest {

  @Test
  void it_wont_match_partial_names() {
    var pm = IncludesExcludes.parsePattern(new File("."), "src/main/java");
    assertThat(pm.matches(new File("src/main/javascript")), is(false));
  }

  /**
   * We only do light testing of this because the tests really are testing the ability of our code
   * to hand off to the dependency.
   */
  @Test
  void it_matches_globs_correctly() {
    File repoRoot = new File(".");
    var allJavaPattern = IncludesExcludes.parsePattern(repoRoot, "{*.java,**/*.java}");
    assertThat(allJavaPattern.matches(new File(repoRoot, "Foo")), is(false));
    assertThat(allJavaPattern.matches(new File(repoRoot, "Foo/")), is(false));
    assertThat(allJavaPattern.matches(new File(repoRoot, "Foo.xml")), is(false));
    assertThat(allJavaPattern.matches(new File(repoRoot, "Foo.java")), is(true));
    assertThat(allJavaPattern.matches(new File(repoRoot, "/Foo.java")), is(true));
    assertThat(allJavaPattern.matches(new File(repoRoot, "foo/Foo.java")), is(true));
    assertThat(allJavaPattern.matches(new File(repoRoot, "foo/bar123/Foo.java")), is(true));

    var onlySrcMainJavaPattern = IncludesExcludes.parsePattern(repoRoot, "src/main/java/**.java");
    assertThat(onlySrcMainJavaPattern.matches(new File(repoRoot, "foo/Foo.java")), is(false));
    assertThat(
        onlySrcMainJavaPattern.matches(new File(repoRoot, "src/main/java/Foo.java")), is(true));
    assertThat(
        onlySrcMainJavaPattern.matches(new File(repoRoot, "/src/main/java/Foo.java")), is(true));

    var onlySrcMainJavaWithSlashPattern =
        IncludesExcludes.parsePattern(repoRoot, "/src/main/java/**.java");
    assertThat(
        onlySrcMainJavaWithSlashPattern.matches(new File(repoRoot, "foo/Foo.java")), is(false));
    assertThat(
        onlySrcMainJavaWithSlashPattern.matches(new File(repoRoot, "src/main/java/Foo.java")),
        is(true));
    assertThat(
        onlySrcMainJavaWithSlashPattern.matches(new File(repoRoot, "/src/main/java/Foo.java")),
        is(true));
  }
}
