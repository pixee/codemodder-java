package io.codemodder.codemods;

import com.contrastsecurity.sarif.PhysicalLocation;
import com.contrastsecurity.sarif.Result;
import com.contrastsecurity.sarif.Run;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.stmt.Statement;
import com.theokanning.openai.completion.chat.*;
import io.codemodder.*;
import io.codemodder.javaparser.JavaParserChanger;
import io.codemodder.plugins.llm.OpenAIService;
import io.codemodder.providers.sarif.semgrep.SemgrepScan;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.inject.Inject;

/** A codemod that removes any sensitive data being logged. */
@Codemod(
    id = "pixee:java/sensitive-data-logging",
    importance = Importance.HIGH,
    reviewGuidance = ReviewGuidance.MERGE_AFTER_REVIEW)
public final class SensitiveDataLoggingCodemod extends JavaParserChanger {

  private final RuleSarif sarif;
  private final OpenAIService service;

  @Inject
  public SensitiveDataLoggingCodemod(
      @SemgrepScan(ruleId = "sensitive-data-logging") final RuleSarif sarif,
      final OpenAIService openAIService) {
    this.sarif = sarif;
    this.service = Objects.requireNonNull(openAIService);
  }

  @Override
  public CodemodFileScanningResult visit(
      final CodemodInvocationContext context, final CompilationUnit cu) {

    List<Result> results = sarif.getResultsByLocationPath(context.path());

    String rawFile;
    try {
      rawFile = Files.readString(context.path());
    } catch (IOException e) {
      throw new UncheckedIOException("Couldn't read file", e);
    }

    List<String> rawLines = rawFile.lines().toList();

    List<CodemodChange> changes = new ArrayList<>();
    for (Result result : results) {
      PhysicalLocation physicalLocation = result.getLocations().get(0).getPhysicalLocation();
      Integer startLine = physicalLocation.getRegion().getStartLine();
      Optional<Statement> statement = getSingleStatement(cu, startLine);

      if (statement.isPresent()) {
        SensitivityAndFixAnalysis analysis;
        try {
          analysis = performSensitivityAnalysis(rawLines, startLine);
        } catch (IOException e) {
          throw new UncheckedIOException("Couldn't perform sensitivity analysis", e);
        }
        if (analysis.isSensitiveAndDirectlyLogged()) {
          String newStatement = analysis.newStatement();
          if (newStatement != null && !newStatement.isBlank()) {
            Statement newStmt = StaticJavaParser.parseStatement(newStatement);
            statement.get().replace(newStmt);

            String analysisText = analysis.isSensitiveAnalysisText();
            CodemodChange change = CodemodChange.from(startLine, analysisText);
            changes.add(change);
          }
        }
      }
    }
    return CodemodFileScanningResult.from(changes, List.of());
  }

  private SensitivityAndFixAnalysis performSensitivityAnalysis(
      final List<String> lines, final Integer startLine) throws IOException {
    String codeSnippet = numberedContextWithExtraLines(lines, startLine, 10, 10);
    String prompt =
        """
              A tool has cited line %d of the code for possibly logging sensitive data:

              %s

              Respond ONLY in the form of JSON with the following keys, in this order:

              sensitive_analysis_text: a careful, thorough analysis of whether the data is sensitive (specifically a password, session ID, security token, SSN, etc -- not a username)
              is_data_directly_logged: a careful, thorough analysis of whether the data is definitely and directly logged (e.g., not just passed to another method inside to the scope, unless that's a method that obviously returns the given string)
              is_it_sensitive_and_directly_logged: a boolean dictating whether it is sensitive and definitely and directly logged
              new_line_to_replace: if sensitive and directly logged, the statement on line %d that should replace it -- remember to correctly JSON escape this value
              """
            .formatted(startLine, codeSnippet, startLine);

    ChatCompletionRequest request =
        ChatCompletionRequest.builder()
            .temperature(0D)
            .model("gpt-4o-2024-05-13")
            //            .model("gpt-4-0613")
            .n(1)
            .messages(List.of(new ChatMessage(ChatMessageRole.USER.value(), prompt)))
            .build();
    ChatCompletionResult completion = service.createChatCompletion(request);
    ChatCompletionChoice response = completion.getChoices().get(0);
    String responseText = response.getMessage().getContent();
    if (responseText.startsWith("```json") && responseText.endsWith("```")) {
      responseText = responseText.substring(7, responseText.length() - 3);
    }
    return new ObjectMapper().readValue(responseText, SensitivityAndFixAnalysisDTO.class);
  }

  /**
   * We can fix if there's only one statement on the given line (meaning, it may span multiple
   * lines, but only one statement is started on the lin.
   */
  private Optional<Statement> getSingleStatement(final CompilationUnit cu, final Integer line) {
    List<Statement> statements =
        cu.findAll(Statement.class).stream()
            .filter(s -> s.getRange().isPresent())
            .filter(s -> s.getRange().get().begin.line == line)
            .toList();
    if (statements.size() == 1) {
      return Optional.of(statements.get(0));
    }
    return Optional.empty();
  }

  /** The results of the sensitivity analysis and, optionally, the fix to apply. */
  private interface SensitivityAndFixAnalysis {

    /**
     * A detailed analysis of whether the data is sensitive, like a password, security token, etc.
     * and its directly logged.
     */
    String isSensitiveAnalysisText();

    /** Whether the statement logs sensitive data. */
    boolean isSensitiveAndDirectlyLogged();

    /** The new statement with which to replace the old. */
    String newStatement();
  }

  private static class SensitivityAndFixAnalysisDTO implements SensitivityAndFixAnalysis {
    @JsonProperty("sensitive_analysis_text")
    private String sensitiveAnalysisText;

    @JsonProperty("is_data_directly_logged")
    private String isDataDirectlyLogged;

    @JsonProperty("is_it_sensitive_and_directly_logged")
    private boolean isSensitiveAndDirectlyLogged;

    @JsonProperty("new_line_to_replace")
    private String newLineToReplace;

    @Override
    public String isSensitiveAnalysisText() {
      return sensitiveAnalysisText;
    }

    @Override
    public boolean isSensitiveAndDirectlyLogged() {
      return isSensitiveAndDirectlyLogged;
    }

    @Override
    public String newStatement() {
      return newLineToReplace;
    }
  }

  private static String numberedContextWithExtraLines(
      final List<String> lines,
      final int line,
      final int extraLinesBefore,
      final int extraLinesAfter) {
    StringBuilder sb = new StringBuilder();
    int startLine = Math.max(0, line - extraLinesBefore);
    int endLine = Math.min(lines.size(), line + extraLinesAfter);
    for (int i = startLine; i < endLine; i++) {
      sb.append(i + 1);
      sb.append(": ");
      sb.append(lines.get(i));
      sb.append("\n");
    }
    return sb.toString();
  }

  @Override
  public boolean shouldRun() {
    List<Run> runs = sarif.rawDocument().getRuns();
    return runs != null && !runs.isEmpty() && !runs.get(0).getResults().isEmpty();
  }
}
