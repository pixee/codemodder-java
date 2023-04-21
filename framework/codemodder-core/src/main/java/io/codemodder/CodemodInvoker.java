package io.codemodder;

import com.github.javaparser.ast.CompilationUnit;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import io.codemodder.javaparser.JavaParserChanger;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.codehaus.plexus.util.StringUtils;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * This is the entry point for codemod authors to invoke their codemods through our framework. Every
 * codemod should create a codemod then create a separate type with a main() method that invokes
 * this entry point.
 */
public final class CodemodInvoker {

  private final List<Changer> codemods;
  private final Path repositoryDir;
  private final List<IdentifiedChanger> changers;

  public CodemodInvoker(
      final List<Class<? extends Changer>> codemodTypes, final Path repositoryDir) {
    this(
        codemodTypes,
        CodemodRegulator.of(DefaultRuleSetting.ENABLED, List.of()),
        repositoryDir,
        Map.of());
  }

  public CodemodInvoker(
      final List<Class<? extends Changer>> codemodTypes,
      final Path repositoryDir,
      final Map<String, List<RuleSarif>> ruleSarifByTool) {
    this(
        codemodTypes,
        CodemodRegulator.of(DefaultRuleSetting.ENABLED, List.of()),
        repositoryDir,
        ruleSarifByTool);
  }

  public CodemodInvoker(
      final List<Class<? extends Changer>> codemodTypes,
      final CodemodRegulator codemodRegulator,
      final Path repositoryDir) {
    this(codemodTypes, codemodRegulator, repositoryDir, Map.of());
  }

  public CodemodInvoker(
      final List<Class<? extends Changer>> codemodTypes,
      final CodemodRegulator codemodRegulator,
      final Path repositoryDir,
      final Map<String, List<RuleSarif>> ruleSarifByTool) {

    // get all the providers ready for dependency injection & codemod instantiation
    final List<CodemodProvider> providers =
        ServiceLoader.load(CodemodProvider.class).stream()
            .map(ServiceLoader.Provider::get)
            .collect(Collectors.toUnmodifiableList());
    final Set<AbstractModule> allModules = new HashSet<>();

    // add default module
    allModules.add(new CodeDirectoryModule(repositoryDir));
    allModules.add(new XPathStreamProcessorModule());

    // add all provider modules
    for (final CodemodProvider provider : providers) {
      final List<String> wantsSarif = getWantsSarif(provider);
      final var allWantedSarifs =
          wantsSarif.stream()
              .flatMap(toolName -> ruleSarifByTool.getOrDefault(toolName, List.of()).stream())
              .collect(Collectors.toUnmodifiableList());
      final Set<AbstractModule> modules =
          provider.getModules(repositoryDir, codemodTypes, allWantedSarifs);
      allModules.addAll(modules);
    }

    // record which changers are associated with which codemod ids
    final List<IdentifiedChanger> changers = new ArrayList<>();

    // validate and instantiate the codemods
    final Injector injector = Guice.createInjector(allModules);
    final Set<String> codemodIds = new HashSet<>();
    final List<Changer> codemods = new ArrayList<>();
    for (final Class<? extends Changer> type : codemodTypes) {
      final Codemod codemodAnnotation = type.getAnnotation(Codemod.class);
      validateRequiredFields(codemodAnnotation);
      final Changer changer = injector.getInstance(type);
      final String codemodId = codemodAnnotation.id();
      if (codemodIds.contains(codemodId)) {
        throw new UnsupportedOperationException("multiple codemods under id: " + codemodId);
      }
      codemodIds.add(codemodId);
      if (codemodRegulator.isAllowed(codemodId)) {
        codemods.add(changer);
        changers.add(new IdentifiedChanger(codemodId, changer));
      }
    }

    this.changers = Collections.unmodifiableList(changers);
    this.codemods = Collections.unmodifiableList(codemods);
    this.repositoryDir = Objects.requireNonNull(repositoryDir);
  }

  private static List<String> getWantsSarif(final CodemodProvider provider) {
    return Optional.ofNullable(provider.getClass().getAnnotation(WantsSarif.class))
        .map(wants -> Arrays.asList(wants.toolNames()))
        .orElse(List.of());
  }

  private static final class IdentifiedChanger {
    final String id;
    final Changer changer;

    private IdentifiedChanger(final String id, final Changer changer) {
      this.id = id;
      this.changer = changer;
    }
  }

  /**
   * Run the codemods we've collected on the given file.
   *
   * @param cu the parsed JavaParser representation of the file
   * @param context a model we should keep updating as we process the file
   */
  public void execute(final Path path, final CompilationUnit cu, final FileWeavingContext context) {
    final List<JavaParserChanger> javaParserChangers =
        codemods.stream()
            .filter(changer -> changer instanceof JavaParserChanger)
            .map(changer -> (JavaParserChanger) changer)
            .collect(Collectors.toUnmodifiableList());
    for (final JavaParserChanger changer : javaParserChangers) {
      final CodemodInvocationContext invocationContext =
          new DefaultCodemodInvocationContext(
              new DefaultCodeDirectory(repositoryDir),
              path,
              changers.stream().filter(ic -> ic.changer == changer).findFirst().orElseThrow().id,
              context);
      changer.visit(invocationContext, cu);
    }
  }

  /**
   * Run the codemods we've collected on the given file.
   *
   * @param path the path to a file
   * @param context a model we should keep updating as we process the file
   * @return the modified file, if changed at all
   */
  public Optional<ChangedFile> executeFile(final Path path, final FileWeavingContext context)
      throws IOException {
    final List<RawFileChanger> rawFileChangers =
        codemods.stream()
            .filter(changer -> changer instanceof RawFileChanger)
            .map(changer -> (RawFileChanger) changer)
            .collect(Collectors.toUnmodifiableList());

    final Path originalFileCopy = Files.createTempFile("codemodder-raw", ".original");
    Files.copy(path, originalFileCopy, StandardCopyOption.REPLACE_EXISTING);

    for (final RawFileChanger changer : rawFileChangers) {
      final CodemodInvocationContext invocationContext =
          new DefaultCodemodInvocationContext(
              new DefaultCodeDirectory(repositoryDir),
              path,
              changers.stream().filter(ic -> ic.changer == changer).findFirst().orElseThrow().id,
              context);
      changer.visitFile(invocationContext);
    }

    final List<Weave> weaves = context.weaves();
    if (weaves.isEmpty()) {
      Files.delete(originalFileCopy);
      return Optional.empty();
    }

    // copy the file that's now been modified by multiple codemods to a temp file
    final Path modifiedFileCopy = Files.createTempFile("codemodder-raw", ".modified");
    Files.copy(path, modifiedFileCopy, StandardCopyOption.REPLACE_EXISTING);

    // restore the original file
    Files.copy(originalFileCopy, path, StandardCopyOption.REPLACE_EXISTING);

    return Optional.of(
        ChangedFile.createDefault(path.toString(), modifiedFileCopy.toString(), weaves));
  }

  private static void validateRequiredFields(final Codemod codemodAnnotation) {
    final String author = codemodAnnotation.author();
    if (StringUtils.isBlank(author)) {
      throw new IllegalArgumentException("must have an author");
    }

    final String id = codemodAnnotation.id();
    if (!isValidCodemodId(id)) {
      throw new IllegalArgumentException("must have valid codemod id");
    }

    final ReviewGuidance reviewGuidance = codemodAnnotation.reviewGuidance();
    if (reviewGuidance == null) {
      throw new IllegalArgumentException("must have review guidance");
    }
  }

  @VisibleForTesting
  static boolean isValidCodemodId(final String codemodId) {
    return codemodIdPattern.matcher(codemodId).matches();
  }

  private static final Pattern codemodIdPattern =
      Pattern.compile("^([A-Za-z0-9]+):([A-Za-z0-9]+)\\/([A-Za-z0-9\\-]+)$");
}
