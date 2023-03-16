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
   * The codemod author, hopefully "email", "First Last (email)", or "Team Name (email)".
   *
   * @return the author of the codemod
   */
  String author();

  /**
   * The review guidance for the changes introduced by this codemod
   *
   * @return review guidance
   */
  ReviewGuidance reviewGuidance();
}
