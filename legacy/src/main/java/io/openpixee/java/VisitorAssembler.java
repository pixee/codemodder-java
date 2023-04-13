package io.openpixee.java;

import io.codemodder.CodemodRegulator;
import io.openpixee.java.plugins.codeql.CodeQlPlugin;
import io.openpixee.java.plugins.contrast.ContrastScanPlugin;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Assembles a list of {@link VisitorFactory} we'll need to weave based on configuration. */
public interface VisitorAssembler {

  /**
   * Given the context, assemble a list of {@link VisitorFactory} we'll use in our Java code
   * weaving.
   *
   * @param repositoryRoot the root directory of the repository we're weaving
   * @param codemodRegulator the rules
   * @param sarifs the SARIF files
   * @return the {@link VisitorFactory} types that are allowed to operate
   */
  List<VisitorFactory> assembleJavaCodeScanningVisitorFactories(
      File repositoryRoot, CodemodRegulator codemodRegulator, List<File> sarifs);

  static VisitorAssembler createDefault() {
    return new Default();
  }

  class Default implements VisitorAssembler {

    @Override
    public List<VisitorFactory> assembleJavaCodeScanningVisitorFactories(
        final File repositoryRoot,
        final CodemodRegulator codemodRegulator,
        final List<File> sarifs) {
      final List<VisitorFactory> defaultVisitorFactories = List.of();

      final List<SarifProcessorPlugin> sarifProcessorPlugins =
          List.of(new CodeQlPlugin(), new ContrastScanPlugin());

      final List<VisitorFactory> pluginFactories =
          new PluginVisitorFinder(sarifs)
              .getPluginFactories(repositoryRoot, codemodRegulator, sarifProcessorPlugins);

      final List<VisitorFactory> factories = new ArrayList<>();
      factories.addAll(defaultVisitorFactories);
      factories.addAll(pluginFactories);

      LOG.debug("Factories available: {}", factories.size());
      factories.removeIf(factory -> !codemodRegulator.isAllowed(factory.ruleId()));
      LOG.debug("Factories after removing disallowed: {}", factories.size());
      return Collections.unmodifiableList(factories);
    }
  }

  Logger LOG = LoggerFactory.getLogger(VisitorAssembler.class);
}
