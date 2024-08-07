package io.codemodder.providers.sarif.appscan;

import java.lang.annotation.*;
import javax.inject.Qualifier;

/**
 * This tells the framework to inject the results of a AppScan scan into the following parameter.
 * This can only inject {@link io.codemodder.RuleSarif} types.
 */
@Documented
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface ProvidedAppScanScan {

  /** The AppScan rule name, which shows up as the "message text" in the SARIF results. */
  String ruleName() default "";

  /** The AppScan rule names, which show up as the "message text" in the SARIF results. */
  String[] ruleNames() default {};
}
