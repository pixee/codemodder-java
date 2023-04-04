package io.openpixee.java;

import io.codemodder.RuleContext;
import io.openpixee.java.plugins.codeql.CodeQlPlugin;
import io.openpixee.java.plugins.contrast.ContrastScanPlugin;
import io.openpixee.java.protections.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Assembles a list of {@link VisitorFactory} we'll need to weave based on configuration. */
public interface VisitorAssembler {

  /**
   * Given the context, assemble a list of {@link VisitorFactory} we'll use in our Java code
   * weaving.
   *
   * @param repositoryRoot the root directory of the repository we're weaving
   * @param ruleContext the rules
   * @param sarifs the SARIF files
   * @return the {@link VisitorFactory} types that are allowed to operate
   */
  List<VisitorFactory> assembleJavaCodeScanningVisitorFactories(
      File repositoryRoot, RuleContext ruleContext, List<File> sarifs);

  /**
   * Given the context, assemble of a list of {@link FileBasedVisitor} we'll use in our non-Java
   * code weaving.
   *
   * @param ruleContext the rules
   * @return the {@link FileBasedVisitor} types that are allowed to operate
   */
  List<FileBasedVisitor> assembleFileVisitors(
      File repositoryRoot, RuleContext ruleContext, List<File> sarifs);

  static VisitorAssembler createDefault() {
    return new Default();
  }

  class Default implements VisitorAssembler {

    @Override
    public List<VisitorFactory> assembleJavaCodeScanningVisitorFactories(
        final File repositoryRoot, final RuleContext ruleContext, final List<File> sarifs) {
      final List<VisitorFactory> defaultVisitorFactories =
          List.of(
              new JakartaForwardVisitoryFactory(),
              new SSLContextGetInstanceVisitorFactory(),
              new SSLEngineSetEnabledProtocolsVisitorFactory(),
              new SSLParametersSetProtocolsVisitorFactory(),
              new SSLSocketSetEnabledProtocolsVisitorFactory(),
              new XStreamVisitorFactory(),
              new ZipFileOverwriteVisitoryFactory());

      final List<SarifProcessorPlugin> sarifProcessorPlugins =
          List.of(new CodeQlPlugin(), new ContrastScanPlugin());

      final List<VisitorFactory> pluginFactories =
          new PluginVisitorFinder(sarifs)
              .getPluginFactories(repositoryRoot, ruleContext, sarifProcessorPlugins);

      final List<VisitorFactory> factories = new ArrayList<>();
      factories.addAll(defaultVisitorFactories);
      factories.addAll(pluginFactories);

      LOG.debug("Factories available: {}", factories.size());
      factories.removeIf(factory -> !ruleContext.isRuleAllowed(factory.ruleId()));
      LOG.debug("Factories after removing disallowed: {}", factories.size());
      return Collections.unmodifiableList(factories);
    }

    @Override
    public List<FileBasedVisitor> assembleFileVisitors(
        final File repositoryRoot, final RuleContext ruleContext, final List<File> sarifs) {
      // Plugin visitors
      final List<SarifProcessorPlugin> sarifProcessorPlugins =
          List.of(new CodeQlPlugin(), new ContrastScanPlugin());
      final List<FileBasedVisitor> pluginVisitors =
          new PluginVisitorFinder(sarifs)
              .getPluginFileBasedVisitors(repositoryRoot, ruleContext, sarifProcessorPlugins);

      // Default visitors -- make sure DependencyInjectingVisitor is last. If other visitors come
      // after it, they won't
      // have a chance to inject their dependencies.
      final List<FileBasedVisitor> defaultVisitors = new ArrayList<>();
      defaultVisitors.add(new DependencyInjectingVisitor());
      defaultVisitors.removeIf(visitor -> !ruleContext.isRuleAllowed(visitor.ruleId()));

      final var allVisitors =
          Stream.concat(pluginVisitors.stream(), defaultVisitors.stream())
              .collect(Collectors.toList());
      allVisitors.removeIf(visitor -> !ruleContext.isRuleAllowed(visitor.ruleId()));

      return Collections.unmodifiableList(allVisitors);
    }
  }

  Logger LOG = LoggerFactory.getLogger(VisitorAssembler.class);
}
