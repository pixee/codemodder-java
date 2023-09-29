package io.codemodder.plugins.jpms;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithName;
import io.codemodder.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Provides Java module management functions to codemods. */
public final class JavaModuleProvider implements ProjectProvider {

  private final ModuleInfoUpdater moduleInfoUpdater;

  public JavaModuleProvider() {
    this(new DefaultModuleInfoUpdater());
  }

  private JavaModuleProvider(final ModuleInfoUpdater moduleInfoUpdater) {
    this.moduleInfoUpdater = moduleInfoUpdater;
  }

  @Override
  public DependencyUpdateResult updateDependencies(
      final Path projectDir, final Path file, final List<DependencyGAV> remainingFileDependencies)
      throws IOException {

    Optional<Path> moduleInfoJava = findModuleInfoJava(projectDir, file);

    DependencyUpdateResult result = DependencyUpdateResult.EMPTY_UPDATE;
    if (moduleInfoJava.isEmpty()) {
      return result;
    }

    if (remainingFileDependencies.stream()
        .allMatch(dependency -> dependency.moduleName().isEmpty())) {
      LOG.debug("No dependencies with module names. Can't inject JPMS provider.");
      return result;
    }

    try {
      result =
          moduleInfoUpdater.update(
              projectDir, moduleInfoJava.get(), file, remainingFileDependencies);
    } catch (IOException e) {
      LOG.error("Problem updating module-info.java", e);
    }
    return result;
  }

  /** From the given file, find the module-info.java file that should be updated, if any exist. */
  private Optional<Path> findModuleInfoJava(final Path projectDir, final Path file)
      throws IOException {
    // start with the parent of the file we're changing
    Path parent = file;
    while (parent != null && !Files.isSameFile(parent, projectDir)) {
      // if we're in a src/main/java dir, check for the presence of `module-info.java`
      Path moduleInfoJava = parent.resolve("module-info.java");
      if (Files.exists(moduleInfoJava) && Files.isRegularFile(moduleInfoJava)) {
        return Optional.of(moduleInfoJava);
      }
      parent = parent.getParent();
    }

    // let's read the package of the file we're trying to change and see if we can find the
    // secondary dir location
    Optional<String> packageName = readPackageNameFromFile(file);
    if (packageName.isEmpty()) {
      return Optional.empty();
    }

    parent = file;
    while (parent != null && !Files.isSameFile(parent, projectDir)) {
      String name = parent.getFileName().toString();
      // if we're in a src/main/java dir, check for the presence of `module-info.java`
      if ("java".equals(name) || "src".equals(name)) {
        List<Path> packageDirs =
            Files.list(parent)
                .filter(Files::isDirectory)
                .filter(
                    path -> path.getFileName().toString().matches("[a-zA-Z0-9]+\\.[a-zA-Z0-9]+"))
                .toList();
        for (Path packageDir : packageDirs) {
          String packageDirName = packageDir.getFileName().toString();
          if (packageName.get().startsWith(packageDirName)) {
            Path moduleInfoJava = packageDir.resolve("module-info.java");
            if (Files.exists(moduleInfoJava) && Files.isRegularFile(moduleInfoJava)) {
              return Optional.of(moduleInfoJava);
            }
          }
        }
      }
      parent = parent.getParent();
    }

    return Optional.empty();
  }

  private Optional<String> readPackageNameFromFile(final Path javaFile) throws IOException {
    JavaParser javaParser = new JavaParser();
    ParserConfiguration parserConfiguration = javaParser.getParserConfiguration();
    parserConfiguration.setLanguageLevel(ParserConfiguration.LanguageLevel.BLEEDING_EDGE);
    ParseResult<CompilationUnit> parseResult = javaParser.parse(javaFile);
    if (!parseResult.isSuccessful()) {
      LOG.warn("Couldn't parse java file to inspect package");
      return Optional.empty();
    }
    Optional<CompilationUnit> result = parseResult.getResult();
    if (result.isEmpty()) {
      LOG.warn("Couldn't find compilation unit to inspect package");
      return Optional.empty();
    }
    CompilationUnit compilationUnit = result.get();
    Optional<PackageDeclaration> packageDeclaration = compilationUnit.getPackageDeclaration();
    return packageDeclaration.map(NodeWithName::getNameAsString);
  }

  private static final Logger LOG = LoggerFactory.getLogger(JavaModuleProvider.class);
}
