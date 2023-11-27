package io.codemodder.providers.sonar;

import java.lang.annotation.*;
import javax.inject.Qualifier;

/** This tells the framework to inject the results of a Sonar scan into the following parameter. */
@Documented
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface ProvidedSonarScan {

  /** The rule ID on the Sonar side. */
  String ruleId();
}
