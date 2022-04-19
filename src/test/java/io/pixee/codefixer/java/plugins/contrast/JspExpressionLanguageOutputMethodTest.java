package io.pixee.codefixer.java.plugins.contrast;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import io.pixee.codefixer.java.JspLineWeave;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

final class JspExpressionLanguageOutputMethodTest {

  @Test
  void it_counts_tokens() {
    JspExpressionLanguageOutputMethod output =
        new JspExpressionLanguageOutputMethod(Collections.emptyList());
    assertThat(output.countPossibleWrites("${foo}"), equalTo(1));
    assertThat(output.countPossibleWrites("${foo}${bar}"), equalTo(2));
    assertThat(output.countPossibleWrites("a ${foo} b ${bar} c ${doSomething()}"), equalTo(3));
  }

  @Test
  void it_rewrites_with_existing_prefix() {
    JspExpressionLanguageOutputMethod output =
        new JspExpressionLanguageOutputMethod(
            List.of(
                "<%@ taglib uri = \"http://java.sun.com/jsp/jstl/functions\" prefix = \"functionz\" %>",
                "Some ${rewrite} here"));
    JspLineWeave weave = output.weaveLine("Some ${rewrite} here", "myRuleId");
    assertThat(weave.getRebuiltLine(), equalTo("Some ${functionz:escapeXml(rewrite)} here"));
    assertThat(weave.getRuleId(), equalTo("myRuleId"));
  }

  @Test
  void it_rewrites_with_no_prefix() {
    JspExpressionLanguageOutputMethod output =
        new JspExpressionLanguageOutputMethod(List.of("Some ${rewrite} here"));
    JspLineWeave weave = output.weaveLine("Some ${rewrite} here", "myRuleId");
    assertThat(weave.getRebuiltLine(), equalTo("Some ${fn:escapeXml(rewrite)} here"));
    assertThat(weave.getRuleId(), equalTo("myRuleId"));
    assertThat(weave.getSupportingTaglib().isPresent(), is(true));
    assertThat(weave.getSupportingTaglib().get(), equalTo(DEFAULT_TAG_LIB));
  }

  private static final String DEFAULT_TAG_LIB =
      "<%@ taglib uri = \"http://java.sun.com/jsp/jstl/functions\" prefix = \"fn\" %>";
}
