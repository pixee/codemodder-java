package io.codemodder;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.inject.Qualifier;

/** Describes a codemod parameter. */
@Retention(RetentionPolicy.RUNTIME)
@Qualifier
@Target(ElementType.PARAMETER)
public @interface CodemodParameter {

  /** The name of the parameter. */
  String name();

  /** A description of the parameter. */
  String description();

  /** The default value if the user didn't specify one. */
  String defaultValue();

  /** A regex pattern that describes the valid values for this parameter. */
  String validationPattern();
}
