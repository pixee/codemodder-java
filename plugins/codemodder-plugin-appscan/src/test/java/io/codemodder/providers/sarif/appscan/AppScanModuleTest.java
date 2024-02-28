package io.codemodder.providers.sarif.appscan;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import com.contrastsecurity.sarif.Region;
import com.google.inject.Guice;
import com.google.inject.Injector;
import io.codemodder.*;
import io.codemodder.codetf.CodeTFReference;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import javax.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class AppScanModuleTest {

  private Path repoDir;

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

  @BeforeEach
  void setup(@TempDir final Path tmpDir) {
    AppScanRuleSarifFactory factory = new AppScanRuleSarifFactory();
    factory.build("appscan", "SA2813462719", null, null);
    this.repoDir = tmpDir;
  }

  @Test
  void it_works_with_appscan_sarif() throws IOException {
    String javaCode = "class Foo { \n Object a = new Thing(); \n }";

    Path javaFile = Files.createTempFile(repoDir, "HasThing", ".java");
    Files.writeString(javaFile, javaCode, StandardOpenOption.TRUNCATE_EXISTING);
    AppScanModule module = createModule(List.of(AppScanSarifTestCodemod.class));
    Injector injector = Guice.createInjector(module);
    AppScanSarifTestCodemod instance = injector.getInstance(AppScanSarifTestCodemod.class);

    RuleSarif ruleSarif = instance.ruleSarif;
    assertThat(ruleSarif, is(notNullValue()));
    List<Region> regions = ruleSarif.getRegionsFromResultsByRule(javaFile);
    assertThat(regions.size(), is(1));
  }

  private AppScanModule createModule(final List<Class<? extends CodeChanger>> codemodTypes) {
    return new AppScanModule(codemodTypes, List.of());
  }
}
