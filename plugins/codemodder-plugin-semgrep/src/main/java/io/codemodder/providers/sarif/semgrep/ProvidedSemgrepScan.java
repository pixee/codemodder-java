package io.codemodder.providers.sarif.semgrep;

import java.lang.annotation.*;
import javax.inject.Qualifier;

/**
 * This tells the framework to inject the results of an offline Semgrep scan into the following
 * parameter. By offline, we mean a scan that took place before this execution took place. This can
 * only inject {@link io.codemodder.RuleSarif} types.
 */
@Documented
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface ProvidedSemgrepScan {

  /** The Semgrep rule "id". */
  String ruleId();
}
