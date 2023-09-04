package io.codemodder.codemods;

import static io.codemodder.CodemodResources.getClassResourceAsString;

import com.github.difflib.patch.Patch;
import io.codemodder.Codemod;
import io.codemodder.ReviewGuidance;
import io.codemodder.RuleSarif;
import io.codemodder.plugins.llm.CodeChangingLLMRemediationOutcome;
import io.codemodder.plugins.llm.NoActionLLMRemediationOutcome;
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
                "false_positive_is_constant",
                """
                                          The class name loaded is actually a constant expression, but it's defined elsewhere in this type. This is of course technically a false positive, but if we can just refactor the code to use the constant expression instead, we can do that to make the tool happy.
                                          """,
                """
                                          If you can refactor the code to make it so the Class.forName() call can reference a member field, change it. If not, add a Semgrep suppression comment above it.
                                          """),
            new CodeChangingLLMRemediationOutcome(
                "false_positive_has_constant_prefix",
                """
                                              The class name loaded is visibly prefixed with a constant expression sometime before loading.
                                              This may be considered a false positive by developers because it's fairly limited what an
                                              attacker could do with such a tightly controlled reflection. We could suppress the issue in
                                              this case, because forcing it through a control like Reflection#verifyAndLoadPackage() seems
                                              redundant.                                                                                                                                                                                                                                                                                                     * redundant.
                                              """,
                """
                                            Add a Semgrep suppression comment above it.
                                            """),
            new CodeChangingLLMRemediationOutcome(
                "unverifiable_but_type_constrained",
                """
                                              The source of the class name can't be verified, but there are clues about what the loaded
                                              class instance type has to be (e.g., it's casted to something after being loaded.) In this
                                              case, if it is not expected to be anything related to code loading, we can use a control like
                                              Reflection#loadClass() and make sure that it can't be a type that would commonly be used
                                              in exploitation.
                                              """,
                """
                                            Use the Java security control API method io.github.pixee.Reflection.loadAndVerify(String) to load the class name instead of directly calling Class.forName().
                                            """),
            new NoActionLLMRemediationOutcome(
                "unverifiable_and_potentially_intentionally_unsafe",
                """
                                             The source of the class name can't be verified, but it's used in a way that suggests it's
                                             related to loading or executing arbitrary code by design, in which case we can't do anything
                                             and we should leave further analysis to the user.
                                              """)));
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
