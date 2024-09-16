package io.codemodder.plugins.llm;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

final class StandardModelTest {
  @Test
  void testId() {
    assertEquals("gpt-4-turbo-2024-04-09", StandardModel.GPT_4_TURBO_2024_04_09.id());
    assertEquals("gpt-4o-2024-05-13", StandardModel.GPT_4O_2024_05_13.id());
  }
}
