package io.openpixee.java.protections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.acme.testcode.XStreamVulnerability;
import com.acme.testcode.XStreamVulnerabilityWeaved;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.ConversionException;
import java.io.IOException;
import org.junit.jupiter.api.Test;

final class XStreamTest {

  @Test
  void it_weaves_xstream_protection() throws IOException {
    WeavingTests.assertJavaWeaveWorkedAndWontReweave(
        "src/test/java/com/acme/testcode/XStreamVulnerability.java", new XStreamVisitorFactory());
  }

  @Test
  void it_protects_against_xstream_attack() {
    ProcessBuilder pb = new ProcessBuilder("open /Applications/Whatever");
    String exploit = new XStream().toXML(pb);

    XStreamVulnerability unpatched = new XStreamVulnerability();
    ProcessBuilder deserializedPb = (ProcessBuilder) unpatched.insecure(exploit);
    assertThat(deserializedPb, is(not(nullValue())));

    // although we usually throw SecurityException, XStream catches that and re-throws a
    // ConversionException
    XStreamVulnerabilityWeaved patched = new XStreamVulnerabilityWeaved();
    assertThrows(ConversionException.class, () -> patched.insecure(exploit));
  }
}
