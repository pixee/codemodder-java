package io.openpixee.java.protections;

import static io.openpixee.java.protections.WeavingTests.assertJavaWeaveWorkedAndWontReweave;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

final class SSLProtocolTest {

  @Test
  void it_prevents_sslcontext_getinstance_protocol_weakness() throws IOException {
    WeavingTests.assertJavaWeaveWorkedAndWontReweave(
        "src/test/java/com/acme/testcode/SSLContextGetInstanceVulnerability.java",
        new SSLContextGetInstanceVisitorFactory());
  }

  @Test
  void it_prevents_sslengine_setenabledprotocols_weakness() throws IOException {
    WeavingTests.assertJavaWeaveWorkedAndWontReweave(
        "src/test/java/com/acme/testcode/SSLEngineSetEnabledProtocolsVulnerability.java",
        new SSLEngineSetEnabledProtocolsVisitorFactory());
  }

  @Test
  void it_prevents_sslparameters_setprotocols_weakness() throws IOException {
    WeavingTests.assertJavaWeaveWorkedAndWontReweave(
        "src/test/java/com/acme/testcode/SSLParametersSetProtocolsVulnerability.java",
        new SSLParametersSetProtocolsVisitorFactory());
  }

  @Test
  void it_prevents_sslsocket_setenabledprotocols_weakness() throws IOException {
    WeavingTests.assertJavaWeaveWorkedAndWontReweave(
        "src/test/java/com/acme/testcode/SSLSocketSetEnabledProtocolsVulnerability.java",
        new SSLSocketSetEnabledProtocolsVisitorFactory());
  }

  @Test
  void checkstyle_config_only_ignores_weaved_files() {
    Pattern p = Pattern.compile("^((?!Weaved).)*\\.java");
    assertThat(p.matcher("Foo.java").matches(), is(true));
    assertThat(p.matcher("FooWeaved.java").matches(), is(false));
  }
}
