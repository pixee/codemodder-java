package io.codemodder.providers.sarif.codeql;

import com.google.inject.AbstractModule;
import io.codemodder.Changer;
import io.codemodder.EmptyRuleSarif;
import io.codemodder.RuleSarif;
import io.codemodder.RuleSarifProvider;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Responsible for distributing the SARIFS to CodeQL based codemods based on rules. */
public class CodeQLModule extends AbstractModule {

  private final List<Class<? extends Changer>> codemodTypes;

  CodeQLModule(final List<Class<? extends Changer>> codemodTypes) {
    this.codemodTypes = Objects.requireNonNull(codemodTypes);
  }

  @Override
  protected void configure() {
    ServiceLoader<RuleSarifProvider> sarifProviders = ServiceLoader.load(RuleSarifProvider.class);
    var allCodeqlRuleSarifs =
        sarifProviders.stream()
            .map(ServiceLoader.Provider::get)
            .flatMap(sp -> sp.getRuleSarifsByTool(CodeQLRuleSarif.TOOL_NAME).stream())
            .collect(Collectors.toList());

    // TODO what if there are multiple sarif files with a given rule?
    Map<String, RuleSarif> map =
        allCodeqlRuleSarifs.stream()
            .collect(Collectors.toUnmodifiableMap(rs -> rs.getRule(), rs -> rs));

    for (Class<? extends Changer> codemodType : codemodTypes) {
      Constructor<?>[] constructors = codemodType.getDeclaredConstructors();

      Optional<CodeQLScan> annotation =
          Stream.of(constructors)
              .filter(constructor -> constructor.getAnnotation(javax.inject.Inject.class) != null)
              .flatMap(constructor -> Stream.of(constructor.getParameters()))
              .flatMap(
                  parameter ->
                      parameter.getAnnotation(CodeQLScan.class) == null
                          ? Stream.of()
                          : Stream.of(parameter.getAnnotation(CodeQLScan.class)))
              .findFirst();

      if (annotation.isPresent()) {
        if (map.containsKey(annotation.get().ruleId())) {
          bind(RuleSarif.class)
              .annotatedWith(annotation.get())
              .toInstance(map.get(annotation.get().ruleId()));
        } else {
          bind(RuleSarif.class).annotatedWith(annotation.get()).toInstance(new EmptyRuleSarif());
        }
      }
    }
  }
}
