package io.codemodder.codemods;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class ConstantNameGeneratorTest {

  @Test
  public void it_tests_basic_alpha_string() {
    String stringLiteralExprValue = "testValue";
    Set<String> declaredVariables = new HashSet<>();
    declaredVariables.add("EXISTING");

    String constantName =
        DefineConstantForLiteralCodemod.ConstantNameGenerator.generateConstantName(
            stringLiteralExprValue, declaredVariables, null);

    assertThat(constantName).isEqualTo("TESTVALUE");
  }

  @Test
  public void it_handles_duplicated_value() {
    String stringLiteralExprValue = "testValue";
    Set<String> declaredVariables = new HashSet<>();
    declaredVariables.add("TESTVALUE");
    declaredVariables.add("TESTVALUE_1");

    String constantName =
        DefineConstantForLiteralCodemod.ConstantNameGenerator.generateConstantName(
            stringLiteralExprValue, declaredVariables, null);

    assertThat(constantName).isEqualTo("TESTVALUE_2");
  }

  @Test
  public void it_handles_empty_declared_variables() {
    String stringLiteralExprValue = "testValue";

    String constantName =
        DefineConstantForLiteralCodemod.ConstantNameGenerator.generateConstantName(
            stringLiteralExprValue, new HashSet<>(), null);

    assertThat(constantName).isEqualTo("TESTVALUE");
  }

  @Test
  public void it_handles_null_declared_variables() {
    String stringLiteralExprValue = "testValue";

    String constantName =
        DefineConstantForLiteralCodemod.ConstantNameGenerator.generateConstantName(
            stringLiteralExprValue, null, null);

    assertThat(constantName).isEqualTo("TESTVALUE");
  }

  @Test
  public void it_handles_any_chars() {
    String stringLiteralExprValue = "(**my 2nd test value**)";

    String constantName =
        DefineConstantForLiteralCodemod.ConstantNameGenerator.generateConstantName(
            stringLiteralExprValue, null, null);

    assertThat(constantName).isEqualTo("MY_2ND_TEST_VALUE");
  }

  @Test
  public void it_handles_first_non_alpha_char() {
    String stringLiteralExprValue = "2 dogs";

    String constantName =
        DefineConstantForLiteralCodemod.ConstantNameGenerator.generateConstantName(
            stringLiteralExprValue, null, null);

    assertThat(constantName).isEqualTo("DOGS");
  }

  @Test
  public void it_handles_first_non_alpha_char_1() {
    String stringLiteralExprValue = "31 de octubre";

    String constantName =
        DefineConstantForLiteralCodemod.ConstantNameGenerator.generateConstantName(
            stringLiteralExprValue, null, null);

    assertThat(constantName).isEqualTo("DE_OCTUBRE");
  }

  @Test
  public void it_handles_first_non_alpha_string() {
    String stringLiteralExprValue = "2/12/22";

    String constantName =
        DefineConstantForLiteralCodemod.ConstantNameGenerator.generateConstantName(
            stringLiteralExprValue, null, null);

    assertThat(constantName).isEqualTo("2_12_22");
  }

  @Test
  public void it_handles_first_non_alphanumeric_string() {
    String stringLiteralExprValue = "(@)";

    String constantName =
        DefineConstantForLiteralCodemod.ConstantNameGenerator.generateConstantName(
            stringLiteralExprValue, null, null);

    assertThat(constantName).isEqualTo("");
  }
}
