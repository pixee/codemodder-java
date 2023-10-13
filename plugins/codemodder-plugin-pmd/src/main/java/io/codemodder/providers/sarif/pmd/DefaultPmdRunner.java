package io.codemodder.providers.sarif.pmd;

import com.contrastsecurity.sarif.SarifSchema210;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import net.sourceforge.pmd.PMDConfiguration;
import net.sourceforge.pmd.PmdAnalysis;
import net.sourceforge.pmd.RulePriority;
import net.sourceforge.pmd.lang.LanguageRegistry;
import net.sourceforge.pmd.renderers.SarifRenderer;
import net.sourceforge.pmd.util.log.MessageReporter;

final class DefaultPmdRunner implements PmdRunner {

  private final ObjectMapper objectMapper;

  DefaultPmdRunner() {
    this.objectMapper = new ObjectMapper();
  }

  @Override
  public SarifSchema210 run(
      final List<String> ruleIds, final Path projectDir, final List<Path> includedFiles) {
    // configure the PMD run
    PMDConfiguration config = new PMDConfiguration();
    config.setDefaultLanguageVersion(LanguageRegistry.PMD.getLanguageVersionById("java", null));
    config.setMinimumPriority(RulePriority.LOW);
    config.setReportFormat(SarifRenderer.NAME);
    config.setReporter(MessageReporter.quiet());

    // create the XML that configures the rules to run based on what codemods need
    String rulesXmlFormat =
        """
        <?xml version="1.0"?>
        <ruleset name="quickstart"
                 xmlns="http://pmd.sourceforge.net/ruleset/2.0.0"
                 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                 xsi:schemaLocation="http://pmd.sourceforge.net/ruleset/2.0.0 https://pmd.sourceforge.io/ruleset_2_0_0.xsd">
            <description>The PMD rules needed for this codemodder run</description>

            %s

        </ruleset>
        """;

    String ruleEntryFormat = "<rule ref=\"%s\"/>";

    String ruleXmlEntries =
        ruleIds.stream().map(ruleEntryFormat::formatted).collect(Collectors.joining("\n"));
    String rulesXml = rulesXmlFormat.formatted(ruleXmlEntries);

    try {
      // write the XML file containing the rules
      Path rulesXmlFile = Files.createTempFile("pmd-rules", ".xml");
      Files.writeString(rulesXmlFile, rulesXml);
      config.addRuleSet(rulesXmlFile.toAbsolutePath().toString());

      // create the SARIF file for PMD to write
      Path sarifFile = Files.createTempFile("pmd", ".sarif");
      config.setReportFile(sarifFile);

      // calculate the source directories for PMD to scan (only looks for src/main/java now)
      includedFiles.forEach(config::addInputPath);

      // run the analysis
      try (PmdAnalysis pmd = PmdAnalysis.create(config)) {
        pmd.performAnalysis();
      }

      // capture the sarif
      return objectMapper.readValue(sarifFile.toFile(), SarifSchema210.class);
    } catch (IOException e) {
      throw new UncheckedIOException("pmd scan failed", e);
    }
  }
}
