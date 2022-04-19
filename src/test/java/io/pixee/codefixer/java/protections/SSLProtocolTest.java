package io.pixee.codefixer.java.protections;

import static io.pixee.codefixer.java.protections.WeavingTests.assertJavaWeaveWorkedAndWontReweave;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

final class SSLProtocolTest {

  @Test
  void it_prevents_bad_protocol() throws IOException {
    assertJavaWeaveWorkedAndWontReweave(
        "src/test/java/com/acme/testcode/SSLProtocolVulnerability.java",
        new SSLProtocolVisitorFactory());
  }

  @Test
  void checkstyle_config_only_ignores_weaved_files() {
    Pattern p = Pattern.compile("^((?!Weaved).)*\\.java");
    assertThat(p.matcher("Foo.java").matches(), is(true));
    assertThat(p.matcher("FooWeaved.java").matches(), is(false));
  }
}
