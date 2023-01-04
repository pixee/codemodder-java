package io.openpixee.java;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;
import org.junit.jupiter.api.Test;

class PathMatcherTest {

  @Test
  public void it_wont_match_partial_names() {
    var pm = IncludesExcludes.parsePattern(new File("."), "src/main/java");
    assertThat(pm.matches(new File("src/main/javascript")), is(false));
  }
}
