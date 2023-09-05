package io.codemodder.codemods;

import static io.codemodder.CodemodResources.getClassResourceAsString;

import com.github.difflib.patch.Patch;
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
                "false_positive_driver_loading",
                "The Class.forName() result is not assigned to anything, the class name being loaded isn't obviously from an untrusted source, and the surrounding code looks like it is related to loading a JDBC driver.",
                "Change the code to add a Semgrep suppression comment above the line cited to prevent it from being reported again."),
            new CodeChangingLLMRemediationOutcome(
                "false_positive_is_constant_from_method",
                "The class name loaded is actually a constant, but it's referenced indirectly through an intermediate method.",
                "Refactor the code to make it so the Class.forName() is passed the same thing as the method, the variable field directly, or a string literal."),
            new CodeChangingLLMRemediationOutcome(
                "false_positive_has_constant_prefix",
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
    return getClassResourceAsString(getClass(), "threat_prompt.txt");
  }

  /**
   * This could make 1-to-N changes that are additions and subtractions, so there's not much ability
   * to verify the patch is rational.
   */
  @Override
  protected boolean isPatchExpected(final Patch<String> patch) {
    return true;
  }
}
