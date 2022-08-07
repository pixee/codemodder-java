package io.pixee.codetl.java;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class VisitorBasedFactoryCreationTest {
  @Test
  void checkVisitorFactoryCreation() {
    var calculator = new VisitorBasedDSLProcessor();
    var expression =
        """
           given method_call getInsecure where
           name = <init>
           type = java.util.Random

          transform
           name = <init>
           type = java.security.SecureRandom""";

    var result = calculator.parse(expression);
    assertNotNull(result);
  }
}
