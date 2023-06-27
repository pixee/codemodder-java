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

  /** The question to ask the user when they are prompted for this parameter. */
  String question();

  /** The name of the parameter. */
  String name();

  /** A description of the parameter. */
  String label();

  /** The default value if the user didn't specify one. */
  String defaultValue();

  /** A regex pattern that describes the valid values for this parameter. */
  String validationPattern();

  /** The type of the parameter. This is used to determine how to parse the value from the user. */
  ParameterType type() default ParameterType.STRING;

  enum ParameterType {
    STRING,
    NUMBER,
    BOOLEAN
  }
}
