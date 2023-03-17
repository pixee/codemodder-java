package io.codemodder;

import static io.codemodder.CodemodInvoker.isValidCodemodId;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.util.List;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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
  static class EmptyCodemodAuthor implements Changer {}

  @Codemod(
      id = "pixee:java/id",
      author = "valid@valid.com",
      reviewGuidance = ReviewGuidance.MERGE_AFTER_CURSORY_REVIEW)
  static class ValidCodemod implements Changer {}

  @Test
  void it_validates_codemod_ids() {
    assertThat(isValidCodemodId("pixee:java/id"), CoreMatchers.is(true));
    assertThat(isValidCodemodId("pixee:java/id-with-slashes-numbers-34"), CoreMatchers.is(true));
    assertThat(isValidCodemodId("some-thing:java/id"), CoreMatchers.is(false));
    assertThat(isValidCodemodId("missing:token"), CoreMatchers.is(false));
    assertThat(isValidCodemodId("missing:separator/"), CoreMatchers.is(false));
  }

  @Test
  void it_blows_up_on_duplicate_codemod_ids(@TempDir Path tmpDir) {
    assertThrows(
        UnsupportedOperationException.class,
        () -> {
          new CodemodInvoker(List.of(ValidCodemod.class, ValidCodemod.class), tmpDir);
        });
  }
}
