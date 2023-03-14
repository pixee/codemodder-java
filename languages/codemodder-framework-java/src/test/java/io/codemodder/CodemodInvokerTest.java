package io.codemodder;

import static io.codemodder.CodemodInvoker.isValidCodemodId;
import static org.hamcrest.MatcherAssert.assertThat;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;

final class CodemodInvokerTest {

  @Codemod(
      id = "test_mod",
      author = "valid@valid.com",
      reviewGuidance = ReviewGuidance.MERGE_AFTER_CURSORY_REVIEW)
  static class InvalidCodemodName implements Changer {}

  @Codemod(
      id = "test_mod",
      author = " ",
      reviewGuidance = ReviewGuidance.MERGE_AFTER_CURSORY_REVIEW)
  class EmptyCodemodAuthor implements Changer {}

  @Codemod(
      id = "pixee:java/id",
      author = "valid@valid.com",
      reviewGuidance = ReviewGuidance.MERGE_AFTER_CURSORY_REVIEW)
  final class ValidCodemod implements Changer {}

  @Test
  void it_validates_codemod_ids() {
    assertThat(isValidCodemodId("pixee:java/id"), CoreMatchers.is(true));
    assertThat(isValidCodemodId("pixee:java/id-with-slashes-numbers-34"), CoreMatchers.is(true));
    assertThat(isValidCodemodId("some-thing:java/id"), CoreMatchers.is(false));
    assertThat(isValidCodemodId("missing:token"), CoreMatchers.is(false));
    assertThat(isValidCodemodId("missing:separator/"), CoreMatchers.is(false));
  }
}
