package io.openpixee.java.plugins.codeql;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

import com.contrastsecurity.sarif.Result;
import com.contrastsecurity.sarif.Run;
import com.contrastsecurity.sarif.SarifSchema210;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.codemodder.DefaultRuleSetting;
import io.codemodder.RuleContext;
import io.openpixee.java.VisitorFactory;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

final class CodeQlPluginTest {

  private CodeQlPlugin plugin;
  private List<Run> runs;

  @BeforeEach
  void setup() throws IOException {
    this.plugin = new CodeQlPlugin();
    String sarifFile = "src/test/resources/webgoat_v8.2.0_codeql.sarif";
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
  void it_creates_expected_sarif_rule_to_findings_map() {
    Map<String, Set<Result>> map = plugin.getRuleIdToResultsMap(runs.get(0));
    assertThat(map.size(), equalTo(8));
    assertThat(map.get("java/insecure-cookie").size(), equalTo(2));
    assertThat(map.get("java/missing-jwt-signature-check").size(), equalTo(7));
    assertThat(map.get("java/ssrf").size(), equalTo(1));
    assertThat(map.get("java/xxe").size(), equalTo(1));
    assertThat(map.get("java/sql-injection").size(), equalTo(10));
    assertThat(map.get("java/stack-trace-exposure").size(), equalTo(1));
    assertThat(map.get("java/weak-cryptographic-algorithm").size(), equalTo(1));
    assertThat(map.get("java/unsafe-deserialization").size(), equalTo(1));
  }

  @Test
  void it_gives_expected_results() {
    List<VisitorFactory> factories =
        plugin.getJavaVisitorFactoriesFor(
            new File("src/test/resources"),
            runs.get(0),
            RuleContext.of(DefaultRuleSetting.ENABLED, Collections.emptyList()));

    assertThat(factories.size(), equalTo(3));
    assertThat(factories.get(0), instanceOf(InsecureCookieVisitorFactory.class));
    assertThat(factories.get(1), instanceOf(UnverifiedJwtParseVisitorFactory.class));
    assertThat(factories.get(2), instanceOf(StackTraceExposureVisitorFactory.class));
  }
}
