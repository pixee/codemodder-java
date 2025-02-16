package io.codemodder.testutils;

import io.codemodder.CodeChanger;
import io.codemodder.ProjectProvider;
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

  /**
   * Some codemods change their behavior based on the context of the app -- like which versions of
   * libraries are present. If you want to simulate specific contextual conditions related to
   * dependencies, you can add a customized provider here A usage example can be found at {@code
   * HardenXStreamCodemodTestProjectProvider}
   */
  Class<? extends ProjectProvider>[] projectProviders() default {};

  /** The expected fix metadata that the codemod should report. */
  int[] expectingFixesAtLines() default {};

  /** The expected failed fix metadata that the codemod should report. */
  int[] expectingFailedFixesAtLines() default {};

  /** Sonar issues file names for testing multiple json files */
  String[] sonarJsonFiles() default {};

  /**
   * Used to filter test execution to only the tests with a display name that matches the given
   * regex. This is a test-driven development tool for iterating on a single, dynamic test case.
   *
   * <pre>
   * &#64;Metadata(
   *   codemodType = LogFailedLoginCodemod.class,
   *   testResourceDir = "log-failed-login",
   *   only = "\\/safe\\/.*",
   *   dependencies = {})
   * public final class LogFailedLoginCodemodTest implements CodemodTestMixin {
   * </pre>
   */
  String only() default "";
}
