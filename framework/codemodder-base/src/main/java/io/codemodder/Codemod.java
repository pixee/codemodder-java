package io.codemodder;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Used to mark types providing codemod functionality and provide the necessary metadata. */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Codemod {

  /**
   * The codemod ID, which must follow this nomenclature:
   *
   * <p>[vendor]:[language]/[unique identifier]
   *
   * <p>Some examples:
   *
   * <ul>
   *   <li>pixee:java/secure-random
   *   <li>codeql:java/xss
   * </ul>
   */
  String id();

  /**
   * The review guidance for the changes introduced by this codemod
   *
   * @return review guidance
   */
  ReviewGuidance reviewGuidance();

  /** How important it is that this codemod execute sooner in the list of codemods being run. */
  CodemodExecutionPriority executionPriority() default CodemodExecutionPriority.NORMAL;

  /**
   * How important this codemod's changes are. Maybe a stand-in for a severity value that we may
   * want to replace this with someday. May be unused locally, but is needed for documentation.
   */
  @SuppressWarnings("unused")
  Importance importance();
}
