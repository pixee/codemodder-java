package io.codemodder.plugins.maven.operator;

import static org.junit.Assert.assertTrue;

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

  @Test
  void testBasic() throws Exception {
    Path pomFile = getResourceAsPath("sample-child-with-relativepath.xml");

    ProjectModel pmf = buildProjectModelFactory(pomFile).build();
  }

  @Test
  void testTwoLevelsWithLoop() throws Exception {
    Path pomFile = getResourceAsPath("sample-child-with-relativepath-and-two-levels.xml");

    ProjectModel pmf = buildProjectModelFactory(pomFile).build();
  }

  @Test
  void testTwoLevelsWithoutLoop() throws Exception {
    Path pomFile = getResourceAsPath("sample-child-with-relativepath-and-two-levels-nonloop.xml");

    ProjectModel pmf = buildProjectModelFactory(pomFile).build();

    System.out.println(pmf.getParentPomFiles().size());

    assertTrue("There must be two parent pom files", pmf.getParentPomFiles().size() == 2);

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

    System.out.println(uniquePaths.size());
    assertTrue("There must be three unique pom files referenced", uniquePaths.size() == 3);
  }

  @Test
  void testMultipleChildren() throws DocumentException, IOException, URISyntaxException {
    for (int index = 1; index <= 3; index++) {
      Path pomFile = getResourceAsPath("nested/child/pom/pom-" + index + "-child.xml");

      ProjectModel pm = buildProjectModelFactory(pomFile).build();

      assertTrue("There must be at least one parent pom file", pm.getParentPomFiles().size() > 0);

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

      assertTrue("There must be at least two unique pom files referenced", uniquePaths.size() >= 2);
    }
  }

  @Test
  void testMissingRelativeParentElement()
      throws DocumentException, IOException, URISyntaxException {
    Path pomFile = getResourceAsPath("nested/child/pom/pom-demo.xml");

    ProjectModel pm = buildProjectModelFactory(pomFile).build();

    assertTrue("There must be a single parent pom file", pm.getParentPomFiles().size() == 1);
  }

  @Test
  void testLegacyWithInvalidRelativePaths()
      throws DocumentException, IOException, URISyntaxException {
    for (int index = 1; index <= 3; index++) {
      String name = "sample-child-with-broken-path-" + index + ".xml";
      Path pomFile = getResourceAsPath(name);

      ProjectModelFactory pmf = buildProjectModelFactory(pomFile);

      assert pmf.build().getParentPomFiles().isEmpty();
    }
  }

  @Test
  void testWithRelativePathEmpty() throws Exception {
    for (int index = 3; index <= 4; index++) {
      Path pomFile = getResourceAsPath("pom-multiple-pom-parent-level-" + index + ".xml");

      try {
        ProjectModelFactory pmf = buildProjectModelFactory(pomFile);

        assertTrue(pmf.build().getParentPomFiles().size() > 0);
      } catch (Exception e) {
        // LOGGER.info("Exception thrown: " + e);

        if (e instanceof InvalidPathException) {
          continue;
        }

        throw e;
      }
    }
  }

  @Test
  void testWithMissingRelativePath() throws DocumentException, IOException, URISyntaxException {
    Path pomFile =
        getResourceAsPath("sample-parent/sample-child/pom-multiple-pom-parent-level-6.xml");

    ProjectModelFactory pmf = buildProjectModelFactory(pomFile);

    assertTrue(pmf.build().getParentPomFiles().size() > 0);
  }
}
