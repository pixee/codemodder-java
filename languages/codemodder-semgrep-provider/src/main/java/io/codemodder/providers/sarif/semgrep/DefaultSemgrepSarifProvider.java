package io.codemodder.providers.sarif.semgrep;

import com.contrastsecurity.sarif.SarifSchema210;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/** This is the memoizing Semgrep runner that we'll give to codemods. */
final class DefaultSemgrepSarifProvider implements SemgrepSarifProvider {

  private final Map<String, SarifSchema210> sarifs;

  DefaultSemgrepSarifProvider() {
    this.sarifs = new HashMap<>();
  }

  @Override
  public SarifSchema210 getSarif(final Path repository, final String rulePath)
      throws IOException, URISyntaxException {
    if (sarifs.containsKey(rulePath)) {
      return sarifs.get(rulePath);
    }

    String ruleYaml =
        Files.readString(Paths.get(getClass().getClassLoader().getResource(rulePath).toURI()));
    Path semgrepRuleFile = Files.createTempFile("semgrep", ".yaml");
    Files.write(semgrepRuleFile, ruleYaml.getBytes(StandardCharsets.UTF_8));

    SarifSchema210 sarif =
        new DefaultSemgrepRunner().runWithSingleRule(semgrepRuleFile, repository);
    sarifs.put(rulePath, sarif);
    return sarif;
  }
}
