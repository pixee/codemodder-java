package io.openpixee.codetl.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import io.openpixee.java.IncludesExcludes;
import java.io.File;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link Application}. */
final class ApplicationTest {

  @Test
  void default_includes_excludes_match_expected_files() {
    IncludesExcludes includesExcludes =
        IncludesExcludes.fromConfiguration(
            new File("."), Application.defaultIncludes, Application.defaultExcludes);
    assertThat(includesExcludes.shouldInspect(file("src/main/java/Foo.java")), is(true));
    assertThat(
        includesExcludes.shouldInspect(file("src/main/resources/WEB-INF/web.xml")), is(true));
    assertThat(includesExcludes.shouldInspect(file("src/test/java/Foo.java")), is(false));
    assertThat(includesExcludes.shouldInspect(file("pom.xml")), is(true));
    assertThat(includesExcludes.shouldInspect(file("foo.jsp")), is(true));

    assertThat(includesExcludes.shouldInspect(file("module1/src/main/java/Foo.java")), is(true));
    assertThat(
        includesExcludes.shouldInspect(file("module1/src/main/resources/WEB-INF/web.xml")),
        is(true));
    assertThat(includesExcludes.shouldInspect(file("module1/src/test/java/Foo.java")), is(false));
    assertThat(includesExcludes.shouldInspect(file("module1/pom.xml")), is(true));
    assertThat(includesExcludes.shouldInspect(file("module1/foo.jsp")), is(true));
  }

  private File file(String s) {
    return new File(s);
  }
}
