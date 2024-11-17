package io.codemodder.providers.sarif.codeql;

import com.google.inject.AbstractModule;
import io.codemodder.CodeChanger;
import io.codemodder.RuleSarif;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Responsible for distributing the SARIFS to CodeQL based codemods based on rules. */
public final class CodeQLModule extends AbstractModule {

  private final List<Class<? extends CodeChanger>> codemodTypes;
  private final List<RuleSarif> allCodeqlRuleSarifs;

  CodeQLModule(
      final List<Class<? extends CodeChanger>> codemodTypes, final List<RuleSarif> sarifs) {
    this.codemodTypes = Objects.requireNonNull(codemodTypes);
    this.allCodeqlRuleSarifs = sarifs;
  }

  @Override
  protected void configure() {
    // What if there are multiple sarif files with a given rule?
    // We can safely ignore this case for now.
    final Map<String, RuleSarif> map =
        allCodeqlRuleSarifs.stream()
            .collect(Collectors.toUnmodifiableMap(RuleSarif::getRule, rs -> rs));

    for (final Class<? extends CodeChanger> codemodType : codemodTypes) {
      final Constructor<?>[] constructors = codemodType.getDeclaredConstructors();

      final Optional<ProvidedCodeQLScan> annotation =
          Stream.of(constructors)
              .filter(constructor -> constructor.getAnnotation(javax.inject.Inject.class) != null)
              .flatMap(constructor -> Stream.of(constructor.getParameters()))
              .map(parameter -> parameter.getAnnotation(ProvidedCodeQLScan.class))
              .filter(Objects::nonNull)
              .findFirst();

      annotation.ifPresent(
          providedCodeQLScan ->
              bind(RuleSarif.class)
                  .annotatedWith(providedCodeQLScan)
                  .toInstance(map.getOrDefault(providedCodeQLScan.ruleId(), RuleSarif.EMPTY)));
    }
  }
}
