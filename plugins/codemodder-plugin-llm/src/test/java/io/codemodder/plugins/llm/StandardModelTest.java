package io.codemodder.plugins.llm;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

final class StandardModelTest {
  @Test
  void testId() {
    assertEquals("gpt-3.5-turbo-0125", StandardModel.GPT_3_5_TURBO_0125.id());
    assertEquals("gpt-4-0613", StandardModel.GPT_4_0613.id());
    assertEquals("gpt-4o-2024-05-13", StandardModel.GPT_4O_2024_05_13.id());
  }
}
