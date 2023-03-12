package io.codemodder.providers.sarif.semgrep;

import com.contrastsecurity.sarif.SarifSchema210;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.IOUtils;

/** This is the memoizing Semgrep runner that we'll give to codemods. */
final class DefaultSemgrepSarifProvider implements SemgrepSarifProvider {

  private final Map<String, SarifSchema210> sarifs;

  DefaultSemgrepSarifProvider() {
    this.sarifs = new HashMap<>();
  }

  @Override
  public SarifSchema210 getSarif(final Path repository, final String rulePath) throws IOException {
    if (sarifs.containsKey(rulePath)) {
      return sarifs.get(rulePath);
    }

    InputStream ruleInputStream = getClass().getClassLoader().getResource(rulePath).openStream();
    String ruleYaml = IOUtils.toString(ruleInputStream, StandardCharsets.UTF_8);
    ruleInputStream.close();

    Path semgrepRuleFile = Files.createTempFile("semgrep", ".yaml");
    Files.write(semgrepRuleFile, ruleYaml.getBytes(StandardCharsets.UTF_8));

    SarifSchema210 sarif =
        new DefaultSemgrepRunner().runWithSingleRule(semgrepRuleFile, repository);
    sarifs.put(rulePath, sarif);
    return sarif;
  }
}
