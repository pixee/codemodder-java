package io.openpixee.java;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;
import org.junit.jupiter.api.Test;

final class JavaFixitCliTest {

  @Test
  void defaults_includes_excludes_are_good() {
    IncludesExcludes includesExcludes =
        IncludesExcludes.fromConfiguration(
            new File("."), JavaFixitCli.defaultIncludes, JavaFixitCli.defaultExcludes);
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
