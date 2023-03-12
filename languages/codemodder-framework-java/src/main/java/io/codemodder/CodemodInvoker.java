package io.codemodder;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
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

  public CodemodInvoker(
      final List<Class<? extends Changer>> codemodTypes, final Path repositoryDir) {
    this(codemodTypes, RuleContext.of(DefaultRuleSetting.ENABLED, List.of()), repositoryDir);
  }

  public CodemodInvoker(
      final List<Class<? extends Changer>> codemodTypes,
      final RuleContext ruleContext,
      final Path repositoryDir) {
    // get all the providers ready for dependency injection & codemod instantiation
    List<CodemodProvider> providers =
        ServiceLoader.load(CodemodProvider.class).stream()
            .map(ServiceLoader.Provider::get)
            .collect(Collectors.toUnmodifiableList());
    Set<AbstractModule> allModules = new HashSet<>();

    // add default module
    allModules.add(new CodeDirectoryModule(repositoryDir));

    for (CodemodProvider provider : providers) {
      Set<AbstractModule> modules = provider.getModules();
      allModules.addAll(modules);
    }

    // validate and instantiate the codemods
    Injector injector = Guice.createInjector(allModules);
    List<Changer> codemods = new ArrayList<>();
    for (Class<? extends Changer> type : codemodTypes) {
      Codemod codemodAnnotation = type.getAnnotation(Codemod.class);
      validateRequiredFields(codemodAnnotation);
      Changer changer = injector.getInstance(type);
      if (ruleContext.isRuleAllowed(changer.getCodemodId())) {
        codemods.add(changer);
      }
    }
    this.codemods = Collections.unmodifiableList(codemods);
    this.repositoryDir = Objects.requireNonNull(repositoryDir);
  }

  /**
   * Run the codemods we've collected on the given file.
   *
   * @param cu the parsed JavaParser representation of the file
   * @param context a model we should keep updating as we process the file
   */
  public void execute(final Path path, final CompilationUnit cu, final FileWeavingContext context) {
    for (Changer changer : codemods) {
      if (changer instanceof JavaParserChanger) {
        JavaParserChanger javaParserChanger = (JavaParserChanger) changer;
        Optional<ModifierVisitor<FileWeavingContext>> modifierVisitor =
            javaParserChanger.createModifierVisitor(
                new DefaultCodeDirectory(repositoryDir), path, cu);
        modifierVisitor.ifPresent(
            changeContextModifierVisitor -> cu.accept(changeContextModifierVisitor, context));
      } else {
        throw new UnsupportedOperationException("unknown or not");
      }
    }
  }

  /**
   * This is the entry point custom-built codemods are supposed to go through. Right now, this is
   * not useful directly as we're worried about
   */
  public static void run(final Class<? extends Changer>... codemodTypes) {
    new CodemodInvoker(Arrays.asList(codemodTypes), Path.of("."));
    // TODO: loop through the files and invoke the codemods on each file
    // codemodInvoker.execute();
  }

  private static void validateRequiredFields(final Codemod codemodAnnotation) {
    String author = codemodAnnotation.author();
    if (StringUtils.isBlank(author)) {
      throw new IllegalArgumentException("must have an author");
    }

    String id = codemodAnnotation.value();
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
