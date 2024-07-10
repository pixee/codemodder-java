package io.codemodder.codemods;

import com.azure.ai.openai.models.ChatRequestUserMessage;
import com.contrastsecurity.sarif.PhysicalLocation;
import com.contrastsecurity.sarif.Result;
import com.contrastsecurity.sarif.Run;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.stmt.Statement;
import io.codemodder.*;
import io.codemodder.javaparser.JavaParserChanger;
import io.codemodder.plugins.llm.OpenAIService;
import io.codemodder.plugins.llm.StandardModel;
import io.codemodder.providers.sarif.semgrep.SemgrepScan;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Inject;

/** A codemod that removes any sensitive data being logged. */
@Codemod(
    id = "pixee:java/sensitive-data-logging",
    importance = Importance.HIGH,
    reviewGuidance = ReviewGuidance.MERGE_AFTER_REVIEW)
public final class SensitiveDataLoggingCodemod extends JavaParserChanger {

  private final RuleSarif sarif;
  private final OpenAIService service;
  private final ObjectReader reader;

  @Inject
  public SensitiveDataLoggingCodemod(
      @SemgrepScan(ruleId = "sensitive-data-logging") final RuleSarif sarif,
      final OpenAIService openAIService) {
    this.sarif = Objects.requireNonNull(sarif);
    this.service = Objects.requireNonNull(openAIService);
    this.reader = new ObjectMapper().readerFor(SensitivityAndFixAnalysisDTO.class);
  }

  @Override
  public CodemodFileScanningResult visit(
      final CodemodInvocationContext context, final CompilationUnit cu) {
    final var source = context.path();
    final List<String> numberedLines;
    try {
      numberedLines = readNumberedLines(source);
    } catch (IOException e) {
      throw new UncheckedIOException("Couldn't read source file", e);
    }
    final List<Result> results = sarif.getResultsByLocationPath(source);
    final List<CodemodChange> changes = new ArrayList<>();
    for (final Result result : results) {
      PhysicalLocation physicalLocation = result.getLocations().get(0).getPhysicalLocation();
      Integer startLine = physicalLocation.getRegion().getStartLine();
      Optional<Statement> statement = getSingleStatement(cu, startLine);
      if (statement.isEmpty()) {
        continue;
      }

      SensitivityAndFixAnalysis analysis;
      try {
        analysis = performSensitivityAnalysis(numberedLines, startLine);
      } catch (IOException e) {
        throw new UncheckedIOException("Couldn't perform sensitivity analysis", e);
      }
      if (analysis.isSensitiveAndDirectlyLogged()) {
        // remove the log statement altogether
        statement.get().remove();
        String analysisText = analysis.sensitiveAnalysisText();
        CodemodChange change = CodemodChange.from(startLine, analysisText);
        changes.add(change);
      }
    }
    return CodemodFileScanningResult.from(changes, List.of());
  }

  private SensitivityAndFixAnalysis performSensitivityAnalysis(
      final List<String> source, final Integer startLine) throws IOException {
    String codeSnippet = snippet(source, startLine);
    String prompt =
        """
              A tool has cited line %d of the code for possibly logging sensitive data:

              %s

              Respond ONLY in the form of JSON with the following keys, in this order:

              sensitive_analysis_text: a careful, thorough analysis of whether the data is sensitive (specifically a password, session ID, security token, SSN, etc -- not a username)
              is_data_directly_logged: a careful, thorough analysis of whether the data is definitely and directly logged (e.g., not just passed to another method inside to the scope, unless that's a method that obviously returns the given string)
              is_it_sensitive_and_directly_logged: a boolean dictating whether it is sensitive and definitely and directly logged
              """
            .formatted(startLine, codeSnippet);

    return service.getResponseForPrompt(
        List.of(new ChatRequestUserMessage(prompt)),
        StandardModel.GPT_4O_2024_05_13,
        SensitivityAndFixAnalysisDTO.class);
  }

  /**
   * We can fix if there's only one statement on the given line (meaning, it may span multiple
   * lines, but only one statement is started on the line).
   */
  private static Optional<Statement> getSingleStatement(
      final CompilationUnit cu, final Integer line) {
    return cu.findAll(Statement.class).stream()
        .filter(s -> s.getRange().isPresent())
        .filter(s -> s.getRange().get().begin.line == line)
        .findFirst();
  }

  @Override
  public boolean shouldRun() {
    List<Run> runs = sarif.rawDocument().getRuns();
    return runs != null && !runs.isEmpty() && !runs.get(0).getResults().isEmpty();
  }

  /** Reads the source code from the given file and numbers each line. */
  private static List<String> readNumberedLines(final Path source) throws IOException {
    final var counter = new AtomicInteger();
    try (final var lines = Files.lines(source)) {
      return lines.map(line -> counter.incrementAndGet() + ": " + line).toList();
    }
  }

  /**
   * Returns a snippet of code surrounding the given line number.
   *
   * @param lines numbered source code lines
   * @param line the line number to center the snippet around
   * @return a snippet of code surrounding the given line number
   */
  private static String snippet(final List<String> lines, final int line) {
    final int start = Math.max(0, line - CONTEXT);
    final int end = Math.min(lines.size(), line + CONTEXT + 1);
    final var snippet = lines.subList(start, end);
    return String.join("\n", snippet);
  }

  /**
   * Number of lines of leading and trailing context surrounding each Semgrep finding to include in
   * the code snippet sent to OpenAI.
   */
  private static final int CONTEXT = 10;

  /** The results of the sensitivity analysis. */
  private interface SensitivityAndFixAnalysis {

    /**
     * A detailed analysis of whether the data is sensitive, like a password, security token, etc.
     * and its directly logged.
     */
    String sensitiveAnalysisText();

    /** Whether the statement logs sensitive data. */
    boolean isSensitiveAndDirectlyLogged();
  }

  private record SensitivityAndFixAnalysisDTO(
      @JsonProperty("sensitive_analysis_text") String sensitiveAnalysisText,
      @JsonProperty("is_data_directly_logged") String isDataDirectlyLogged,
      @JsonProperty("is_it_sensitive_and_directly_logged") boolean isSensitiveAndDirectlyLogged)
      implements SensitivityAndFixAnalysis {}
}
