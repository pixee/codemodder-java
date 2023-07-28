package io.codemodder.providers.sarif.pmd;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.inject.Qualifier;

/**
 * This tells the framework to inject the results of a PMD scan into the following parameter. This
 * can only inject {@link io.codemodder.RuleSarif} types.
 */
@Documented
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface PmdScan {

  /** The PMD rule ID. */
  String ruleId();
}
