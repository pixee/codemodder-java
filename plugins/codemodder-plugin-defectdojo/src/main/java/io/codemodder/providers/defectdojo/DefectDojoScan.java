package io.codemodder.providers.defectdojo;

import java.lang.annotation.*;
import javax.inject.Qualifier;

/**
 * This tells the framework to inject the findings from DefectDojo into the following parameter.
 * This can only inject {@link io.codemodder.providers.defectdojo.RuleFindings} types.
 */
@Documented
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface DefectDojoScan {

  /** The DefectDojo tool rule "id" field, meaning the original scanner's rule ID. */
  String ruleId();
}
