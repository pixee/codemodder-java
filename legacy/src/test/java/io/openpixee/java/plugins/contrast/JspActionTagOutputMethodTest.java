package io.openpixee.java.plugins.contrast;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

final class JspActionTagOutputMethodTest {

  @Test
  void it_detects_jsp_action_tags() {
    JspActionTagOutputMethod actionTag = new JspActionTagOutputMethod();
    assertThat(actionTag.countPossibleWrites("hi <c:out name=\"test\"/> \ntest"), equalTo(1));
    assertThat(
        actionTag.countPossibleWrites("hi <c:out name=\"test\"/> and <c:forEach>"), equalTo(2));
  }
}
