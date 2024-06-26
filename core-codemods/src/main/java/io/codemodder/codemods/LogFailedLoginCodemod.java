package io.codemodder.codemods;

import io.codemodder.*;
import io.codemodder.plugins.llm.CodeChangingLLMRemediationOutcome;
import io.codemodder.plugins.llm.NoActionLLMRemediationOutcome;
import io.codemodder.plugins.llm.OpenAIService;
import io.codemodder.plugins.llm.SarifToLLMForMultiOutcomeCodemod;
import io.codemodder.plugins.llm.StandardModel;
import io.codemodder.providers.sarif.semgrep.SemgrepScan;
import java.util.List;
import javax.inject.Inject;

@Codemod(
    id = "pixee:java/log-failed-login",
    importance = Importance.HIGH,
    reviewGuidance = ReviewGuidance.MERGE_AFTER_REVIEW)
public final class LogFailedLoginCodemod extends SarifToLLMForMultiOutcomeCodemod {

  @Inject
  public LogFailedLoginCodemod(
      @SemgrepScan(ruleId = "log-failed-login") final RuleSarif sarif, final OpenAIService openAI) {
    super(
        sarif,
        openAI,
        List.of(
            new NoActionLLMRemediationOutcome(
                "logs_failed_login_with_logger",
                """
                  The code uses a logger to log a message that indicates a failed login attempt.
                  The message is logged at the INFO or higher level.
                  """
                    .replace('\n', ' ')),
            new NoActionLLMRemediationOutcome(
                "logs_failed_login_with_console",
                """
                  The code sends a message to the console that indicates a failed login attempt.
                  The code may output this message to either System.out or System.err.
                  """
                    .replace('\n', ' ')),
            new NoActionLLMRemediationOutcome(
                "throws_exception",
                """
                  The code throws an exception that indicates a failed login attempt.
                  Throwing such an exception is a reasonable alternative to logging the failed login attempt.
                  When the username for the failed login is in-scope, the exception message includes the username.
                  """
                    .replace('\n', ' ')),
            new NoActionLLMRemediationOutcome(
                "no_authentication",
                """
                  The login validation fails because the request lacks credentials to validate. This is not considered a failed login attempt that requires auditing.
                  """
                    .replace('\n', ' ')),
            new CodeChangingLLMRemediationOutcome(
                "add_missing_logging",
                """
                  None of the other outcomes apply.
                  The code that validates the login credentials does not log a message when the login attempt fails,
                  NOR does it throw an exception that reasonably indicates a failed login attempt and includes the username in the exception message.
                  """
                    .replace('\n', ' '),
                """
                  Immediately following the login failure, add precisely one statement to log the failed login attempt at the INFO level.
                  If the username for the failed login is in scope, the new log message references the username.
                  Add exactly one such log statement! Exactly one!
                  The new log statement is consistent with the rest of the code with respect to formatting, braces, casing, etc.
                  When no logger is in scope, the new code emits a log message to the console.
                  """
                    .replace('\n', ' '))),
        StandardModel.GPT_4O,
        StandardModel.GPT_4);
  }

  @Override
  protected String getThreatPrompt() {
    return """
            The tool has cited an authentication check that does not include a means for auditing failed login attempt.
            """
        .replace('\n', ' ');
  }
}
