package io.codemodder.codemods;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

public class ConstantNameGeneratorTest {

    @Test
    public void it_tests_basic_alpha_string() {
        // Given
        String stringLiteralExprValue = "testValue";
        Set<String> declaredVariables = new HashSet<>();
        declaredVariables.add("EXISTING");

        // When
        String constantName = DefineConstantForLiteralCodemod.ConstantNameGenerator.buildConstantName(stringLiteralExprValue, declaredVariables);

        // Then
        assertThat(constantName).isEqualTo("TESTVALUE"); // Expected constant name in upper case
    }

    @Test
    public void it_handles_duplicated_value() {
        // Given
        String stringLiteralExprValue = "testValue";
        Set<String> declaredVariables = new HashSet<>();
        declaredVariables.add("TESTVALUE");
        declaredVariables.add("TESTVALUE_1");

        // When
        String constantName = DefineConstantForLiteralCodemod.ConstantNameGenerator.buildConstantName(stringLiteralExprValue, declaredVariables);

        // Then
        assertThat(constantName).isEqualTo("TESTVALUE_2"); // Expected constant name in upper case
    }

    @Test
    public void testBuildConstantNameWithEmptyDeclaredVariables() {
        // Given
        String stringLiteralExprValue = "testValue";
        Set<String> emptyDeclaredVariables = new HashSet<>();

        // When generating a constant name with an empty declared variables set
        String constantName = DefineConstantForLiteralCodemod.ConstantNameGenerator.buildConstantName(stringLiteralExprValue, emptyDeclaredVariables);

        // Then, the generated constant name should match the input string in upper case
        assertThat(constantName).isEqualTo("TESTVALUE");
    }

    @Test
    public void testBuildConstantNameWithNullDeclaredVariables() {
        // Given
        String stringLiteralExprValue = "testValue";

        // When generating a constant name with null declared variables
        String constantName = DefineConstantForLiteralCodemod.ConstantNameGenerator.buildConstantName(stringLiteralExprValue, null);

        // Then, the generated constant name should match the input string in upper case
        assertThat(constantName).isEqualTo("TESTVALUE");
    }
}

