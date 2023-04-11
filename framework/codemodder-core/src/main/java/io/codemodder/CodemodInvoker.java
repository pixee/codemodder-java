package io.codemodder;

import com.github.javaparser.ast.CompilationUnit;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.codehaus.plexus.util.StringUtils;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        RuleContext.of(DefaultRuleSetting.ENABLED, List.of()),
        repositoryDir,
        Map.of());
  }

  public CodemodInvoker(
      final List<Class<? extends Changer>> codemodTypes,
      final RuleContext ruleContext,
      final Path repositoryDir) {
    this(codemodTypes, ruleContext, repositoryDir, Map.of());
  }

  public CodemodInvoker(
      final List<Class<? extends Changer>> codemodTypes,
      final RuleContext ruleContext,
      final Path repositoryDir,
      final Map<String, List<RuleSarif>> ruleSarifByTool) {

    // get all the providers ready for dependency injection & codemod instantiation
    List<CodemodProvider> providers =
        ServiceLoader.load(CodemodProvider.class).stream()
            .map(ServiceLoader.Provider::get)
            .collect(Collectors.toUnmodifiableList());
    Set<AbstractModule> allModules = new HashSet<>();

    // add default module
    allModules.add(new CodeDirectoryModule(repositoryDir));
    allModules.add(new XPathStreamProcessorModule());

    // add all provider modules
    for (CodemodProvider provider : providers) {
      List<String> wantsSarif = getWantsSarif(provider);
      var allWantedSarifs =
          wantsSarif.stream()
              .flatMap(toolName -> ruleSarifByTool.getOrDefault(toolName, List.of()).stream())
              .collect(Collectors.toUnmodifiableList());
      Set<AbstractModule> modules =
          provider.getModules(repositoryDir, codemodTypes, allWantedSarifs);
      allModules.addAll(modules);
    }

    // record which changers are associated with which codemod ids
    List<IdentifiedChanger> changers = new ArrayList<>();

    // validate and instantiate the codemods
    Injector injector = Guice.createInjector(allModules);
    Set<String> codemodIds = new HashSet<>();
    List<Changer> codemods = new ArrayList<>();
    for (Class<? extends Changer> type : codemodTypes) {
      Codemod codemodAnnotation = type.getAnnotation(Codemod.class);
      validateRequiredFields(codemodAnnotation);
      Changer changer = injector.getInstance(type);
      String codemodId = codemodAnnotation.id();
      if (codemodIds.contains(codemodId)) {
        throw new UnsupportedOperationException("multiple codemods under id: " + codemodId);
      }
      codemodIds.add(codemodId);
      if (ruleContext.isRuleAllowed(codemodId)) {
        codemods.add(changer);
        changers.add(new IdentifiedChanger(codemodId, changer));
      }
    }

    this.changers = Collections.unmodifiableList(changers);
    this.codemods = Collections.unmodifiableList(codemods);
    this.repositoryDir = Objects.requireNonNull(repositoryDir);
  }

  private static final List<String> getWantsSarif(final CodemodProvider provider) {
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
    List<JavaParserChanger> javaParserChangers =
        codemods.stream()
            .filter(changer -> changer instanceof JavaParserChanger)
            .map(changer -> (JavaParserChanger) changer)
            .collect(Collectors.toUnmodifiableList());
    for (JavaParserChanger changer : javaParserChangers) {
      CodemodInvocationContext invocationContext =
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
    List<RawFileChanger> rawFileChangers =
        codemods.stream()
            .filter(changer -> changer instanceof RawFileChanger)
            .map(changer -> (RawFileChanger) changer)
            .collect(Collectors.toUnmodifiableList());

    Path originalFileCopy = Files.createTempFile("codemodder-raw", ".original");
    Files.copy(path, originalFileCopy, StandardCopyOption.REPLACE_EXISTING);

    for (RawFileChanger changer : rawFileChangers) {
      CodemodInvocationContext invocationContext =
          new DefaultCodemodInvocationContext(
              new DefaultCodeDirectory(repositoryDir),
              path,
              changers.stream().filter(ic -> ic.changer == changer).findFirst().orElseThrow().id,
              context);
      changer.visitFile(invocationContext);
    }

    List<Weave> weaves = context.weaves();
    if (weaves.isEmpty()) {
      Files.delete(originalFileCopy);
      return Optional.empty();
    }

    // copy the file that's now been modified by multiple codemods to a temp file
    Path modifiedFileCopy = Files.createTempFile("codemodder-raw", ".modified");
    Files.copy(path, modifiedFileCopy, StandardCopyOption.REPLACE_EXISTING);

    // restore the original file
    Files.copy(originalFileCopy, path, StandardCopyOption.REPLACE_EXISTING);

    return Optional.of(
        ChangedFile.createDefault(path.toString(), modifiedFileCopy.toString(), weaves));
  }

  /**
   * This is the entry point custom-built codemods are supposed to go through. Right now, this is
   * not useful directly as we're worried primarily about the legacy entrypoints.
   */
  @SuppressWarnings("unused")
  public static void run(String[] args, final Class<? extends Changer>... codemodTypes) {
    CodemodInvoker invoker = new CodemodInvoker(Arrays.asList(codemodTypes), Path.of("."));
  }

  private static void validateRequiredFields(final Codemod codemodAnnotation) {
    String author = codemodAnnotation.author();
    if (StringUtils.isBlank(author)) {
      throw new IllegalArgumentException("must have an author");
    }

    String id = codemodAnnotation.id();
    if (!isValidCodemodId(id)) {
      throw new IllegalArgumentException("must have valid codemod id");
    }

    ReviewGuidance reviewGuidance = codemodAnnotation.reviewGuidance();
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

  private static final Logger log = LoggerFactory.getLogger(CodemodInvoker.class);
}
