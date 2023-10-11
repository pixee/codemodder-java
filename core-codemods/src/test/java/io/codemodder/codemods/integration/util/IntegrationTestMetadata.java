package io.codemodder.codemods.integration.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface IntegrationTestMetadata {

  /** The codemod being tested. */
  String codemodId();

  TestPropertiesMetadata[] tests();
}
