package io.codemodder;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
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

  private final Set<AbstractModule> allModules;
  private final Injector injector;
  private final List<Class<? extends Changer>> codemodTypes;
  private final Path repsitoryDir;

  public CodemodInvoker(final List<Class<? extends Changer>> codemodTypes, Path repositoryDir) {
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

    this.repsitoryDir = Objects.requireNonNull(repositoryDir);
    this.allModules = Collections.unmodifiableSet(allModules);
    this.injector = Guice.createInjector(allModules);
    this.codemodTypes = Collections.unmodifiableList(codemodTypes);

    // validate all of the codemods
    for (Class<? extends Changer> type : codemodTypes) {
      Codemod codemodAnnotation = type.getAnnotation(Codemod.class);
      validateRequiredFields(codemodAnnotation);
    }
  }

  // will invoke every changer for every file, collecting the diffs
  // will collate report
  // will spit out CodeTF or whatever

  /**
   * @param file
   * @param context
   * @return
   */
  public void execute(final Path file, final FileWeavingContext context) {

    // find a provider that can handle invoking the codemod "change phase"
    for (Class<? extends Changer> type : codemodTypes) {
      //      Changer changer = injector.getInstance(type);
      //      if(changer instanceof JavaParserChanger) {
      ////        CompilationUnit cu = parseJavaCode(javaFilePath);
      ////        ((JavaParserChanger) changer).createModifierVisitor(cu);
      ////        serializeBack();
      //      } else if (changer instanceof SpoonChanger) {
      //
      //      } else {
      //        throw new IllegalArgumentException("");
      //      }
      //      for(CodemodProvider provider : providers) {
      //        provider.get
      //      }
    }
    return;
  }

  /** Invoke the given codemods. */
  public static void run(final Class<? extends Changer>... codemodTypes) {
    // loop through every file
    // new CodemodInvoker(codemodTypes).execute();
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
