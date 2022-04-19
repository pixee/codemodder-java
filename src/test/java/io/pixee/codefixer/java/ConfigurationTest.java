package io.pixee.codefixer.java;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;

final class ConfigurationTest {

    @Test
    void it_loads_valid_config() throws IOException {
        Configuration configuration = Configuration.fromString(FileUtils.readFileToString(new File("src/test/resources/config/default.yml")));
        assertThat(configuration.getVersion(), equalTo("1"));
        assertThat(configuration.getPaths().getIncludes(), hasItems("src/"));
        assertThat(configuration.getPaths().getExcludes(), hasItems("src/backup/"));
        assertThat(configuration.getPaths().getExcludes(), hasItems("src/test/"));
        assertThat(configuration.getRules().getDefaultRuleSetting(), equalTo("disabled"));
        assertThat(configuration.getRules().getExceptions(), hasItems("acme:ruleId", "pixee:java/reflected-xss"));
    }

}