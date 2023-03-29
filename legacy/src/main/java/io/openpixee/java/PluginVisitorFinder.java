package io.openpixee.java;

import com.contrastsecurity.sarif.Run;
import com.contrastsecurity.sarif.SarifSchema210;
import com.contrastsecurity.sarif.ToolComponent;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.codemodder.RuleContext;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** This type is responsible for gathering the factories from all the plugins. */
final class PluginVisitorFinder {

  private final List<SarifSchema210> sarifs;

  PluginVisitorFinder(final List<File> sarifFiles) {
    this.sarifs = processSARIFFiles(sarifFiles);
  }

  private Optional<SarifSchema210> readSarifFile(final File sarifFile) {
    try {
      return Optional.of(
          new ObjectMapper().readValue(new FileReader(sarifFile), SarifSchema210.class));
    } catch (final IOException e) {
      LOG.error("Problem deserializing SARIF file: {}", sarifFile, e);
      return Optional.empty();
    }
  }

  private List<SarifSchema210> processSARIFFiles(final List<File> sarifFiles) {
    return sarifFiles.stream()
        .flatMap(f -> readSarifFile(f).stream())
        .collect(Collectors.toUnmodifiableList());
  }

  List<VisitorFactory> getPluginFactories(
      final File repositoryRoot,
      final RuleContext ruleContext,
      final List<SarifProcessorPlugin> sarifProcessorPlugins) {
    final Function<Run, Stream<VisitorFactory>> runToFactories =
        run -> {
          final ToolComponent tool = run.getTool().getDriver();
          LOG.debug(
              "Processing SARIF file (name={}, product={}, org={}, version={}",
              tool.getName(),
              tool.getProduct(),
              tool.getOrganization(),
              tool.getVersion());
          return sarifProcessorPlugins.stream()
              .flatMap(
                  plugin ->
                      plugin.getJavaVisitorFactoriesFor(repositoryRoot, run, ruleContext).stream());
        };

    return sarifs.stream()
        .flatMap(sarif -> sarif.getRuns().stream())
        .flatMap(runToFactories)
        .collect(Collectors.toList());
  }

  List<FileBasedVisitor> getPluginFileBasedVisitors(
      final File repositoryRoot,
      final RuleContext ruleContext,
      final List<SarifProcessorPlugin> sarifProcessorPlugins) {
    final Function<Run, Stream<FileBasedVisitor>> runToVisitors =
        run -> {
          final ToolComponent tool = run.getTool().getDriver();
          LOG.debug(
              "Processing SARIF file (name={}, product={}, org={}, version={}",
              tool.getName(),
              tool.getProduct(),
              tool.getOrganization(),
              tool.getVersion());
          return sarifProcessorPlugins.stream()
              .flatMap(
                  plugin -> plugin.getFileWeaversFor(repositoryRoot, run, ruleContext).stream());
        };

    return sarifs.stream()
        .flatMap(sarif -> sarif.getRuns().stream())
        .flatMap(runToVisitors)
        .collect(Collectors.toList());
  }

  private static final Logger LOG = LoggerFactory.getLogger(PluginVisitorFinder.class);
}
