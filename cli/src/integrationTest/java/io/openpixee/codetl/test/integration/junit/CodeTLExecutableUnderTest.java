package io.openpixee.codetl.test.integration.junit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Marks an integration test parameter of type {@link CodeTLExecutable} such that the test framework
 * will inject the right instance of {@link CodeTLExecutable}].
 */
@ExtendWith(CodeTLExecutableExtension.class)
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface CodeTLExecutableUnderTest {}
