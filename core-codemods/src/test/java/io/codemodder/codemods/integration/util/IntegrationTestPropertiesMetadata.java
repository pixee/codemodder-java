package io.codemodder.codemods.integration.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Annotation to configure test cases for codemod integration test */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface IntegrationTestPropertiesMetadata {

  /** The endpoint of the test. */
  String endpoint();

  /** Endpoint verb. */
  String httpVerb() default "GET";

  /** The expected response of the endpoint. */
  String expectedResponse();
}
