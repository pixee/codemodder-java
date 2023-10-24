package io.codemodder.codemods.integration.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Metadata to set up codemods integration tests framework */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface IntegrationTestMetadata {

  /** The codemod being tested. */
  String codemodId();

  /** The set of tests to perform in the test project. */
  IntegrationTestPropertiesMetadata[] tests();
}
