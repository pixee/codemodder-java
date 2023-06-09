package io.codemodder.plugins.contrast;

import javax.inject.Qualifier;
import java.lang.annotation.*;

/**
 * This tells the framework to inject the results of a Contrast Assess results into the following parameter.
 * This can only inject {@link io.codemodder.RuleSarif} types.
 */
@Documented
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface ContrastAssessSnapshot {

    /**
     * The Contrast Assess rule (e.g., "reflected-xss") to inject results for.
     */
    String ruleId();
}
