package io.codemodder.providers.sarif.semgrep;

import io.codemodder.CodeChanger;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.Optional;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.VisibleForTesting;

final class DefaultSemgrepRuleFactory implements SemgrepRuleFactory {

  @Override
  public SemgrepRule createRule(
      final Class<? extends CodeChanger> codemodType,
      final SemgrepScan semgrepScan,
      final String packageName) {

    String yamlPath = semgrepScan.pathToYaml();
    String declaredRuleId = semgrepScan.ruleId();
    Path yamlPathToWrite = null;
    boolean foundYaml = false;

    if (!declaredRuleId.isEmpty()) {
      String classpathYamlPath =
          "/" + packageName.replace(".", "/") + "/" + declaredRuleId + ".yaml";

      if (!"".equals(yamlPath)) {
        classpathYamlPath = yamlPath;
      }
      Optional<Path> path = saveClasspathResourceToTemp(codemodType, classpathYamlPath);
      if (path.isPresent()) {
        foundYaml = true;
        yamlPathToWrite = path.get();
      }
    }
    String inlineYaml = semgrepScan.yaml();
    if (!"".equals(inlineYaml)) {
      if (foundYaml) {
        throw new IllegalArgumentException(
            "Cannot specify both inline yaml and yaml file path: " + codemodType.getName());
      }
      foundYaml = true;
      yamlPathToWrite = saveStringToTemp(inlineYaml);
    }

    if (!foundYaml) {
      throw new IllegalArgumentException("no semgrep yaml found for: " + codemodType.getName());
    }

    try {
      if (StringUtils.isEmpty(declaredRuleId)) {
        String rawYaml = Files.readString(yamlPathToWrite);
        declaredRuleId = detectSingleRuleFromYaml(rawYaml);
      }
    } catch (IOException e) {
      throw new UncheckedIOException("Problem inspecting yaml: " + codemodType.getName(), e);
    }

    try {
      addMissingPropertiesIfNeeded(yamlPathToWrite);
    } catch (IOException e) {
      throw new UncheckedIOException("Problem fixing up yaml: " + codemodType.getName(), e);
    }

    return new SemgrepRule(semgrepScan, declaredRuleId, yamlPathToWrite);
  }

  /**
   * Fix up the yaml and add missing "message", "languages" and "severity" properties if they aren't
   * there. This makes rule writing easier.
   */
  private void addMissingPropertiesIfNeeded(Path yamlPathToWrite) throws IOException {
    // add missing properties to the yaml
    boolean changed = false;
    String yamlAsString = Files.readString(yamlPathToWrite);
    if (!yamlAsString.contains("message:")) {
      changed = true;
      yamlAsString += "\n    message: Semgrep found a match\n";
    }
    if (!yamlAsString.contains("severity:")) {
      changed = true;
      yamlAsString += "\n    severity: WARNING\n";
    }
    if (!yamlAsString.contains("languages:")) {
      changed = true;
      yamlAsString += "\n    languages:\n      - java\n";
    }
    if (changed) {
      Files.writeString(yamlPathToWrite, yamlAsString, StandardOpenOption.TRUNCATE_EXISTING);
    }
  }

  /** Save the YAML string given to a temporary file. */
  private Path saveStringToTemp(final String yamlAsString) {
    try {
      Path file = Files.createTempFile("semgrep", ".yaml");
      Files.writeString(file, yamlAsString);
      return file;
    } catch (IOException e) {
      throw new UncheckedIOException("Problem saving yaml string to temp", e);
    }
  }

  /**
   * Turn the yaml resource in the classpath into a file accessible via {@link Path}. Forgive the
   * exception re-throwing but this is being used from a lambda and this shouldn't fail ever anyway.
   */
  private Optional<Path> saveClasspathResourceToTemp(
      final Class<?> codemodType, final String yamlClasspathResourcePath) {
    InputStream ruleInputStream = codemodType.getResourceAsStream(yamlClasspathResourcePath);
    if (ruleInputStream == null) {
      return Optional.empty();
    }
    try {
      Path semgrepRuleFile = Files.createTempFile("semgrep", ".yaml");
      Objects.requireNonNull(ruleInputStream);
      Files.copy(ruleInputStream, semgrepRuleFile, StandardCopyOption.REPLACE_EXISTING);
      ruleInputStream.close();
      return Optional.of(semgrepRuleFile);
    } catch (IOException e) {
      throw new UncheckedIOException("Problem reading/copying semgrep yaml from classpath", e);
    } finally {
      IOUtils.closeQuietly(ruleInputStream);
    }
  }

  @VisibleForTesting
  static String detectSingleRuleFromYaml(final String rawYaml) {
    String ruleIdStartToken = "- id:";
    int count = StringUtils.countMatches(rawYaml, ruleIdStartToken);
    if (count > 1) {
      throw new IllegalArgumentException(
          "Multiple rules found in yaml, must specify rule single rule id if implicit");
    } else if (count == 0) {
      throw new IllegalArgumentException(
          "No rules found in yaml, must specify rule single rule id if implicit");
    }
    int start = rawYaml.indexOf(ruleIdStartToken);
    int end = rawYaml.indexOf("\n", start);
    return rawYaml.substring(start + ruleIdStartToken.length(), end).trim();
  }
}
