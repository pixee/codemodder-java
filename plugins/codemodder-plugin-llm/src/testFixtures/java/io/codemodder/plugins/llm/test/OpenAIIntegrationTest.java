package io.codemodder.plugins.llm.test;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import org.junit.jupiter.api.extension.ExtendWith;

/** Marks a test as an OpenAI integration test. */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RUNTIME)
@Documented
@ExtendWith(OpenAIIntegrationTestExtension.class)
public @interface OpenAIIntegrationTest {}
