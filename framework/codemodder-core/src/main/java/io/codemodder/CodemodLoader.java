package io.codemodder;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toUnmodifiableList;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ScanResult;
import java.lang.reflect.Executable;
import java.lang.reflect.Parameter;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import javax.inject.Inject;
import org.codehaus.plexus.util.StringUtils;
import org.jetbrains.annotations.VisibleForTesting;

/** This type is responsible for loading codemods and the surrounding subsystem. */
public final class CodemodLoader {

  private final List<CodemodIdPair> codemods;

  public CodemodLoader(
      final List<Class<? extends CodeChanger>> codemodTypes, final Path repositoryDir) {
    this(
        codemodTypes,
        CodemodRegulator.of(DefaultRuleSetting.ENABLED, List.of()),
        repositoryDir,
        Map.of(),
        List.of());
  }

  public CodemodLoader(
      final List<Class<? extends CodeChanger>> codemodTypes,
      final Path repositoryDir,
      final List<ParameterArgument> parameterArguments) {
    this(
        codemodTypes,
        CodemodRegulator.of(DefaultRuleSetting.ENABLED, List.of()),
        repositoryDir,
        Map.of(),
        parameterArguments);
  }

  public CodemodLoader(
      final List<Class<? extends CodeChanger>> codemodTypes,
      final Path repositoryDir,
      final Map<String, List<RuleSarif>> ruleSarifByTool) {
    this(
        codemodTypes,
        CodemodRegulator.of(DefaultRuleSetting.ENABLED, List.of()),
        repositoryDir,
        ruleSarifByTool,
        List.of());
  }

  public CodemodLoader(
      final List<Class<? extends CodeChanger>> codemodTypes,
      final CodemodRegulator codemodRegulator,
      final Path repositoryDir,
      final Map<String, List<RuleSarif>> ruleSarifByTool,
      final List<ParameterArgument> codemodParameters) {

    // get all the providers ready for dependency injection & codemod instantiation
    final List<CodemodProvider> providers =
        ServiceLoader.load(CodemodProvider.class).stream()
            .map(ServiceLoader.Provider::get)
            .collect(toUnmodifiableList());
    final Set<AbstractModule> allModules = new HashSet<>();

    // get all the injectable parameters
    Set<String> packagesScanned = new HashSet<>();
    List<Parameter> injectableParameters = new ArrayList<>();
    for (Class<? extends CodeChanger> codemodType : codemodTypes) {
      String packageName = codemodType.getPackageName();
      if (!packagesScanned.contains(packageName)) {
        ScanResult scan =
            new ClassGraph().enableAllInfo().acceptPackagesNonRecursive(packageName).scan();
        ClassInfoList classesWithMethodAnnotation =
            scan.getClassesWithMethodAnnotation(Inject.class);
        List<Class<?>> injectableClasses = classesWithMethodAnnotation.loadClasses();
        List<java.lang.reflect.Parameter> targetedParams =
            injectableClasses.stream()
                .map(Class::getDeclaredConstructors)
                .flatMap(Arrays::stream)
                .filter(constructor -> constructor.isAnnotationPresent(Inject.class))
                .map(Executable::getParameters)
                .flatMap(Arrays::stream)
                .filter(param -> param.getAnnotations().length > 0)
                .collect(toList());
        injectableParameters.addAll(targetedParams);
        packagesScanned.add(packageName);
      }
    }

    // add default modules
    allModules.add(new CodeDirectoryModule(repositoryDir));
    allModules.add(new XPathStreamProcessorModule());
    allModules.add(new ParameterModule(codemodParameters, injectableParameters));

    // add all provider modules
    for (final CodemodProvider provider : providers) {
      final List<String> wantsSarif = getWantsSarif(provider);
      final var allWantedSarifs =
          wantsSarif.stream()
              .flatMap(toolName -> ruleSarifByTool.getOrDefault(toolName, List.of()).stream())
              .collect(toUnmodifiableList());
      final Set<AbstractModule> modules =
          provider.getModules(repositoryDir, codemodTypes, allWantedSarifs);
      allModules.addAll(modules);
    }

    // record which changers are associated with which codemod ids
    final List<CodemodIdPair> codemods = new ArrayList<>();

    // validate and instantiate the codemods
    final Injector injector = Guice.createInjector(allModules);
    final Set<String> codemodIds = new HashSet<>();
    for (final Class<? extends CodeChanger> type : codemodTypes) {
      final Codemod codemodAnnotation = type.getAnnotation(Codemod.class);
      validateRequiredFields(codemodAnnotation);
      final CodeChanger codeChanger = injector.getInstance(type);
      final String codemodId = codemodAnnotation.id();
      if (codemodIds.contains(codemodId)) {
        throw new UnsupportedOperationException("multiple codemods under id: " + codemodId);
      }
      codemodIds.add(codemodId);
      if (codemodRegulator.isAllowed(codemodId)) {
        codemods.add(new CodemodIdPair(codemodId, codeChanger));
      }
    }

    this.codemods = Collections.unmodifiableList(codemods);
  }

  public List<CodemodIdPair> getCodemods() {
    return codemods;
  }

  private static List<String> getWantsSarif(final CodemodProvider provider) {
    return Optional.ofNullable(provider.getClass().getAnnotation(WantsSarif.class))
        .map(wants -> Arrays.asList(wants.toolNames()))
        .orElse(List.of());
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
      Pattern.compile("^([A-Za-z0-9]+):(([A-Za-z0-9]+)\\/)+([A-Za-z0-9\\-]+)$");
}
