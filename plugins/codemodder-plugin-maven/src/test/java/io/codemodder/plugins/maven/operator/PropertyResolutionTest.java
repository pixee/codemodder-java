package io.codemodder.plugins.maven.operator;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.dom4j.DocumentException;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class PropertyResolutionTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(PropertyResolutionTest.class);

  /**
   * Tests property resolution when a profile is deactivated forcefully. Verifies that the 'foo'
   * property is not present when a specific profile is deactivated.
   */
  @Test
  void it_resolves_property_when_profile_is_deactivated_forcefully()
      throws DocumentException, IOException, URISyntaxException {
    Map<String, String> resolvedProperties = resolveWithProfiles("!test-profile");

    assertThat(resolvedProperties.containsKey("foo")).isFalse();
  }

  /**
   * Tests property resolution when a profile is missing. Verifies that the 'foo' property is not
   * present when no profile is provided.
   */
  @Test
  void it_resolves_property_when_profile_is_missing()
      throws DocumentException, IOException, URISyntaxException {
    Map<String, String> resolvedProperties = resolveWithProfiles();

    assertThat(resolvedProperties.containsKey("foo")).isFalse();
  }

  /**
   * Tests property resolution when a profile is activated. Verifies that the 'foo' property is
   * present and has the expected value when a specific profile is activated.
   */
  @Test
  void it_resolves_property_when_profile_is_activated()
      throws DocumentException, IOException, URISyntaxException {
    Map<String, String> resolvedProperties = resolveWithProfiles("test-profile");

    assertThat(resolvedProperties).containsKey("foo");
    assertThat(resolvedProperties).containsEntry("foo", "bar");
  }

  private Map<String, String> resolveWithProfiles(String... profilesToUse)
      throws DocumentException, IOException, URISyntaxException {
    LOGGER.debug("resolving with profiles: " + Arrays.toString(profilesToUse));

    Dependency dependencyToUpgrade =
        new Dependency("org.dom4j", "dom4j", "2.0.2", null, null, null);
    ProjectModel context =
        ProjectModelFactory.load(POMOperatorTest.class.getResource("pom-1.xml"))
            .withDependency(dependencyToUpgrade)
            .withActiveProfiles(profilesToUse)
            .build();

    LOGGER.debug("Resolved Properties: " + context.resolvedProperties());

    return new HashMap<>(context.resolvedProperties());
  }
}
