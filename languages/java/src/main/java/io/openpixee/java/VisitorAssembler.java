package io.openpixee.java;

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
      List<VisitorFactory> defaultVisitorFactories =
          List.of(
              new ApacheMultipartVisitorFactory(),
              new DeserializationVisitorFactory(),
              new HeaderInjectionVisitorFactory(),
              new JakartaForwardVisitoryFactory(),
              new SQLParameterizerVisitorFactory(),
              new SSLContextGetInstanceVisitorFactory(),
              new SSLEngineSetEnabledProtocolsVisitorFactory(),
              new SSLParametersSetProtocolsVisitorFactory(),
              new SSLSocketSetEnabledProtocolsVisitorFactory(),
              new SSRFVisitorFactory(),
              new PredictableSeedVisitorFactory(),
              new RuntimeExecVisitorFactory(),
              new SpringMultipartVisitorFactory(),
              new UnsafeReadlineVisitorFactory(),
              new WeakPRNGVisitorFactory(),
              new XMLDecoderVisitorFactory(),
              new XStreamVisitorFactory(),
              new XXEVisitorFactory(),
              new ZipFileOverwriteVisitoryFactory());

      final List<SarifProcessorPlugin> sarifProcessorPlugins =
          List.of(new CodeQlPlugin(), new ContrastScanPlugin());

      List<VisitorFactory> pluginFactories =
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
      List<FileBasedVisitor> pluginVisitors =
          new PluginVisitorFinder(sarifs)
              .getPluginFileBasedVisitors(repositoryRoot, ruleContext, sarifProcessorPlugins);
      pluginVisitors.removeIf(visitor -> !ruleContext.isRuleAllowed(visitor.ruleId()));

      // Default visitors
      List<FileBasedVisitor> defaultVisitors = new ArrayList<>();
      defaultVisitors.add(new DependencyInjectingVisitor());
      defaultVisitors.add(new JspScriptletXSSVisitor());
      defaultVisitors.add(new VerbTamperingVisitor());
      defaultVisitors.removeIf(visitor -> !ruleContext.isRuleAllowed(visitor.ruleId()));

      return Stream.concat(defaultVisitors.stream(), pluginVisitors.stream())
          .collect(Collectors.toUnmodifiableList());
    }
  }

  Logger LOG = LoggerFactory.getLogger(VisitorAssembler.class);
}
