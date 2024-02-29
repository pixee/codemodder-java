package io.codemodder.providers.sarif.appscan;

import static org.assertj.core.api.Assertions.assertThat;

import com.contrastsecurity.sarif.Region;
import com.contrastsecurity.sarif.Result;
import com.contrastsecurity.sarif.SarifSchema210;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.codemodder.RuleSarif;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class AppScanRuleSarifFactoryTest {

  /**
   * In this test we'll attempt to load the SARIF file and build a {@link RuleSarif} from it, adn
   * confirm we can get the right locations for result guid 9833fe60-ded1-ee11-9f02-14cb65725114.
   */
  @Test
  void it_parses_sarif_and_maps_java_locations(@TempDir final Path tmpDir) throws IOException {

    // create the webgoat file in the repository dir
    String expectedPath =
        "src/main/java/org/owasp/webgoat/lessons/challenges/challenge5/Assignment5.java";
    Path actualAssignmentedJavaFilePath = tmpDir.resolve(expectedPath);
    actualAssignmentedJavaFilePath.toFile().getParentFile().mkdirs();

    // create the file contents that this SARIF has results for
    Files.copy(Path.of("src/test/resources/Assignment5.java.txt"), actualAssignmentedJavaFilePath);

    // read the SARIF file and build a RuleSarif from it
    AppScanRuleSarifFactory appScanRuleSarifFactory = new AppScanRuleSarifFactory();
    SarifSchema210 rawSarif =
        new ObjectMapper()
            .readValue(
                new File("src/test/resources/webgoat_2023_8_binary.sarif"), SarifSchema210.class);
    Optional<RuleSarif> sarifRef =
        appScanRuleSarifFactory.build(
            "HCL AppScan Static Analyzer", "SA2813462719", rawSarif, tmpDir);
    assertThat(sarifRef.isPresent()).isTrue();
    RuleSarif ruleSarif = sarifRef.get();

    // verify the rule sarif has the right stuff
    assertThat(ruleSarif.getRule()).isEqualTo("SA2813462719");
    assertThat(ruleSarif.getDriver()).isEqualTo("HCL AppScan Static Analyzer");
    assertThat(ruleSarif.rawDocument()).isEqualTo(rawSarif);

    // get the results for the file path (not the weird AppScan thing) and confirm we have the right
    // results
    List<Result> resultsForPath =
        ruleSarif.getResultsByLocationPath(actualAssignmentedJavaFilePath);
    assertThat(resultsForPath).isNotEmpty();

    // get the regions affected by the given file
    List<Region> regions = ruleSarif.getRegionsFromResultsByRule(actualAssignmentedJavaFilePath);

    // there is an injection in two form parameters in the SQL, so these 2 share the same sink
    assertThat(regions).hasSize(2);
    assertThat(regions.get(0).getStartLine()).isEqualTo(59);
    assertThat(regions.get(1).getStartLine()).isEqualTo(59);
  }
}
