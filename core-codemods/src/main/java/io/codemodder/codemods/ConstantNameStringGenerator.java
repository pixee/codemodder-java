package io.codemodder.codemods;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** This class generates constant names based on given values and parent node name. */
final class ConstantNameStringGenerator {

  private ConstantNameStringGenerator() {}

  /**
   * Generates a unique constant name based on the provided string literal expression value,
   * declared variables, and the name of the parent node (if available). If there's a collision, a
   * suffix counter is added.
   */
  static String generateConstantName(
      final String stringLiteralExprValue,
      final Set<String> declaredVariables,
      final String parentNodeName,
      final boolean isSnakeCase) {
    final String snakeCaseConstantName =
        formatStringValueToConstantSnakeCaseNomenclature(stringLiteralExprValue, parentNodeName);

    final String constantName =
        isSnakeCase ? snakeCaseConstantName : convertSnakeCaseToCamelCase(snakeCaseConstantName);

    StringBuilder constantNameBuilder = new StringBuilder(constantName);
    int counter = 0;

    while (existsVariable(constantNameBuilder.toString(), declaredVariables)) {
      // If the constant name already exists, append a counter to make it unique
      constantNameBuilder = new StringBuilder(constantName);
      if (counter != 0) {
        if (isSnakeCase) {
          constantNameBuilder.append("_");
        }
        constantNameBuilder.append(counter);
      }
      counter++;
    }

    return constantNameBuilder.toString();
  }

  /**
   * This method takes a snake-cased string as input and transforms it into a camel-cased string. It
   * splits the input string by underscores, capitalizes the first letter of each subsequent word,
   * and concatenates the words to form the camel-cased result.
   */
  private static String convertSnakeCaseToCamelCase(String snakeCaseString) {
    StringBuilder camelCaseBuilder = new StringBuilder();

    // Split the snake case string by underscores
    String[] words = snakeCaseString.split("_");

    // Append the first word with lowercase
    camelCaseBuilder.append(words[0].toLowerCase());

    // Capitalize the first letter of each subsequent word and append
    for (int i = 1; i < words.length; i++) {
      String capitalizedWord =
          words[i].substring(0, 1).toUpperCase() + words[i].substring(1).toLowerCase();
      camelCaseBuilder.append(capitalizedWord);
    }

    return camelCaseBuilder.toString();
  }

  /**
   * Formats the value to be used in the constant name. The process involves removing leading
   * numeric characters, special characters, and spaces from the name, and converting it to
   * uppercase to comply with Java constant naming conventions.
   */
  private static String formatStringValueToConstantSnakeCaseNomenclature(
      final String stringLiteralExprValue, final String parentNodeName) {

    final String constName = buildName(stringLiteralExprValue, parentNodeName);

    final String sanitizedString = sanitizeStringToOnlyAlphaNumericAndUnderscore(constName);

    final String stringWithoutLeadingNumericCharacters =
        sanitizedString.replaceAll("^\\d*(_)*", "");

    return stringWithoutLeadingNumericCharacters.toUpperCase();
  }

  /**
   * Builds the name to be used in the constant name. It checks if the provided string literal
   * expression value contains only non-alphabetical characters. If it doesn't, the original value
   * is returned as is. Otherwise, the method combines the provided string literal expression value
   * with an optional prefix based on the parent node name (if available) to create a base name.
   */
  private static String buildName(
      final String stringLiteralExprValue, final String parentNodeName) {

    if (!containsOnlyNonAlpha(stringLiteralExprValue)) {
      return stringLiteralExprValue;
    }

    return parentNodeName != null ? parentNodeName : "CONST";
  }

  /** Checks if the input contains only non-alpha characters. */
  private static boolean containsOnlyNonAlpha(final String input) {
    // Use a regular expression to check if the string contains only non-alpha characters
    return input.matches("[^a-zA-Z]+");
  }

  /** Sanitizes the input string by keeping only alphanumeric characters and underscores. */
  private static String sanitizeStringToOnlyAlphaNumericAndUnderscore(final String input) {
    // Use a regular expression to keep only alphanumeric characters and underscores
    final Pattern pattern = Pattern.compile("\\W");
    final Matcher matcher = pattern.matcher(input);

    // Replace non-alphanumeric characters with a single space
    final String stringWithSpaces = matcher.replaceAll(" ");

    // Replace consecutive spaces with a single space
    final String stringWithSingleSpaces = stringWithSpaces.replaceAll("\\s+", " ");

    // Replace spaces with underscores
    return stringWithSingleSpaces.trim().replace(" ", "_");
  }

  /**
   * Checks if a variable with the given constant name already exists in the declared variables set.
   */
  private static boolean existsVariable(
      final String constantName, final Set<String> declaredVariables) {

    if (declaredVariables == null || declaredVariables.isEmpty()) {
      return false;
    }

    return declaredVariables.contains(constantName);
  }
}
