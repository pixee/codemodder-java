package io.codemodder.providers.sarif.codeql;

import java.lang.annotation.*;
import javax.inject.Qualifier;

/**
 * This tells the framework to inject the results of a CodeQL scan into the following parameter.
 * This can only inject {@link io.codemodder.RuleSarif} types.
 */
@Documented
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface CodeQLScan {

  /** The CodeQL rule "id" field from the sarif. */
  String ruleId();
}
