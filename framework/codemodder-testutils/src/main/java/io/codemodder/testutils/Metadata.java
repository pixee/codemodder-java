package io.codemodder.testutils;

import io.codemodder.CodeChanger;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Metadata {

  /** The codemod being tested. */
  Class<? extends CodeChanger> codemodType();

  /** The classpath-based test directory containing the test case artifacts. */
  String testResourceDir();

  /**
   * The GAV coordinates of any dependencies that should be added to the project after the codemod's
   * execution.
   */
  String[] dependencies();

  /**
   * Test files should always be renamed to this before execution. Helps test codemods that only
   * target certain file names. Also supports directories in the path.
   */
  String renameTestFile() default "";

  /**
   * Whether to re-run the transformed code through a second transformation with the same inputs,
   * but with the transformed code, to see if another transformation is erroneously made.
   */
  boolean doRetransformTest() default true;

  Class<?>[] projectProviders() default {};
}
