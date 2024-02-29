package io.codemodder.providers.sarif.appscan;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import com.contrastsecurity.sarif.SarifSchema210;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Guice;
import com.google.inject.Injector;
import io.codemodder.*;
import io.codemodder.codetf.CodeTFReference;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class AppScanModuleTest {

  @Codemod(
      id = "appscan-test:java/finds-stuff",
      importance = Importance.LOW,
      reviewGuidance = ReviewGuidance.MERGE_AFTER_CURSORY_REVIEW)
  static class AppScanSarifTestCodemod implements CodeChanger {
    private final RuleSarif ruleSarif;

    @Inject
    AppScanSarifTestCodemod(@ProvidedAppScanScan(ruleId = "SA2813462719") RuleSarif ruleSarif) {
      this.ruleSarif = ruleSarif;
    }

    @Override
    public String getSummary() {
      return null;
    }

    @Override
    public String getDescription() {
      return null;
    }

    @Override
    public List<CodeTFReference> getReferences() {
      return null;
    }

    @Override
    public String getIndividualChangeDescription(Path filePath, CodemodChange change) {
      return null;
    }
  }

  private static final String emptySarif =
      """
                  {
                    "$schema": "https://schemastore.azurewebsites.net/schemas/json/sarif-2.1.0-rtm.5.json",
                    "version": "2.1.0",
                    "runs": [
                      {
                        "tool": {
                          "driver": {
                            "name": "HCL AppScan Static Analyzer"
                          }
                        },
                        "artifacts": [
                          {
                            "location": {
                              "uri": "file:///com/acme/MyVulnerableType.java"
                            }
                          }
                        ],
                        "results": []
                      }
                    ]
                  }
                  """;

  /** This only tests that the module binds the rule sarif to the codemod. */
  @Test
  void it_works_with_appscan_sarif(@TempDir final Path repoDir) throws IOException {
    SarifSchema210 rawSarif =
        new ObjectMapper().readValue(AppScanModuleTest.emptySarif, SarifSchema210.class);
    AppScanRuleSarifFactory ruleSarifFactory = new AppScanRuleSarifFactory();
    Optional<RuleSarif> ruleSarif =
        ruleSarifFactory.build("HCL AppScan Static Analyzer", "SA2813462719", rawSarif, repoDir);
    assertThat(ruleSarif.isPresent(), is(true));
    AppScanModule module =
        new AppScanModule(List.of(AppScanSarifTestCodemod.class), List.of(ruleSarif.get()));
    Injector injector = Guice.createInjector(module);
    AppScanSarifTestCodemod instance = injector.getInstance(AppScanSarifTestCodemod.class);
    assertThat(instance, is(notNullValue()));
    assertThat(instance.ruleSarif, is(notNullValue()));
  }
}
