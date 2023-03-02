package io.openpixee.java;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.nullValue;

import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

final class ConfigurationTest {

  @Test
  void it_loads_valid_config() throws IOException {
    Configuration configuration =
        Configuration.fromString(
            FileUtils.readFileToString(new File("src/test/resources/config/default.yml")));
    assertThat(configuration.getVersion(), equalTo("1"));
    assertThat(configuration.getPaths().getIncludes(), hasItems("src/"));
    assertThat(configuration.getPaths().getExcludes(), hasItems("src/backup/"));
    assertThat(configuration.getPaths().getExcludes(), hasItems("src/test/"));
    assertThat(configuration.getRules().getDefaultRuleSetting(), equalTo("disabled"));
    assertThat(
        configuration.getRules().getExceptions(),
        hasItems("acme:ruleId", "pixee:java/reflected-xss"));
  }

  @Test
  void empty_default_config_produces_expected_includes_excludes() throws IOException {
    Configuration configuration =
        Configuration.fromString(
            FileUtils.readFileToString(new File("src/test/resources/config/empty.yml")));
    assertThat(configuration.getPaths().getIncludes(), is(nullValue()));
    assertThat(configuration.getPaths().getExcludes(), is(nullValue()));
    assertThat(configuration.getRules().getDefaultRuleSetting(), equalTo("enabled"));
  }
}
