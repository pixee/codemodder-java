package io.codemodder.codemods;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class ConstantNameStringGeneratorTest {

  @Test
  void it_tests_basic_alpha_string() {
    String stringLiteralExprValue = "testValue";
    Set<String> declaredVariables = new HashSet<>();
    declaredVariables.add("EXISTING");

    String constantName =
        ConstantNameStringGenerator.generateConstantName(
            stringLiteralExprValue, declaredVariables, null, true);

    assertThat(constantName).isEqualTo("TESTVALUE");
  }

  @Test
  void it_handles_duplicated_value() {
    String stringLiteralExprValue = "testValue";
    Set<String> declaredVariables = new HashSet<>();
    declaredVariables.add("TESTVALUE");
    declaredVariables.add("TESTVALUE_1");

    String constantName =
        ConstantNameStringGenerator.generateConstantName(
            stringLiteralExprValue, declaredVariables, null, true);

    assertThat(constantName).isEqualTo("TESTVALUE_2");
  }

  @Test
  void it_handles_duplicated_value_camel_case() {
    String stringLiteralExprValue = "testValue";
    Set<String> declaredVariables = new HashSet<>();
    declaredVariables.add("TESTVALUE");
    declaredVariables.add("TESTVALUE_1");

    String constantName =
        ConstantNameStringGenerator.generateConstantName(
            stringLiteralExprValue, declaredVariables, null, false);

    assertThat(constantName).isEqualTo("testvalue");
  }

  @Test
  void it_handles_empty_declared_variables() {
    String stringLiteralExprValue = "testValue";

    String constantName =
        ConstantNameStringGenerator.generateConstantName(
            stringLiteralExprValue, new HashSet<>(), null, true);

    assertThat(constantName).isEqualTo("TESTVALUE");
  }

  @Test
  void it_handles_null_declared_variables() {
    String stringLiteralExprValue = "testValue";

    String constantName =
        ConstantNameStringGenerator.generateConstantName(stringLiteralExprValue, null, null, true);

    assertThat(constantName).isEqualTo("TESTVALUE");
  }

  @Test
  void it_handles_any_chars() {
    String stringLiteralExprValue = "(**my 2nd test value**)";

    String constantName =
        ConstantNameStringGenerator.generateConstantName(stringLiteralExprValue, null, null, true);

    assertThat(constantName).isEqualTo("MY_2ND_TEST_VALUE");
  }

  @Test
  void it_handles_any_chars_camelCase() {
    String stringLiteralExprValue = "(**my 2nd test value**)";

    String constantName =
        ConstantNameStringGenerator.generateConstantName(stringLiteralExprValue, null, null, false);

    assertThat(constantName).isEqualTo("my2ndTestValue");
  }

  @Test
  void it_handles_first_non_alpha_char() {
    String stringLiteralExprValue = "2 dogs";

    String constantName =
        ConstantNameStringGenerator.generateConstantName(stringLiteralExprValue, null, null, true);

    assertThat(constantName).isEqualTo("DOGS");
  }

  @Test
  void it_handles_first_non_alpha_char_camel_case() {
    String stringLiteralExprValue = "2 dogs";

    String constantName =
        ConstantNameStringGenerator.generateConstantName(stringLiteralExprValue, null, null, false);

    assertThat(constantName).isEqualTo("dogs");
  }

  @Test
  void it_handles_first_non_alpha_char_1() {
    String stringLiteralExprValue = "31 de octubre";

    String constantName =
        ConstantNameStringGenerator.generateConstantName(stringLiteralExprValue, null, null, true);

    assertThat(constantName).isEqualTo("DE_OCTUBRE");
  }

  @Test
  void it_handles_first_non_alpha_char_1_camel_case() {
    String stringLiteralExprValue = "31 de octubre";

    String constantName =
        ConstantNameStringGenerator.generateConstantName(stringLiteralExprValue, null, null, false);

    assertThat(constantName).isEqualTo("deOctubre");
  }

  @Test
  void it_handles_first_non_alpha_string_no_parent_node_name_provided() {
    String stringLiteralExprValue = "2/12/22";

    String constantName =
        ConstantNameStringGenerator.generateConstantName(stringLiteralExprValue, null, null, true);

    assertThat(constantName).isEqualTo("CONST");
  }

  @Test
  void it_handles_first_non_alphanumeric_string_no_parent_node_name_provided() {
    String stringLiteralExprValue = "(@)";

    String constantName =
        ConstantNameStringGenerator.generateConstantName(stringLiteralExprValue, null, null, true);

    assertThat(constantName).isEqualTo("CONST");
  }

  @Test
  void it_handles_first_non_alpha_string_with_parent_node_name_provided() {
    String stringLiteralExprValue = "2/12/22";

    String constantName =
        ConstantNameStringGenerator.generateConstantName(
            stringLiteralExprValue, null, "displayDate", true);

    assertThat(constantName).isEqualTo("DISPLAYDATE");
  }

  @Test
  void it_handles_first_non_alphanumeric_string_with_parent_node_name_provided() {
    String stringLiteralExprValue = "***";

    String constantName =
        ConstantNameStringGenerator.generateConstantName(
            stringLiteralExprValue, null, "setWildcard", true);

    assertThat(constantName).isEqualTo("SETWILDCARD");
  }
}
