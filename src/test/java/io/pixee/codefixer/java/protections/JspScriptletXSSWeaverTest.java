package io.pixee.codefixer.java.protections;

import static io.pixee.codefixer.java.protections.JspScriptletXSSVisitor.xssJspScriptletRuleId;

import io.pixee.codefixer.java.Weave;
import java.io.IOException;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

final class JspScriptletXSSWeaverTest {

  @Test
  void it_collects_vulnerable_scriptlets() throws IOException {
    var changedFile =
        WeavingTests.assertFileWeaveWorkedAndReweave(
            "src/test/resources/jsps/has_xss.jsp", new JspScriptletXSSVisitor());
    var weaves = changedFile.weaves();
    MatcherAssert.assertThat(
        weaves,
        CoreMatchers.hasItems(
            Weave.from(3, xssJspScriptletRuleId), Weave.from(5, xssJspScriptletRuleId)));
  }
}
