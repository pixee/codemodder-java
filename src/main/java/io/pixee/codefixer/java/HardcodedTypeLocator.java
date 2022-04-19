package io.pixee.codefixer.java;

import static java.util.Objects.requireNonNull;

import com.github.javaparser.ast.expr.Expression;

/** This locator always returns a value. It's intended for use with a chain of locators. */
final class HardcodedTypeLocator implements TypeLocator {

  private final String hardcodedValue;

  HardcodedTypeLocator(final String hardcodedValue) {
    this.hardcodedValue = requireNonNull(hardcodedValue);
  }

  @Override
  public String locateType(final Expression expression) {
    return hardcodedValue;
  }
}
