package io.codemodder.plugins.maven.operator;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import org.dom4j.DocumentException;
import org.junit.jupiter.api.Test;

final class POMScannerTest extends AbstractTestBase {
  private final Path currentDirectory = Paths.get(System.getProperty("user.dir"));

  private ProjectModelFactory buildProjectModelFactory(final Path pomFile)
      throws DocumentException, IOException, URISyntaxException {
    return new POMScanner(pomFile, currentDirectory).scanFrom();
  }

  /**
   * Tests scanning a child with a relative path in the POM file. Verifies the correct parsing of a
   * sample child with a relative path.
   */
  @Test
  void scans_child_with_relative_path() throws Exception {
    Path pomFile = getResourceAsPath("sample-child-with-relativepath.xml");

    ProjectModel pmf = buildProjectModelFactory(pomFile).build();
  }

  /**
   * Tests scanning a POM with two levels and a loop. Verifies the correct handling of a sample POM
   * with two levels and a loop.
   */
  @Test
  void scans_two_level_pom_with_loop() throws Exception {
    Path pomFile = getResourceAsPath("sample-child-with-relativepath-and-two-levels.xml");

    ProjectModel pmf = buildProjectModelFactory(pomFile).build();
  }

  /**
   * Tests scanning a two-level POM without a loop. Verifies the correct handling of a POM with two
   * levels and no loop.
   */
  @Test
  void scans_two_level_pom_without_loop() throws Exception {
    Path pomFile = getResourceAsPath("sample-child-with-relativepath-and-two-levels-nonloop.xml");

    ProjectModel pmf = buildProjectModelFactory(pomFile).build();

    assertThat(pmf.getParentPomFiles()).hasSize(2);

    List<String> uniquePaths =
        pmf.allPomFiles().stream()
            .map(
                pom -> {
                  try {
                    return pom.getPomPath().toURI().normalize().toString();
                  } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                  }
                })
            .collect(Collectors.toList());

    String uniquePathsAsString = String.join(" ", uniquePaths);

    // LOGGER.info("uniquePathsAsString: " + uniquePathsAsString);

    assertThat(uniquePaths).hasSize(3);
  }

  /**
   * Tests scanning multiple children POMs. Verifies handling of multiple child POMs and their
   * relationships.
   */
  @Test
  void scans_multiple_children_pom() throws DocumentException, IOException, URISyntaxException {
    for (int index = 1; index <= 3; index++) {
      Path pomFile = getResourceAsPath("nested/child/pom/pom-" + index + "-child.xml");

      ProjectModel pm = buildProjectModelFactory(pomFile).build();

      assertThat(pm.getParentPomFiles()).isNotEmpty();

      List<String> uniquePaths =
          pm.allPomFiles().stream()
              .map(
                  pom -> {
                    try {
                      return pom.getPomPath().toURI().normalize().toString();
                    } catch (URISyntaxException e) {
                      throw new RuntimeException(e);
                    }
                  })
              .collect(Collectors.toList());

      String uniquePathsAsString = String.join(" ", uniquePaths);

      // LOGGER.info("uniquePathsAsString: " + uniquePathsAsString);

      assertThat(uniquePaths).hasSizeGreaterThanOrEqualTo(2);
    }
  }

  /**
   * Tests scanning a POM with missing relative parent elements. Verifies handling a POM where the
   * relative parent elements are missing.
   */
  @Test
  void scans_pom_with_missing_relative_parent_element()
      throws DocumentException, IOException, URISyntaxException {
    Path pomFile = getResourceAsPath("nested/child/pom/pom-demo.xml");

    ProjectModel pm = buildProjectModelFactory(pomFile).build();

    assertThat(pm.getParentPomFiles()).hasSize(1);
  }

  /**
   * Tests scanning a POM with invalid relative paths. Verifies handling a POM with broken or
   * invalid relative paths.
   */
  @Test
  void scans_pom_with_invalid_relative_paths()
      throws DocumentException, IOException, URISyntaxException {
    for (int index = 1; index <= 3; index++) {
      String name = "sample-child-with-broken-path-" + index + ".xml";
      Path pomFile = getResourceAsPath(name);

      ProjectModelFactory pmf = buildProjectModelFactory(pomFile);

      assert pmf.build().getParentPomFiles().isEmpty();
    }
  }

  /**
   * Tests scanning a POM with empty relative paths. Verifies handling a POM with empty relative
   * paths.
   */
  @Test
  void scans_pom_with_empty_relative_path() throws Exception {
    for (int index = 3; index <= 4; index++) {
      Path pomFile = getResourceAsPath("pom-multiple-pom-parent-level-" + index + ".xml");

      try {
        ProjectModelFactory pmf = buildProjectModelFactory(pomFile);

        assertThat(pmf.build().getParentPomFiles()).isNotEmpty();
      } catch (Exception e) {
        // LOGGER.info("Exception thrown: " + e);

        if (e instanceof InvalidPathException) {
          continue;
        }

        throw e;
      }
    }
  }

  /**
   * Tests scanning a POM with missing relative paths. Verifies handling a POM with missing relative
   * paths.
   */
  @Test
  void scans_pom_with_missing_relative_path()
      throws DocumentException, IOException, URISyntaxException {
    Path pomFile =
        getResourceAsPath("sample-parent/sample-child/pom-multiple-pom-parent-level-6.xml");

    ProjectModelFactory pmf = buildProjectModelFactory(pomFile);

    assertThat(pmf.build().getParentPomFiles()).isNotEmpty();
  }
}
