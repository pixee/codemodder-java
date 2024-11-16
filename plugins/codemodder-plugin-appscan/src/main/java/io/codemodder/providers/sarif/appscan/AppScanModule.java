package io.codemodder.providers.sarif.appscan;

import io.codemodder.CodeChanger;
import io.codemodder.Codemod;
import io.codemodder.CodemodCheckingAbstractModule;
import io.codemodder.RuleSarif;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Responsible for distributing the SARIFS to AppScan based codemods based on rules. */
public final class AppScanModule extends CodemodCheckingAbstractModule {

  private final List<Class<? extends CodeChanger>> codemodTypes;
  private final List<RuleSarif> allAppScanRuleSarifs;

  public AppScanModule(
      final List<Class<? extends CodeChanger>> codemodTypes, final List<RuleSarif> sarifs) {
    super(codemodTypes);
    this.codemodTypes = Objects.requireNonNull(codemodTypes);
    this.allAppScanRuleSarifs = sarifs;
  }

  @Override
  protected boolean isResponsibleFor(final Class<? extends CodeChanger> codemod) {
    return codemod.getAnnotation(Codemod.class).id().startsWith("appscan:");
  }

  @Override
  protected void doConfigure() {
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
          providedAppScanScan -> {
            if (!providedAppScanScan.ruleName().isEmpty()) {
              bind(RuleSarif.class)
                  .annotatedWith(providedAppScanScan)
                  .toInstance(map.getOrDefault(providedAppScanScan.ruleName(), RuleSarif.EMPTY));
            } else if (providedAppScanScan.ruleNames().length > 0) {

              RuleSarif ruleSarif = RuleSarif.EMPTY;
              for (final String ruleName : providedAppScanScan.ruleNames()) {
                final var result = map.get(ruleName);
                if (result != null) {
                  ruleSarif = result;
                  break;
                }
              }

              bind(RuleSarif.class).annotatedWith(providedAppScanScan).toInstance(ruleSarif);
            } else {
              throw new IllegalStateException("No rule name provided in " + providedAppScanScan);
            }
          });
    }
  }
}
