package io.codemodder.codemods;

import io.codemodder.Codemod;
import io.codemodder.ReviewGuidance;
import io.codemodder.RuleSarif;
import io.codemodder.plugins.llm.CodeChangingLLMRemediationOutcome;
import io.codemodder.plugins.llm.OpenAIService;
import io.codemodder.plugins.llm.SarifToLLMForMultiOutcomeCodemod;
import io.codemodder.providers.sarif.semgrep.ProvidedSemgrepScan;
import java.util.List;
import javax.inject.Inject;

/**
 * Fix, harden or suppress Semgrep issues reported under the id
 * "java.lang.security.audit.unsafe-reflection.unsafe-reflection".
 */
@Codemod(
    id = "semgrep:java/java.lang.security.audit.unsafe-reflection.unsafe-reflection",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW)
public class ReflectionInjectionCodemod extends SarifToLLMForMultiOutcomeCodemod {

  @Inject
  public ReflectionInjectionCodemod(
      @ProvidedSemgrepScan(ruleId = "java.lang.security.audit.unsafe-reflection.unsafe-reflection")
          final RuleSarif sarif,
      final OpenAIService openAIService) {
    super(
        sarif,
        openAIService,
        List.of(
            new CodeChangingLLMRemediationOutcome(
                "driver_loading",
                "The Class.forName() result is not assigned to anything, the class name being loaded isn't obviously from an untrusted source, and the surrounding code looks like it is related to loading a JDBC driver.",
                "Change the code to add a Semgrep suppression comment above the line cited to prevent it from being reported again."),
            new CodeChangingLLMRemediationOutcome(
                "is_constant_from_method",
                "The class name loaded is actually a constant, but it's referenced indirectly through an intermediate method.",
                "Change the code to so the Class.forName() is passed the same thing as the method. You can pass it the variable field directly, or a string literal."),
            new CodeChangingLLMRemediationOutcome(
                "has_constant_prefix",
                "The class name is prefixed with a constant or literal expression before loading so the package is hardcoded.",
                "Change the code to add a Semgrep suppression comment above the line cited to prevent it from being reported again."),
            new CodeChangingLLMRemediationOutcome(
                "unverifiable_and_potentially_intentionally_unsafe",
                "The code context suggests that the this feature is related to OSGi or other classloading middleware, and that it is intentionally executing arbitrary code by design.",
                "Change the code to add a Semgrep suppression comment above the line cited to prevent it from being reported again."),
            new CodeChangingLLMRemediationOutcome(
                "not_obvious_but_can_introduce_control",
                "Select this outcome if none of the other outcomes feel clear. You cannot select this outcome if there is a static prefix used in the type name.",
                "Change the code to use a Java security control API. Add a static import for the method io.github.pixee.Reflection.loadAndVerify(String) and use it to load the class name instead of directly calling Class.forName().")));
  }

  @Override
  protected String getThreatPrompt() {
    return """
            A security tool has cited this code as being vulnerable to Reflection Injection. Code is vulnerable to this threat if external actors can control the value of a string that is passed to a method that uses reflection to load a class. If the string is not validated, an attacker can pass the name of a class that is not expected by the developer. This can lead to the attacker executing arbitrary code.

            - You unfortunately cannot help with any APIs being cited as vulnerable besides Class.forName().
            - The Semgrep rule that cites it is java.lang.security.audit.unsafe-reflection.unsafe-reflection

            """;
  }
}
