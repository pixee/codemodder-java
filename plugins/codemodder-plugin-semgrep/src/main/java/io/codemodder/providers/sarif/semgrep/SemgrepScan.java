package io.codemodder.providers.sarif.semgrep;

import io.codemodder.Codemod;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.inject.Qualifier;

/**
 * This tells the framework to inject the results of a Semgrep scan into the following parameter.
 * This can only inject {@link io.codemodder.RuleSarif} types.
 */
@Documented
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface SemgrepScan {

  /**
   * The classpath resource path of the type. It is assumed the path will be in the same package as
   * the {@link Codemod}.
   *
   * <p>So, for instance, if you had a codemod in <code>com.acme.codemods</code>, and a YAML rule
   * file in /com/acme/codemods/my-rule.yaml, you would simply specify "my-rule.yaml" for this
   * value.
   */
  String pathToYaml() default "";

  /**
   * The Semgrep rule "id" field from the YAML. This is needed to disambiguate Semgrep results as we
   * consolidate Semgrep rules into one scan.
   */
  String ruleId();
}
