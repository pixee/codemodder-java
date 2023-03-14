package io.openpixee.java.plugins.contrast;

import static io.openpixee.java.Results.buildSimpleResult;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import com.contrastsecurity.sarif.Result;
import io.codemodder.ChangedFile;
import io.codemodder.DependencyGAV;
import io.codemodder.Weave;
import io.openpixee.java.protections.WeavingTests;
import java.io.IOException;
import java.util.List;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;

final class SarifBasedJspScriptletXSSWeaverTest {

  @Test
  void it_weaves_supported_lines_and_avoids_unsupported_lines() throws IOException {
    List<Result> results = buildBigTestResults();
    SarifBasedJspScriptletXSSVisitor weaver =
        new SarifBasedJspScriptletXSSVisitor(results, "reflected-xss");
    ChangedFile changedFile =
        WeavingTests.assertFileWeaveWorkedAndReweave("src/test/resources/jsps/sarif.jsp", weaver);
    List<Weave> weaves = changedFile.weaves();
    assertThat(
        weaves,
        CoreMatchers.hasItems(
            Weave.from(4, "contrast-scan:java/reflected-xss", DependencyGAV.OWASP_XSS_JAVA_ENCODER),
            Weave.from(
                5, "contrast-scan:java/reflected-xss", DependencyGAV.OWASP_XSS_JAVA_ENCODER)));

    // make sure we don't import the Java XSS dependency if it's an expression, because for those we
    // use built-in JSP functions
    Weave jspExpressionWeave =
        weaves.stream().filter(weave -> weave.lineNumber() == 12).findFirst().get();
    List<DependencyGAV> jspExpressionWeaveDependencies = jspExpressionWeave.getDependenciesNeeded();
    assertThat(jspExpressionWeaveDependencies.isEmpty(), is(true));
  }

  private List<Result> buildBigTestResults() {
    return List.of(
        // these are the ones we can fix
        buildSimpleResult("/sarif.jsp", 4, "ignored", "reflected-xss"),
        buildSimpleResult("/sarif.jsp", 5, "ignored", "reflected-xss"),
        buildSimpleResult("/sarif.jsp", 6, "ignored", "reflected-xss"),
        buildSimpleResult("/sarif.jsp", 7, "ignored", "reflected-xss"),
        buildSimpleResult("/sarif.jsp", 8, "ignored", "reflected-xss"),
        buildSimpleResult("/sarif.jsp", 9, "ignored", "reflected-xss"),
        buildSimpleResult("/sarif.jsp", 10, "ignored", "reflected-xss"),
        buildSimpleResult("/sarif.jsp", 11, "ignored", "reflected-xss"),
        buildSimpleResult("/sarif.jsp", 12, "ignored", "reflected-xss"),
        buildSimpleResult("/sarif.jsp", 13, "ignored", "reflected-xss"),
        buildSimpleResult("/sarif.jsp", 14, "ignored", "reflected-xss"),

        // the ones we can't
        buildSimpleResult("/sarif.jsp", 17, "ignored", "reflected-xss"),
        buildSimpleResult("/sarif.jsp", 18, "ignored", "reflected-xss"),
        buildSimpleResult("/sarif.jsp", 19, "ignored", "reflected-xss"),
        buildSimpleResult("/sarif.jsp", 19, "ignored", "reflected-xss"),
        buildSimpleResult("/sarif.jsp", 20, "ignored", "reflected-xss"),
        buildSimpleResult("/sarif.jsp", 20, "ignored", "reflected-xss"),
        buildSimpleResult("/sarif.jsp", 21, "ignored", "reflected-xss"),
        buildSimpleResult("/sarif.jsp", 21, "ignored", "reflected-xss"),
        buildSimpleResult("/sarif.jsp", 22, "ignored", "reflected-xss"),
        buildSimpleResult("/sarif.jsp", 22, "ignored", "reflected-xss"),
        buildSimpleResult("/sarif.jsp", 23, "ignored", "reflected-xss"),
        buildSimpleResult("/sarif.jsp", 23, "ignored", "reflected-xss"),
        buildSimpleResult("/sarif.jsp", 24, "ignored", "reflected-xss"),
        buildSimpleResult("/sarif.jsp", 25, "ignored", "reflected-xss"));
  }

  @Test
  void it_adds_header_for_el_when_no_functions_defined() throws IOException {
    List<Result> results =
        List.of(buildSimpleResult("/el_but_no_taglib.jsp", 3, "ignored", "stored-xss"));
    SarifBasedJspScriptletXSSVisitor weaver =
        new SarifBasedJspScriptletXSSVisitor(results, "stored-xss");
    ChangedFile changedFile =
        WeavingTests.assertFileWeaveWorkedAndReweave(
            "src/test/resources/jsps/el_but_no_taglib.jsp", weaver);
    List<Weave> weaves = changedFile.weaves();
    assertThat(weaves, CoreMatchers.hasItems(Weave.from(3, "contrast-scan:java/stored-xss")));
  }
}
