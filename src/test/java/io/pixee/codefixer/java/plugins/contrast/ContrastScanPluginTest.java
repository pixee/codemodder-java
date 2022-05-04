package io.pixee.codefixer.java.plugins.contrast;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import com.contrastsecurity.sarif.Result;
import com.contrastsecurity.sarif.Run;
import com.contrastsecurity.sarif.SarifSchema210;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.pixee.codefixer.java.DefaultRuleSetting;
import io.pixee.codefixer.java.RuleContext;
import io.pixee.codefixer.java.VisitorFactory;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

final class ContrastScanPluginTest {

  private ContrastScanPlugin plugin;
  private List<Run> runs;

  @BeforeEach
  void setup() throws IOException {
    this.plugin = new ContrastScanPlugin();
    String sarifFile = "src/test/resources/webgoat_v8.2.2_contrast.sarif";
    SarifSchema210 sarifSchema210 =
        new ObjectMapper().readValue(new FileReader(sarifFile), SarifSchema210.class);
    this.runs = sarifSchema210.getRuns();
    assertThat(runs.size(), equalTo(1));
  }

  @Test
  void it_supports_codeql() {
    assertThat(plugin.supports(runs.get(0).getTool()), is(true));
  }

  @Test
  void it_creates_correct_number_of_factories() {
    List<VisitorFactory> factories =
        plugin.getJavaVisitorFactoriesFor(
            new File("."),
            runs.get(0),
            RuleContext.of(DefaultRuleSetting.ENABLED, Collections.emptyList()));

    assertThat(factories, is(notNullValue()));

    List<Result> results = runs.get(0).getResults();

    Set<Result> xssResults =
        results.stream()
            .filter(
                r -> "stored-xss".equals(r.getRuleId()) || "reflected-xss".equals(r.getRuleId()))
            .collect(Collectors.toUnmodifiableSet());
    Set<String> filesWithXss =
        xssResults.stream()
            .map(r -> r.getLocations().get(0).getPhysicalLocation().getArtifactLocation().getUri())
            .collect(Collectors.toUnmodifiableSet());
    Set<Map.Entry<String, Set<Result>>> reflectedResultsPerFile =
        plugin.getRuleEntries(results, List.of("reflected-xss"));
    Set<Map.Entry<String, Set<Result>>> storedResultsPerFile =
        plugin.getRuleEntries(results, List.of("stored-xss"));

    assertThat(filesWithXss.size(), equalTo(23));
    assertThat(reflectedResultsPerFile.size(), equalTo(20));
    assertThat(storedResultsPerFile.size(), equalTo(10));
    assertThat(
        factories.size(), equalTo(reflectedResultsPerFile.size() + storedResultsPerFile.size()));
  }
}
