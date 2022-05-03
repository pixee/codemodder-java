package io.pixee.codefixer.java;

import com.contrastsecurity.sarif.Run;
import com.contrastsecurity.sarif.SarifSchema210;
import com.contrastsecurity.sarif.ToolComponent;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** This type is responsible for gathering the factories from all the plugins. */
final class PluginFactoryFinder {

  /**
   * Given the SARIF files fed into our tool and the given plugins, generate the factories they'll
   * use to update the code.
   */
  List<VisitorFactory> getPluginFactories(
      final File repositoryRoot,
      final RuleContext ruleContext,
      final List<File> sarifs,
      final List<SarifProcessorPlugin> sarifProcessorPlugins) {
    List<VisitorFactory> factories = new ArrayList<>();
    for (File sarifFile : sarifs) {
      try {
        SarifSchema210 sarif =
            new ObjectMapper().readValue(new FileReader(sarifFile), SarifSchema210.class);
        for (Run run : sarif.getRuns()) {
          ToolComponent tool = run.getTool().getDriver();
          LOG.info(
              "Processing SARIF file (name={}, product={}, org={}, version={}",
              tool.getName(),
              tool.getProduct(),
              tool.getOrganization(),
              tool.getVersion());
          for (SarifProcessorPlugin plugin : sarifProcessorPlugins) {
            factories.addAll(plugin.getJavaVisitorFactoriesFor(repositoryRoot, run, ruleContext));
          }
        }
      } catch (IOException e) {
        LOG.error("Problem deserializing SARIF file: {}", sarifFile, e);
      }
    }
    return Collections.unmodifiableList(factories);
  }

  List<VisitorFactoryNg> getPluginFactoriesNg(
          final File repositoryRoot,
          final RuleContext ruleContext,
          final List<File> sarifs,
          final List<SarifProcessorPlugin> sarifProcessorPlugins) {
    List<VisitorFactoryNg> factories = new ArrayList<>();
    for (File sarifFile : sarifs) {
      try {
        SarifSchema210 sarif =
                new ObjectMapper().readValue(new FileReader(sarifFile), SarifSchema210.class);
        for (Run run : sarif.getRuns()) {
          ToolComponent tool = run.getTool().getDriver();
          LOG.info(
                  "Processing SARIF file (name={}, product={}, org={}, version={}",
                  tool.getName(),
                  tool.getProduct(),
                  tool.getOrganization(),
                  tool.getVersion());
          for (SarifProcessorPlugin plugin : sarifProcessorPlugins) {
            factories.addAll(plugin.getJavaVisitorFactoriesForNg(repositoryRoot, run, ruleContext));
          }
        }
      } catch (IOException e) {
        LOG.error("Problem deserializing SARIF file: {}", sarifFile, e);
      }
    }
    return Collections.unmodifiableList(factories);
  }

  private static final Logger LOG = LogManager.getLogger(PluginFactoryFinder.class);
}
