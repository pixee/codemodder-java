package io.codemodder.providers.sarif.appscan;

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

/** Responsible for distributing the SARIFS to AppScan based codemods based on rules. */
public final class AppScanModule extends AbstractModule {

  private final List<Class<? extends CodeChanger>> codemodTypes;
  private final List<RuleSarif> allAppScanRuleSarifs;

  public AppScanModule(
      final List<Class<? extends CodeChanger>> codemodTypes, final List<RuleSarif> sarifs) {
    this.codemodTypes = Objects.requireNonNull(codemodTypes);
    this.allAppScanRuleSarifs = sarifs;
  }

  @Override
  protected void configure() {
    final Map<String, RuleSarif> map =
        allAppScanRuleSarifs.stream()
            .collect(Collectors.toUnmodifiableMap(RuleSarif::getRule, rs -> rs));

    for (final Class<? extends CodeChanger> codemodType : codemodTypes) {
      final Constructor<?>[] constructors = codemodType.getDeclaredConstructors();

      final Optional<ProvidedAppScanScan> annotation =
          Stream.of(constructors)
              .filter(constructor -> constructor.getAnnotation(javax.inject.Inject.class) != null)
              .flatMap(constructor -> Stream.of(constructor.getParameters()))
              .map(parameter -> parameter.getAnnotation(ProvidedAppScanScan.class))
              .filter(Objects::nonNull)
              .findFirst();

      annotation.ifPresent(
          providedAppScanScan ->
              bind(RuleSarif.class)
                  .annotatedWith(providedAppScanScan)
                  .toInstance(map.getOrDefault(providedAppScanScan.ruleId(), RuleSarif.EMPTY)));
    }
  }
}
